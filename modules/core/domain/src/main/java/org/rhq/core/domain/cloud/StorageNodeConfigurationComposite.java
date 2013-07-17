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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("storageNode.addresss=").append(storageNode.getAddress()).append(", ");
        builder.append("heapSize=").append(heapSize).append(", ");
        builder.append("jmxPort=").append(jmxPort).append("");
        return builder.toString();
    }
}
