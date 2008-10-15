 /*
  * Jopr Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
  * All rights reserved.
  *
  * This program is free software; you can redistribute it and/or modify
  * it under the terms of the GNU General Public License, version 2, as
  * published by the Free Software Foundation, and/or the GNU Lesser
  * General Public License, version 2.1, also as published by the Free
  * Software Foundation.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License and the GNU Lesser General Public License
  * for more details.
  *
  * You should have received a copy of the GNU General Public License
  * and the GNU Lesser General Public License along with this program;
  * if not, write to the Free Software Foundation, Inc.,
  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
  */
package org.rhq.plugins.jbossas.test.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.plugins.jbossas.util.DatasourceConfigurationEditor;

/**
 * DatasourceConfigurationEditor Tester.
 *
 * @author  mspritzler
 * @since   1.0
 * @created June 21, 2007
 */
@Test(groups = "jboss-plugin")
public class DatasourceConfigurationEditorTest {
    private Log LOG = LogFactory.getLog(DatasourceConfigurationEditorTest.class);

    @BeforeClass
    public void setUp() {
    }

    @Test
    public void testLoadDatasource() {
        String testName = "testLoadDatasource";
        LOG.info(testName);
        String dsXmlFile = "loadDS.xml";
        String datasourceName = "loadingTestDS";
        File xmlFile = getDSXmlFile(dsXmlFile);

        // Load here will do the asserts
        Configuration config = DatasourceConfigurationEditor.loadDatasource(xmlFile, datasourceName);
        assertLoadingOfConfiguration(config, datasourceName, testName);
    }

    @Test(dependsOnMethods = "testLoadDatasource")
    public void testCreateDatasource() {
        String testName = "testCreateDatasource";
        LOG.info(testName);
        String dsXmlFile = "newDS.xml";
        String datasourceName = "NewDS";
        File xmlFile = getDSXmlFile(dsXmlFile);

        Configuration config = createConfiguration(new Configuration(), datasourceName);

        ResourceType type = new ResourceType("datasource", "jbossas", ResourceCategory.SERVICE, null);
        CreateResourceReport report = new CreateResourceReport(datasourceName, type, null, config, null);
        DatasourceConfigurationEditor.updateDatasource(xmlFile, datasourceName, report);

        config = DatasourceConfigurationEditor.loadDatasource(xmlFile, datasourceName);
        assertLoadingOfConfiguration(config, datasourceName, testName);

        // Now lets test creating a new datasource into an already existing xxx-ds.XMl file
        dsXmlFile = "loadDS.xml";
        datasourceName = "NewAndImprovedDS";
        xmlFile = getDSXmlFile(dsXmlFile);

        config = createConfiguration(new Configuration(), datasourceName);

        type = new ResourceType("datasource", "jbossas", ResourceCategory.SERVICE, null);
        report = new CreateResourceReport(datasourceName, type, null, config, null);
        DatasourceConfigurationEditor.updateDatasource(xmlFile, datasourceName, report);

        config = DatasourceConfigurationEditor.loadDatasource(xmlFile, datasourceName);

        assertLoadingOfConfiguration(config, datasourceName, testName);
    }

    @Test(dependsOnMethods = "testCreateDatasource")
    public void testUpdateDatasource() {
        String testName = "testUpdateDatasource";
        LOG.info(testName);

        String dsXmlFile = "updateDS.xml";
        String datasourceName = "UpdateDS";
        File xmlFile = getDSXmlFile(dsXmlFile);

        Configuration config = DatasourceConfigurationEditor.loadDatasource(xmlFile, datasourceName);
        assertLoadingOfConfiguration(config, datasourceName, testName);

        config = this.createConfiguration(config.deepCopy(), datasourceName);
        PropertySimple property = config.getSimple("max-pool-size");
        property.setIntegerValue(100);

        ConfigurationUpdateReport report = new ConfigurationUpdateReport(config);

        DatasourceConfigurationEditor.updateDatasource(xmlFile, datasourceName, report);

        assert (report.getStatus().equals(ConfigurationUpdateStatus.SUCCESS)) : "Update report status should be successful";

        config = DatasourceConfigurationEditor.loadDatasource(xmlFile, datasourceName);
        assertLoadingOfConfiguration(config, datasourceName, testName);

        Map<String, String> valuesToCheck = new HashMap<String, String>();
        valuesToCheck.put("max-pool-size", "100");
        assertConfigurationValues(config, valuesToCheck, testName);
    }

    @Test(dependsOnMethods = "testUpdateDatasource")
    public void testDeleteDatasource() {
        String testName = "testDeleteDatasource";
        LOG.info(testName);

        String dsXmlFile = "deleteDS.xml";
        String datasourceName = "deleteDS";
        File xmlFile = getDSXmlFile(dsXmlFile);

        Configuration config; // = DatasourceConfigurationEditor.loadDatasource(xmlFile, datasourceName);

        //@TODO this assert should be in the test, but since Maven is running these tests twice
        // The second pass through the test the config will be null because the first run through
        // already deleted the datasource
        //assert config != null: "configuration should not be null";

        DatasourceConfigurationEditor.deleteDataSource(xmlFile, datasourceName);

        config = DatasourceConfigurationEditor.loadDatasource(xmlFile, datasourceName);

        assert config == null : "configuration should be null if it was deleted";

        dsXmlFile = "delete2DS.xml";
        datasourceName = "delete2DS";
        xmlFile = getDSXmlFile(dsXmlFile);

        //config = DatasourceConfigurationEditor.loadDatasource(xmlFile, datasourceName);

        //@TODO this assert should be in the test, but since Maven is running these tests twice
        // The second pass through the test the config will be null because the first run through
        // already deleted the datasource
        //assert config != null: "configuration should not be null";

        DatasourceConfigurationEditor.deleteDataSource(xmlFile, datasourceName);

        config = DatasourceConfigurationEditor.loadDatasource(xmlFile, datasourceName);

        assert config == null : "configuration should be null if it was deleted";
    }

    public void testDeleteDatasourceAfterCreatingNew() throws Exception {
        String testName = "testCreateDatasource";
        LOG.info(testName);
        String dsXmlFile = "newDSForDeleteTest.xml";
        String datasourceName = "NewDSForDelete";
        File xmlFile = getDSXmlFile(dsXmlFile);

        Configuration config = createConfiguration(new Configuration(), datasourceName);

        ResourceType type = new ResourceType("datasource", "jbossas", ResourceCategory.SERVICE, null);
        CreateResourceReport report = new CreateResourceReport(datasourceName, type, null, config, null);
        DatasourceConfigurationEditor.updateDatasource(xmlFile, datasourceName, report);

        config = DatasourceConfigurationEditor.loadDatasource(xmlFile, datasourceName);
        assertLoadingOfConfiguration(config, datasourceName, testName);

        DatasourceConfigurationEditor.deleteDataSource(xmlFile, datasourceName);

        config = DatasourceConfigurationEditor.loadDatasource(xmlFile, datasourceName);

        assert config == null : "configuration should be null if it was deleted";
    }

    private File getDSXmlFile(String fileName) {
        String path = "target//test-classes//" + fileName;
        return new File(path);
    }

    private void assertLoadingOfConfiguration(Configuration config, String datasourceName, String testName) {
        assert config != null : "Unable to find Datasource in " + testName;
        PropertySimple jndiNameProperty = config.getSimple("jndi-name");

        assert jndiNameProperty != null : "Unable to get JNDI Name property";

        assert jndiNameProperty.getStringValue().equals(datasourceName) : "Loaded config from Create Datasource test does not have the correct jndi-name value";
    }

    private void assertConfigurationValues(Configuration config, Map<String, String> valuesToCheck, String testName) {
        for (String xmlTagString : DatasourceConfigurationEditor.COMMON_PROPS) {
            PropertySimple property = config.getSimple(xmlTagString);
            if (property != null) {
                String propertyName = property.getName();

                if (valuesToCheck.containsKey(propertyName)) {
                    String propertyValue = property.getStringValue();
                    String valueToCheck = valuesToCheck.get(propertyName);
                    assert valueToCheck.equals(propertyValue) : propertyName
                        + " is not set correctly in the configuration in test: " + testName;
                }
            }
        }
    }

    private Configuration createConfiguration(Configuration config, String datasourceName) {
        PropertySimple jndiName = config.getSimple("jndi-name");
        if (jndiName == null) {
            jndiName = new PropertySimple("jndi-name", datasourceName);
        }

        config.put(jndiName);

        PropertySimple type = config.getSimple("type");
        if (type == null) {
            type = new PropertySimple("type", "local-tx-datasource");
        }

        config.put(type);

        PropertySimple maxPoolSize = config.getSimple("max-pool-size");
        if (maxPoolSize == null) {
            maxPoolSize = new PropertySimple("max-pool-size", 100);
        }

        config.put(maxPoolSize);

        PropertyMap mapProperty = config.getMap("connection-property");
        if (mapProperty == null) {
            mapProperty = new PropertyMap("connection-property");
            Map<String, Property> map = new HashMap<String, Property>();
            mapProperty.setMap(map);
            config.put(mapProperty);
        }

        return config;
    }
}