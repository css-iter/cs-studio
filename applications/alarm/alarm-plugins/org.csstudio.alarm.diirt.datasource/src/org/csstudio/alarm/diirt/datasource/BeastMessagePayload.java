/**
 * Copyright (C) 2010-14 diirt developers. See COPYRIGHT.TXT
 * All rights reserved. Use is subject to license terms. See LICENSE.TXT
 */
package org.csstudio.alarm.diirt.datasource;

import org.csstudio.alarm.beast.TimestampHelper;
import org.csstudio.alarm.beast.client.AlarmTreeItem;
import org.csstudio.alarm.beast.client.AlarmTreeLeaf;
import org.csstudio.alarm.beast.client.AlarmTreePV;


/**
 * A message payload used by the beast datasource.
 *
 * @author Kunal Shroff
 *
 */
public class BeastMessagePayload {

    private final AlarmTreeItem pv;

    public BeastMessagePayload(AlarmTreeItem initialState) {
        this.pv = initialState;
    }

    public String getName(){
        return pv.getName();
    }

    public String getDescription(){
        return pv.getMessage();
    }

    public String getAlarmSeverity() {
        return pv.getSeverity().getDisplayName();
    }

    public String getValue() {
        if(pv instanceof AlarmTreePV){
            return ((AlarmTreePV) pv).getValue();
        } else{
            return pv.getToolTipText();
        }
    }

    public boolean isActive(){
        return pv.getSeverity().isActive();
    }

    public boolean getEnable() {
        if(pv instanceof AlarmTreePV){
            return ((AlarmTreePV) pv).isEnabled();
        } else{
            //TODO return enable status for item
            return true;
        }
    }

    public String getCurrentSeverity(){
        return pv.getCurrentSeverity().getDisplayName();
    }

    public String getCurrentMessage(){
        if(pv instanceof AlarmTreePV){
            return ((AlarmTreePV)pv).getCurrentMessage();
        }else {
            return "NA";
        }
    }

    public String getType(){
        if(pv instanceof AlarmTreePV){
            return "leaf";
        } else {
            return "node";
        }
    }

    /**
     * @return The number of PVs in alarm (0 or 1 if this is a PV, 0 or more if this is an AlarmTreeNode)
     */
    public int getAlarmsCount() {
        return pv.getAlarmsCount();
    }

    public String getTime(){
        if(pv instanceof AlarmTreeLeaf){
            return TimestampHelper.format(((AlarmTreeLeaf)pv).getTimestamp());
        }else{
            return "NA";
        }
    }
}
