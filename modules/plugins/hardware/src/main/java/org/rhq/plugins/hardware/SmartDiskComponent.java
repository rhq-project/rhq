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

import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.NativeSystemInfo;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.StringReader;
import java.io.BufferedReader;

/**
 * @author Greg Hinkle
 */
public class SmartDiskComponent implements ResourceComponent, MeasurementFacet {

    private ResourceContext context;
    private Map<String, Double> data = new HashMap<String, Double>();

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.context = resourceContext;
    }

    public void stop() {
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {

        updateData();
        for (MeasurementScheduleRequest request : metrics) {
            Double val = data.get(request.getName());
            if (val != null) {
                report.addData(new MeasurementDataNumeric(request, val));
            }
        }
    }

    public AvailabilityType getAvailability() {
        // TODO
        return AvailabilityType.UP;
    }

    public void updateData() throws Exception {
        String prefix = context.getPluginConfiguration().getSimple("prefix").getStringValue();
        String command = context.getPluginConfiguration().getSimple("command").getStringValue();


        ProcessExecution proc;
        if (prefix != null) {
            proc = new ProcessExecution(prefix);
            proc.setArguments(new String[]{command, "--attributes", context.getResourceKey()});
        } else {
            proc = new ProcessExecution(command);
            proc.setArguments(new String[]{"--attributes", context.getResourceKey()});
        }

        proc.setCaptureOutput(true);
        proc.setWaitForCompletion(4000);
        ProcessExecutionResults results = context.getSystemInformation().executeProcess(proc);

        StringReader r = new StringReader(results.getCapturedOutput());
        BufferedReader br = new BufferedReader(r);
        String line = null;
        try {
            String model = null;
            Pattern p = Pattern.compile("^\\s*\\d*\\s*(\\S*)\\s*\\p{XDigit}*\\s*\\S*\\s*\\S*\\s*\\S*\\s*\\S*\\s*\\S*\\s*\\S*\\s*\\S*\\s*(\\d*).*$");
            while ((line = br.readLine()) != null) {
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    try {
                        String name = m.group(1);
                        if (name != null && m.group(2) != null) {
                            Double val = Double.valueOf(m.group(2));
                            data.put(name, val);
                        }
                    } catch (Exception e) {
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws Exception {
        SmartDiskComponent sdc = new SmartDiskComponent();
        sdc.start(new ResourceContext(new Resource("/dev/sda", "foo", new ResourceType()), null, null, null, null, null, null, null, null, null));
        sdc.getValues(null, null);

    }

}
