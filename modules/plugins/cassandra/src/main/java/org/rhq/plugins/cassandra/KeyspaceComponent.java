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

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

import java.io.File;
import java.util.HashMap;

import com.datastax.driver.core.Query;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.QueryBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.rhq.plugins.cassandra.util.KeyspaceService;
import org.rhq.plugins.jmx.JMXComponent;

/**
 * @author John Sanda
 */
public class KeyspaceComponent implements ResourceComponent<ResourceComponent<?>>, ConfigurationFacet,
    JMXComponent<ResourceComponent<?>>, OperationFacet {

    private final Log log = LogFactory.getLog(KeyspaceComponent.class);

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
        Session session = getCassandraSession();
        Query q = QueryBuilder.select().from("system", "schema_keyspaces")
            .where(eq("keyspace_name", context.getResourceKey()));
        ResultSet resultSet = session.execute(q);

        if (resultSet.isExhausted()) {
            return null;
        }

        Row result = resultSet.one();

        Configuration config = new Configuration();
        config.put(new PropertySimple("name", result.getString("keyspace_name")));
        config.put(new PropertySimple("strategyClass", result.getString("strategy_class")));
        config.put(new PropertySimple("durableWrites", result.getBool("durable_writes")));

        //TODO: Enable back if needed but it needs JSON parsing added to the plugin
        PropertyList list = new PropertyList("strategyOptions");
        //Map<String, String> strategyOptions = result.getMap("strategy_options", String.class, String.class);
        for (String optionName : (new HashMap<String, String>()).keySet()) {
            PropertyMap map = new PropertyMap("strategyOptionsMap");
            map.put(new PropertySimple("strategyOptionName", optionName));
            //map.put(new PropertySimple("strategyOptionValue", strategyOptions.get(optionName)));
            map.put(new PropertySimple("strategyOptionValue", ""));
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
        } else if (name.equals("cleanup")) {
            return cleanup();
        }

        OperationResult failedOperation = new OperationResult();
        failedOperation.setErrorMessage("Operation not implemented.");

        return failedOperation;
    }

    public OperationResult repairKeyspace() {
        KeyspaceService keyspaceService = new KeyspaceService(getEmsConnection());

        String keyspace = context.getResourceKey();

        log.info("Executing repair on keyspace [" + keyspace + "]");
        long start = System.currentTimeMillis();
        keyspaceService.repair(keyspace);
        long end = System.currentTimeMillis();
        log.info("Finished repair on keyspace [" + keyspace + "] in " + (end - start) + " ms");

        return new OperationResult();
    }

    public OperationResult cleanup() {
        KeyspaceService keyspaceService = new KeyspaceService(getEmsConnection());
        String keyspace = context.getResourceKey();

        log.info("Executing cleanup on keyspace [" + keyspace + "]");
        long start = System.currentTimeMillis();
        keyspaceService.cleanup(keyspace);
        long end = System.currentTimeMillis();

        log.info("Finished cleanup on keyspace [" + keyspace + "] in " + (end - start) + " ms");

        return new OperationResult();
    }

    public OperationResult compactKeyspace() {
        KeyspaceService keyspaceService = new KeyspaceService(getEmsConnection());
        String keyspace = context.getResourceKey();

        log.info("Executing compaction on  keyspace [" + keyspace + "]");
        long start = System.currentTimeMillis();
        keyspaceService.compact(keyspace);
        long end = System.currentTimeMillis();
        log.info("Finished compaction on keysapce [" + keyspace + "] in " + (end - start) + " ms");

        return new OperationResult();
    }

    public OperationResult takeSnapshot(Configuration parameters, String... columnFamilies) {
        String keyspace = context.getResourceKey();
        String snapshotName = parameters.getSimpleValue("snapshotName");
        if (snapshotName == null || snapshotName.trim().isEmpty()) {
            snapshotName = System.currentTimeMillis() + "";
        }

        log.info("Taking snapshot of keyspace [" + keyspace + "]");
        log.info("Snapshot name set to [" + snapshotName + "]");
        long start = System.currentTimeMillis();
        EmsBean emsBean = loadBean(KeyspaceService.STORAGE_SERVICE_BEAN);
        if (columnFamilies == null || columnFamilies.length == 0) {
            EmsOperation operation = emsBean.getOperation("takeSnapshot", String.class, String[].class);
            operation.invoke(snapshotName, new String[]{keyspace});
        } else {
            EmsOperation operation = emsBean.getOperation("takeColumnFamilySnapshot", String.class, String.class,
                String.class);

            for (String columnFamily : columnFamilies) {
                if (log.isDebugEnabled()) {
                    log.debug("Taking snapshot of column family [" + columnFamily + "]");
                }
                operation.invoke(keyspace, columnFamily, snapshotName);
            }
        }

        long end = System.currentTimeMillis();
        log.info("Finished taking snapshot of keyspace [" + keyspace + "] in " + (end - start) + " ms");

        return new OperationResult();
    }

    public PropertyList getKeySpaceDataFileLocations() {
        EmsBean emsBean = loadBean(KeyspaceService.STORAGE_SERVICE_BEAN);
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
        EmsBean emsBean = loadBean(KeyspaceService.STORAGE_SERVICE_BEAN);
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
     * @return keyspace name
     */
    public String getKeyspaceName() {
        return this.context.getResourceKey();
    }

    /**
     * Retrieves a cluster connection from the parent resource.
     *
     * @return the cluster connection.
     */
    public Session getCassandraSession() {
        CassandraNodeComponent parent = (CassandraNodeComponent) context.getParentResourceComponent();
        return parent.getCassandraSession();
    }
}
