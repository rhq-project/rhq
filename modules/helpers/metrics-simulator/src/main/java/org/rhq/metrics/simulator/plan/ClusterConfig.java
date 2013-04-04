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

package org.rhq.metrics.simulator.plan;

import java.io.File;

/**
 * @author John Sanda
 */
public class ClusterConfig {

    private boolean embedded = true;

    private String clusterDir = new File(System.getProperty("rhq.metrics.simulator.basedir")).getAbsolutePath();

    private int numNodes = 2;

    private String heapSize = "256M";

    private String heapNewSize = "64M";

    private String stackSize = "180k";

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        this.embedded = embedded;
    }

    public String getClusterDir() {
        return clusterDir;
    }

    public void setClusterDir(String clusterDir) {
        this.clusterDir = clusterDir;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }

    public String getHeapSize() {
        return heapSize;
    }

    public void setHeapSize(String heapSize) {
        this.heapSize = heapSize;
    }

    public String getHeapNewSize() {
        return heapNewSize;
    }

    public void setHeapNewSize(String heapNewSize) {
        this.heapNewSize = heapNewSize;
    }

    public String getStackSize() {
        return stackSize;
    }

    public void setStackSize(String stackSize) {
        this.stackSize = stackSize;
    }
}
