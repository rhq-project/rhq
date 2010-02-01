/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.services;

import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freedesktop.dbus.DBusConnection;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.services.ServiceManager;

/**
 * The ResourceComponent for the "Services File" ResourceType.
 *
 * @author Partha Aji
 */
public class ServicesComponent<T extends ResourceComponent> implements ResourceComponent<T>, ConfigurationFacet {

    private final Log log = LogFactory.getLog(this.getClass());
    private ResourceContext<T> resourceContext;
    private String resourceDescription;
    private List<String> sysVServices;
    private List<String> xinetdServices;

    public void start(ResourceContext resourceContext) throws InvalidPluginConfigurationException, Exception {
        this.resourceContext = resourceContext;
        this.resourceDescription = this.resourceContext.getResourceType() + " Resource with key ["
            + this.resourceContext.getResourceKey() + "]";
    }

    public void stop() {
        return;
    }

    public AvailabilityType getAvailability() {
        return AvailabilityType.UP;
    }

    private void setupServices() throws Exception {
        DBusConnection conn = null;
        try {
            conn = DBusConnection.getConnection(DBusConnection.SYSTEM);
            sysVServices = ServiceManager.instance().listSysVServices(conn);
            xinetdServices = ServiceManager.instance().listXinetdServices(conn);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public List<String> getSysVServices() throws Exception {
        if (sysVServices == null) {
            setupServices();
        }
        return sysVServices;
    }

    public List<String> getXinetDServices() throws Exception {
        if (xinetdServices == null) {
            setupServices();
        }
        return xinetdServices;
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        Configuration resourceConfig = new Configuration();
        resourceConfig.setNotes("Loaded at " + new Date());
        PropertyList entriesProp = new PropertyList("services");
        resourceConfig.put(entriesProp);

        for (String name : getSysVServices()) {
            PropertyMap entryProp = new PropertyMap("service");
            entriesProp.add(entryProp);
            entryProp.put(new PropertySimple("name", name));
            entryProp.put(new PropertySimple("type", "Sys V Service"));
        }

        for (String name : getXinetDServices()) {
            PropertyMap entryProp = new PropertyMap("service");
            entriesProp.add(entryProp);
            entryProp.put(new PropertySimple("name", name));
            entryProp.put(new PropertySimple("type", "XinetD Service"));
        }
        return resourceConfig;

    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        /**
         *         Configuration resourceConfig = report.getConfiguration();                
                Services newServices = new Services();

                PropertyList entriesProp = resourceConfig.getList(".");
                for (Property entryProp: entriesProp.getList()) {
                    PropertyMap entryPropMap = (PropertyMap) entryProp;
                    String ipAddress = entryPropMap.getSimple("ipaddr").getStringValue();
                    String canonicalName = entryPropMap.getSimple("canonical").getStringValue();
                    String aliases = entryPropMap.getSimpleValue("alias", null);
                    Set<String> aliasSet;
                    if (aliases == null) {
                        aliasSet = null;
                    } else {
                        String[] aliasArray = aliases.trim().split("\\s+");
                        aliasSet = new LinkedHashSet<String>(aliasArray.length);
                        for (String alias : aliasArray) {
                            aliasSet.add(alias);
                        }
                    }
                    ServicesEntry entry = new ServicesEntry(ipAddress, canonicalName, aliasSet);
                    newServices.addEntry(entry);
                }

                File servicesFile = (File)this.servicesComponent.getConfigurationFiles().get(0);
                try {
                    Services.store(newServices, servicesFile);
                }
                catch (IOException e) {
                    throw new RuntimeException("Failed to write services file [" + servicesFile + "].", e);
                }

                report.setStatus(ConfigurationUpdateStatus.SUCCESS);
                return;
         */

    }
}
