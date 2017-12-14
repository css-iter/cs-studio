package org.csstudio.simplepv.pvmanager;

import java.util.logging.Level;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;

/** Preferences for PV Manager. */
public class Preferences {

    private Preferences(){}

    public static final String WRITE_LOG_LEVEL = "pv_manager_write_log_level";
    public static final String WRITE_LOG_MESSAGE = "pv_manager_write_log_message";

    private static final IPreferencesService prefService = Platform.getPreferencesService();

    public static Level getWriteLogLevel() {
        Level defaultLevel = Level.INFO;

        String txt = prefService.getString(Activator.PLUGIN_ID, WRITE_LOG_LEVEL, defaultLevel.getName(), null);

        try {
            return Level.parse(txt);
        } catch (IllegalArgumentException ex) {
            return defaultLevel;
        }
    }

    public static String getLogMessage() {
        return prefService.getString(Activator.PLUGIN_ID, WRITE_LOG_MESSAGE, "", null);
    }
}
