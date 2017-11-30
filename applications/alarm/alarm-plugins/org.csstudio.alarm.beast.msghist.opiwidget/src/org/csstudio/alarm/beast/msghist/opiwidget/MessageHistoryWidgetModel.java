/*******************************************************************************
 * Copyright (c) 2010-2017 ITER Organization.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.alarm.beast.msghist.opiwidget;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.csstudio.alarm.beast.msghist.Preferences;
import org.csstudio.alarm.beast.msghist.PropertyColumnPreference;
import org.csstudio.alarm.beast.msghist.model.FilterQuery;
import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.properties.StringProperty;
import org.csstudio.opibuilder.properties.WidgetPropertyCategory;
import org.eclipse.osgi.util.NLS;

/**
 *
 * <code>MessageHistoryWidgetModel</code> is the OPI Builder model for the Message history widget.
 *
 * @author Borut Terpinc
 *
 */
public class MessageHistoryWidgetModel extends AbstractWidgetModel {

    private static final String ID = "org.csstudio.alarm.beast.msghist";

    private static final Logger LOGGER = Logger.getLogger(MessageHistoryWidgetModel.class.getName());

    static final String PROP_TIMEFORMAT = "time_format";
    static final String PROP_FILTER = "filter";
    static final String PROP_COLUMNS = "columns";

    @Override
    protected void configureProperties() {
        String defaultTimeFormat = Preferences.getTimeFormat();
        addProperty(new StringProperty(PROP_TIMEFORMAT, Messages.TimeFormat, WidgetPropertyCategory.Behavior,
                defaultTimeFormat), false);

        String defaultQuery = FilterQuery.fromTimeSpec(Preferences.getDefaultStart(), Preferences.getDefaultEnd());
        addProperty(new FilterProperty(PROP_FILTER, Messages.Filter, WidgetPropertyCategory.Behavior, defaultQuery),
                false);

        try {
            PropertyColumnPreference[] defaultColumns = Preferences.getPropertyColumns();
            addProperty(new ColumnsProperty(PROP_COLUMNS, Messages.Columns, WidgetPropertyCategory.Behavior,
                    new ColumnsInput(defaultColumns)), false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, NLS.bind(Messages.PreferenceReadError, e.getMessage()));
        }

    }

    @Override
    public String getTypeID() {
        return ID;
    }

    /**
     * @return query used for filtering message history items.
     */
    public String getFilter() {
        return getCastedPropertyValue(PROP_FILTER);
    }

    /**
     * @return the array of columns and their properties in proper order
     */
    public PropertyColumnPreference[] getColumns() {
        ColumnsInput columns = getCastedPropertyValue(PROP_COLUMNS);
        return columns.getColumns();
    }

    /**
     * @return the format used for formating the date and time column data
     */
    public DateTimeFormatter getTimeFormat() {
        String format = getCastedPropertyValue(PROP_TIMEFORMAT);
        return DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault());
    }
}
