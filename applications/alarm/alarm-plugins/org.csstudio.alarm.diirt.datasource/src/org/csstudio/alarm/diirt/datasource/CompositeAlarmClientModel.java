package org.csstudio.alarm.diirt.datasource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.csstudio.alarm.beast.client.AADataStructure;
import org.csstudio.alarm.beast.client.AlarmTreeItem;
import org.csstudio.alarm.beast.client.AlarmTreePV;
import org.csstudio.alarm.beast.client.AlarmTreeRoot;
import org.csstudio.alarm.beast.client.GDCDataStructure;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModelConfigListener;

public class CompositeAlarmClientModel extends AlarmClientModel {
    private static final Logger LOG = Logger.getLogger(CompositeAlarmClientModel.class.getCanonicalName());

    private CompositeAlarmTreeRoot compositeRoot;
    private final List<AlarmClientModel> models;

    /**
     * @param configName the fake composite configuration name.
     */
    public CompositeAlarmClientModel(final String configName) {
        super(configName);
        compositeRoot = new CompositeAlarmTreeRoot(configName);
        models = Collections.synchronizedList(new ArrayList<>());
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
        compositeRoot.addAlarmTreeRoot(child.getConfigTree());
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

    @Override
    public void requestMaintenanceMode(boolean maintenance) {
        throw new UnsupportedOperationException("Maintenance mode not supported on Composite Alarm Client Model.");
    }

    @Override
    synchronized public AlarmTreeRoot getConfigTree() {
        return compositeRoot;
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
        LOG.log(Level.FINE, "PV enable not supported on Composite Alarm Client Model.");
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
        // TODO find in children... or always return null?
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
}
