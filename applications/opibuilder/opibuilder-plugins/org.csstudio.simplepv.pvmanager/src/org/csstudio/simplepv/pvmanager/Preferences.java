package org.csstudio.simplepv.pvmanager;

import java.util.logging.Level;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;

/**
 * Preferences for PV Manager.
 *
 * @author Borut Terpinc
 */
public class Preferences {
    // preference keys
    public static final String LOG_WRITE_LEVEL = "pv_write_log_level";
    public static final String LOG_WRITE_MESSAGE_FORMAT = "pv_write_log_message_format";
    public static final String LOG_WRITE_EXCLUDE_PV_PREFIXES = "pv_write_log_exclude_pv_prefixes";

    public static final String LOG_WRITE_EXCLUDE_PV_PREFIXES_DELIMITER = ",";

    // default preference values
    public static final Level LOG_WRITE_DEFAULT_LEVEL = Level.WARNING;
    public static final String LOG_WRITE_DEFAULT_MESSAGE_FORMAT = "PV write attempt: {0}, Old value: {1}, New value: {2}";
    public static final String[] LOG_WRITE_DEFAULT_EXCLUDE_PV_PREFIXES = new String[] { "loc://", "sim://" };

    /**
     * @return log level to be used for PV writes.
     */
    public static Level getLogWriteLevel() {
        final IPreferencesService preferences = Platform.getPreferencesService();
        String raw = preferences.getString(Activator.PLUGIN_ID, LOG_WRITE_LEVEL, LOG_WRITE_DEFAULT_LEVEL.getName(),
                null);

        try {
            return Level.parse(raw);
        } catch (IllegalArgumentException ex) {
            return LOG_WRITE_DEFAULT_LEVEL;
        }
    }

    /**
     * @return log message format to be used for PV writes.
     */
    public static String getLogWriteMessageFormat() {
        final IPreferencesService preferences = Platform.getPreferencesService();
        String messageFormat = preferences.getString(Activator.PLUGIN_ID, LOG_WRITE_MESSAGE_FORMAT,
                LOG_WRITE_DEFAULT_MESSAGE_FORMAT, null);

        return messageFormat;
    }

    /**
     * @return list of PV name prefixes whose PV writes shoudln't be logged.
     */
    public static String[] getLogWriteExcludePVPrefixes() {
        final IPreferencesService preferences = Platform.getPreferencesService();

        String defaultExcludes = String.join(LOG_WRITE_EXCLUDE_PV_PREFIXES_DELIMITER,
                LOG_WRITE_DEFAULT_EXCLUDE_PV_PREFIXES);
        String raw = preferences.getString(Activator.PLUGIN_ID, LOG_WRITE_EXCLUDE_PV_PREFIXES, defaultExcludes, null);

        return raw.split(LOG_WRITE_EXCLUDE_PV_PREFIXES_DELIMITER);
    }
}
