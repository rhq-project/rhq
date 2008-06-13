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
package org.rhq.plugins.apt;

import java.util.Collection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;

/**
 * @author Jason Dobies
 */
public class AptSourcesComponentTest {

    private AptSourcesComponent component = new AptSourcesComponent();

    private Configuration pluginConfiguration = new Configuration();

    private final Log log = LogFactory.getLog(this.getClass());

    @BeforeSuite
    public void initPluginConfiguration() throws Exception {
        pluginConfiguration.put(new PropertySimple("lenses-path", "/usr/local/share/augeas/lenses"));
        pluginConfiguration.put(new PropertySimple("root-path", "/"));
        pluginConfiguration.put(new PropertySimple("apt-sources-path", "/etc/apt/sources.list"));
        pluginConfiguration.put(new PropertySimple("augeas-apt-sources-path", "/files/etc/apt/sources.list/*"));
    }

    @Test
    public void loadResourceConfiguration() throws Exception {
        Configuration configuration = component.loadResourceConfiguration(pluginConfiguration);

        assert configuration != null : "Null configuration returned from load call";

        Collection<Property> allProperties = configuration.getProperties();

        assert allProperties.size() == 1 : "Incorrect number of properties found. Expected: 1, Found: " + allProperties.size();

        PropertyList entryList = (PropertyList)allProperties.iterator().next();

        for (Property property : entryList.getList()) {
            PropertyMap entry = (PropertyMap)property;

            Property typeProperty = entry.get("type");
            Property uriProperty = entry.get("uri");
            Property distributionProperty = entry.get("distribution");

            assert typeProperty != null : "Type was null in entry";
            assert uriProperty != null : "URI was null in entry";
            assert distributionProperty != null : "Distribution was null in entry";

            log.info("Type: " + ((PropertySimple)typeProperty).getStringValue());
            log.info("URI: " + ((PropertySimple)uriProperty).getStringValue());
            log.info("Distribution: " + ((PropertySimple)distributionProperty).getStringValue());
        }

    }
}
