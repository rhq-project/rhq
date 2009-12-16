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

import net.augeas.Augeas;

import org.rhq.core.domain.configuration.*;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.plugins.augeas.AugeasConfigurationComponent;
import org.rhq.plugins.augeas.helper.AugeasNode;


/**
 * TODO
 */
public class SambaServerComponent extends AugeasConfigurationComponent {
    static final String ENABLE_RECYCLING = "enableRecycleBin";
    private ResourceContext resourceContext;


    public void start(ResourceContext resourceContext) throws Exception {
        this.resourceContext = resourceContext;
        super.start(resourceContext);
        updateSmbAds(resourceContext);
    }

    public void stop() {
        super.stop();
    }

    public AvailabilityType getAvailability() {
        return super.getAvailability();
    }

    public Configuration loadResourceConfiguration() throws Exception {
        return super.loadResourceConfiguration();
    }

    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        super.updateResourceConfiguration(report);
    }

    @Override
    public CreateResourceReport createResource(CreateResourceReport reportIn) {
        CreateResourceReport report = reportIn;
        Configuration config = report.getResourceConfiguration();
        String name = config.getSimple(SambaShareComponent.NAME_RESOURCE_CONFIG_PROP).getStringValue();
        report.setResourceKey(name);
        report.setResourceName(name);
        return super.createResource(report);
    }

    @Override
    protected String getChildResourceConfigurationRootPath(ResourceType resourceType, Configuration resourceConfig) {
        if (resourceType.getName().equals(SambaShareComponent.RESOURCE_TYPE_NAME)) {
            String targetName = resourceConfig.getSimple(SambaShareComponent.NAME_RESOURCE_CONFIG_PROP)
                .getStringValue();
            return "/files/etc/samba/smb.conf/target[.='" + targetName + "']";
        } else {
            throw new IllegalArgumentException("Unsupported child Resource type: " + resourceType);
        }
    }

    @Override
    protected String getChildResourceConfigurationRootLabel(ResourceType resourceType, Configuration resourceConfig) {
        if (resourceType.getName().equals(SambaShareComponent.RESOURCE_TYPE_NAME)) {
            return resourceConfig.getSimple(SambaShareComponent.NAME_RESOURCE_CONFIG_PROP).getStringValue();
        } else {
            throw new IllegalArgumentException("Unsupported child Resource type: " + resourceType);
        }
    }

    @Override
    protected void setNodeFromPropertySimple(Augeas augeas, AugeasNode node, PropertyDefinitionSimple propDefSimple,
        PropertySimple propSimple) {
        if (ENABLE_RECYCLING.equals(propDefSimple.getName())) {
            if (propSimple.getBooleanValue()) {
                String path = node.getParent().getPath();
                augeas.set(path + "/vfs\\ objects", "recycle");
                augeas.set(path + "/recycle:repository", ".recycle");
                augeas.set(path + "/recycle:keeptree", "yes");
                augeas.set(path + "/recycle:versions", "yes");
            } else {
                String path = node.getParent().getPath();
                augeas.remove(path + "/vfs\\ objects");
                augeas.remove(path + "/recycle:repository");
                augeas.remove(path + "/recycle:keeptree");
                augeas.remove(path + "/recycle:versions");
            }
        } else {
            super.setNodeFromPropertySimple(augeas, node, propDefSimple, propSimple);
        }
    }

    protected Object toPropertyValue(PropertyDefinitionSimple propDefSimple, Augeas augeas, AugeasNode node) {
        if (ENABLE_RECYCLING.equals(propDefSimple.getName())) {
            return "recycle".equals(augeas.get(node.getParent().getPath() + "/vfs\\ objects"));
        }
        return super.toPropertyValue(propDefSimple, augeas, node);
    }

    private void updateSmbAds(ResourceContext resourceContext) {
        StringBuilder args = new StringBuilder();
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();

        String realm = pluginConfig.getSimple("realm").getStringValue();
        String controller = pluginConfig.getSimple("controller").getStringValue();
        String domain = pluginConfig.getSimple("domain").getStringValue();

        args.append("--smbservers=\"" + controller + "\" ");
        args.append("--smbrealm=\"" + realm + "\" ");
        args.append("--enablewinbind --smbsecurity=\"ads\" ");

        args.append("--smbidmapuid=\"15000-20000\" --smbidmapgid=\"15000-20000\" ");
        args.append("--winbindtemplateshell=\"/bin/bash\" ");
        args.append("--update");

        executeExecutable(args.toString(), 1000L, true);
    }

    private ProcessExecutionResults executeExecutable(String args, long wait, boolean captureOutput)
        throws InvalidPluginConfigurationException {

        SystemInfo sysInfo = this.resourceContext.getSystemInformation();
        Configuration pluginConfig = this.resourceContext.getPluginConfiguration();
        ProcessExecutionResults results = executeExecutable(sysInfo, pluginConfig, args, wait, captureOutput);

        return results;
    }

    protected ProcessExecutionResults executeExecutable(SystemInfo sysInfo, Configuration pluginConfig,
        String args, long wait, boolean captureOutput) throws InvalidPluginConfigurationException {

        ProcessExecution processExecution = getProcessExecutionInfo(pluginConfig);
        if (args != null) {
            processExecution.setArguments(args.split(" "));
        }
        processExecution.setCaptureOutput(captureOutput);
        processExecution.setWaitForCompletion(wait);
        processExecution.setKillOnTimeout(true);

        ProcessExecutionResults results = sysInfo.executeProcess(processExecution);

        return results;
   }

    private ProcessExecution getProcessExecutionInfo(Configuration pluginConfig)
        throws InvalidPluginConfigurationException {

        String executable = "/usr/bin/authconfig";

        ProcessExecution processExecution = new ProcessExecution(executable);

        return processExecution;
    }

}
