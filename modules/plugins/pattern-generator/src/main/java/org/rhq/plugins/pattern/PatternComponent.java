/*
 * RHQ Management Platform
 * Copyright (C) 2011-2013 Red Hat, Inc.
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
package org.rhq.plugins.pattern;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ListPropertySimpleWrapper;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;

@SuppressWarnings("unused")
public class PatternComponent implements ResourceComponent, MeasurementFacet {

    /**
     * <code>Number List</code> plug-in configuration property name
     */
    public static final String PLUGIN_CONFIG_NUMBER_LIST = "numberList"; //$NON-NLS-1$

    /**
     * <code>Pattern 3 Metric</code> prperty name
     */
    public static final String METRIC_PATTERN_3 = "pattern3"; //$NON-NLS-1$

    private ResourceContext<?> resourceContext;

    private final Log log = LogFactory.getLog(this.getClass());

    int count = 0;
    int traitCount = 0;

    int number = 0; // We start with returning zeros
    int numberForTrait = 0;

    int[] wanted = new int[2];

    long delay = 0L; // in seconds

    /**
     * The current index within <code>numberList</code> for metric named pattern3
     */
    int pattern3Idx = 0;

    /**
     * Return availability of this resource
     * 
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#getAvailability()
     */
    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    /**
     * Start the resource connection
     * 
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#start(org.rhq.core.pluginapi.inventory.ResourceContext)
     */
    public void start(ResourceContext context) throws InvalidPluginConfigurationException {
        resourceContext = context;
        Configuration conf = context.getPluginConfiguration();
        String tmp = conf.getSimpleValue("ones", "1");
        int wantedOnes = Integer.parseInt(tmp);
        if (wantedOnes < 1)
            throw new InvalidPluginConfigurationException("Ones must be > 0");
        tmp = conf.getSimpleValue("zeros", "1");
        int wantedZeros = Integer.parseInt(tmp);
        if (wantedZeros < 1)
            throw new InvalidPluginConfigurationException("Zeros must be > 0");

        wanted[0] = wantedZeros;
        wanted[1] = wantedOnes;

        tmp = conf.getSimpleValue("delay", "0");
        delay = Long.parseLong(tmp);
        if (delay < 0L)
            delay = 0L;
    }

    /**
     * Tear down the resource connection
     * 
     * @see org.rhq.core.pluginapi.inventory.ResourceComponent#stop()
     */
    public void stop() {
        // Nothing to do.
    }

    /**
     * Gather "measurement" data.
     * <p>
     * Measurement data is provided based on patterns or data series provided by plug-in configuration.
     * 
     * @see org.rhq.core.pluginapi.measurement.MeasurementFacet#getValues(org.rhq.core.domain.measurement.MeasurementReport,
     *      java.util.Set)
     */
    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> requests) throws Exception {

        if (delay > 0L) {
            Thread.sleep(delay * 1000L);
        }

        boolean flipMetrics = false;
        boolean flipTraits = false;
        for (MeasurementScheduleRequest request : requests) {
            String metricName = request.getName();

            if (metricName.startsWith("pattern")) {
                double value = Double.NaN;
                if (metricName.equals("pattern1")) {
                    value = number;
                    flipMetrics = true;
                } else if (metricName.equals("pattern2")) {
                    // pattern2
                    value = 1 - number;
                } else if (metricName.equals(METRIC_PATTERN_3)) {
                    List<Double> numberList = getNumberList();
                    if (numberList != null && numberList.size() > 0) {
                        if (pattern3Idx >= numberList.size()) {
                            pattern3Idx = 0;
                        }
                        value = numberList.get(pattern3Idx);
                        pattern3Idx++;
                    } else {
                        value = Double.NaN;
                    }
                }
                MeasurementDataNumeric datum = new MeasurementDataNumeric(request, value);
                report.addData(datum);
            } else if (metricName.startsWith("text")) {

                double value;
                if (metricName.equals("text1")) {
                    value = numberForTrait;
                    flipTraits = true;
                } else {
                    // text2
                    value = 1 - numberForTrait;
                }
                String traitValue = (value == 0) ? "red" : "green";

                MeasurementDataTrait datum = new MeasurementDataTrait(request, traitValue);
                report.addData(datum);
            }
        }
        if (flipMetrics) {
            count++;
            if (count >= wanted[number]) {
                // Flip over to the other number and reset the series.
                number = 1 - number;
                count = 0;
            }
        }
        if (flipTraits) {
            traitCount++;
            if (traitCount >= wanted[numberForTrait]) {
                // Flip over to the other number and reset the series.
                numberForTrait = 1 - numberForTrait;
                traitCount = 0;
            }
        }
    }

    /**
     * Get a list of numbers assigned to the <code>numberList</code> plug-in configuration property.
     * 
     * The list of values are parsed as doubles and returned as a <code>List&lt;Double&gt;</code> value.
     * 
     * @return the numbers assigned to the <code>numberList</code> connection setting
     */
    private List<Double> getNumberList() {
        PropertySimple prop = resourceContext.getPluginConfiguration().getSimple(PLUGIN_CONFIG_NUMBER_LIST);
        List<Double> numberList = new ArrayList<Double>();
        List<String> numberListStr = (prop != null) ? new ListPropertySimpleWrapper(prop).getValue()
            : new ArrayList<String>();
        // parse the numbers from the configuration into our numeric list object
        for (String numStr : numberListStr) {
            numberList.add(Double.parseDouble(numStr.trim()));
        }
        return numberList;
    }
}
