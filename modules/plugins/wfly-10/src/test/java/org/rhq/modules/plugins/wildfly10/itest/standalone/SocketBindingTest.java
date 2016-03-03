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

package org.rhq.modules.plugins.wildfly10.itest.standalone;

import java.util.Iterator;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.agent.PluginContainerException;
import org.rhq.core.clientapi.agent.configuration.ConfigurationUpdateRequest;
import org.rhq.core.clientapi.server.configuration.ConfigurationUpdateResponse;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.modules.plugins.wildfly10.itest.AbstractJBossAS7PluginTest;
import org.rhq.modules.plugins.wildfly10.test.util.Constants;
import org.rhq.test.arquillian.RunDiscovery;

/**
 * Test stuff around socket bindings.
 * This could actually also run for domain mode
 * @author Heiko W. Rupp
 */
@Test(groups = { "integration", "pc", "standalone" }, singleThreaded = true)
public class SocketBindingTest extends AbstractJBossAS7PluginTest {

    public static final ResourceType RESOURCE_TYPE = new ResourceType("SocketBindingGroup", Constants.PLUGIN_NAME,
        ResourceCategory.SERVICE, null);
    private static final String RESOURCE_KEY = "socket-binding-group=standard-sockets";
    private static final String PORT_EXPR = "port:expr";
    private static final String MULTICAST_PORT_EXPR = "multicast-port:expr";

    private Resource bindings;

    @Test(priority = 10)
    @RunDiscovery
    public void initialDiscoveryTest() throws Exception {
        Resource platform = validatePlatform();
        Resource serverResource = waitForResourceByTypeAndKey(platform, platform, Constants.STANDALONE_RESOURCE_TYPE,
            Constants.STANDALONE_RESOURCE_KEY);
        bindings = waitForResourceByTypeAndKey(platform, serverResource, RESOURCE_TYPE, RESOURCE_KEY);
    }

    @Test(priority = 11)
    public void loadBindings() throws Exception {

        loadConfig();
    }

    @Test(priority = 11)
    public void addBinding() throws Exception {

        Configuration configuration = loadConfig();
        PropertyMap map = new PropertyMap("binding");
        PropertySimple ps = new PropertySimple("name", "bla");
        map.put(ps);
        ps = new PropertySimple(PORT_EXPR, 12345);
        map.put(ps);
        ps = new PropertySimple("fixed-port", false);
        map.put(ps);
        ps = new PropertySimple(MULTICAST_PORT_EXPR, "${foo.bar.baz:12346}");
        map.put(ps);
        PropertyList pl = (PropertyList) configuration.get("*");
        int count = pl.getList().size();
        pl.add(map);

        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(1, configuration, getResource().getId());
        ConfigurationUpdateResponse response = pluginContainer.getConfigurationManager()
            .executeUpdateResourceConfigurationImmediately(request);
        assert response != null;
        assert response.getErrorMessage() == null : "Config add resulted in this error: " + response.getErrorMessage();
        assert response.getConfigurationUpdateId() == 1;

        configuration = loadConfig();
        pl = (PropertyList) configuration.get("*");
        //        assert pl.getList().size() == count+1 : "Got only " + pl.getList().size() + " items, expected "+ (count+1);
    }

    @Test(priority = 11)
    public void addModifyBinding() throws Exception {

        Configuration configuration = loadConfig();
        PropertyMap map = new PropertyMap("binding");
        PropertySimple ps = new PropertySimple("name", "bla2");
        map.put(ps);
        ps = new PropertySimple(PORT_EXPR, 12355);
        map.put(ps);
        ps = new PropertySimple("fixed-port", false);
        map.put(ps);
        ps = new PropertySimple(MULTICAST_PORT_EXPR, "${foo.bar.baz:12356}");
        map.put(ps);
        PropertyList pl = (PropertyList) configuration.get("*");
        pl.add(map);

        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(1, configuration, getResource().getId());
        ConfigurationUpdateResponse response = pluginContainer.getConfigurationManager()
            .executeUpdateResourceConfigurationImmediately(request);
        assert response != null;
        assert response.getErrorMessage() == null : "Config add resulted in this error: " + response.getErrorMessage();

        configuration = loadConfig();
        map = null;
        pl = (PropertyList) configuration.get("*");
        for (Property prop : pl.getList()) {
            PropertyMap pm = (PropertyMap) prop;
            PropertySimple ps2 = pm.getSimple("name");
            if (!ps2.getStringValue().equals("bla2"))
                continue;

            map = pm;
        }
        assert map != null : "Did not find 'bla2' in the returned config";
        map.put(new PropertySimple(PORT_EXPR, 22355));
        map.put(new PropertySimple("fixed-port", true));
        request = new ConfigurationUpdateRequest(2, configuration, getResource().getId());
        response = pluginContainer.getConfigurationManager().executeUpdateResourceConfigurationImmediately(request);
        assert response != null;
        assert response.getErrorMessage() == null : "Config update resulted in this error: "
            + response.getErrorMessage();

    }

    @Test(priority = 11)
    public void addRemoveBinding() throws Exception {

        Configuration configuration = loadConfig();
        PropertyMap map = new PropertyMap("binding");
        PropertySimple ps = new PropertySimple("name", "bla3");
        map.put(ps);
        ps = new PropertySimple(PORT_EXPR, 12365);
        map.put(ps);
        ps = new PropertySimple("fixed-port", false);
        map.put(ps);
        ps = new PropertySimple(MULTICAST_PORT_EXPR, "${foo.bar.baz:12366}");
        map.put(ps);
        PropertyList pl = (PropertyList) configuration.get("*");
        int count = pl.getList().size();
        pl.add(map);

        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(1, configuration, getResource().getId());
        ConfigurationUpdateResponse response = pluginContainer.getConfigurationManager()
            .executeUpdateResourceConfigurationImmediately(request);
        assert response != null;
        assert response.getErrorMessage() == null : "Config add resulted in this error: " + response.getErrorMessage();

        configuration = loadConfig();
        pl = (PropertyList) configuration.get("*");
        assert pl.getList().size() == count + 1;

        Iterator<Property> iter = pl.getList().iterator();
        while (iter.hasNext()) {
            Property prop = iter.next();
            PropertyMap pm = (PropertyMap) prop;
            if (pm.getSimpleValue("name", "xxx").equals("bla3")) {
                iter.remove();
                break;
            }
        }
        assert pl.getList().size() == count;

        request = new ConfigurationUpdateRequest(3, configuration, getResource().getId());
        response = pluginContainer.getConfigurationManager().executeUpdateResourceConfigurationImmediately(request);
        assert response != null;
        assert response.getErrorMessage() == null : "Property removal resulted in this error: "
            + response.getErrorMessage();
    }

    /** THIS ONE WILL NOT WORK IN DOMAIN MODE */
    @Test(priority = 11)
    public void changePortOffset() throws Exception {

        Configuration configuration = loadConfig();
        PropertySimple ps = configuration.getSimple("port-offset");
        String original = ps.getStringValue();

        configuration.put(new PropertySimple("port-offset", 0));

        ConfigurationUpdateRequest request = new ConfigurationUpdateRequest(1, configuration, getResource().getId());
        ConfigurationUpdateResponse response = pluginContainer.getConfigurationManager()
            .executeUpdateResourceConfigurationImmediately(request);
        assert response != null;
        assert response.getErrorMessage() == null : "Changing the port-offset resulted in this error: "
            + response.getErrorMessage();

        configuration = loadConfig();
        ps = configuration.getSimple("port-offset");
        assert ps != null;
        assert ps.getIntegerValue() != null;
        assert ps.getIntegerValue() == 0;

        configuration.put(new PropertySimple("port-offset", original));

        request = new ConfigurationUpdateRequest(2, configuration, getResource().getId());
        response = pluginContainer.getConfigurationManager().executeUpdateResourceConfigurationImmediately(request);
        assert response != null;
        assert response.getErrorMessage() == null : "Changing the port-offset back resulted in this error: "
            + response.getErrorMessage();

    }

    @NotNull
    private Configuration loadConfig() throws PluginContainerException {
        Configuration config = pluginContainer.getConfigurationManager().loadResourceConfiguration(
            getResource().getId());
        assert config != null;
        assert !config.getProperties().isEmpty();
        return config;
    }

    private Resource getResource() {

        return bindings;
    }

}
