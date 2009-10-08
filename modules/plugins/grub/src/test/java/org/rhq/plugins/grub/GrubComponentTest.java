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
package org.rhq.plugins.grub;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Jason Dobies
 */
public class GrubComponentTest {

    private GrubComponent component = new GrubComponent();

    private Configuration pluginConfiguration = new Configuration();

    private final Log log = LogFactory.getLog(this.getClass());

    @BeforeSuite
    public void initPluginConfiguration() throws Exception {
        pluginConfiguration.put(new PropertySimple("lenses-path", "/usr/local/share/augeas/lenses"));
        pluginConfiguration.put(new PropertySimple("root-path", "/"));
        pluginConfiguration.put(new PropertySimple("grub-conf-path", "/etc/grub.conf"));
        pluginConfiguration.put(new PropertySimple("augeas-grub-path", "/files/etc/grub.conf/*"));
    }

    @Test
    public void loadResourceConfiguration() throws Exception {
        Configuration configuration;
        try {
            configuration = component.loadResourceConfiguration(pluginConfiguration);
        } catch (UnsatisfiedLinkError ule) {
            // Skip tests if augeas not available
            return;
        }

        assert configuration != null : "Null configuration returned from load call";

        Collection<Property> allProperties = configuration.getProperties();

        assert allProperties.size() == 2 : "Incorrect number of properties found. Expected: 2, Found: "
            + allProperties.size();

        Iterator<Property> propertyIterator = allProperties.iterator();

        // General properties
        PropertyMap generalProperties = (PropertyMap) propertyIterator.next();

        assert generalProperties != null : "General properties map was null";

        Map<String, Property> map = generalProperties.getMap();
        for (Property property : map.values()) {
            PropertySimple propertySimple = (PropertySimple) property;
            log.info(property.getName() + ": " + propertySimple.getStringValue());
        }

        // Kernel list
        PropertyList entryList = (PropertyList) propertyIterator.next();

        for (Property property : entryList.getList()) {
            PropertyMap entry = (PropertyMap) property;

            Property titleProperty = entry.get("title");
            Property rootProperty = entry.get("root");
            Property kernelProperty = entry.get("kernel");
            Property initrdProperty = entry.get("initrd");

            assert titleProperty != null : "Title was null in entry";
            assert rootProperty != null : "Root was null in entry";
            assert kernelProperty != null : "Kernel was null in entry";
            assert initrdProperty != null : "Initrd was null in entry";

            log.info("Title: " + ((PropertySimple) titleProperty).getStringValue());
            log.info("Root: " + ((PropertySimple) rootProperty).getStringValue());
            log.info("Kernel: " + ((PropertySimple) kernelProperty).getStringValue());
            log.info("Initrd: " + ((PropertySimple) initrdProperty).getStringValue());
        }

    }
}
