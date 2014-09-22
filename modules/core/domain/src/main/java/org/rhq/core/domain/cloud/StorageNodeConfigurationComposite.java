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
package org.rhq.core.domain.cloud;

import java.io.Serializable;
import java.util.List;

/**
 * @author Stefan Negrea
 */
public class StorageNodeConfigurationComposite implements Serializable {
    private static final long serialVersionUID = 2L;

    private StorageNode storageNode;
    private int jmxPort;
    private String heapSize;
    private String threadStackSize;
    private String heapNewSize;
    private String commitLogLocation;
    private List<String> dataLocations;
    private String savedCachesLocation;

    public StorageNodeConfigurationComposite() {
        // GWT needs this
    }

    public StorageNodeConfigurationComposite(StorageNode storageNode) {
        this.storageNode = storageNode;
    }

    /**
     * @return associated storage node
     */
    public StorageNode getStorageNode() {
        return storageNode;
    }

    /**
     * @param storageNode storage node
     */
    protected void setStorageNode(StorageNode storageNode) {
        this.storageNode = storageNode;
    }


    /**
     * @return the JMX port
     */
    public int getJmxPort() {
        return jmxPort;
    }

    /**
     * @param jmxPort JMX port to set
     */
    public void setJmxPort(int jmxPort) {
        this.jmxPort = jmxPort;
    }

    /**
     * @return the heap size
     */
    public String getHeapSize() {
        return heapSize;
    }

    /**
     * @param heapSize heap size to set
     */
    public void setHeapSize(String heapSize) {
        this.heapSize = heapSize;
    }

    /**
     * @return the threadStackSize
     */
    public String getThreadStackSize() {
        return threadStackSize;
    }

    /**
     * @param threadStackSize the threadStackSize to set
     */
    public void setThreadStackSize(String threadStackSize) {
        this.threadStackSize = threadStackSize;
    }

    /**
     * @return the heapNewSize
     */
    public String getHeapNewSize() {
        return heapNewSize;
    }

    /**
     * @param heapNewSize the heapNewSize to set
     */
    public void setHeapNewSize(String heapNewSize) {
        this.heapNewSize = heapNewSize;
    }

    /**
     * @return the commitlog directory
     */
    public String getCommitLogLocation() {
        return commitLogLocation;
    }

    /**
     * @param commitLogLocation the commitlog directory to set
     */
    public void setCommitLogLocation(String commitLogLocation) {
        this.commitLogLocation = commitLogLocation;
    }

    /**
     * @return the data directory
     */
    public List<String> getDataLocations() {
        return dataLocations;
    }

    /**
     * @param dataLocations the data directory to set
     */
    public void setDataLocations(List<String> dataLocations) {
        this.dataLocations = dataLocations;
    }

    /**
     * @return the saved-caches directory
     */
    public String getSavedCachesLocation() {
        return savedCachesLocation;
    }

    /**
     * @param savedCachesLocation the saved-caches directory to set
     */
    public void setSavedCachesLocation(String savedCachesLocation) {
        this.savedCachesLocation = savedCachesLocation;
    }

    public boolean validate() {
        //validate heap settings
        boolean validHeap = false;

        String heapSize = getHeapSize() == null ? null : (getHeapSize().trim().length() == 0 ? null : getHeapSize()
            .trim().toLowerCase());
        String heapNewSize = getHeapNewSize() == null ? null : (getHeapNewSize().trim().length() == 0 ? null
            : getHeapNewSize().trim().toLowerCase());

        if (heapSize == null && heapNewSize == null) {
            validHeap = true;
        } else if (heapSize != null && heapNewSize != null) {
            try {
                int heapSizeParsed = 0;
                if (heapSize.contains("g")) {
                    heapSizeParsed = Integer.parseInt(heapSize.replace("g", "")) * 1024;
                } else if (heapSize.contains("m")) {
                    heapSizeParsed = Integer.parseInt(heapSize.toLowerCase().replace("m", ""));
                } else {
                    throw new IllegalArgumentException();
                }

                int heapNewSizeParsed = 0;
                if (heapNewSize.contains("g")) {
                    heapNewSizeParsed = Integer.parseInt(heapNewSize.replace("g", "")) * 1024;
                } else if (heapNewSize.contains("m")) {
                    heapNewSizeParsed = Integer.parseInt(heapNewSize.toLowerCase().replace("m", ""));
                } else {
                    throw new IllegalArgumentException();
                }

                if (heapNewSizeParsed < heapSizeParsed) {
                    validHeap = true;
                }
            } catch (Exception e) {
                //Nothing to do heap settings are not valid since parsing failed at some point
            }
        }

        //validate JMX Port
        boolean validJMXPort = false;
        if (this.getJmxPort() < 65535) {
            validJMXPort = true;
        }

        return validHeap && validJMXPort;
    }

    public boolean isDirectoriesEqual(StorageNodeConfigurationComposite other) {
        if (commitLogLocation != null && !commitLogLocation.equals(other.getCommitLogLocation())) {
            return false;
        } else if (commitLogLocation == null && other.getCommitLogLocation() != null) {
            return false;
        }
        if (dataLocations != null && !dataLocations.equals(other.getDataLocations())) {
            return false;
        } else if(dataLocations == null && other.getDataLocations() != null) {
            return false;
        }
        if (savedCachesLocation != null && !savedCachesLocation.equals(other.getSavedCachesLocation())) {
            return false;
        } else if(savedCachesLocation == null && other.getSavedCachesLocation() != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((heapNewSize == null) ? 0 : heapNewSize.hashCode());
        result = prime * result + ((heapSize == null) ? 0 : heapSize.hashCode());
        result = prime * result + jmxPort;
        result = prime * result + ((threadStackSize == null) ? 0 : threadStackSize.hashCode());
        result = prime * result + ((commitLogLocation == null) ? 0 : commitLogLocation.hashCode());
        result = prime * result + ((dataLocations == null) ? 0 : dataLocations.hashCode());
        result = prime * result + ((savedCachesLocation == null) ? 0 : savedCachesLocation.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        StorageNodeConfigurationComposite other = (StorageNodeConfigurationComposite) obj;
        if (heapNewSize == null) {
            if (other.heapNewSize != null)
                return false;
        } else if (!heapNewSize.equals(other.heapNewSize))
            return false;
        if (heapSize == null) {
            if (other.heapSize != null)
                return false;
        } else if (!heapSize.equals(other.heapSize))
            return false;
        if (jmxPort != other.jmxPort)
            return false;
        if (threadStackSize == null) {
            if (other.threadStackSize != null)
                return false;
        } else if (!threadStackSize.equals(other.threadStackSize))
            return false;
        if(!isDirectoriesEqual(other)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("storageNode.addresss=").append(storageNode == null ? "unknown" : storageNode.getAddress())
            .append(", ");
        builder.append("jmxPort=").append(jmxPort).append(",");
        builder.append("heapSize=").append(heapSize).append(", ");
        builder.append("heapNewSize=").append(heapSize).append(", ");
        builder.append("threadStackSize=").append(threadStackSize).append(", ");
        builder.append("commitlog=").append(commitLogLocation).append(", ");
        builder.append("data=").append(dataLocations).append(", ");
        builder.append("saved-caches=").append(savedCachesLocation).append("");
        return builder.toString();
    }
}
