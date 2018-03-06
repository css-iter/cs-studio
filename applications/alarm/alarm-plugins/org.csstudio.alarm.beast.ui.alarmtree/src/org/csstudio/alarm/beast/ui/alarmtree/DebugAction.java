/*******************************************************************************
 * Copyright (c) 2010 Oak Ridge National Laboratory.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui.alarmtree;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

/** Action to trigger debug on server
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class DebugAction extends Action
{
    final private Shell shell;
    final private ModelProvider modelProvider;

    public DebugAction(final Shell shell, final ModelProvider modelProvider)
    {
        super("Debug", Activator.getImageDescriptor("icons/debug.gif"));
        setToolTipText("Send debug trigger to alarm server");
        this.shell = shell;
        this.modelProvider = modelProvider;
    }

    @Override
    public void run()
    {
        if (MessageDialog.openConfirm(shell, "Send debug trigger", "Send 'debug' trigger to alarm server?"))
            modelProvider.getModel().triggerDebug();
    }
}
