/*******************************************************************************
 * Copyright (c) 2010-2019 ITER Organization.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.notifier.logbook;

import java.util.logging.Logger;

public class Activator {

    /** Plugin ID defined in MANIFEST.MF */
    final public static String PLUGIN_ID = "org.csstudio.alarm.beast.notifier.logbook";

    final private static Logger logger = Logger.getLogger(PLUGIN_ID);

    /** @return Logger for plugin ID */
    public static Logger getLogger() {
        return logger;
    }

}
