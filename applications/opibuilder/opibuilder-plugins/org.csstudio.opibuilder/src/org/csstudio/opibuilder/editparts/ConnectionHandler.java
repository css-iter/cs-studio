/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.opibuilder.editparts;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.alarm.beast.client.AlarmTreePV;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModelListener;
import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModelsCache;
import org.csstudio.opibuilder.preferences.PreferencesHelper;
import org.csstudio.opibuilder.util.AlarmRepresentationScheme;
import org.csstudio.opibuilder.visualparts.BorderStyle;
import org.csstudio.simplepv.IPV;
import org.csstudio.simplepv.IPVListener;
import org.csstudio.ui.util.thread.UIBundlingThread;
import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.widgets.Display;

/**
 * The handler help a widget to handle the pv connection event such as
 * PVs' disconnection, connection recovered. It will show a disconnect border on the widget
 * if any one of the PVs is disconnected. The detailed disconnected information will be displayed
 * as tooltip.
 * For BEAST datasource we handle connections by checking the AlarmClient model for every different channel.
 * @author Xihui Chen
 * @author Miha Vitorovic
 */
public class ConnectionHandler {

    private final Logger log = Logger.getLogger(ConnectionHandler.class.getName());

    // listener for PV connections
    private final class PVConnectionListener extends IPVListener.Stub {

        private boolean lastValueIsNull;

        @Override
        public void valueChanged(IPV pv) {
            if (isBeastChannel) {
                return;
            }
            log.log(Level.FINEST, () -> new String ("Pv " + pv.getName() + " value changed to:" + pv.getValue()));
            if(lastValueIsNull && pv.getValue()!=null){
                lastValueIsNull = false;
                widgetConnectionRecovered(pv, true);
            }
        }

        @Override
        public void connectionChanged(IPV pv) {
            if (isBeastChannel) {
                return;
            }
            log.log(Level.FINEST, () -> new String ("Pv " + pv.getName() + " connection changed to, is connected:" + pv.isConnected()));
            if(pv.isConnected()){
                lastValueIsNull = (pv.getValue()==null);
                widgetConnectionRecovered(pv, false);
            }
            else
                markWidgetAsDisconnected(pv);
        }

    }

    // listener for BEAST server connections
    private final class BEASTModelListener implements AlarmClientModelListener{

        @Override
        public void newAlarmConfiguration(AlarmClientModel model) {
            log.log(Level.FINE, () -> new String ("Alarm configuration for " + model.getConfigurationName() + " changed"));
            checkModelStateAndMarkBorder(model);
        }

        @Override
        public void serverModeUpdate(AlarmClientModel model, boolean maintenance_mode) {
            // ignore this state
        }

        @Override
        public void serverTimeout(AlarmClientModel model) {
            log.log(Level.FINE, () -> new String ("Alarm server for " + model.getConfigurationName() + " timeout"));
            display.asyncExec(() -> figure.setBorder(AlarmRepresentationScheme.getDisonnectedBorder()));

        }

        @Override
        public void newAlarmState(AlarmClientModel model, AlarmTreePV pv, boolean parent_changed) {
            checkModelStateAndMarkBorder(model);
        }
    }


    private Map<String, IPV> pvMap;

    /**
     * True if all PVs are connected.
     */
    private boolean connected;

    private String toolTipText;

    private IFigure figure;

    private Display display;

    protected AbstractBaseEditPart editPart;

    private boolean hasNullValue;

    private boolean isBeastChannel = true;
    private String beastPvName;
    private AlarmClientModel model;
    private BEASTModelListener beastModelListener;
    private AlarmClientModelsCache beastModelsCache;

    /**
     * @param editpart the widget editpart to be handled.
     */
    public ConnectionHandler(AbstractBaseEditPart editpart) {
        this.editPart = editpart;
        figure = editpart.getFigure();
        this.display = editpart.getViewer().getControl().getDisplay();
        pvMap = new ConcurrentHashMap<>();
        beastModelsCache = AlarmClientModelsCache.INSTANCE;
        connected = true;
    }

    /**
     * Add a PV to this handler, so its connection event can be handled.
     *
     * @param pvName
     *            name of the PV.
     * @param pv
     *            the PV object.
     */
    public void addPV(final String pvName, final IPV pv) {
        pvMap.put(pvName, pv);
        checkForBeastDs(pvName);
        if(isBeastChannel){
            checkModelStateAndMarkBorder(model);
        } else {
            markWidgetAsDisconnectedFirstTime(pv);
        }
        pv.addListener(new PVConnectionListener());
    }

    // check if all PV's are beast DS.
    private void checkForBeastDs(String pvName) {
        isBeastChannel = isBeastChannel && AlarmClientModelsCache.isPvBeastPV(pvName);
        log.log(Level.FINE, () -> ("Pv: " + pvName + "  is beast channel: " + isBeastChannel));
        if (isBeastChannel) {
            beastPvName = pvName;
            getBeastModel();
        }
    }

    // this method updates the GUI based on model state
    private void checkModelStateAndMarkBorder(AlarmClientModel model) {
        refreshBEASTTooltip();
        UIBundlingThread.getInstance().addRunnable(display, new Runnable() {
            @Override
            public void run() {
                if (model == null){
                    figure.setBorder(AlarmRepresentationScheme.getDisonnectedBorder());
                    return;
                }
                log.log(Level.FINER, () -> "Model: " + model.getConfigurationName() + "  is alive: " + model.isServerAlive());
                if (!model.isServerAlive()) {
                    figure.setBorder(AlarmRepresentationScheme.getDisonnectedBorder());
                } else {
                    figure.setBorder(editPart.calculateBorder());
                }
            }
        });
    }


    // get and add listener to beast model
    private void getBeastModel() {
        if (model != null) {
            if (beastModelListener != null) {
                model.removeListener(beastModelListener);
            }
            model.release();
            model = null;
        }
        if (model == null) {
            try {
                model = beastModelsCache.getModel(beastPvName);
                log.log(Level.FINER, () -> "BEAST model for:" + beastPvName + " is " + model);
                log.log(Level.FINER, () -> "BEAST model " + model + " is alive:" + model.isServerAlive());
                if (model == null) {
                    isBeastChannel = false;
                    return;
                }
                beastModelListener = new BEASTModelListener();
                model.addListener(beastModelListener);
            } catch (Exception e) {
                log.log(Level.WARNING, () -> "No instance of BEAST model:" + beastPvName + "exsist. ");
                return;
            }
        }
    }

    //handle the BEAST tool-tip
    private void refreshBEASTTooltip(){
        StringBuilder sb = new StringBuilder();
        if (model == null || !model.isServerAlive())
            sb.append(beastPvName + " is disconnected.\n");
        if(sb.length()>0){
            sb.append("------------------------------\n");
            toolTipText = sb.toString();
        }else
            toolTipText = "";
    }


    public void removePV(final String pvName){
        if(pvMap == null){
            return;
        }
        pvMap.remove(pvName);
    }


    private void refreshModelTooltip(){
        StringBuilder sb = new StringBuilder();
        for(Entry<String, IPV> entry : pvMap.entrySet()){
            if(!entry.getValue().isConnected()){
                sb.append(entry.getKey() + " is disconnected.\n");
            }else if(entry.getValue().getValue() == null){
                sb.append(entry.getKey() + " has null value.\n");
            }
        }
        if(sb.length()>0){
            sb.append("------------------------------\n");
            toolTipText = sb.toString();
        }else
            toolTipText = "";
    }

    /**Mark a widget as disconnected.
     * @param pvName the name of the PV that is disconnected.
     */
    protected void markWidgetAsDisconnected(IPV pv){
        refreshModelTooltip();
        if(!connected)
            return;
        connected = false;
        //Making this task execute in UI Thread
        //It will also delay the disconnect marking requested during widget activating
        //to execute after widget is fully activated.
        UIBundlingThread.getInstance().addRunnable(display, new Runnable(){
            @Override
            public void run() {
                figure.setBorder(AlarmRepresentationScheme.getDisonnectedBorder());
            }
        });
    }

    /** Thread executor queue used to mark widgets as disconnected the first time */
    private static final ScheduledExecutorService queue = Executors.newSingleThreadScheduledExecutor();

    /**
     * Mark a widget as disconnected the first time.
     * To avoid Flashes, this method waits a GUIRefreshCycle timeout to let the widget know if its connected or not, and then decide if display the Disconnected border
     * @param pv
     */
    protected void markWidgetAsDisconnectedFirstTime(IPV pv){
        refreshModelTooltip();
        if(!connected)
            return;
        connected = false;


        // Wait for "opi_gui_refresh_cycle" ms to give time to connect before display Disconnected border
        queue.schedule(new Runnable() {
            @Override
            public void run() {
                if(!connected) {
                    //Making this task execute in UI Thread
                    //It will also delay the disconnect marking requested during widget activating
                    //to execute after widget is fully activated.
                    UIBundlingThread.getInstance().addRunnable(display, new Runnable(){
                        @Override
                        public void run() {
                            // re-check if we are already connected
                            if(!connected) {
                                figure.setBorder(AlarmRepresentationScheme.getDisonnectedBorder());
                            }
                        }
                    });
                }
            }
        }, PreferencesHelper.getGUIRefreshCycle(), TimeUnit.MILLISECONDS);

    }


    /**Update the widget when a PV' connection is recovered.
     * @param pvName the name of the PV whose connection is recovered.
     * @param valueChangedFromNull true if this is called because value changed from null value.
     */
    protected void widgetConnectionRecovered(IPV pv, boolean valueChangedFromNull){

        if (connected && !valueChangedFromNull)
            return;
        boolean allConnected = true;
        hasNullValue = false;
        for (IPV pv2 : pvMap.values()) {
            allConnected &= pv2.isConnected();
            hasNullValue |=(pv2.getValue()==null);
        }
        refreshModelTooltip();
        if (allConnected) {
            connected = true;
            UIBundlingThread.getInstance().addRunnable(display, new Runnable() {
                @Override
                public void run() {
                    if(hasNullValue)
                        figure.setBorder(
                                AlarmRepresentationScheme.getInvalidBorder(BorderStyle.DOTTED));
                    else
                        figure.setBorder(editPart.calculateBorder());

                }
            });
        }
    }

    /**
     * @return true if all pvs are connected.
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * @return true if one or some PVs have null values.
     */
    public boolean isHasNullValue() {
        return hasNullValue;
    }

    /**
     * @return the map with all PVs. It is not allowed to change the Map.
     */
    public Map<String, IPV> getAllPVs() {
        return pvMap;
    }

    public String getToolTipText() {
        return toolTipText;
    }

}
