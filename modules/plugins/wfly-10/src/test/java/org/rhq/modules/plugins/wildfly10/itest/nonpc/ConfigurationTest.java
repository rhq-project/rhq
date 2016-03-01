/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.modules.plugins.wildfly10.itest.nonpc;

import static org.rhq.modules.plugins.wildfly10.test.util.ASConnectionFactory.getDomainControllerASConnection;

import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.modules.plugins.wildfly10.ConfigurationLoadDelegate;
import org.rhq.modules.plugins.wildfly10.ConfigurationWriteDelegate;
import org.rhq.modules.plugins.wildfly10.json.Address;

/**
 * Tests related to configurations
 *
 * @author Heiko W. Rupp
 */
public class ConfigurationTest extends AbstractIntegrationTest {

    @Override
    @BeforeSuite
    void loadPluginDescriptor() throws Exception {
        super.loadPluginDescriptor();
    }

    // TODO: Re-enable if appropriate.
    @Test(enabled = false)
    public void readSocketBindings() throws Exception {
        ConfigurationDefinition configDef = loadServiceDescriptor("SocketBindingGroupDomain");

        Address a = new Address("socket-binding-group=standard-sockets");
        ConfigurationLoadDelegate loadDelegate = new ConfigurationLoadDelegate(configDef,
            getDomainControllerASConnection(), a);
        Configuration conf = loadDelegate.loadResourceConfiguration();

        assert conf != null : "Did not get a configuration back";
        PropertySimple ps = conf.getSimple("default-interface");
        assert ps != null : "No property 'default-interface' found";
        String tmp = ps.getStringValue();
        assert tmp != null;
        assert tmp.equals("public");
        PropertyList pl = conf.getList("*");
        assert pl != null;
        List<Property> listContent = pl.getList();
        assert !listContent.isEmpty() : "List '*' has no elements";
        assert listContent.get(0) instanceof PropertyMap : "List content is no map";
        for (Property p : listContent) {
            PropertyMap pm = (PropertyMap) p;
            Map<String, Property> pmap = pm.getMap();
            assert pmap.size() == 6;
            PropertySimple name = (PropertySimple) pmap.get("name");
            assert name != null;
            assert name.getStringValue() != null;
            if ("http".equals(name.getStringValue())) {
                PropertySimple portProp = (PropertySimple) pmap.get("port");
                assert portProp != null;
                assert portProp.getStringValue() != null;
                assert "8080".equals(portProp.getStringValue()) : "Http port property was not 8080, but "
                    + portProp.getStringValue();
            }
        }
    }

    // TODO: Re-enable if appropriate.
    @Test(enabled = false)
    public void readUpdateSocketBindings() throws Exception {
        ConfigurationDefinition configDef = loadServiceDescriptor("SocketBindingGroupDomain");

        Address a = new Address("socket-binding-group=standard-sockets");
        ConfigurationLoadDelegate loadDelegate = new ConfigurationLoadDelegate(configDef,
            getDomainControllerASConnection(), a);
        Configuration conf = loadDelegate.loadResourceConfiguration();

        assert conf != null : "Did not get a configuration back";

        PropertyList pl = conf.getList("*");
        assert pl != null;
        List<Property> listContent = pl.getList();
        assert !listContent.isEmpty() : "List '*' has no elements";
        assert listContent.get(0) instanceof PropertyMap : "List content is no map";
        for (Property p : listContent) {
            PropertyMap pm = (PropertyMap) p;
            Map<String, Property> pmap = pm.getMap();
            assert pmap.size() == 6;
            PropertySimple name = (PropertySimple) pmap.get("name");
            assert name != null;
            assert name.getStringValue() != null;
            if ("http".equals(name.getStringValue())) {
                PropertySimple portProp = (PropertySimple) pmap.get("port");
                assert portProp != null;
                assert portProp.getStringValue() != null;

                portProp.setStringValue("8081");
            }
            if ("https".equals(name.getStringValue())) {
                PropertySimple portProp = (PropertySimple) pmap.get("port");
                assert portProp != null;
                assert portProp.getStringValue() != null;

                portProp.setStringValue("9443");
            }
        }

        // We have changed http port to 8081 and https to 9443, lets write back

        ConfigurationWriteDelegate cwd = new ConfigurationWriteDelegate(configDef, getDomainControllerASConnection(), a);
        ConfigurationUpdateReport report = new ConfigurationUpdateReport(conf);
        cwd.updateResourceConfiguration(report);
        assert report.getStatus() == ConfigurationUpdateStatus.SUCCESS;

        // now check the result (and change back)

        Configuration conf2 = loadDelegate.loadResourceConfiguration();
        assert conf2 != null : "Did not get a configuration back";
        List<Property> pl2 = conf.getList("*").getList();
        for (Property p : pl2) {
            PropertyMap pm = (PropertyMap) p;
            Map<String, Property> pmap = pm.getMap();
            PropertySimple name = (PropertySimple) pmap.get("name");
            assert name != null;
            assert name.getStringValue() != null;
            if ("http".equals(name.getStringValue())) {
                PropertySimple portProp = (PropertySimple) pmap.get("port");
                assert portProp != null;
                assert portProp.getStringValue() != null;

                portProp.setStringValue("8080");
            }
            if ("https".equals(name.getStringValue())) {
                PropertySimple portProp = (PropertySimple) pmap.get("port");
                assert portProp != null;
                assert portProp.getStringValue() != null;

                portProp.setStringValue("8443");
            }
        }
        report = new ConfigurationUpdateReport(conf);
        cwd.updateResourceConfiguration(report);
        assert report.getStatus() == ConfigurationUpdateStatus.SUCCESS;
    }

}
