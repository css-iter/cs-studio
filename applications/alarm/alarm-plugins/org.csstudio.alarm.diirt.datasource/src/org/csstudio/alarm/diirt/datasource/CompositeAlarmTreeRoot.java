package org.csstudio.alarm.diirt.datasource;

import java.util.List;

import org.csstudio.alarm.beast.client.AlarmTreeItem;
import org.csstudio.alarm.beast.client.AlarmTreeLeaf;
import org.csstudio.alarm.beast.client.AlarmTreeRoot;

public class CompositeAlarmTreeRoot extends AlarmTreeRoot {
    private static final long serialVersionUID = 8577521363075445385L;

    public CompositeAlarmTreeRoot(String name) {
        super(name, -1);
    }

    @Override
    public synchronized int getLeafCount() {
        return super.getLeafCount();
    }

    public synchronized void addAlarmTreeRoot(AlarmTreeRoot child) {
        addChild(child);
    }

    public synchronized void removeAlarmTreeRoot(AlarmTreeRoot child) {
        removeChild(child);
    }

    @Override
    public void addLeavesToList(final List<AlarmTreeLeaf> leaves) {
        super.addLeavesToList(leaves);
    }

    @Override
    public synchronized int getElementCount() {
        return super.getElementCount();
    }

    @Override
    public AlarmTreeRoot getRoot() {
        return this;
    }

    @Override
    public AlarmTreeItem getParent() {
        return null;
    }

    @Override
    public AlarmTreeItem getChild(final int i) {
        return super.getChild(i);
    }

    @Override
    public AlarmTreeItem getChild(final String name) {
        return super.getChild(name);
    }

    @Override
    public String getConfigTime() {
        return super.getConfigTime();
    }

    @Override
    public void acknowledge(final boolean acknowledge) {
        super.acknowledge(acknowledge);
    }

    @Override
    public synchronized boolean maximizeSeverity() {
        return super.maximizeSeverity();
    }
}
