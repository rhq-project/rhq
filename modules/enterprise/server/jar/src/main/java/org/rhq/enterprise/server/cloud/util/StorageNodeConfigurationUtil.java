/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.cloud.util;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.cloud.StorageNode;
import org.rhq.core.domain.cloud.StorageNodeConfigurationComposite;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertySimple;

/**
 * @author Michael Burman
 */
public class StorageNodeConfigurationUtil {

    private static final Log log = LogFactory.getLog(StorageNodeConfigurationUtil.class);

    public static final String RHQ_STORAGE_JMX_PORT_PROPERTY = "jmxPort";
    public static final String RHQ_STORAGE_HEAP_MAX_PROPERTY = "maxHeapSize";
    public static final String RHQ_STORAGE_HEAP_MIN_PROPERTY = "minHeapSize";
    public static final String RHQ_STORAGE_HEAP_NEW_PROPERTY = "heapNewSize";
    public static final String RHQ_STORAGE_THREAD_STACK_PROPERTY = "threadStackSize";

    public static final String RHQ_STORAGE_SAVED_CACHES_PROPERTY = "SavedCachesLocation";
    public static final String RHQ_STORAGE_COMMIT_LOG_PROPERTY = "CommitLogLocation";
    public static final String RHQ_STORAGE_DATA_FILE_PROPERTY = "AllDataFileLocations";
    public static final String RHQ_STORAGE_DATA_FILE_DIR_PROPERTY = "directory";

    public static final String RHQ_STORAGE_CONNECTOR_PROPERTY = "connectorAddress";
    public static final String RHQ_STORAGE_NOTIFY_DIR_CHANGE_PROPERTY = "dataDirectoriesChanged";

    public static StorageNodeConfigurationComposite createCompositeConfiguration(Configuration storageNodeConfiguration, Configuration storageNodePluginConfiguration, StorageNode storageNode) {
        StorageNodeConfigurationComposite configuration = new StorageNodeConfigurationComposite(storageNode);
        configuration.setHeapSize(storageNodeConfiguration.getSimpleValue(RHQ_STORAGE_HEAP_MAX_PROPERTY));
        configuration.setHeapNewSize(storageNodeConfiguration.getSimpleValue(RHQ_STORAGE_HEAP_NEW_PROPERTY));
        configuration.setThreadStackSize(storageNodeConfiguration.getSimpleValue(RHQ_STORAGE_THREAD_STACK_PROPERTY));
        configuration.setCommitLogLocation(storageNodeConfiguration.getSimpleValue(RHQ_STORAGE_COMMIT_LOG_PROPERTY));
        configuration.setSavedCachesLocation(storageNodeConfiguration.getSimpleValue(RHQ_STORAGE_SAVED_CACHES_PROPERTY));
        PropertyList allDataFileLocations = storageNodeConfiguration.getList(RHQ_STORAGE_DATA_FILE_PROPERTY);
        if(allDataFileLocations != null) {
            List<String> dataDirectories = new LinkedList<String>();
            for (Property property : allDataFileLocations.getList()) {
                PropertySimple dataFileLocation = (PropertySimple) property;
                dataDirectories.add(dataFileLocation.getStringValue());
            }
            configuration.setDataLocations(dataDirectories);
        }

        configuration.setJmxPort(Integer.parseInt(storageNodePluginConfiguration
                .getSimpleValue(RHQ_STORAGE_JMX_PORT_PROPERTY)));

        return configuration;
    }

    public static void updateValuesToConfiguration(StorageNodeConfigurationComposite storageNodeConfigurationComposite, Configuration storageNodeConfiguration) {
        storageNodeConfiguration.setSimpleValue(RHQ_STORAGE_JMX_PORT_PROPERTY, Integer.toString(storageNodeConfigurationComposite.getJmxPort()));
        storageNodeConfiguration.setSimpleValue(RHQ_STORAGE_HEAP_MAX_PROPERTY, storageNodeConfigurationComposite.getHeapSize());
        storageNodeConfiguration.setSimpleValue(RHQ_STORAGE_HEAP_NEW_PROPERTY, storageNodeConfigurationComposite.getHeapNewSize());

        storageNodeConfiguration.setSimpleValue(RHQ_STORAGE_THREAD_STACK_PROPERTY, storageNodeConfigurationComposite.getThreadStackSize());
        storageNodeConfiguration.setSimpleValue(RHQ_STORAGE_SAVED_CACHES_PROPERTY, storageNodeConfigurationComposite.getSavedCachesLocation());
        storageNodeConfiguration.setSimpleValue(RHQ_STORAGE_COMMIT_LOG_PROPERTY, storageNodeConfigurationComposite.getCommitLogLocation());
        storageNodeConfiguration.put(getAllDataFileLocationsProperties(storageNodeConfigurationComposite.getDataLocations()));
    }

    public static PropertyList getAllDataFileLocationsProperties(List<String> fileLocations) {
        return getPropertyList(RHQ_STORAGE_DATA_FILE_PROPERTY, RHQ_STORAGE_DATA_FILE_DIR_PROPERTY, fileLocations);
    }

    /*
    org.rhq.enterprise.server.rest.helper.ConfigurationHelper has similar method, getPropertyList(String propertyName, List<Object> objects);
    Merge these two?
     */
    private static PropertyList getPropertyList(String listName, String propertyName, List<? extends Object> objects) {
        PropertyList propertyList = new PropertyList(listName);
        for(Object o : objects) {
            propertyList.add(new PropertySimple(propertyName, o));
        }
        return propertyList;
    }

    /**
     * If new value is null, replace that with existing configuration value
     * @param newConfig
     * @param oldConfig
     */
    public static void syncConfigs(StorageNodeConfigurationComposite newConfig, StorageNodeConfigurationComposite oldConfig) {
        try {
            for (Field field : StorageNodeConfigurationComposite.class.getDeclaredFields()) {
                field.setAccessible(true);
                if(field.isAccessible() && field.getType() != StorageNode.class) {
                    Object o = field.get(newConfig);
                    if(o == null) {
                        Object oldValue = field.get(oldConfig);
                        if(oldValue != null) {
                            field.set(newConfig, oldValue);
                        }
                    }
                }
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not process StorageNodeConfigurationComposite, ", e);
        }
    }
}
