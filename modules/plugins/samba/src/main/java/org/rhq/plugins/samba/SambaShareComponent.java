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
package org.rhq.plugins.samba;

import java.util.Date;
import java.util.List;
import java.util.Set;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;

/**
 * @author Greg Hinkle
 */
public class SambaShareComponent implements ResourceComponent<SambaServerComponent>, ConfigurationFacet,
    MeasurementFacet {

    private ResourceContext<SambaServerComponent> resourceContext;

    private static String[] PROPERTIES = { "path", "comment", "public", "browseable", "writable", "printable",
        "write list", "guest ok", "share modes", "printable", "valid users" };

    public void start(ResourceContext<SambaServerComponent> sambaServerComponentResourceContext)
        throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = sambaServerComponentResourceContext;
    }

    public void stop() {

    }

    public AvailabilityType getAvailability() {
        return null;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        String path = getAugeasPath();
        Augeas augeas = this.resourceContext.getParentResourceComponent().getAugeas();

        List<String> matches = augeas.match(path);

        // Parse out the properties
        Configuration configuration = new Configuration();
        configuration.setNotes("Loaded from Augeas at " + new Date());

        for (String prop : PROPERTIES) {
            String value = augeas.get(path + "/" + prop.replaceAll(" ", "\\\\ "));
            configuration.put(new PropertySimple(prop, value));
        }

        return configuration;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        try {
            String path = getAugeasPath();
            Augeas augeas = this.resourceContext.getParentResourceComponent().getAugeas();

            // Parse out the properties
            Configuration configuration = report.getConfiguration();
            for (String prop : PROPERTIES) {
                augeas.set(path + "/" + prop.replaceAll(" ", "\\\\ "), configuration.getSimpleValue(prop, ""));
            }
        } catch (Exception e) {
            report.setErrorMessageFromThrowable(e);
        }
    }

    private String getAugeasPath() throws Exception {
        SambaServerComponent serverComponent = this.resourceContext.getParentResourceComponent();

        Augeas augeas = serverComponent.getAugeas();

        String path = null;
        for (String p : augeas.match(serverComponent.getAugeasPath())) {
            if (this.resourceContext.getResourceKey().equals(augeas.get(p))) {
                path = p;
            }
        }

        return path;
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        ProcessExecution exec = new ProcessExecution("smbstatus");
        exec.setArguments(new String[] { "-S" });
        exec.setCaptureOutput(true);

        ProcessExecutionResults results = this.resourceContext.getSystemInformation().executeProcess(exec);

        String output = results.getCapturedOutput();

        String[] lines = output.split("\\n");
        boolean dataLines = false;
        int count = 0;
        for (String line : lines) {
            if (!dataLines && line.startsWith("--------------")) {
                dataLines = true;
                continue;
            }
            //            String[] row = line.split("\\s", 4);

            count++;
        }

    }
}
