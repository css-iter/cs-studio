package org.csstudio.alarm.beast.ui.clientmodel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This sigleton acts as a cache for AlarmClientModels used by the PWvidgets.
 * It will return model for given beast PV.
 *
 *  @author Miha Vitorovic
 *
 */

public enum AlarmClientModelsCache {
    INSTANCE;

    private final ConcurrentMap<String, AlarmClientModel> beastModels = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AlarmClientModel> pvModelsCache = new ConcurrentHashMap<>();;
    private AlarmClientModel defaultModel;
    private final Logger Log = Logger.getLogger(AlarmClientModelsCache.class.getName());
    private static final String BEAST_PREFIX = "beast://";

    private AlarmClientModelsCache() {
        getBeastModels();
    }

    private  void getBeastModels() {
        try {
            defaultModel = AlarmClientModel.getInstance();
            synchronized (defaultModel) {
                for (String config : defaultModel.getConfigurationNames()) {
                    if (!config.equals(defaultModel.getConfigurationName())) {
                        final AlarmClientModel modelInst = AlarmClientModel.getInstance(config);
                        beastModels.put(modelInst.getConfigurationName(), modelInst);
                        Log.info(() -> "Putting " + modelInst.getConfigurationName() + " to models cache");
                    }
                }
            }
        } catch (Exception e) {
            Log.log(Level.SEVERE, () -> "Error initialising beast models");
        }
    }

    public static boolean isPvBeastPV(String overridePvName) {
        return overridePvName.startsWith(BEAST_PREFIX);
    }

    //if in cache, return form the cache.
    public AlarmClientModel getModel(String pvName) {

        final AlarmClientModel model = pvModelsCache.get(pvName);
        if (model != null) {
            Log.fine(() -> String.format("Model %s for pv %s is read from cache ", model, pvName));
            return model;
        }

        final AlarmClientModel resolvedModel = resolveModel(pvName);
        Log.fine(() -> String.format("Model %s for pv %s is was resolved ", resolvedModel, pvName));
        if (resolvedModel != null)
            pvModelsCache.put(pvName, resolvedModel);
        return resolvedModel;
    }

    //resolve model from the beast PV uri
    private AlarmClientModel resolveModel(String pvNameWithPrefix) {
         if (beastModels.size() < 1)
            getBeastModels();

        String pvName = pvNameWithPrefix.substring(pvNameWithPrefix.indexOf(BEAST_PREFIX) + 8);

        // if PV name is direct model
        if (beastModels.containsKey(pvName)) {
            return beastModels.get(pvName);
        }

        if (pvName.contains("/")) {
            // Alarm root defined
            final String lookupName = channelHandlerLookupName(pvName);
            final String root;
            if (lookupName.contains("/"))
                root = lookupName.substring(0, lookupName.indexOf('/'));
            else
                root = lookupName;
            Log.log(Level.FINE, () -> "root: " + root);
            if (defaultModel.getConfigurationName().equals(root)) {
                Log.fine("Returning default model.");
                return defaultModel;
            }
            if (beastModels.containsKey(root)) {
                Log.fine("Returning named model.");
                return beastModels.get(root);
            }
            // for the direct root use the type
            try {
                AlarmClientModel testRootModel = AlarmClientModel.getInstance(root);
                        if (("CompositeAlarmClientModel").equals(testRootModel.getClass().getSimpleName())) {
                        return testRootModel;
                }
            } catch (Exception e) {
                Log.log(Level.WARNING, String.format("Error geting the model instance for %s", pvName));
            }

            Log.log(Level.WARNING, () -> String.format("Root not found: %s, lookupname %s", root, lookupName));
            return null;
        } else {
            // PV only - search models in the DIIRT defined order, but search default first.
            if (pvName.contains(".")) {
                pvName = pvName.substring(0, pvName.indexOf("."));
            }
            if (defaultModel.findPV(pvName) != null)
                return defaultModel;
            for (AlarmClientModel entry : beastModels.values()) {
                if (entry.findPV(pvName) != null) {
                    Log.log(Level.FINE, new String(String.format("Found model %s for pv: %s ", entry, pvName)));
                    return entry;
                }
            }
            return null;
        }
    }

    private String channelHandlerLookupName(String channelName) {
        String channel = channelName;
        if (channel != null && !channel.equals("/") && !channel.isEmpty()) {
            if (channel.endsWith("/"))
                channel = channel.substring(0, channel.length() - 1);
            if (channel.startsWith("/"))
                channel = channel.substring(1);
        }
        return channel;
    }

}
