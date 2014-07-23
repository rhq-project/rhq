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

package org.rhq.plugins.cassandra.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

/**
 * @author John Sanda
 */
public class KeyspaceService {

    public static final String STORAGE_SERVICE_BEAN = "org.apache.cassandra.db:type=StorageService";

    public static final String REPAIR_OPERATION = "forceTableRepair";

    public static final String REPAIR_PRIMARY_RANGE = "forceTableRepairPrimaryRange";

    public static final String CLEANUP_OPERATION = "forceTableCleanup";

    public static final String COMPACT_OPERATION = "forceTableCompaction";

    public static final String SNAPSHOT_OPERATION = "takeSnapshot";

    public static final String CF_SNAPSHOT_OPERATION = "takeColumnFamilySnapshot";

    private EmsConnection emsConnection;

    public KeyspaceService(EmsConnection emsConnection) {
        this.emsConnection = emsConnection;
    }

    public void repair(String keyspace, String... columnFamilies) {
        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        EmsOperation operation = emsBean.getOperation(REPAIR_OPERATION, String.class, boolean.class,
            boolean.class, String[].class);

        //  The isSequential param has to be false; otherwise, repair will fail as a result
        // of https://issues.apache.org/jira/browse/CASSANDRA-5512.
        boolean isSequential = false;  // perform sequential repair using snapshot

        boolean isLocal = true;        // local to data center

        operation.invoke(keyspace, isSequential, isLocal, columnFamilies);
    }

    public void repairPrimaryRange(String keyspace, String... columnFamilies) {
        EmsBean emsBean = loadBean(KeyspaceService.STORAGE_SERVICE_BEAN);
        EmsOperation operation = emsBean.getOperation(REPAIR_PRIMARY_RANGE, String.class, boolean.class, boolean.class,
            String[].class);

        //  The isSequential param has to be false; otherwise, repair will fail as a result
        // of https://issues.apache.org/jira/browse/CASSANDRA-5512.
        boolean isSequential = false;  // perform sequential repair using snapshot

        boolean isLocal = true;        // local to data center

        operation.invoke(keyspace, isSequential, isLocal, columnFamilies);
    }

    public void cleanup(String keyspace) {
        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        EmsOperation operation = emsBean.getOperation(CLEANUP_OPERATION, String.class, String[].class);

        operation.invoke(keyspace, new String[] {});
    }

    public void compact(String keyspace, String... columnFamilies) {
        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        EmsOperation operation = emsBean.getOperation(COMPACT_OPERATION, String.class, String[].class);

        operation.invoke(keyspace, columnFamilies);
    }

    public void takeSnapshot(String keyspace, String snapshotName) {
        takeSnapshot(new String[]{keyspace}, snapshotName);
    }

    public void takeSnapshot(String[] keySpaces, String snapshotName) {
        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        EmsOperation operation = emsBean.getOperation(SNAPSHOT_OPERATION, String.class, String[].class);
        operation.invoke(snapshotName, keySpaces);
    }

    public String[] getKeySpaceDataFileLocations() {
        EmsBean emsBean = loadBean(KeyspaceService.STORAGE_SERVICE_BEAN);
        EmsAttribute attribute = emsBean.getAttribute("AllDataFileLocations");
        return (String[]) attribute.getValue();
    }

    @SuppressWarnings("unchecked")
    public List<Object> getKeyspaces() {
        List<Object> value = null;

        EmsBean emsBean = loadBean(STORAGE_SERVICE_BEAN);
        if (emsBean != null) {
            EmsAttribute attribute = emsBean.getAttribute("Keyspaces");
            if (attribute != null) {
                value = (List<Object>) attribute.refresh();
            }
        }
        if (value == null) {
            value = new ArrayList<Object>();
        }

        return value;
    }

    public List<String> getColumnFamilyDirs() {
        List<EmsBean> beans = emsConnection.queryBeans("org.apache.cassandra.db:type=ColumnFamilies,*");
        List<String> dirs = new ArrayList<String>(beans.size());
        for (EmsBean bean : beans) {
            String keySpace = bean.getBeanName().getKeyProperty("keyspace");
            String colFamily = bean.getBeanName().getKeyProperty("columnfamily");
            dirs.add(keySpace + File.separator + colFamily);
        }
        return dirs;
    }

    private EmsBean loadBean(String objectName) {
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

}
