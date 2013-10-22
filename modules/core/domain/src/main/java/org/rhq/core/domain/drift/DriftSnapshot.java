/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.domain.drift;

import static org.rhq.core.domain.drift.DriftCategory.FILE_REMOVED;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A representation of an agent's drift file monitoring. 
 * 
 * @author John Sanda
 */
public class DriftSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The request that generated this snapshot */
    private DriftSnapshotRequest request;

    /** 
     * The version of this snapshot.  It may differ from the requested version if the requested version
     * does not exist.  -1 indicates that the definition does not yet have an initial snapshot. 
     */
    private int version = -1;

    /** Map of directory path to information about that directory.  Includes only directories with drift instances */
    private Map<String, DriftSnapshotDirectory> directoryMap;

    /** Map of path to Drift instance.  */
    private Map<String, Drift<?, ?>> driftMap;

    /**
     * This should not be used. It's to satisfy smartgwt.
     */
    public DriftSnapshot() {
    }

    public DriftSnapshot(DriftSnapshotRequest request) {
        super();
        this.request = request;
        if (this.request.isIncludeDriftDirectories()) {
            directoryMap = new TreeMap<String, DriftSnapshotDirectory>();
        }
        if (this.request.isIncludeDriftInstances()) {
            driftMap = new TreeMap<String, Drift<?, ?>>();
        }
    }

    public int getVersion() {
        return version;
    }

    public Collection<Drift<?, ?>> getDriftInstances() {
        if (null == driftMap) {
            return null;
        }

        return driftMap.values();
    }

    public Collection<DriftSnapshotDirectory> getDriftDirectories() {
        if (null == directoryMap) {
            return null;
        }

        return directoryMap.values();
    }

    /**
     * @param changeSet The drifts must be set for the changeSet
     * @return the snapshot
     */
    public DriftSnapshot addChangeSet(DriftChangeSet<? extends Drift<?, ?>> changeSet) {

        String dirFilter = request.getDirectory();
        Set<? extends Drift<?, ?>> drifts = changeSet.getDrifts();

        for (Drift<?, ?> drift : drifts) {
            String path = drift.getPath();

            if (request.isIncludeDriftDirectories()) {
                int i = path.lastIndexOf("/");
                String key = (i != -1) ? path.substring(0, i) : "";
                DriftSnapshotDirectory dir = directoryMap.get(key);
                if (null == dir) {
                    dir = new DriftSnapshotDirectory(key);
                    directoryMap.put(key, dir);
                }
                switch (drift.getCategory()) {
                case FILE_ADDED:
                    dir.incrementAdded();
                    dir.incrementFiles();
                    break;
                case FILE_CHANGED:
                    dir.incrementChanged();
                    break;
                case FILE_REMOVED:
                    dir.incrementRemoved();
                    dir.decrementFiles();
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected drift category: " + drift.getCategory().name());
                }
            }

            if (request.isIncludeDriftInstances()) {

                if ((null != dirFilter) && (!dirFilter.equals(drift.getDirectory()))) {
                    continue;
                }

                if (drift.getCategory() == FILE_REMOVED) {
                    driftMap.remove(path);
                } else {
                    driftMap.put(drift.getPath(), drift);
                }
            }
        }
        version = changeSet.getVersion();
        return this;
    }

    public DriftDiffReport<?> diff(DriftSnapshot right) {
        if (!(right.request.isIncludeDriftInstances() && this.request.isIncludeDriftInstances())) {
            throw new IllegalArgumentException("Cannot compare DriftSnapshots that do not contain drift instances");
        }

        DriftSnapshot left = this;
        DriftDiffReport<Drift<?, ?>> diff = new DriftDiffReport<Drift<?, ?>>();

        for (Map.Entry<String, Drift<?, ?>> entry : left.driftMap.entrySet()) {
            if (!right.driftMap.containsKey(entry.getKey())) {
                diff.elementNotInRight(entry.getValue());
            }
        }

        for (Map.Entry<String, Drift<?, ?>> entry : right.driftMap.entrySet()) {
            if (!left.driftMap.containsKey(entry.getKey())) {
                diff.elementNotInLeft(entry.getValue());
            }
        }

        for (Map.Entry<String, Drift<?, ?>> entry : left.driftMap.entrySet()) {
            Drift<?, ?> rightDrift = right.driftMap.get(entry.getKey());
            if (rightDrift != null) {
                DriftFile leftFile = entry.getValue().getNewDriftFile();
                DriftFile rightFile = rightDrift.getNewDriftFile();

                if (!leftFile.getHashId().equals(rightFile.getHashId())) {
                    diff.elementInConflict(entry.getValue());
                }
            }
        }

        return diff;
    }

    public DriftSnapshotRequest getRequest() {
        return request;
    }

    public static class DriftSnapshotDirectory implements Serializable {

        private static final long serialVersionUID = 1L;

        private String directoryPath;
        private int files = 0;
        private int added = 0;
        private int changed = 0;
        private int removed = 0;

        /** for smartgwt, do not call */
        public DriftSnapshotDirectory() {
        }

        public DriftSnapshotDirectory(String directoryPath) {
            this.directoryPath = directoryPath;
        }

        public String getDirectoryPath() {
            return directoryPath;
        }

        public int getFiles() {
            return files;
        }

        protected void decrementFiles() {
            if (this.files > 0) {
                --this.files;
            }
        }

        protected void incrementFiles() {
            ++this.files;
        }

        public int getAdded() {
            return added;
        }

        protected void incrementAdded() {
            ++this.added;
        }

        public int getChanged() {
            return changed;
        }

        protected void incrementChanged() {
            ++this.changed;
        }

        public int getRemoved() {
            return removed;
        }

        protected void incrementRemoved() {
            ++this.removed;
        }

    }

}
