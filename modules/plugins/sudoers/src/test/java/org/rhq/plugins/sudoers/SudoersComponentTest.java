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
package org.rhq.plugins.sudoers;

import java.util.Collection;

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
 * @author Partha Aji
 */
public class SudoersComponentTest {

    private SudoersComponent component = new SudoersComponent();

    private Configuration pluginConfiguration = new Configuration();

    private final Log log = LogFactory.getLog(this.getClass());

    @BeforeSuite
    public void initPluginConfiguration() throws Exception {
        pluginConfiguration.put(new PropertySimple("lenses-path", "/usr/local/share/augeas/lenses"));
        pluginConfiguration.put(new PropertySimple("root-path", "/tmp"));
        pluginConfiguration.put(new PropertySimple("sudoers-path", "/etc/sudoers"));
        pluginConfiguration.put(new PropertySimple("augeas-sudoers-path", "/files/etc/sudoers/spec[*]"));
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

        assert allProperties.size() == 1 : "Incorrect number of properties found. Expected: 1, Found: "
            + allProperties.size();

        PropertyList entryList = (PropertyList) allProperties.iterator().next();

        for (Property property : entryList.getList()) {
            PropertyMap entry = (PropertyMap) property;

            Property user = entry.get("user");
            Property host = entry.get("host");

            assert user != null : "IP was null in entry";
            assert host != null : "Canonical was null in entry";

            System.out.println(entry);

            log.info("USER: " + ((PropertySimple) user).getStringValue());
            log.info("host: " + ((PropertySimple) host).getStringValue());
        }

    }
}
