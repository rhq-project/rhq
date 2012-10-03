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

package org.rhq.cassandra.bundle;

import java.util.Properties;

/**
 * @author John Sanda
 */
public class DeploymentOptions {

    private String bundleFileName;
    private String bundleName;
    private String bundleVersion;
    private String clusterDir;
    private Integer numNodes;
    private Boolean autoDeploy;
    private Boolean embedded;
    private String loggingLevel;

    public DeploymentOptions(Properties properties) {
        setBundleFileName(properties.getProperty("rhq.cassandra.bundle.filename"));
        setBundleName(properties.getProperty("rhq.cassandra.bundle.name"));
        setBundleVersion(properties.getProperty("rhq.cassandra.bundle.version"));
        setClusterDir(properties.getProperty("rhq.cassandra.cluster.dir"));
        setNumNodes(Integer.parseInt(properties.getProperty("rhq.cassandra.cluster.num-nodes")));
        setAutoDeploy(Boolean.valueOf(properties.getProperty("rhq.cassandra.cluster.auto-deploy")));
        setEmbedded(Boolean.valueOf(properties.getProperty("rhq.cassandra.cluster.is-embedded")));
        setLoggingLevel(properties.getProperty("rhq.cassandra.logging.level"));
    }

    public String getBundleFileName() {
        return bundleFileName;
    }

    public void setBundleFileName(String name) {
        if (bundleFileName == null) {
            bundleFileName = name;
        }
    }

    public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String name) {
        if (bundleName == null) {
            bundleName = name;
        }
    }

    public String getBundleVersion() {
        return bundleVersion;
    }

    public void setBundleVersion(String version) {
        if (bundleVersion == null) {
            bundleVersion = version;
        }
    }

    public String getClusterDir() {
        return clusterDir;
    }

    public void setClusterDir(String dir) {
        if (clusterDir == null) {
            clusterDir = dir;
        }
    }

    public int getNumNodes() {
        return numNodes;
    }

    public void setNumNodes(int numNodes) {
        if (this.numNodes == null) {
            this.numNodes = numNodes;
        }
    }

    public boolean isAutoDeploy() {
        return autoDeploy;
    }

    public void setAutoDeploy(boolean autoDeploy) {
        if (this.autoDeploy == null) {
            this.autoDeploy = autoDeploy;
        }
    }

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        if (this.embedded == null) {
            this.embedded = embedded;
        }
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        if (this.loggingLevel == null) {
            this.loggingLevel = loggingLevel;
        }
    }

}
