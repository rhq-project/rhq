/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.coregui.client.components.measurement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.coregui.client.util.MeasurementUtility;

public class CustomConfigMeasurementRangeEditor extends AbstractMeasurementRangeEditor {

    public static final String PREF_METRIC_RANGE = Constant.METRIC_RANGE;
    public static final String PREF_METRIC_RANGE_LASTN = Constant.METRIC_RANGE_LASTN;
    public static final String PREF_METRIC_RANGE_UNIT = Constant.METRIC_RANGE_UNIT;
    public static final String PREF_METRIC_RANGE_BEGIN_END_FLAG = Constant.METRIC_RANGE_BEGIN_END_FLAG;
    public static final String ALERT_METRIC_RANGE_ENABLE = Constant.METRIC_RANGE_ENABLE;

    public static final String DEFAULT_VALUE_RANGE_RO = Boolean.FALSE.toString();
    public static final Integer DEFAULT_VALUE_RANGE_LASTN = Integer.valueOf(Constant.METRIC_RANGE_LASTN_DEFAULT);
    public static final Integer DEFAULT_VALUE_RANGE_UNIT = Integer.valueOf(Constant.METRIC_RANGE_UNIT_DEFAULT);

    private ConfigurationMeasurementPreferences measurementPrefs;

    public CustomConfigMeasurementRangeEditor(Configuration config) {
        super();
        measurementPrefs = new ConfigurationMeasurementPreferences(config);
        setDisplaySetButton(false);
        setDisplayEnableButton(true);
        setDisplayCheckboxLabel(true);
        setWidth(650);
    }

    @Override
    public List<Long> getBeginEndTimes() {
        List<Long> beginEndTimes = new ArrayList<Long>();
        if (advanced) {
            beginEndTimes.add(advancedStartItem.getValueAsDate().getTime());
            beginEndTimes.add(advancedEndItem.getValueAsDate().getTime());
            return beginEndTimes;
        } else {
            int lastN = Integer.valueOf(simpleLastValuesItem.getValueAsString());
            String unit = simpleLastUnitsItem.getValueAsString();
            measurementPrefs.metricRangePreferences.lastN = lastN;
            measurementPrefs.metricRangePreferences.unit = Integer.valueOf(unit);
            return MeasurementUtility.calculateTimeFrame(lastN, Integer.valueOf(unit));
        }
    }

    @Override
    public MetricRangePreferences getMetricRangePreferences() {
        return measurementPrefs.getMetricRangePreferences();
    }

    @Override
    public void setMetricRangeProperties(MetricRangePreferences prefs) {
        measurementPrefs.setMetricRangePreferences(prefs);
    }

    public String getSimpleProperty(String propertyKey) {
        String propertyValue = "";
        if ((propertyKey != null) && (propertyKey.trim().length() > 0)) {
            if ((measurementPrefs != null) && (measurementPrefs.configuration != null)) {
                PropertySimple property = measurementPrefs.configuration.getSimple(propertyKey);
                if (property != null) {
                    propertyValue = property.getStringValue();
                }
            }
        }
        return propertyValue;
    }

    public void setSimpleProperty(String propertyKey, String value) {
        if ((propertyKey != null) && (propertyKey.trim().length() > 0) && (value != null) && (!value.trim().isEmpty())) {
            if ((measurementPrefs != null) && (measurementPrefs.configuration != null)) {
                measurementPrefs.configuration.put(new PropertySimple(propertyKey, value));
            }
        }
    }

    class ConfigurationMeasurementPreferences {

        private MetricRangePreferences metricRangePreferences;
        private Configuration configuration;

        public ConfigurationMeasurementPreferences(Configuration config) {
            if (metricRangePreferences == null) {
                metricRangePreferences = new MetricRangePreferences();
            }
            //parse config and lazily init timing elements
            metricRangePreferences.explicitBeginEnd = Boolean.valueOf(
                config.getSimple(PREF_METRIC_RANGE_BEGIN_END_FLAG).getStringValue()).booleanValue();
            //check to display advanced settings widget components
            if (metricRangePreferences.explicitBeginEnd == false) {
                //retrieve lastN
                metricRangePreferences.lastN = config.getSimple(PREF_METRIC_RANGE_LASTN).getIntegerValue();
                //retrieve lastN units
                metricRangePreferences.unit = config.getSimple(PREF_METRIC_RANGE_UNIT).getIntegerValue();

                List<Long> range = MeasurementUtility.calculateTimeFrame(metricRangePreferences.lastN,
                    metricRangePreferences.unit);
                metricRangePreferences.begin = range.get(0);
                metricRangePreferences.end = range.get(1);
            } else {//in advanced view
                try {
                    String rangeString = config.getSimpleValue(PREF_METRIC_RANGE, "");
                    if (rangeString != null && rangeString.trim().length() > 0) {
                        if (rangeString.contains(",")) { // legacy support: old prefs used to use commas
                            rangeString = rangeString.replace(",", "|");
                            //userPrefs.setPreference(PREF_METRIC_RANGE, rangeString); // TODO set only if we don't support JSF anymore
                        }
                        String[] beginEnd = rangeString.split("\\|");
                        metricRangePreferences.begin = Long.parseLong(beginEnd[0]);
                        metricRangePreferences.end = Long.parseLong(beginEnd[1]);
                    }
                } catch (IllegalArgumentException iae) {
                    // that's OK, range will remain null and we might use the lastN / unit
                    List<Long> range = MeasurementUtility.calculateTimeFrame(DEFAULT_VALUE_RANGE_LASTN,
                        DEFAULT_VALUE_RANGE_UNIT);
                    metricRangePreferences.begin = range.get(0);
                    metricRangePreferences.end = range.get(1);
                }
            }
            //            update configuration
            this.configuration = config;
        }

        public void setMetricRangePreferences(MetricRangePreferences metricRangePreferences) {
            this.metricRangePreferences = metricRangePreferences;
        }

        public MetricRangePreferences getMetricRangePreferences() {
            return metricRangePreferences;
        }
    }

    @Override
    protected void onInit() {
        super.onInit();
        //turn on date entry validation
        enableRangeItem.setWidth(30);
        //      advancedStartItem.setEnforceDate(true);
        //      advancedEndItem.setEnforceDate(true);
        //disable text field view to prevent bad data entry. Use widget or dropdowns.
        //TODO: spinder 3/9/11: this should be renabled to allow minute/second tuning here too, but need to handle validation.
        advancedStartItem.setUseTextField(false);
        advancedEndItem.setUseTextField(false);
        advancedStartItem.setType("selection");
        simpleLastValuesItem.setWidth(50);
        simpleLastUnitsItem.setWidth(70);

        //set fields to previously populated values
        if (Boolean.valueOf(measurementPrefs.configuration.getSimple(ALERT_METRIC_RANGE_ENABLE).getStringValue())) {
            enableRangeItem.setValue(true);
            enableMeasurementRange(false);
        } else {
            enableRangeItem.setValue(false);
            enableMeasurementRange(true);
        }
        //is advanced
        boolean advanced = measurementPrefs.metricRangePreferences.explicitBeginEnd;
        if (advanced) {
            ArrayList<Long> beginEnd = measurementPrefs.metricRangePreferences.getBeginEndTimes();
            if ((beginEnd != null) && (!beginEnd.isEmpty())) {
                advancedStartItem.setValue(beginEnd.get(0));
                advancedEndItem.setValue(beginEnd.get(1));
            }
        } else {//simple: set LastN and Units
            if (lastUnits.containsKey(String.valueOf(measurementPrefs.metricRangePreferences.unit))) {
                simpleLastUnitsItem.setValue(String.valueOf(measurementPrefs.metricRangePreferences.unit));
            }
            if (Arrays.asList(lastValues).contains(String.valueOf(measurementPrefs.metricRangePreferences.lastN))) {
                simpleLastValuesItem.setValue(String.valueOf(measurementPrefs.metricRangePreferences.lastN));
            }
        }
    }
}
