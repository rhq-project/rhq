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

package org.rhq.plugins.sshd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.measurement.MeasurementFacet;
import org.rhq.core.pluginapi.util.ObjectUtil;
import org.rhq.core.system.AggregateProcessInfo;
import org.rhq.core.system.NetworkStats;
import org.rhq.core.system.ProcessInfo;
import org.rhq.plugins.augeas.Augeas;

/**
 * @author Greg Hinkle
 */
public class OpenSSHDComponent implements ResourceComponent, ConfigurationFacet, MeasurementFacet {

    private ResourceContext resourceContext;
    private AggregateProcessInfo processInfo;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        getSSHDProcess();
    }

    public void stop() {
    }

    public AvailabilityType getAvailability() {
        return processInfo.isRunning() ? AvailabilityType.UP : AvailabilityType.DOWN;
    }

    private void getSSHDProcess() {

        List<ProcessInfo> procs =
            resourceContext.getSystemInformation().getProcesses("process|basename|match=sshd,process|basename|nomatch|parent=sshd");

        if (procs.size() == 1) {
            this.processInfo = procs.get(0).getAggregateProcessTree();
        }
    }

    public Configuration loadResourceConfiguration() throws Exception {

        Configuration pluginConfiguration = resourceContext.getPluginConfiguration();

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

        Augeas augeas = new Augeas(rootPath, lensesPath);

        // Find out where to look for sshd configuration files
        PropertySimple sshdPathProperty = pluginConfiguration.getSimple("config-directory");

        if (sshdPathProperty == null) {
            throw new Exception("SSHD configuration path not found in plugin configuration, cannot retrive configuration");
        }

        String sshdPath = sshdPathProperty.getStringValue();

        // Usage of this value expects it to end with a slash, so make sure it's here
        if (!sshdPath.endsWith("/")) {
            sshdPath += "/";
        }

        List<String> matches = augeas.match(sshdPath + "*");
        if (matches.size() == 0) {
            throw new Exception("Unable to load sshd_config data from augeas");
        }

        ConfigurationDefinition resourceConfigurationDefinition =
            resourceContext.getResourceType().getResourceConfigurationDefinition();
        Collection<PropertyDefinition> properties = resourceConfigurationDefinition.getPropertyDefinitions().values();

        Configuration config = new Configuration();
        config.setNotes("Loaded from Augeas at " + new Date());

        for (PropertyDefinition p : properties) {
            if (p instanceof PropertyDefinitionSimple) {
                PropertyDefinitionSimple property = (PropertyDefinitionSimple) p;
                String value = augeas.get(sshdPath + property.getName());

                if (value == null)
                    continue;

                if (property.getType() == PropertySimpleType.BOOLEAN) {
                    config.put(new PropertySimple(property.getName(), value.equalsIgnoreCase("yes")));
                } else {
                    config.put(new PropertySimple(property.getName(), value));
                }
            } else if (p instanceof PropertyDefinitionList) {
                // This very hackish bit of code is to suport the list-of-maps standard with a single simple definition in the map
                PropertyDefinitionList listDef = ((PropertyDefinitionList) p);
                PropertyDefinitionMap mapDef = ((PropertyDefinitionMap) listDef.getMemberDefinition());
                PropertyDefinitionSimple simpleDef = (PropertyDefinitionSimple) mapDef.getPropertyDefinitions().values().iterator().next();
                String name = simpleDef.getName();

                List<String> allValues = new ArrayList<String>();

                List<String> tests = augeas.match(sshdPath + "*");
                for (String test : tests) {
                    if (test.matches(sshdPath + name + ".*")) {
                        String data = augeas.get(test);
                        allValues.addAll(Arrays.asList(data.split(" ")));
                    }
                }
                PropertyList list = new PropertyList(listDef.getName());
                for (String value : allValues) {
                    PropertyMap map = new PropertyMap(mapDef.getName(), new PropertySimple(simpleDef.getName(), value));
                    list.add(map);
                }
            }
        }

        return config;
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
    }

    public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> metrics) throws Exception {
        NetworkStats stats = resourceContext.getSystemInformation().getNetworkStats("localhost", 22);
        processInfo.refresh();
        for (MeasurementScheduleRequest request : metrics) {
            if (request.getName().startsWith("NetworkStat.")) {
                int val = stats.getByName(request.getName().substring("NetworkStat.".length()));
                report.addData(new MeasurementDataNumeric(request, (double) val));
            } else if (request.getName().startsWith("Process.")) {
                Double value = ObjectUtil.lookupDeepNumericAttributeProperty(processInfo, request.getName().substring("Process.".length()));
                report.addData(new MeasurementDataNumeric(request, value));
            }
        }
    }
}
