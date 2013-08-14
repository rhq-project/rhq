/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

/**
 * @author Stefan Negrea
 */
public class StorageNodeConfigurationComposite implements Serializable {
    private static final long serialVersionUID = 1L;

    private StorageNode storageNode;
    private int jmxPort;
    private String heapSize;
    private String threadStackSize;
    private String heapNewSize;

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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("storageNode.addresss=").append(storageNode == null ? "unknown" : storageNode.getAddress())
            .append(", ");
        builder.append("jmxPort=").append(jmxPort).append(",");
        builder.append("heapSize=").append(heapSize).append(", ");
        builder.append("heapNewSize=").append(heapSize).append(", ");
        builder.append("threadStackSize=").append(threadStackSize).append("");
        return builder.toString();
    }
}
