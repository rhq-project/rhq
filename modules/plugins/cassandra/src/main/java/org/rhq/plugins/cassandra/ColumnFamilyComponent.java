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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.configuration.ConfigurationUpdateReport;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.core.util.file.FileUtil;

/**
 * @author John Sanda
 */
public class ColumnFamilyComponent extends ComplexConfigurationResourceComponent {

    private Log log = LogFactory.getLog(ColumnFamilyComponent.class);

    @Override
    public Configuration loadResourceConfiguration() {
        Configuration config = super.loadResourceConfiguration();

        if (log.isDebugEnabled()) {
            ResourceContext<?> context = getResourceContext();
            log.debug("Loading resource context for column family " + context.getResourceKey());
        }

        config.put(this.getSnapshotsWithDetails());

        return config;
    }

    @Override
    public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
        String columnFamilyName = this.getResourceContext().getResourceKey();
        if (name.equals("repair")) {
            return this.getParentKeyspace().repairKeyspace(columnFamilyName);
        } else if (name.equals("compact")) {
            return this.getParentKeyspace().compactKeyspace(columnFamilyName);
        } else if (name.equals("takeSnapshot")) {
            return this.getParentKeyspace().takeSnapshot(parameters, columnFamilyName);
        } else if (name.equals("restoreSnapshot")){
            return this.restoreSnapshot(parameters);
        }

        return super.invokeOperation(name, parameters);
    };

    @Override
    public void updateResourceConfiguration(ConfigurationUpdateReport report) {
        if (log.isDebugEnabled()) {
            log.debug("Updating resource configuration for column family " + getResourceContext().getResourceKey());
        }

        report.getConfiguration().remove("snapshots");
        super.updateResourceConfiguration(report);
    }

    private PropertyList getSnapshotsWithDetails() {
        PropertyList listOfSnapshots = new PropertyList("snapshots");

        KeyspaceComponent parentKeyspace = this.getParentKeyspace();
        PropertyList parentDataFileLocations = parentKeyspace.getKeySpaceDataFileLocations();

        for (Property directory : parentDataFileLocations.getList()) {
            String directoryName = ((PropertySimple) directory).getStringValue();
            directoryName += "/" + this.getResourceContext().getPluginConfiguration().getSimpleValue("name");
            directoryName += "/snapshots";

            File snapshotDirectory = new File(directoryName);

            if (snapshotDirectory.exists()) {
                File[] files = snapshotDirectory.listFiles();

                for (File individualSnapshot : files) {
                    if (individualSnapshot.isDirectory()) {
                        PropertyMap snapshotDetails = new PropertyMap("snapshot");
                        snapshotDetails.put(new PropertySimple("name", individualSnapshot.getName()));
                        snapshotDetails.put(new PropertySimple("folder", individualSnapshot.getAbsolutePath()));
                        listOfSnapshots.add(snapshotDetails);
                    }
                }
            }
        }

        return listOfSnapshots;
    }

    private OperationResult restoreSnapshot(Configuration parameters) {
        OperationResult result = new OperationResult();

        //1. Find the list of snapshots discovered
        String requestedSnapshotName = parameters.getSimpleValue("snapshotName");
        PropertyList listOfSnapShots = this.getSnapshotsWithDetails();

        String snapshotDirectoryName = null;
        for (Property property : listOfSnapShots.getList()) {
            String snapshotName = ((PropertyMap) property).getSimpleValue("name", null);
            if (requestedSnapshotName.equals(snapshotName)) {
                snapshotDirectoryName = ((PropertyMap) property).getSimpleValue("folder", null);
                break;
            }
        }

        //2. Find out if the discovered snapshot still exists on disk
        if (snapshotDirectoryName == null) {
            result.setErrorMessage("Restore failed! The snapshot does not exist!");
            return result;
        }

        File snapshotDirectory = new File(snapshotDirectoryName);
        if (!snapshotDirectory.exists() || !snapshotDirectory.isDirectory()) {
            result.setErrorMessage("Restore failed! The snapshot does not exist on disk!");
            return result;
        }

        //3. Shutdown Cassandra
        CassandraNodeComponent node = this.getParentKeyspace().getCassandraNodeComponent();
        node.shutdownNode();

        //4. Remove the entire commit log
        KeyspaceComponent parentKeyspace = this.getParentKeyspace();

        parentKeyspace.clearCommitLog();

        //5. Copy the snapshot files to the column family folders
        PropertyList parentDataFileLocations = parentKeyspace.getKeySpaceDataFileLocations();

        for (Property dataFileDirectoryProperty : parentDataFileLocations.getList()) {
            String columnFamilyDirectoryName = ((PropertySimple) dataFileDirectoryProperty).getStringValue();
            columnFamilyDirectoryName += "/" + this.getResourceContext().getPluginConfiguration().getSimpleValue("name");

            File columnFamilyDirectory = new File(columnFamilyDirectoryName);
            if(columnFamilyDirectory.exists()){
                //5.1 Remove existing data files
                File[] originalColumnFamilyDataFiles = columnFamilyDirectory.listFiles();
                if (originalColumnFamilyDataFiles != null) {
                    for (File file : originalColumnFamilyDataFiles) {
                        if (file.isFile()) {
                            file.delete();
                        }
                    }
                }

                //5.2 Copy snapshots files to column family folder
                File[] filesToBeRestored = snapshotDirectory.listFiles();
                if (filesToBeRestored != null) {
                    for (File fileToBeRestored : filesToBeRestored) {
                        if (fileToBeRestored.isFile()) {
                            File destinationFile = new File(columnFamilyDirectory, fileToBeRestored.getName());
                            try {
                                FileUtil.copyFile(fileToBeRestored, destinationFile);
                            } catch (Exception e) {
                                result.setErrorMessage("Restore failed! The file copying process failed!");
                                return result;
                            }
                        }
                    }
                }
            }
        }

        //6. Restart Cassandra
        node.startNode();

        result.setSimpleResult("Snapshot restored succesfully...");

        return result;
    }

    /**
     * @return parent resource component
     */
    private KeyspaceComponent getParentKeyspace() {
        return (KeyspaceComponent) this.getResourceContext().getParentResourceComponent();
    }
}
