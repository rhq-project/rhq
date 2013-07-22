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

package org.rhq.plugins.storage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;
import org.mc4j.ems.connection.bean.operation.EmsOperation;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationFacet;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.operation.OperationFacet;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.plugins.cassandra.CassandraNodeComponent;
import org.rhq.plugins.cassandra.util.KeyspaceService;

/**
 * @author John Sanda
 */
public class StorageNodeComponent extends CassandraNodeComponent implements OperationFacet, ConfigurationFacet {

    private Log log = LogFactory.getLog(StorageNodeComponent.class);

    private static final String SYSTEM_AUTH_KEYSPACE = "system_auth";

    private static final String RHQ_KEYSPACE = "rhq";

    @Override
    public Configuration loadResourceConfiguration() throws Exception {
        return new StorageNodeConfigDelegate(getBasedir()).loadResourceConfiguration();
    }

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport configurationUpdateReport) {
        StorageNodeConfigDelegate configDelegate = new StorageNodeConfigDelegate(getBasedir());
        configDelegate.updateResourceConfiguration(configurationUpdateReport);
    }

    private File getBasedir() {
        Configuration pluginConfig = getResourceContext().getPluginConfiguration();
        return new File(pluginConfig.getSimpleValue("baseDir"));
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        if (name.equals("addNodeMaintenance")) {
            return nodeAdded(parameters);
        } else if (name.equals("prepareForUpgrade")) {
            return prepareForUpgrade(parameters);
        } else if (name.equals("readRepair")) {
            return readRepair();
        } else if (name.equals("updateConfiguration")) {
            return updateConfiguration(parameters);
        } else if (name.equals("updateKnownNodes")) {
            return updateKnownNodes(parameters);
        } else {
            return super.invokeOperation(name, parameters);
        }
    }

    private OperationResult updateConfiguration(Configuration params) {
        OperationResult result = new OperationResult("Configuration updated.");

        //update storage node jvm settings
        Configuration config = new Configuration();
        config.put(new PropertySimple("minHeapSize", params.getSimpleValue("heapSize")));
        config.put(new PropertySimple("maxHeapSize", params.getSimpleValue("heapSize")));
        config.put(new PropertySimple("heapNewSize", params.getSimpleValue("heapNewSize")));
        config.put(new PropertySimple("threadStackSize", params.getSimpleValue("threadStackSize")));

        ConfigurationUpdateReport configurationUpdate = new ConfigurationUpdateReport(config);
        this.updateResourceConfiguration(configurationUpdate);

        if (!configurationUpdate.getStatus().equals(ConfigurationUpdateStatus.SUCCESS)) {
            result.setErrorMessage(configurationUpdate.getErrorMessage());
        }

        return result;
    }

    private OperationResult updateKnownNodes(Configuration params) {
        OperationResult result = new OperationResult();

        PropertyList propertyList = params.getList("ipAddresses");
        Set<String> ipAddresses = new HashSet<String>();

        for (Property property : propertyList.getList()) {
            PropertySimple propertySimple = (PropertySimple) property;
            ipAddresses.add(propertySimple.getStringValue());
        }

        log.info("Updating known nodes to " + ipAddresses);

        File confDir = new File(getBasedir(), "conf");
        File authFile = new File(confDir, "rhq-storage-auth.conf");
        File authBackupFile = new File(confDir, "." + authFile.getName() + ".bak");

        if (authBackupFile.exists()) {
            if (log.isDebugEnabled()) {
                log.debug(authBackupFile + " already exists. Deleting it now in preparation of creating new backup " +
                    "for " + authFile.getName());
            }
            if (!authBackupFile.delete()) {
                String msg = "Failed to delete backup file " + authBackupFile + ". The operation will abort " +
                    "since " + authFile + " cannot reliably be backed up before making changes. Please delete " +
                    authBackupFile + " manually and reschedule the operation once the file has been removed.";
                log.error(msg);
                result.setErrorMessage(msg);

                return result;
            }
        }

        try {
            FileUtil.copyFile(authFile, authBackupFile);
        } catch (IOException e) {
            String msg = "Failed to backup " + authFile + " prior to making updates. The operation will abort due " +
                "to unexpected error";
            log.error(msg, e);
            result.setErrorMessage(msg + ": " + ThrowableUtil.getRootMessage(e));
            return result;
        }

        try {
            StreamUtil.copy(new StringReader(StringUtil.collectionToString(ipAddresses, "\n")),
                new FileWriter(authFile), true);
        } catch (IOException e) {
            log.error("An error occurred while updating " + authFile, e);
            try {
                FileUtil.copyFile(authBackupFile, authFile);
            } catch (IOException e1) {
                log.error("Failed to revert backup of " + authFile, e1);
            }
            result.setErrorMessage("There was an unexpected error while updating " + authFile + ". Make sure that " +
                "it matches " + authBackupFile + " and then reschedule the operation.");
            return result;
        }

        EmsBean authBean = getEmsConnection().getBean("org.rhq.cassandra.auth:type=RhqInternodeAuthenticator");
        EmsOperation emsOperation = authBean.getOperation("reloadConfiguration");
        emsOperation.invoke();

        result.setSimpleResult("Successfully updated the set of known nodes.");

        return result;
    }

    private OperationResult nodeAdded(Configuration params) {
        boolean runRepair = params.getSimple("runRepair").getBooleanValue();
        boolean updateSeedsList = params.getSimple("updateSeedsList").getBooleanValue();

        EmsConnection emsConnection = getEmsConnection();
        KeyspaceService keyspaceService = new KeyspaceService(emsConnection);
        boolean hasErrors = false;
        OperationResult result = new OperationResult();
        Configuration resultConfig = result.getComplexResults();
        PropertyList resultsList = new PropertyList("results");

        OpResult opResult = null;
        if (runRepair) {
            opResult = repairKeyspace(keyspaceService, SYSTEM_AUTH_KEYSPACE);
            if (!opResult.succeeded) {
                hasErrors = true;
            }
            resultsList.add(toPropertyMap(opResult));
        }

        opResult = cleanupKeyspace(keyspaceService, SYSTEM_AUTH_KEYSPACE);
        if (!opResult.succeeded) {
            hasErrors = true;
        }
        resultsList.add(toPropertyMap(opResult));

        if (runRepair) {
            opResult = repairKeyspace(keyspaceService, RHQ_KEYSPACE);
            if (!opResult.succeeded) {
                hasErrors = true;
            }
            resultsList.add(toPropertyMap(opResult));
        }

        opResult = cleanupKeyspace(keyspaceService, RHQ_KEYSPACE);
        if (!opResult.succeeded) {
            hasErrors = true;
        }
        resultsList.add(toPropertyMap(opResult));

        if (updateSeedsList) {
            List<String> addresses = getAddresses(params.getList("seedsList"));
            try {
                opResult = new OpResult();
                opResult.operation = "Update seeds list";
                updateSeedsList(addresses);
                opResult.succeeded = true;
            } catch (Exception e) {
                log.error("An error occurred while updating the seeds lists for " + getResourceContext().getResourceKey(),
                    e);
                opResult.succeeded = false;

                Throwable rootCause = ThrowableUtil.getRootCause(e);
                opResult.details = "An error occurred while updating the seeds list: " +
                    ThrowableUtil.getStackAsString(rootCause);
            }
            resultsList.add(toPropertyMap(opResult));
        }

        resultConfig.put(resultsList);

        if (hasErrors) {
            result.setErrorMessage("One or more tasks failed to complete successfully.");
        }
        return result;
    }

    private OperationResult readRepair() {
        KeyspaceService keyspaceService = new KeyspaceService(getEmsConnection());
        OperationResult result = new OperationResult();
        Configuration resultConfig = result.getComplexResults();
        PropertyList resultsList = new PropertyList("results");

        OpResult opResult = repairKeyspace(keyspaceService, RHQ_KEYSPACE);
        resultsList.add(toPropertyMap(opResult));

        opResult = repairKeyspace(keyspaceService, SYSTEM_AUTH_KEYSPACE);
        resultsList.add(toPropertyMap(opResult));

        resultConfig.put(resultsList);

        return result;
    }

    private OpResult repairKeyspace(KeyspaceService keyspaceService, String keyspace) {
        OpResult result = new OpResult();
        result.operation = "repair " + keyspace + " keyspace";
        try {
            if (log.isDebugEnabled()) {
                log.debug("Running primary range repair on " + keyspace + " keyspace");
            }
            long start = System.currentTimeMillis();
            keyspaceService.repairPrimaryRange(keyspace);
            long end = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("Finsihed primary range repair on " + keyspace + " keyspace in " + (end - start) + " ms");
            }
            result.succeeded = true;
            result.details = "Completed repair operation in " + (end - start) + " ms.";
        } catch (Exception e) {
            log.error("An error occurred while running repair on " + keyspace, e);
            Throwable rootCause = ThrowableUtil.getRootCause(e);

            result.succeeded = false;
            result.details = "An error occurred while running repair: " + ThrowableUtil.getStackAsString(rootCause);
        }
        return result;
    }

    private OpResult cleanupKeyspace(KeyspaceService keyspaceService, String keyspace) {
        OpResult result = new OpResult();
        result.operation = "cleanup " + keyspace + " keyspace";

        long start;
        long end;
        if (log.isDebugEnabled()) {
            log.debug("Running cleanup on " + keyspace + " keyspace");
        }
        start = System.currentTimeMillis();
        try {
            keyspaceService.cleanup(keyspace);
            end = System.currentTimeMillis();
            if (log.isDebugEnabled()) {
                log.debug("Finished cleanup on " + keyspace + " keyspace in " + (end - start) + " ms");
            }
            result.succeeded = true;
        } catch (Exception e) {
            log.error("An error occurred while running cleanup on " + keyspace + " keyspace", e);
            Throwable rootCause = ThrowableUtil.getRootCause(e);

            result.succeeded = false;
            result.details = "An error occurred while running cleanup: " + ThrowableUtil.getStackAsString(rootCause);
        }
        return result;
    }

    private OperationResult prepareForUpgrade(Configuration parameters) throws Exception {
        EmsConnection emsConnection = getEmsConnection();
        EmsBean storageService = emsConnection.getBean("org.apache.cassandra.db:type=StorageService");
        Class<?>[] emptyParams = new Class<?>[0];

        if (log.isDebugEnabled()) {
            log.debug("Disabling native transport...");
        }
        EmsOperation operation = storageService.getOperation("stopNativeTransport", emptyParams);
        operation.invoke((Object[]) emptyParams);

        if (log.isDebugEnabled()) {
            log.debug("Disabling gossip...");
        }
        operation = storageService.getOperation("stopGossiping", emptyParams);
        operation.invoke((Object[]) emptyParams);

        if (log.isDebugEnabled()) {
            log.debug("Taking the snapshot...");
        }
        operation = storageService.getOperation("takeSnapshot", String.class, String[].class);
        String snapshotName = parameters.getSimpleValue("snapshotName");
        if (snapshotName == null || snapshotName.trim().isEmpty()) {
            snapshotName = System.currentTimeMillis() + "";
        }
        operation.invoke(snapshotName, new String[] {});

        // max 2 sec
        waitForTaskToComplete(500, 10, 150);

        if (log.isDebugEnabled()) {
            log.debug("Initiating drain...");
        }
        operation = storageService.getOperation("drain", emptyParams);
        operation.invoke((Object[]) emptyParams);

        return new OperationResult();
    }

    private void waitForTaskToComplete(int initialWaiting, int maxTries, int sleepMillis) {
        // initial waiting
        try {
            Thread.sleep(initialWaiting);
        } catch (InterruptedException e) {
            if (log.isWarnEnabled()) {
                log.warn(e);
            }
        }
        EmsConnection emsConnection = getEmsConnection();
        EmsBean flushWriterBean = emsConnection.getBean("org.apache.cassandra.internal:type=FlushWriter");
        EmsAttribute attribute = flushWriterBean.getAttribute("PendingTasks");

        Long valueObject = (Long) attribute.refresh();
        // wait until org.apache.cassandra.internal:type=FlushWriter / PendingTasks == 0
        while (valueObject > 0 && maxTries-- > 0) {
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                if (log.isWarnEnabled()) {
                    log.warn(e);
                }
            }
            valueObject = (Long) attribute.refresh();
        }
        flushWriterBean.unload();
    }

    private PropertyMap toPropertyMap(OpResult opResult) {
        PropertyMap map = new PropertyMap("resultsMap");
        map.put(new PropertySimple("task", opResult.operation));
        map.put(new PropertySimple("succeeded", opResult.succeeded));
        map.put(new PropertySimple("details", opResult.details));

        return map;
    }

    private static class OpResult {
        String operation;
        boolean succeeded;
        String details;
    }
}
