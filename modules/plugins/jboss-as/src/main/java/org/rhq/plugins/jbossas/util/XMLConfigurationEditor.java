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
package org.rhq.plugins.jbossas.util;

import java.io.File;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;

/**
 * @author Mark Spritzer
 */
public interface XMLConfigurationEditor {
    // Constants
    public static final String DATASOURCE_ROOT_ELEMENT = "datasources";
    public static final String JMS_ROOT_ELEMENT = "server";

    // Resource subtypes

    public static final String DATASOURCE_MBEAN_NAME = "jboss.jca:service=DataSourceBinding";
    public static final String CONNECTION_MBEAN_NAME = "jboss.jca:service=ManagedConnectionPool";
    public static final String NO_TX_TYPE = "no-tx-datasource";
    public static final String LOCAL_TX_TYPE = "local-tx-datasource";
    public static final String XA_TX_TYPE = "xa-tx-datasource";

    public static final String JMQ_TOPIC_CODE = "org.jboss.mq.server.jmx.Topic";
    public static final String JMQ_QUEUE_CODE = "org.jboss.mq.server.jmx.Queue";

    // Adapter Types from Config To XML
    public static final int MAP_DEFAULT = 1;
    public static final int MAP_SUBTAG = 2;
    public static final int MAP_ATTRIBUTE = 3;
    public static final int LIST_DEFAULT = 4;
    public static final int LIST_SUBTAG = 5;
    public static final int LIST_ATTRIBUTE = 6;
    public static final int SIMPLE_DEFAULT = 7;
    public static final int SIMPLE_SUBTAG = 8;
    public static final int SIMPLE_ATTRIBUTE = 9;

    /**
     * This method loads an XML file and converts it into a @see Configuration object
     *
     * @param  file @see File object for the xml file to read
     * @param  name name of the component to retrieve from the xml file
     *
     * @return Configuration the Configuration object filled in with all the properties from the xml file for component
     */
    public Configuration loadConfiguration(File file, String name);

    /**
     * This method will save a Configuration object into the xml file supplied where the component already existing. The
     * @see ConfigurationUpdateReport will be updated with the success or failure of the update. If there is a failure
     * an error message will be added to the report.
     *
     * @param deploymentFile @see File object for the xml file to updated
     * @param name           name of the component in the xml file to save
     * @param report         the Plugin's @see ConfigurationUpdateReport that holds the new configuration.
     */
    public void updateConfiguration(File deploymentFile, String name, ConfigurationUpdateReport report);

    /**
     * This method will save a Configuration object into the xml file supplied where the component does not already
     * existing. This means this is a new Resource being created, the file may or may not already exist. If it doesn't
     * exist a new file is created, if it does, the configuration will be appended to the file in the appropriate place.
     * The @see CreateResourceReport will be updated with the success or failure of the update. If there is a failure an
     * error message will be added to the report.
     *
     * @param deploymentFile @see File object for the xml file to created or updated
     * @param name           name of the component for the xml file to save
     * @param report         the Plugin's @see CreateResourceReport that holds the new configuration.
     */
    public void updateConfiguration(File deploymentFile, String name, CreateResourceReport report);

    /**
     * This method will delete a configuration for a component from an xml file.
     *
     * @param deploymentFile @see File object for the xml file to delete from
     * @param name           name of the component in the xml file to delete
     */
    public void deleteComponent(File deploymentFile, String name);
}