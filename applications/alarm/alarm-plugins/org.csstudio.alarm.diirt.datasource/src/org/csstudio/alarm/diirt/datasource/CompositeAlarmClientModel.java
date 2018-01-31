package org.csstudio.alarm.diirt.datasource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.alarm.beast.Activator;
import org.csstudio.alarm.beast.Messages;
import org.csstudio.alarm.beast.client.AADataStructure;
import org.csstudio.alarm.beast.client.AlarmTreeItem;
import org.csstudio.alarm.beast.client.AlarmTreePV;
import org.csstudio.alarm.beast.client.AlarmTreeRoot;
import org.csstudio.alarm.beast.client.GDCDataStructure;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModelConfigListener;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModelListener;

public class CompositeAlarmClientModel extends AlarmClientModel {
    private static final Logger LOG = Logger.getLogger(CompositeAlarmClientModel.class.getCanonicalName());

    private CompositeAlarmTreeRoot compositeRoot;
    private final List<AlarmClientModel> models;
    private volatile boolean allLoaded;
    private final Set<String> disconnectedModels;
    // register this listener to all the children so that we can use the notifications for the root
    private final AlarmClientModelListener childrenListener;
    /** Listeners who registered for notifications */
    final private List<AlarmClientModelListener> listeners =  new CopyOnWriteArrayList<>();

    private AtomicBoolean configLoopPrevention;
    private AtomicBoolean timeoutLoopPrevention;
    private AtomicBoolean modeLoopPrevention;
    private AtomicBoolean alarmLoopPrevention;

    private static class ChildrenModelListener implements AlarmClientModelListener {
        private final CompositeAlarmClientModel parent;

        private ChildrenModelListener(CompositeAlarmClientModel parent) {
            this.parent = parent;
        }

        @Override
        public void newAlarmConfiguration(AlarmClientModel model) {
            LOG.log(Level.FINE, () -> "New config for model " + model.getConfigurationName());
            parent.compositeNewConfig();
        }

        @Override
        public void serverTimeout(AlarmClientModel model) {
            LOG.log(Level.FINE, () -> "Server timeout for model " + model.getConfigurationName());
            parent.compositeServerTimeout(model.getConfigurationName());
        }

        @Override
        public void serverModeUpdate(AlarmClientModel model, boolean maintenance_mode) {
            LOG.log(Level.FINE, () -> "Mode change for model " + model.getConfigurationName());
            parent.compositeModeUpdate(model.getConfigurationName());
        }

        @Override
        public void newAlarmState(AlarmClientModel model, AlarmTreePV pv, boolean parent_changed) {
            LOG.log(Level.FINE, () -> "New alarm state update for model " + model.getConfigurationName());
            parent.compositeNewAlarmState(pv, parent_changed);
        }
    }

    /**
     * @param configName the fake composite configuration name.
     */
    public CompositeAlarmClientModel(final String configName) {
        super(configName);
        compositeRoot = new CompositeAlarmTreeRoot(configName);
        models = new CopyOnWriteArrayList<>();
        allLoaded = false;
        childrenListener = new ChildrenModelListener(this);
        disconnectedModels = Collections.synchronizedSet(new HashSet<>());
        configLoopPrevention = new AtomicBoolean();
        timeoutLoopPrevention = new AtomicBoolean();
        modeLoopPrevention = new AtomicBoolean();
        alarmLoopPrevention = new AtomicBoolean();
    }

    @Override
    public synchronized String[] getConfigurationNames() {
        return getConfigurationNames();
    }

    /**
     * Add a new {@link AlarmClientModel} to the composite.
     * @param child
     */
    public synchronized void addAlarmClientModel(AlarmClientModel child) {
        models.add(child);
        child.addListener(childrenListener);
        compositeRoot.addAlarmTreeRoot(child.getConfigTree());
    }

    @Override
    protected void finalize() throws Throwable {
        for (final AlarmClientModel child : models) {
            child.removeListener(childrenListener);
        }
        super.finalize();
    }

    @Override
    public boolean setConfigurationName(final String new_root_name, final AlarmClientModelConfigListener listener) {
        throw new UnsupportedOperationException("Configuration name of this model cannot be changed.");
    }

    @Override
    public boolean isWriteAllowed() {
        return false;
    }

    @Override
    public String getJMSServerInfo() {
        if (!models.isEmpty())
            return models.get(0).getJMSServerInfo();
        else
            return "No Communicator";
    }

    @Override
    public void updateServerState(boolean maintenance_mode) {
        // no topic for Composite model, so ignore these messages
        LOG.log(Level.WARNING, "Server state update requested on Composite Alarm Client Model.");
    }

    @Override
    public boolean isServerAlive() {
        if (!models.isEmpty())
            return models.get(0).isServerAlive();
        else
            return false;
    }

    @Override
    public boolean inMaintenanceMode() {
        return false;
    }

    /** The method registers the {@link CompositeAlarmClientModel} model with the {@link AlarmClientModel}s. */
    public void registerWithClientModels() {
        addComposite(this);
    }

    /** Notifies all listeners, that the {@link CompositeAlarmClientModel} has finished adding all related {@link AlarmClientModel}s. */
    public void compositeModelsLoaded() {
        fireNewConfig();
    }

    @Override
    public void requestMaintenanceMode(boolean maintenance) {
        throw new UnsupportedOperationException("Maintenance mode not supported on Composite Alarm Client Model.");
    }

    @Override
    synchronized public AlarmTreeRoot getConfigTree() {
        if (!allLoaded) return createPseudoAlarmTree(Messages.AlarmClientModel_NotInitialized);
        return compositeRoot;
    }

    private AlarmTreeRoot createPseudoAlarmTree(String info) {
        AlarmTreeRoot  configTree = new AlarmTreeRoot("Pseudo", -1);
        new AlarmTreeItem(configTree, info, 0);
        return configTree;
    }

    @Override
    synchronized public AlarmTreePV[] getActiveAlarms() {
        final List<AlarmTreePV> retval = new ArrayList<>();
        synchronized (models) {
            models.stream().forEach(model -> retval.addAll(Arrays.asList(model.getActiveAlarms())));
        }
        return retval.toArray(new AlarmTreePV[retval.size()]);
    }

    @Override
    synchronized public AlarmTreePV[] getAcknowledgedAlarms() {
        final List<AlarmTreePV> retval = new ArrayList<>();
        synchronized (models) {
            LOG.log(Level.FINE, () -> String.format("Adding alarms on all models: %d", models.size()));
            models.stream().forEach(model -> retval.addAll(Arrays.asList(model.getAcknowledgedAlarms())));
        }
        return retval.toArray(new AlarmTreePV[retval.size()]);
    }

    @Override
    public void addComponent(final AlarmTreeItem root_or_component, final String name) throws Exception {
        // Ignored silently since composite tree is not writable
        LOG.log(Level.FINE, "Component add not supported on Composite Alarm Client Model.");
    }

    @Override
    public void addPV(final AlarmTreeItem component, final String name) throws Exception {
        // Ignored silently since composite tree is not writable
        LOG.log(Level.FINE, "PV add not supported on Composite Alarm Client Model.");
    }

    @Override
    public void configureItem(final AlarmTreeItem item,
            final GDCDataStructure guidance[], final GDCDataStructure displays[],
            final GDCDataStructure commands[], final AADataStructure auto_actions[]) throws Exception {
        // Ignored silently since composite tree is not writable
        LOG.log(Level.FINE, "Item configuration not supported on Composite Alarm Client Model.");
    }

    @Override
    public void configurePV(final AlarmTreePV pv, final String description,
            final boolean enabled, final boolean annunciate, final boolean latch,
            final int delay, final int count, final String filter,
            final GDCDataStructure guidance[], final GDCDataStructure displays[],
            final GDCDataStructure commands[], final AADataStructure auto_actions[]) throws Exception {
        // Ignored silently since composite tree is not writable
        LOG.log(Level.FINE, "PV configuration not supported on Composite Alarm Client Model.");
    }

    @Override
    public void enable(final AlarmTreePV pv, final boolean enabled) throws Exception {
        // Ignored silently since composite tree is not writable
        LOG.log(Level.FINE, () -> "PV enable not supported on Composite Alarm Client Model. pv: " + pv.getPathName() + ", enabled: " + enabled);
    }

    @Override
    public void rename(final AlarmTreeItem item, final String new_name) throws Exception {
        // Ignored silently since composite tree is not writable
        LOG.log(Level.FINE, "Item rename not supported on Composite Alarm Client Model.");

    }

    @Override
    public void move(final AlarmTreeItem item, final String new_path) throws Exception {
        // Ignored silently since composite tree is not writable
        LOG.log(Level.FINE, "Item move not supported on Composite Alarm Client Model.");
    }

    @Override
    public void duplicatePV(final AlarmTreePV pv, final String new_path_and_pv) throws Exception {
        // Ignored silently since composite tree is not writable
        LOG.log(Level.FINE, "PV duplication not supported on Composite Alarm Client Model.");
    }

    @Override
    public void readConfig(final String path) throws Exception {
        // Ignored silently since this does not make sense on composite tree
        LOG.log(Level.FINE, () -> "Reading configuration not supported on Composite Alarm Client Model. path: " + path);
    }

    @Override
    public void updateEnablement(final String name, final boolean enabled) {
        // Ignored silently since this does not make sense on composite tree
        LOG.log(Level.FINE, () -> "Update enablement not supported on Composite Alarm Client Model. name: " + name + ", enabled: " + enabled);
    }

    @Override
    public synchronized AlarmTreePV findPV(final String name) {
        synchronized (models) {
            for (AlarmClientModel model : models) {
                final AlarmTreePV pv = model.findPV(name);
                if (pv != null) return pv;
            }
        }
        // not found
        return null;
    }

    @Override
    public void acknowledge(final AlarmTreePV pv, final boolean acknowledge) {
        // Ignored silently since this does not make sense on composite tree
        LOG.log(Level.FINE, () -> "Acknowledge not supported on Composite Alarm Client Model. pv: " + pv.getPathName() + ", acknowledge: " + acknowledge);
    }

    @Override
    public void triggerDebug() {
        // Ignored silently since this does not make sense on composite tree
        LOG.log(Level.FINE, "Sending DEBUG to server not supported on Composite Alarm Client Model.");
    }

    @Override
    public String toString() {
        return "CompositeAlarmClientModel: " + getConfigurationName();
    }

    @Override
    public synchronized void dump() {
        // Ignored silently since this does not make sense on composite tree
        LOG.log(Level.FINE, "Dump not supported on Composite Alarm Client Model.");
    }

    @Override
    public void addListener(final AlarmClientModelListener listener) {
        listeners.add(listener);
        LOG.log(Level.FINER, () -> "Listener added. n=" + listeners.size());
    }

    @Override
    public void removeListener(final AlarmClientModelListener listener) {
        listeners.remove(listener);
        LOG.log(Level.FINER, () -> "Listener removed. n=" + listeners.size());
    }

    public boolean isAllLoaded() {
        return allLoaded;
    }

    public void setAllLoaded(boolean allLaoded) {
        this.allLoaded = allLaoded;
    }

    // Inform listeners about server timeout
    private void compositeServerTimeout(String configName) {
        if (!timeoutLoopPrevention.compareAndSet(false, true))
            return; // already in the loop
        // The root is disconnected only if all children are disconnected
        disconnectedModels.add(configName);
        if (disconnectedModels.size() >= models.size()) {
            LOG.log(Level.WARNING, "All Alarm Client Models are diconnected from the JMS server.");
            for (AlarmClientModelListener listener : listeners) {
                try {
                    listener.serverTimeout(this);
                } catch (Throwable ex) {
                    Activator.getLogger().log(Level.WARNING, "Server timeout notification error", ex);
                }
            }
        }
        timeoutLoopPrevention.set(false);
    }

    // Inform listeners that server is OK and in which mode
    private void compositeModeUpdate(String configName) {
        if (!modeLoopPrevention.compareAndSet(false, true))
            return; // already in the loop
        disconnectedModels.remove(configName);
        // composite can never go into maintenance mode
        for (AlarmClientModelListener listener : listeners) {
            try {
                // never in maintenance mode
                listener.serverModeUpdate(this, false);
            } catch (Throwable ex) {
                Activator.getLogger().log(Level.WARNING, "Model update notification error", ex);
            }
        }
        modeLoopPrevention.set(false);
    }

    // Inform listeners about overall change to alarm tree configuration:
    //   Items added, removed.
    private void compositeNewConfig() {
        if (!configLoopPrevention.compareAndSet(false, true))
            return; // already in the loop
        for (AlarmClientModelListener listener : listeners) {
            try {
                listener.newAlarmConfiguration(this);
            } catch (Throwable ex) {
                Activator.getLogger().log(Level.WARNING, "Model config notification error", ex);
            }
        }
        configLoopPrevention.set(false);
    }

    /*  Inform listeners about change in alarm state.
     *
     *  Typically, this is invoked with the PV that changed state.
     *  May be called with a 'null' PV to indicate that messages were received
     *  after a server timeout.
     *
     *  pv                  PV that might have changed the alarm state or 'null'
     *  parent_changed    'true' if a parent item was updated as well
     */
    private void compositeNewAlarmState(final AlarmTreePV pv, final boolean parent_changed) {
        if (!alarmLoopPrevention.compareAndSet(false, true))
            return; // already in the loop
        for (AlarmClientModelListener listener : listeners) {
            try {
                final boolean changed = compositeRoot.maximizeSeverity();
                if (changed) {
                    LOG.log(Level.FINE, () -> String.format("Current Severity: %s, Severity: %s, Message: %s",
                            compositeRoot.getCurrentSeverity().toString(),
                            compositeRoot.getSeverity().toString(),
                            compositeRoot.getMessage()));
                }
                listener.newAlarmState(this, pv, true);
            } catch (Throwable ex) {
                Activator.getLogger().log(Level.WARNING, "Alarm update notification error", ex);
            }
        }
        alarmLoopPrevention.set(false);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof CompositeAlarmClientModel)) return false;
        final CompositeAlarmClientModel other = (CompositeAlarmClientModel) obj;
        return Objects.equals(getConfigurationName(), other.getConfigurationName());
    }

    @Override
    public int hashCode() {
        final String name = getConfigurationName();
        return name == null ? 1 : 33 + name.hashCode();
    }
}
