/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui.alarmtree;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;
import org.csstudio.apputil.ui.swt.DropdownToolbarAction;

/** (Toolbar) action that shows the currently selected alarm configuration name
 *  and allows selection of a different alarm configuration
 *  @author Kay Kasemir
 */
public class SelectConfigurationAction extends DropdownToolbarAction
{
    private static final Logger LOG = Logger.getLogger(Activator.ID);
    private ModelProvider modelProvider;

    public SelectConfigurationAction(final ModelProvider modelProvider)
    {
        super(modelProvider.getModel().getConfigurationName(), Messages.SelectAlarmConfiguration);
        this.modelProvider = modelProvider;
        setSelection(modelProvider.getModel().getConfigurationName());
    }

    /** {@inheritDoc} */
    @Override
    public String[] getOptions()
    {
        return modelProvider.getModel().getConfigurationNames();
    }

    /** {@inheritDoc} */
    @Override
    public void handleSelection(final String option)
    {
        // Use item text to set model name
        try
        {
            LOG.log(Level.FINE, () -> String.format("User selected new model: %s", option)); //$NON-NLS-1$
            modelProvider.setModel(AlarmClientModel.getInstance(option));   // setModel will also notify all listeners
            setText(option);
        }
        catch (Exception ex)
        {
            LOG.log(Level.SEVERE, "Cannot change alarm model", ex); //$NON-NLS-1$
        }
    }
}
