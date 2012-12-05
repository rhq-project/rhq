/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2012 Red Hat, Inc.
 *  * All rights reserved.
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License, version 2, as
 *  * published by the Free Software Foundation, and/or the GNU Lesser
 *  * General Public License, version 2.1, also as published by the Free
 *  * Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License and the GNU Lesser General Public License
 *  * for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * and the GNU Lesser General Public License along with this program;
 *  * if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 */

package org.rhq.plugins.cassandra;

import java.io.File;
import java.util.Map;

import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.ddl.KeyspaceDefinition;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * @author John Sanda
 */
public class KeyspaceComponent implements ResourceComponent<ResourceComponent<?>>, ConfigurationFacet,
    JMXComponent<ResourceComponent<?>>, OperationFacet {

    private static final String STORAGE_SERVICE_BEAN = "org.apache.cassandra.db:type=StorageService";

    private static final String COMPACT_OPERATION = "forceTableCompaction";
    private static final String REPAIR_OPERATION = "forceTableRepair";

    private ResourceContext<ResourceComponent<?>> context;

    @Override
    public void start(ResourceContext<ResourceComponent<?>> context) throws Exception {
        this.context = context;
    }

    @Override
    public void stop() {
    }

    @Override
    public AvailabilityType getAvailability() {
        return context.getParentResourceComponent().getAvailability();
    }

    @Override
    public EmsConnection getEmsConnection() {
        JMXComponent<?> parent = (JMXComponent<?>) context.getParentResourceComponent();
        return parent.getEmsConnection();
    }

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        KeyspaceDefinition keyspaceDef = getKeyspaceDefinition();

        Configuration config = new Configuration();
        config.put(new PropertySimple("name", keyspaceDef.getName()));
        config.put(new PropertySimple("replicationFactor", keyspaceDef.getReplicationFactor()));
        config.put(new PropertySimple("strategyClass", keyspaceDef.getStrategyClass()));
        config.put(new PropertySimple("durableWrites", keyspaceDef.isDurableWrites()));

        PropertyList list = new PropertyList("strategyOptions");
        Map<String, String> strategyOptions = keyspaceDef.getStrategyOptions();
        for (String optionName : strategyOptions.keySet()) {
            PropertyMap map = new PropertyMap("strategyOptionsMap");
            map.put(new PropertySimple("strategyOptionName", optionName));
            map.put(new PropertySimple("strategyOptionValue", strategyOptions.get(optionName)));
            list.add(map);
        }
        config.put(list);
        config.put(this.getKeySpaceDataFileLocations());

        return config;
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        report.setStatus(ConfigurationUpdateStatus.NOCHANGE);
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (name.equals("repair")) {
            return repairKeyspace();
        } else if (name.equals("compact")) {
            return compactKeyspace();
        } else if (name.equals("takeSnapshot")) {
            return takeSnapshot(parameters);
        }

        OperationResult failedOperation = new OperationResult();
        failedOperation.setErrorMessage("Operation not implemented.");

        return failedOperation;
    }

    public OperationResult repairKeyspace(String... columnFamilies) {
        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        EmsOperation operation = emsBean.getOperation(REPAIR_OPERATION, String.class, boolean.class, String[].class);

        String keyspace = context.getResourceKey();
        if (columnFamilies == null) {
            columnFamilies = new String[] {};
        }
        operation.invoke(keyspace, true, columnFamilies);

        return new OperationResult();
    }

    public OperationResult compactKeyspace(String... columnFamilies) {
        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        EmsOperation operation = emsBean.getOperation(COMPACT_OPERATION, String.class, String[].class);

        String keyspace = context.getResourceKey();
        if (columnFamilies == null) {
            columnFamilies = new String[] {};
        }
        operation.invoke(keyspace, new String[] {});

        return new OperationResult();
    }

    public KeyspaceDefinition getKeyspaceDefinition() {
        return this.getThriftConnection().describeKeyspace(context.getResourceKey());
    }

    public OperationResult takeSnapshot(Configuration parameters, String... columnFamilies) {
        String keyspace = context.getResourceKey();
        String snapshotName = parameters.getSimpleValue("snapshotName");
        if (snapshotName == null || snapshotName.trim().isEmpty()) {
            snapshotName = System.currentTimeMillis() + "";
        }

        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        if (columnFamilies == null || columnFamilies.length == 0) {
            EmsOperation operation = emsBean.getOperation("takeSnapshot", String.class, String[].class);
            operation.invoke(snapshotName, new String[] { keyspace });
        } else {
            EmsOperation operation = emsBean.getOperation("takeColumnFamilySnapshot", String.class, String.class,
                String.class);

            for (String columnFamily : columnFamilies) {
                operation.invoke(keyspace, columnFamily, snapshotName);
            }
        }

        return new OperationResult();
    }

    public PropertyList getKeySpaceDataFileLocations() {
        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        EmsAttribute attribute = emsBean.getAttribute("AllDataFileLocations");

        PropertyList list = new PropertyList("keyspaceFileLocations");
        String[] dirs = (String[]) attribute.getValue();
        for (String dir : dirs) {
            if (!dir.endsWith("/")) {
                dir = dir + "/";
            }

            list.add(new PropertySimple("directory", dir + context.getResourceKey()));
        }

        return list;
    }

    public PropertySimple getCommitLogProperty() {
        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        EmsAttribute attribute = emsBean.getAttribute("CommitLogLocation");
        return new PropertySimple("CommitLogLocation", attribute.refresh());
    }

    public boolean clearCommitLog() {
        PropertySimple commitLogProperty = this.getCommitLogProperty();

        File commitLogFolder = new File(commitLogProperty.getStringValue());

        File[] commitLogFiles = commitLogFolder.listFiles();
        for (File file : commitLogFiles) {
            file.delete();
        }

        return true;
    }

    public CassandraNodeComponent getCassandraNodeComponent() {
        return (CassandraNodeComponent) this.context.getParentResourceComponent();
    }

    /**
     * Loads the bean with the given object name.
     *
     * Subclasses are free to override this method in order to load the bean.
     *
     * @param objectName the name of the bean to load
     * @return the bean that is loaded
     */
    protected EmsBean loadBean(String objectName) {
        EmsConnection emsConnection = getEmsConnection();

        if (emsConnection != null) {
            EmsBean bean = emsConnection.getBean(objectName);
            if (bean == null) {
                // In some cases, this resource component may have been discovered by some means other than querying its
                // parent's EMSConnection (e.g. ApplicationDiscoveryComponent uses a filesystem to discover EARs and
                // WARs that are not yet deployed). In such cases, getBean() will return null, since EMS won't have the
                // bean in its cache. To cover such cases, make an attempt to query the underlying MBeanServer for the
                // bean before giving up.
                emsConnection.queryBeans(objectName);
                bean = emsConnection.getBean(objectName);
            }

            return bean;
        }

        return null;
    }

    /**
     * Retrieves a cluster connection from the parent resource.
     *
     * @return the cluster connection.
     */
    public Cluster getThriftConnection() {
        CassandraNodeComponent parent = (CassandraNodeComponent) context.getParentResourceComponent();
        return parent.getThriftConnection();
    }
}
