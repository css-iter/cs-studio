/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.csstudio.alarm.diirt.datasource;

import static org.diirt.util.concurrent.Executors.namedPool;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.alarm.beast.client.AlarmTreeItem;
import org.csstudio.alarm.beast.client.AlarmTreePV;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModelConfigListener;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModelListener;
import org.diirt.datasource.ChannelHandler;
import org.diirt.datasource.DataSource;
import org.diirt.datasource.vtype.DataTypeSupport;
import org.eclipse.core.runtime.Platform;

/**
 * @author Kunal Shroff
 *
 */
public class BeastDataSource extends DataSource implements AlarmClientModelConfigListener {
    private static final Logger log = Logger.getLogger(BeastDataSource.class.getName());

    private static final String PREF_QUALIFIER = "org.csstudio.alarm.diirt.datasource";
    private static final String PREF_KEY_COMPOSITE = "composite.model.name";
    private static final String PREF_DEFAULT_COMPOSITE_MODEL_NAME = "Composite";

    // The model, activeAlarms and acknowledgedAlarms is shared by the entire
    // datasource, the benefit of does this at the datasource level instead of
    // in each channel is that they need to be computed only once and only a single
    // copy needs to be maintained.
    private AlarmClientModel model;

    private Map<String, List<Consumer>> channelConsumers = Collections.synchronizedMap(new HashMap<String, List<Consumer>>());
    private Map<String, AlarmClientModel> models = Collections.synchronizedMap(new HashMap<String, AlarmClientModel>());
    private AtomicInteger loaded;
    private AtomicInteger toLoad;

    private Executor executor = Executors.newScheduledThreadPool(4);

    private final ExecutorService exec = Executors.newSingleThreadExecutor(namedPool("RemoveChannel " + getClass().getSimpleName() + " Worker "));

    private BeastTypeSupport typeSupport;

    private BeastDataSourceConfiguration configuration;

    private BeastAlarmClientModelListener modelListener;

    private CompositeAlarmClientModel compositeModel;

    static {
        // Install type support for the types it generates.
        DataTypeSupport.install();
    }

    private class BeastAlarmClientModelListener implements AlarmClientModelListener{
        private BeastDataSource parent;

        BeastAlarmClientModelListener(BeastDataSource parent) {
            this.parent = parent;
        }

        @Override
        public void newAlarmConfiguration(AlarmClientModel model) {
            log.config("beast  datasource: new alarm configuration --- " + model);
            // new model loaded
            parent.loaded.incrementAndGet();
            if (!parent.compositeModel.equals(model)) parent.compositeModel.addAlarmClientModel(model);
            if (parent.loaded.get() == parent.toLoad.get()) {
                parent.compositeModel.setAllLoaded(true);
                log.log(Level.FINE, "All models loaded - notifying Composite model listeners.");
                parent.compositeModel.compositeModelsLoaded();
            }
            synchronized (parent.channelConsumers) {
                for (String channelName : parent.channelConsumers.keySet()) {
                    BeastChannelHandler channel = (BeastChannelHandler) getChannels()
                            .get(channelHandlerLookupName(channelName));
                    channel.reconnect();
                }
            }
            notifyCompositeBeastChannelListeners();
        }

        @Override
        public void serverTimeout(AlarmClientModel model) {
            log.warning("beast datasource: server timeout (server alive: " + model.isServerAlive() + ")");
            synchronized (parent.channelConsumers) {
                for (String channelName : parent.channelConsumers.keySet()) {
                    BeastChannelHandler channel = (BeastChannelHandler) getChannels()
                            .get(channelHandlerLookupName(channelName));
                    // notify the ChannelHandler that we lost connection
                    // (causes ConnectionChanged event + Listeners' PVReader.isConnected() will return the correct state)
                    channel.connectionStateChanged(false);
                }
            }
        }

        @Override
        public void serverModeUpdate(AlarmClientModel model, boolean maintenance_mode) {
            log.fine("beast  datasource: server mode update");
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void newAlarmState(AlarmClientModel alarmModel, AlarmTreePV pv, boolean parent_changed) {
            if (pv != null) {
                log.fine(pv.getPathName());
                synchronized (parent.channelConsumers) {
                    parent.channelConsumers.forEach((key, pathHandlers) -> {
                        if (BeastTypeSupport.getStrippedChannelName(key).equals(pv.getPathName().substring(1))
                                || BeastTypeSupport.getStrippedChannelName(key).equals(pv.getName())) {
                            if (pathHandlers != null) {
                                for (Consumer consumer : pathHandlers) {
                                    consumer.accept(pv);
                                }
                            }
                        }
                    });
                }
                // Notify parent nodes (regardless of parent_changed - because the parents' AlarmPVsCount)
                AlarmTreeItem parent = pv.getParent();
                while (parent != null) {
                    final String parentPath = parent.getPathName();
                    synchronized (this.parent.channelConsumers) {
                        this.parent.channelConsumers.forEach((key, pathHandlers) -> {
                            if (BeastTypeSupport.getStrippedChannelName(key).equals(parentPath.substring(1))) {
                                if (pathHandlers != null) {
                                    for (Consumer consumer : pathHandlers) {
                                        try {
                                            consumer.accept(getState(parentPath));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        });
                    }
                    parent = parent.getParent();
                }
            } else {
                // The AlarmClientModel has recovered from a disconnection or is notifying us that the first
                // messages have been received after initial connection.
                synchronized (parent.channelConsumers) {
                    for (String channelName : parent.channelConsumers.keySet()) {
                        BeastChannelHandler channel = (BeastChannelHandler) getChannels()
                                .get(channelHandlerLookupName(channelName));
                        if (channel!=null)
                            channel.reconnect(); // will send connection state + current AlarmTreeItem state
                    }
                }
            }
            notifyCompositeBeastChannelListeners();
        }

        private void notifyCompositeBeastChannelListeners() {
            synchronized (parent.channelConsumers) {
                final BeastChannelHandler compositeChannel = (BeastChannelHandler) getChannels()
                        .get(channelHandlerLookupName(parent.compositeModel.getConfigurationName()));
                if (compositeChannel != null) {
                    // there is an actual connection to the composite root
                    log.fine("Sending message to the composite tree channel.");
                    parent.compositeModel.getConfigTree().maximizeSeverity();
                    compositeChannel.accept(parent.compositeModel.getConfigTree());
                }
            }
        }
    }

    public BeastDataSource(BeastDataSourceConfiguration configuration) {
        super(true);
        this.configuration = configuration;
        loaded = new AtomicInteger(0);
        toLoad = new AtomicInteger(-1);
        final String compositeModelName = Platform.getPreferencesService().
                getString(PREF_QUALIFIER, PREF_KEY_COMPOSITE, PREF_DEFAULT_COMPOSITE_MODEL_NAME, null);
        log.log(Level.CONFIG, () -> String.format("Using '%s' as the composite alarm model name.", compositeModelName));
        compositeModel = new CompositeAlarmClientModel(compositeModelName);
        compositeModel.registerWithClientModels();
        // one global listener for all models
        modelListener = new BeastAlarmClientModelListener(this);
        try {

            // Create an instance to the AlarmClientModel
            final CompletableFuture<Void> future = CompletableFuture
                    .supplyAsync(() -> initialize(configuration), executor)
                    .thenAccept((model) -> {
                        this.model = model;
                        this.models.put(model.getConfigurationName(), model);
                        this.model.addListener(modelListener);
                    });
            typeSupport = new BeastTypeSupport();
            compositeModel.addListener(modelListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AlarmClientModel initialize(BeastDataSourceConfiguration configuration) {
        AlarmClientModel alarmModel;
        try {
            if (configuration.getConfigName() != null && !configuration.getConfigName().isEmpty()) {
                alarmModel = AlarmClientModel.getInstance(configuration.getConfigName(), this);
            } else {
                alarmModel = AlarmClientModel.getInstance(this);
            }
            return alarmModel;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected ChannelHandler createChannel(String channelName) {
        return new BeastChannelHandler(channelName, BeastTypeSupport.getChannelType(channelName), this);
    }

    @Override
    public void close() {
        super.close();
        if (!models.isEmpty()) {
            for (final AlarmClientModel aModel : models.values()) aModel.release();
        } else {
            model.release();
        }
    }

    /**
     * Override of default channelHandlerLookupName.
     * This implementation makes a leading and trailing forward slash ("/") optional.
     * All four of these will resolve to the same channel:
     * "/demo/test/", "/demo/test", "demo/test/" & "demo/test".
     *
     * @see org.diirt.datasource.DataSource#channelHandlerLookupName(java.lang.String)
     */
    @Override
    protected String channelHandlerLookupName(String channelName) {
        String channel = channelName;
        if (channel != null && !channel.equals("/") && !channel.isEmpty()) {
            if (channel.endsWith("/"))
                channel = channel.substring(0, channel.length() - 1);
            if (channel.startsWith("/"))
                channel = channel.substring(1);
        }

        return channel;
    }

    @SuppressWarnings("rawtypes")
    protected void add(String channelName, Consumer beastChannelHandler) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                String beastChannel = channelHandlerLookupName(channelName);
                synchronized (channelConsumers) {
                    List<Consumer> list = channelConsumers.get(beastChannel);
                    if (list == null) {
                        list = new ArrayList<>();
                        channelConsumers.put(beastChannel, list);
                    }
                    list.add(beastChannelHandler);
                }
            }
        });
    }

    @SuppressWarnings("rawtypes")
    protected void remove(String channelName, Consumer beastChannelHandler) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                String beastChannel = channelHandlerLookupName(channelName);
                synchronized (channelConsumers) {
                    if (channelConsumers.containsKey(beastChannel)) {
                        channelConsumers.get(beastChannel).remove(beastChannelHandler);
                    }
                }
            }
        });
    }

    protected AlarmTreeItem getState(String channelName) throws Exception {
        URI uri = URI.create(URLEncoder.encode(BeastTypeSupport.getStrippedChannelName(channelName), "UTF-8"));
        String pvName = uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
        final AlarmClientModel aModel = selectModel(uri);
        if (aModel != null) {
            AlarmTreePV alarmTreePV = aModel.findPV(pvName);
            if (alarmTreePV != null) {
                return alarmTreePV;
            } else {
                String path = URLDecoder.decode(uri.getPath(), "UTF-8");
                AlarmTreeItem alarmTreeItem = aModel.getConfigTree().getItemByPath(path);
                return alarmTreeItem;
            }
        } else {
            throw new Exception("Model hasn't been created yet: " + uri.getPath());
        }
    }

    private AlarmClientModel selectModel(URI uri) throws Exception {
        if (model == null) {
            log.warning("NO Default model. Abort!");
            return null;
        }
        final String decodedUri = URLDecoder.decode(uri.getPath(), "UTF-8");
        log.log(Level.FINE, () -> "decodedUri: " + decodedUri);
        if (uri.getPath().contains("/")) {
            // Alarm root defined
            final String lookupName = channelHandlerLookupName(decodedUri);
            //log.log(Level.INFO, "With root: " + lookupName);
            final String root;
            if (lookupName.contains("/"))
                root = lookupName.substring(0, lookupName.indexOf('/'));
            else
                root = lookupName;
            log.log(Level.FINE, () -> "root: " + root);
            if (model.getConfigurationName().equals(root)) {
                log.fine("Returning default model.");
                return model;
            }
            if (models.containsKey(root)) {
                log.fine("Returning named model.");
                return models.get(root);
            }
            if (root.equals(compositeModel.getConfigurationName())) {
                log.fine("Returning composite model.");
                return compositeModel;
            }
            log.log(Level.WARNING, () -> String.format("Root not found: %s", root));
            return null;
        } else {
            // PV only - search models in the DIIRT defined order, but search default first.
            final String pvName = decodedUri.substring(decodedUri.lastIndexOf("/") + 1);
            //log.log(Level.INFO, "no root. pvName: " + pvName);
            if (model.findPV(pvName) != null) return model;
            for (final String configName : configuration.getConfigNames()) {
                final AlarmClientModel aModel = models.get(configName);
                if ((aModel != null) && (aModel.findPV(pvName) != null)) return aModel;
            }
            // PV not found anywhere!!! Return default.
            return model;
        }
    }

    protected boolean isConnected() {
        if (model != null) {
            return model.isServerAlive();
        } else {
            return false;
        }
    }

    protected boolean isWriteAllowed() {
        // this is governed by the org.csstudio.alarm.beast.Preferences so it is the same for all models
        if (model != null) {
            return model.isServerAlive() && model.isWriteAllowed();
        } else {
            return false;
        }
    }

    protected void acknowledge(String channelName, boolean acknowledge) throws Exception {
        getState(channelName).acknowledge(acknowledge);
    }

    // implementing the enable disable mechanism using the example of the DisableComponentAction
    protected void enable(String channelName, boolean enable) throws Exception {
        AlarmTreeItem item = getState(channelName);
        final List<AlarmTreePV> pvs = new ArrayList<>();
        final CompletableFuture<Void> future = CompletableFuture
                .runAsync(() -> addPVs(pvs, item, enable), executor)
                .thenRun(() -> {
                    for (AlarmTreePV alarmTreePV : pvs) {
                        try {
                            model.enable(alarmTreePV, enable);
                        } catch (Exception e) {
                            new Exception("Failed to enable/disable : " + ((AlarmTreePV) item).getName(), e);
                        }
                    }
                });
    }

    /** @param pvs List where PVs to enable/disable will be added
     *  @param item Item for which to locate PVs, recursively
     */
    private void addPVs(final List<AlarmTreePV> pvs, final AlarmTreeItem item, boolean enable) {
        if (item instanceof AlarmTreePV) {
            final AlarmTreePV pv = (AlarmTreePV) item;
            if (pv.isEnabled() != enable)
                pvs.add(pv);
        } else {
            final int N = item.getChildCount();
            for (int i=0; i<N; ++i)
                addPVs(pvs, item.getChild(i), enable);
        }
    }

    public BeastTypeSupport getTypeSupport() {
        return typeSupport;
    }

    @Override
    public void newAlarmConfiguration(AlarmClientModel model) {
        // initial model loaded
        // store the default model in a list of all models
        log.log(Level.CONFIG, () -> "Default alarm client model loaded: " + model.getConfigurationName());

        // now obtain all roots from it, and then see if you have anything else in the DIIRT configuration
        final String[] confNames = model.getConfigurationNames();
        toLoad.set(confNames.length);
        if (confNames.length == 1) compositeModel.setAllLoaded(true);   // there is only one model, and that one was just loaded.
        final List<String> diirtConfNames = new ArrayList<>(configuration.getConfigNames()); // original is unmodifiable
        for (final String confName : confNames) {
            try {
                if (!models.containsKey(confName)) {
                    log.log(Level.FINER, () -> "Loading model: " + confName);
                    final AlarmClientModel aModel = AlarmClientModel.getInstance(confName, modelListener);
                    models.put(confName, aModel);
                }
                diirtConfNames.remove(confName);
            } catch (final Exception e) {
                // Log warning if not debugging
                if (!log.isLoggable(Level.FINER)) log.log(Level.WARNING, "Error loading alarm Client Model: " + confName);
                log.log(Level.FINER, "Error loading alarm Client Model.", e);
            }
        }
        if (!diirtConfNames.isEmpty()) {
            log.log(Level.WARNING,
                    "DIIRT beast configuration defines alarm models not configured in the database: "
                    + diirtConfNames.toString());
        }
    }
}
