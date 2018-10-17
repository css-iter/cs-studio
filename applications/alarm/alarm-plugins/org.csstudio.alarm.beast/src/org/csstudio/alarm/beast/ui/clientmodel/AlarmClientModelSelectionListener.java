package org.csstudio.alarm.beast.ui.clientmodel;

public interface AlarmClientModelSelectionListener {
    /**
     * <p>
     * This method can be used by interested parties (UI widgets), to get notified that some component
     * selected another {@link AlarmClientModel}, usually through a user action. The method is also called
     * from {@link AlarmClientModel#getInstance()} (all variants), but in this case the <code>id</code>
     * and <code>oldMode</code> are both <code>null</code>.
     *  </p><p>
     *  The notification that a new {@link AlarmClientModel} was selected must be sent explicitly by the widget,
     *  as the code cannot guess that a new model was selected and what the old one is.
     *  </p>
     * @param id -
     *          the ID of the widget triggering the change. Can be used to filter out your own selection.
     *          Can be <code>null</code>
     * @param oldModel - the previous {@link AlarmClientModel}. Can be <code>null</code>.
     * @param newModel - the new (selected) {@link AlarmClientModel}. Cannot be <code>null</code>.
     */
    public void alarmModelSelection(String id, AlarmClientModel oldModel, AlarmClientModel newModel);
}
