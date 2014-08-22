/*
 *
 *  * RHQ Management Platform
 *  * Copyright (C) 2005-2014 Red Hat, Inc.
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
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.pluginapi.operation.OperationResult;

public class TakeSnapshotOperation {

    private final String R_STRATEGY_KEEP_ALL = "Keep All";
    private final String R_STRATEGY_KEEP_LASTN = "Keep Last N";
    private final String R_STRATEGY_DEL_OLDERN = "Delete Older Than N days";

    private final String D_STRATEGY_DEL = "Delete";
    private final String D_STRATEGY_MOVE = "Move";

    private final KeyspaceService service;
    private final Configuration parameters;

    private final Log log = LogFactory.getLog(TakeSnapshotOperation.class);

    public TakeSnapshotOperation(KeyspaceService service, Configuration parameters) {
        this.service = service;
        this.parameters = parameters;
    }

    public OperationResult invoke() {
        OperationResult result = new OperationResult();
        String snapshotName = parameters.getSimpleValue("snapshotName", "" + System.currentTimeMillis()).trim();
        if (snapshotName.isEmpty()) {
            result.setErrorMessage("Snapshot Name parameter cannot be an empty string");
            return result;
        }
        String retentionStrategy = parameters.getSimpleValue("retentionStrategy", R_STRATEGY_KEEP_ALL);
        Integer count = parameters.getSimple("count").getIntegerValue();
        String deletionStrategy = parameters.getSimpleValue("deletionStrategy", D_STRATEGY_DEL);
        String location = parameters.getSimpleValue("location");

        // validate parameters
        if (R_STRATEGY_KEEP_LASTN.equals(retentionStrategy) || R_STRATEGY_DEL_OLDERN.equals(retentionStrategy)) {
            if (count == null) {
                result.setErrorMessage("Invalid input parameters. Selected Retention Strategy [" + retentionStrategy
                    + "] but 'count' parameter was null");
                return result;
            }
        }
        if (D_STRATEGY_MOVE.equals(deletionStrategy)) {
            if (location == null) {
                result.setErrorMessage("Invalid input parameters. Selected Deletion Strategy [" + deletionStrategy
                    + "] but 'location' parameter was null");
                return result;
            }
            File locationDir = new File(location);
            if (!locationDir.exists()) {
                try {
                    if (!locationDir.mkdirs()) {
                        result.setErrorMessage("Location [" + locationDir.getAbsolutePath()
                            + "] did not exist and failed to be created");
                        return result;
                    }
                } catch (Exception e) {
                    result.setErrorMessage("Location [" + locationDir.getAbsolutePath()
                        + "] did not exist and failed to be created - " + e.getMessage());
                    return result;
                }
            }
            if (!locationDir.isDirectory()) {
                result.setErrorMessage("Location [" + locationDir.getAbsolutePath() + "] must be directory");
                return result;
            }
            if (!locationDir.canWrite()) {
                result.setErrorMessage("Location [" + locationDir.getAbsolutePath() + "] must be writable");
            }
        }

        String[] keyspaces = service.getKeyspaces().toArray(new String[] {});
        log.info("Taking snapshot of keyspaces " + Arrays.toString(keyspaces));
        long startTime = System.currentTimeMillis();
        service.takeSnapshot(keyspaces, snapshotName);
        log.info("Snapshot taken in " + (System.currentTimeMillis() - startTime) + "ms");

        if (R_STRATEGY_KEEP_ALL.equals(retentionStrategy)) { // do nothing
            return result;
        }

        List<String> columnFamilyDirs = service.getColumnFamilyDirs();

        // obtain list of snapshot dirs to be moved or deleted
        List<String[]> eligibleSnapshots = findEligibleSnapshots(service.getKeySpaceDataFileLocations(),
            columnFamilyDirs, retentionStrategy, count);

        if (eligibleSnapshots.isEmpty()) {
            return result;
        }
        if (D_STRATEGY_DEL.equals(deletionStrategy)) {
            log.info("Strategy [" + deletionStrategy + "] is set, deleting " + eligibleSnapshots.size() + " snapshots");
            for (String[] snapPath : eligibleSnapshots) {
                File snapDir = new File(snapPath[0], snapPath[1]);
                log.info("Deleting " + snapDir);
                if (!FileUtils.deleteQuietly(snapDir)) {
                    log.warn("Failed to delete " + snapDir.getAbsolutePath());
                }
            }
        }
        if (D_STRATEGY_MOVE.equals(deletionStrategy)) {
            log.info("Strategy [" + deletionStrategy + "] is set, moving " + eligibleSnapshots.size() + " snapshots");
            for (String[] snapPath : eligibleSnapshots) {
                File snapDir = new File(snapPath[0], snapPath[1]);
                File snapTargetDir = new File(location, snapPath[1]);
                log.info("Moving  " + snapDir + " to " + snapTargetDir);
                try {
                    FileUtils.moveDirectoryToDirectory(snapDir, snapTargetDir.getParentFile(), true);
                } catch (IOException e) {
                    log.warn("Failed to move directory : " + e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * find eligible snapshot dirs - to be deleted or moved. Dirs are returned as pairs, we need this - in case 'Move' is required we'll have to create 
     * snapshot subdirectory relative to target location
     * @param dataDirs root dataDirs
     * @param colFamilyDirs relative paths to dataDirs
     * @param retentionStrategy
     * @param count
     * @return list of pairs <dataDir,snapShotDir>
     */
    private List<String[]> findEligibleSnapshots(String[] dataDirs, List<String> colFamilyDirs,
        String retentionStrategy, Integer count) {
        List<String[]> eligibleSnapshots = new ArrayList<String[]>();
        for (String dataRoot : dataDirs) {
            for (String keySpace : colFamilyDirs) {
                String colFamilyDir = keySpace + File.separator + "snapshots";
                File keySpaceDir = new File(dataRoot, colFamilyDir);
                if (keySpaceDir.exists()) { // might not exist in case there are several dataRoots
                    File[] snapshots = keySpaceDir.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return pathname.isDirectory();
                        }
                    });
                    // sort, newest first
                    Arrays.sort(snapshots, new Comparator<File>() {
                        @Override
                        public int compare(File o1, File o2) {
                            return o1.lastModified() > o2.lastModified() ? -1 : 1;
                        }
                    });
                    if (R_STRATEGY_KEEP_LASTN.equals(retentionStrategy)) {
                        int index = 0;
                        for (File f : snapshots) {
                            if (index >= count) {
                                eligibleSnapshots.add(new String[] { dataRoot,
                                    colFamilyDir + File.separator + f.getName() });
                            }
                            index++;
                        }
                    }
                    if (R_STRATEGY_DEL_OLDERN.equals(retentionStrategy)) {
                        long cutOff = System.currentTimeMillis() - (count * 86400L * 1000L);
                        for (File f : snapshots) {
                            if (f.lastModified() < cutOff) {
                                eligibleSnapshots.add(new String[] { dataRoot,
                                    colFamilyDir + File.separator + f.getName() });
                            }
                        }
                    }
                }
            }
        }
        return eligibleSnapshots;
    }
}
