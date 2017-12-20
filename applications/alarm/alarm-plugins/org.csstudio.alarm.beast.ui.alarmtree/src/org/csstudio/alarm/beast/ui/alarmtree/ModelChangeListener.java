/*******************************************************************************
 * Copyright (c) 2010-2017 ITER Organization.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui.alarmtree;

import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;

/**
 * <p>
 * This interface defines na internal listener for Alarm Tree package.
 * </p><p>
 * Since the GUI registers a AlarmClientModelListener to the currently displayed alarm tree
 * it needs to be notified when the user selects a new Alarm Model configuration.
 * </p><p>
 * At that time, the GUI needs to unregister the listener that was set to the previously displayed
 * {@link AlarmClientModel} and set up a new listener to the new {@link AlarmClientModel}.
 * </p>
 * @author <a href="mailto:miha.vitorovic@cosylab.com">Miha Vitorovic</a>
 */
public interface ModelChangeListener {
    /**
     * This method is invoked when the model changes.
     *
     * @param oldModel the {@link AlarmClientModel} we are changing from
     * @param newModel the {@link AlarmClientModel} we are changing to
     */
    public void modelChange(AlarmClientModel oldModel, AlarmClientModel newModel);
}
