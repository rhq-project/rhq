/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.hardware;

import org.rhq.core.domain.measurement.*;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Greg Hinkle
 */
public class HardwareComponent implements ResourceComponent, MeasurementFacet {

    private ResourceContext context;
    private HardwareDiscoveryComponent.HardwareType type;
    private Map<String, Double> numericData = new HashMap<String, Double>();
    private Map<String, String> traitData = new HashMap<String, String>();


    public void start(ResourceContext resourceContext)
            throws InvalidPluginConfigurationException, Exception {

        this.type =
                HardwareDiscoveryComponent.HardwareType.valueOf(
                        resourceContext.getPluginConfiguration().getSimple("type").getStringValue());
        this.context = resourceContext;

        updateSmoltData();
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {


        switch (type) {
            case ACPI_IBM:
                updateACPIIBMValues();
        }

        for (MeasurementScheduleRequest request : metrics) {
            if (request.getDataType() == DataType.MEASUREMENT) {
                report.addData(new MeasurementDataNumeric(request, numericData.get(request.getName())));
            } else if (request.getDataType() == DataType.TRAIT) {
                report.addData(new MeasurementDataTrait(request, traitData.get(request.getName())));
            }
        }

    }


    public void updateACPIIBMValues() throws Exception {

        File file = new File("/proc/acpi/ibm/thermal");

        FileInputStream fis = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
        String line = reader.readLine();
        fis.close();
        Pattern p = Pattern.compile("^temperatures\\:\\s*(.*)$");
        Matcher m = p.matcher(line);
        m.matches();
        String[] numbers = m.group(1).split("\\s");

        // These really only match the T60 (would need to detect model and adjust to get proper data)
        // see: http://www.thinkwiki.org/wiki/Thermal_sensors
        numericData.put("cpuAverageTemperature", Double.valueOf(numbers[0]));
        numericData.put("gpusAverageTemperature", Double.valueOf(numbers[3]));
        numericData.put("batteriesAverageTemperature", Double.valueOf(numbers[4]));
        numericData.put("hddAverageTemperature", Double.valueOf(numbers[1]));


        file = new File("/proc/acpi/ibm/fan");

        fis = new FileInputStream(file);
        reader = new BufferedReader(new InputStreamReader(fis));
        p = Pattern.compile("^speed\\:\\s*(\\d*)$");
        while ((line = reader.readLine()) != null) {
            m = p.matcher(line);
            if (m.matches()) {
                numericData.put("fanAverageSpeed", Double.valueOf(m.group(1)));
            }
        }

        fis.close();
    }

    public void updateSmoltData() {

        ProcessExecution smolt = new ProcessExecution("/usr/bin/smoltSendProfile");
        smolt.setArguments(new String[]{"-p"});
        smolt.setCaptureOutput(true);
        smolt.setWaitForCompletion(4000);
        ProcessExecutionResults results = this.context.getSystemInformation().executeProcess(smolt);

        StringReader r = new StringReader(results.getCapturedOutput());
        BufferedReader br = new BufferedReader(r);
        String line = null;
        try {
            Pattern p = Pattern.compile("^\\s*([\\w\\s]*)\\:\\s*(.*)$");
            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    traitData.put("smolt." + m.group(1), m.group(2));
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws Exception {
        new HardwareComponent().updateACPIIBMValues();

        new HardwareComponent().updateSmoltData();

    }
}
