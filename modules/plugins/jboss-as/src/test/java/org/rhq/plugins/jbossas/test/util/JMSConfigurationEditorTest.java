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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.plugins.jbossas.util.JMSConfigurationEditor;
import org.rhq.plugins.jbossas.util.XMLConfigurationEditor;

/**
 * @author Mark Spritzler
 */
public class JMSConfigurationEditorTest {
    private static final String JMS_QUEUE = "JMQ JMS Queue";

    private static final String JMS_TOPIC = "JMQ JMS Topic";

    private Log LOG = LogFactory.getLog(JMSConfigurationEditorTest.class);

    private static final String TEST_FILE_NAME = "jms.xml";

    private String[] dependsProperties = { "DestinationManager", "SecurityManager", "ExpiryDestination" };

    private String[] simpleAttributeProperties = { "JNDIName", "InMemory", "RedeliveryLimit", "RedeliveryDelay",
        "MessageCounterHistoryDayLimit", "MaxDepth", "RecoveryRetries" };
    private Map<String, String> values;

    private Map<String, String> securityConfs;

    private XMLConfigurationEditor xmlEditor;

    @BeforeClass
    public void setUp() {
        values = new HashMap<String, String>();
        values.put("InMemory", "false");
        values.put("RedeliveryLimit", "2");
        values.put("RedeliveryDelay", "1000");
        values.put("MessageCounterHistoryDayLimit", "5");
        values.put("MaxDepth", "10");
        values.put("DestinationManager", "jboss.mq:service=DestinationManager");
        values.put("SecurityManager", "jboss.mq:service=SecurityManager");
        values.put("ExpiryDestination", "jboss.mq.destination:service=Topic,name=testTopic");

        securityConfs = new HashMap<String, String>();
        securityConfs.put("roleName1", "guest");
        securityConfs.put("roleRead1", "true");
        securityConfs.put("roleWrite1", "false");
        securityConfs.put("roleCreate1", "false");
        securityConfs.put("roleName2", "admin");
        securityConfs.put("roleRead2", "true");
        securityConfs.put("roleWrite2", "true");
        securityConfs.put("roleCreate2", "true");
        securityConfs.put("roleName3", "user");
        securityConfs.put("roleRead3", "true");
        securityConfs.put("roleWrite3", "true");
    }

    @Test
    public void testLoadConfiguration() {
        String testName = "testLoadConfiguration";
        LOG.info(testName);

        String topicName = "testTopic";
        File xmlFile = getXmlFile(TEST_FILE_NAME);

        xmlEditor = new JMSConfigurationEditor(JMS_TOPIC);
        Configuration config = xmlEditor.loadConfiguration(xmlFile, topicName);
        assertLoadingOfConfiguration(config, topicName, topicName);
        //Now lets load a queue
    }

    @Test
    public void testDeleteComponent() {
        String testName = "testDeleteComponent";
        LOG.info(testName);

        String topicName = "deleteTopic";
        File xmlFile = getXmlFile(TEST_FILE_NAME);

        xmlEditor = new JMSConfigurationEditor(JMS_TOPIC);
        xmlEditor.deleteComponent(xmlFile, topicName);

        // Until this test only runs once in Maven, keep commented out so it passes, because
        // on the second run it will fail since the first run actually removed it from the file.
        Configuration config = xmlEditor.loadConfiguration(xmlFile, topicName);
        assert config == null : "configuration object should no longer exist";
    }

    @Test
    public void testCreateNewComponent() {
        String testName = "testCreateNewComponent";
        LOG.info(testName);

        File xmlFile = getXmlFile(TEST_FILE_NAME);
        String queueName = "createQueue";

        Configuration testQueueConfiguration = createConfiguration(queueName);
        ResourceType type1 = new ResourceType("queue", "jbossas", ResourceCategory.SERVICE, null);
        CreateResourceReport createReport1 = new CreateResourceReport(queueName, type1, null, testQueueConfiguration,
            null);
        createReport1.setStatus(CreateResourceStatus.IN_PROGRESS);

        xmlEditor = new JMSConfigurationEditor(JMS_QUEUE);
        xmlEditor.updateConfiguration(xmlFile, queueName, createReport1);

        Configuration config = xmlEditor.loadConfiguration(xmlFile, queueName);
        assert config != null : "configuration object should not be null";

        assertTestConfig(config, queueName);

        String topicName = "createTopic";
        Configuration testTopicConfiguration = createConfiguration(topicName);
        ResourceType type2 = new ResourceType("topic", "jbossas", ResourceCategory.SERVICE, null);
        CreateResourceReport createReport2 = new CreateResourceReport(topicName, type2, null, testTopicConfiguration,
            null);
        createReport2.setStatus(CreateResourceStatus.IN_PROGRESS);

        xmlEditor = new JMSConfigurationEditor(JMS_TOPIC);
        xmlEditor.updateConfiguration(xmlFile, topicName, createReport2);

        config = xmlEditor.loadConfiguration(xmlFile, topicName);
        assert config != null : "configuration object should not be null";

        assertTestConfig(config, topicName);
    }

    @Test
    public void testUpdateComponent() {
        String testName = "testCreateNewComponent";
        LOG.info(testName);

        String mBeanName = "updateTopic";
        File xmlFile = getXmlFile(TEST_FILE_NAME);

        xmlEditor = new JMSConfigurationEditor(JMS_TOPIC);
        Configuration config = xmlEditor.loadConfiguration(xmlFile, mBeanName);
        assertLoadingOfConfiguration(config, mBeanName, mBeanName);

        assert config != null : "Configuration should not be null loading updateTopic";
        config = createConfiguration(mBeanName);

        ConfigurationUpdateReport updateReport = new ConfigurationUpdateReport(config);
        xmlEditor.updateConfiguration(xmlFile, mBeanName, updateReport);

        config = xmlEditor.loadConfiguration(xmlFile, mBeanName);
        assert config != null : "configuration object should not be null";

        assertTestConfig(config, mBeanName);
    }

    private File getXmlFile(String fileName) {
        String path = "target//test-classes//" + fileName;
        return new File(path);
    }

    private void assertLoadingOfConfiguration(Configuration config, String resourceName, String testName) {
        assert config != null : "Unable to find Resource in " + testName;

        PropertySimple mBeanNameProperty = config.getSimple("MBeanName");

        assert mBeanNameProperty != null : "Unable to get JNDI Name property";
        assert resourceName != null;

        assert mBeanNameProperty.getStringValue().equals(resourceName) : "Loaded config from Create Resource test does not have the correct JNDIName value";
    }

    private Configuration createConfiguration(String resourceName) {
        Configuration config = new Configuration();
        PropertySimple mBeanName = config.getSimple("MBeanName");
        if (mBeanName == null) {
            mBeanName = new PropertySimple("MBeanName", resourceName);
        }

        config.put(mBeanName);

        PropertySimple type = new PropertySimple("type", "mbean");
        config.put(type);

        for (String simpleAttribute : simpleAttributeProperties) {
            String value = values.get(simpleAttribute);
            if (value != null) {
                PropertySimple property = new PropertySimple(simpleAttribute, value);
                config.put(property);
            }
        }

        for (String simpleDepends : dependsProperties) {
            String value = values.get(simpleDepends);
            if (value != null) {
                PropertySimple property = new PropertySimple(simpleDepends, value);
                config.put(property);
            }
        }

        PropertyList rolesList = new PropertyList("SecurityConf");
        for (int i = 0; i < 3; i++) {
            PropertyMap map = new PropertyMap("role");
            String name = "roleName" + (i + 1);
            String read = "roleRead" + (i + 1);
            String write = "roleWrite" + (i + 1);
            String create = "roleCreate" + (i + 1);
            String roleName = securityConfs.get(name);
            String roleRead = securityConfs.get(read);
            String roleWrite = securityConfs.get(write);
            String roleCreate = securityConfs.get(create);
            PropertySimple property1 = new PropertySimple("name", roleName);
            PropertySimple property2 = new PropertySimple("read", roleRead);
            PropertySimple property3 = new PropertySimple("write", roleWrite);
            if (i != 2) {
                PropertySimple property4 = new PropertySimple("create", roleCreate);
                map.put(property4);
            }

            map.put(property1);
            map.put(property2);
            map.put(property3);
            rolesList.add(map);
        }

        config.put(rolesList);

        return config;
    }

    private void assertTestConfig(Configuration configuration, String topicName) {
        Configuration testConfig = createConfiguration(topicName);
        Collection<Property> retreivedProperties = configuration.getProperties();
        Collection<Property> testProperties = testConfig.getProperties();
        assert testProperties.containsAll(retreivedProperties) : "Not sure if this will ever pass";
    }
}