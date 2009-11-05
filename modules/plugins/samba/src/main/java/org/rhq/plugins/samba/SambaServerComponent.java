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

import java.io.File;
import java.util.Date;
import java.util.List;

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;

/**
 * @author Greg Hinkle, shughes
 */
public class SambaServerComponent implements ResourceComponent, ConfigurationFacet {

    private ResourceContext resourceContext;
    private File smbConfFile;
    //private ProcessInfo processInfo;
    //private static final int PORT = 445;

    private static final String[] GLOBAL_PROPS = { "workgroup", "server string", "security", "encrypt passwords",
        "load printers", "cups options" };

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;

        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();

        PropertySimple smbConfPathProperty = pluginConfiguration.getSimple("smb-path");

        if (smbConfPathProperty == null) {
            throw new InvalidPluginConfigurationException(
                "Hosts path not found in the plugin configuration, cannot start resource component");
        }

        String smbConfPath = smbConfPathProperty.getStringValue();

        smbConfFile = new File(smbConfPath);

        if (!smbConfFile.exists()) {
            throw new InvalidPluginConfigurationException("smb.conf file not found at specified location: "
                + smbConfPath);
        }

        //getProcess();
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        if (smbConfFile == null) {
            return AvailabilityType.DOWN;
        }

        return smbConfFile.exists() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    public Configuration loadResourceConfiguration() throws Exception {
        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();

        return loadResourceConfiguration(pluginConfiguration);
    }

    public Configuration loadResourceConfiguration(Configuration pluginConfiguration) throws Exception {

        Augeas augeas = getAugeas();
        String sambaAugPath = getAugeasPath();

        // Request data from augeas
        List<String> matches = augeas.match(sambaAugPath);
        if (matches.size() == 0) {
            throw new Exception("Unable to load hosts data from augeas:" + sambaAugPath);
        }

        String globalNode = matches.get(0);

        // Parse out the properties
        Configuration configuration = new Configuration();
        configuration.setNotes("Loaded from Augeas at " + new Date());

        for (String prop : GLOBAL_PROPS) {
            String value = augeas.get(globalNode + "/" + prop.replaceAll(" ", "\\\\ "));
            configuration.put(new PropertySimple(prop, value));
        }

        return configuration;
    }

    public String getAugeasPath() throws Exception {
        // Find out where to look for the hosts tree
        PropertySimple augeasTreeNodeProperty = this.resourceContext.getPluginConfiguration().getSimple(
            "augeas-smb-path");

        if (augeasTreeNodeProperty == null) {
            throw new Exception("Augeas tree node not specified for hosts, cannot retrive configuration");
        }

        String sambaAugPath = augeasTreeNodeProperty.getStringValue();

        return sambaAugPath;
    }

    public Augeas getAugeas() throws Exception {
        Configuration pluginConfiguration = this.resourceContext.getPluginConfiguration();
        // Gather data necessary to create the Augeas hook
        PropertySimple lensesPathProperty = pluginConfiguration.getSimple("lenses-path");

        if (lensesPathProperty == null) {
            throw new Exception("Lenses path not found in plugin configuration, cannot retrieve configuration");
        }

        PropertySimple rootPathProperty = pluginConfiguration.getSimple("root-path");

        if (rootPathProperty == null) {
            throw new Exception("Root path not found in plugin configuration, cannot retrieve configuration");
        }

        String lensesPath = lensesPathProperty.getStringValue();
        String rootPath = rootPathProperty.getStringValue();

        Augeas augeas = new Augeas(rootPath, lensesPath, Augeas.NONE);
        return augeas;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        try {
            Augeas augeas = getAugeas();
            String sambaAugPath = getAugeasPath();

            // Request data from augeas
            List<String> matches = augeas.match(sambaAugPath);
            if (matches.size() == 0) {
                throw new Exception("Unable to load hosts data from augeas");
            }

            String globalNode = null;

            for (String entyNode : matches) {
                if ("global".equals(augeas.get(entyNode))) {

                    globalNode = entyNode;
                }
            }

            Configuration configuration = report.getConfiguration();

            for (String prop : GLOBAL_PROPS) {
                augeas.set(globalNode + "/" + prop.replaceAll(" ", "\\\\ "), configuration.getSimpleValue(prop, ""));
            }

        } catch (Exception e) {
            throw new RuntimeException("Unable to save samba configuration", e);
        }
    }
    /*
    private void getProcess() {

        List<ProcessInfo> procs = resourceContext.getSystemInformation().getProcesses(
            "process|basename|match=smbd,process|basename|nomatch|parent=smbd");

        if (procs.size() == 1) {
            this.processInfo = procs.get(0).getAggregateProcessTree();
        }
    }
    */
    /*
        public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
            NetworkStats stats = resourceContext.getSystemInformation().getNetworkStats("localhost", PORT);
            processInfo.refresh();
            for (MeasurementScheduleRequest request : metrics) {
                if (request.getName().startsWith("NetworkStat.")) {
                    int val = stats.getByName(request.getName().substring("NetworkStat.".length()));
                    report.addData(new MeasurementDataNumeric(request, (double) val));
                } else if (request.getName().startsWith("Process.")) {
                    Double value = ObjectUtil.lookupDeepNumericAttributeProperty(processInfo, request.getName().substring(
                        "Process.".length()));
                    report.addData(new MeasurementDataNumeric(request, value));
                }
            }
        }
    */
}
