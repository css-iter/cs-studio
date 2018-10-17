/*******************************************************************************
 * Copyright (c) 2010-2018 ITER Organization.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.ui.alarmtree;

import org.csstudio.alarm.beast.ui.clientmodel.AlarmClientModel;

/**
 * <p>
 * Since the entire plugin must logically act on a single model, the
 * model needs to be stored in one place, and all the plugin components
 * need to act on the same model.
 * </p><p>
 * Since previously the model was the same and its configuration changed,
 * all the plugin components could store a reference to that unique model.
 * But with multiple BEAST datasources, switching the model actually means
 * obtaining a new (already loaded) model, not replacing the configuration on
 * the already obtained one.
 * </p><p>
 * This interface provides capability to support the single source of the model
 * (the Model provider).
 * </p>
 *
 * @author <a href="mailto:miha.vitorovic@cosylab.com">Miha Vitorovic</a>
 */
public interface ModelProvider {
    /** @return the {@link AlarmClientModel} */
    public AlarmClientModel getModel();

    /**
     * @param model sets a new {@link AlarmClientModel}, since a user requested the change
     */
    public void setModel(AlarmClientModel model);
}
