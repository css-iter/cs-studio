package org.csstudio.alarm.diirt.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.csstudio.alarm.beast.Messages;
import org.csstudio.alarm.beast.client.AlarmTreeItem;
import org.csstudio.alarm.beast.client.AlarmTreeLeaf;
import org.csstudio.alarm.beast.client.AlarmTreeRoot;

public class CompositeAlarmTreeRoot extends AlarmTreeRoot {
    private static final long serialVersionUID = 8577521363075445385L;

    private final List<AlarmTreeRoot> subRoots;

    public CompositeAlarmTreeRoot(String name) {
        super(name, -1);
        subRoots = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public synchronized int getLeafCount() {
        int count = 0;
        synchronized (subRoots) {
            for (AlarmTreeRoot child : subRoots) count += child.getLeafCount();
        }
        return count;
    }

    public synchronized void addAlarmTreeRoot(AlarmTreeRoot child) {
        subRoots.add(child);
        addChild(child);
    }

    @Override
    public void addLeavesToList(final List<AlarmTreeLeaf> leaves) {
        synchronized (subRoots) {
            subRoots.stream().forEach(root -> root.addLeavesToList(leaves));
        }
    }

    @Override
    public synchronized int getElementCount() {
        int count = 1;   // count the root as well
        synchronized (subRoots) {
            for (AlarmTreeRoot root : subRoots) count += root.getElementCount();
        }
        return count;
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
        return subRoots.get(i);
    }

    @Override
    public AlarmTreeItem getChild(final String name) {
        synchronized (subRoots) {
            for (final AlarmTreeRoot root : subRoots) {
                if (root.getName().equals(name)) return root;
            }
        }
        return null;
    }

    @Override
    public String getConfigTime() {
        return Messages.Unknown;
    }

    @Override
    public void acknowledge(final boolean acknowledge) {
        synchronized (subRoots) {
            for (final AlarmTreeRoot root : subRoots) {
                root.acknowledge(acknowledge);
            }
        }
    }

    @Override
    public synchronized boolean maximizeSeverity() {
        // This method synchronizes on the components before calling the parent.
        synchronized (subRoots) {
            return super.maximizeSeverity();
        }
    }
}
