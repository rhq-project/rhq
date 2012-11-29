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

package org.rhq.cassandra;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author John Sanda
 */
public class DeploymentOptions {

    private final Log log = LogFactory.getLog(DeploymentOptions.class);

    private String bundleFileName;
    private String bundleName;
    private String bundleVersion;
    private String clusterDir;
    private Integer numNodes;
    private Boolean autoDeploy;
    private Boolean embedded;
    private String loggingLevel;
    private Long ringDelay;
    private Integer numTokens;
    private Integer nativeTransportPort;
    private Integer nativeTransportMaxThreads;

    public DeploymentOptions() {
    }

    public void load() throws IOException {
        InputStream stream = null;
        try {
            stream = getClass().getResourceAsStream("/cassandra.properties");
            Properties props = new Properties();
            props.load(stream);

            init(props);
        }  catch (IOException e) {
            log.warn("Unable to load deployment options from cassandra.properties.");
            log.info("The following error occurred while trying to load options.", e);
            throw e;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    String msg = "An error occurred while closing input stream on cassandra.properties";
                    log.info(msg, e);
                }
            }
        }
    }

    private void init(Properties properties) {
        setBundleFileName(properties.getProperty("rhq.cassandra.bundle.filename"));
        setBundleName(properties.getProperty("rhq.cassandra.bundle.name"));
        setBundleVersion(properties.getProperty("rhq.cassandra.bundle.version"));
        setClusterDir(loadProperty("rhq.cassandra.cluster.dir", properties));
        setNumNodes(Integer.parseInt(loadProperty("rhq.cassandra.cluster.num-nodes", properties)));
        setAutoDeploy(Boolean.valueOf(loadProperty("rhq.cassandra.cluster.auto-deploy", properties)));
        setEmbedded(Boolean.valueOf(loadProperty("rhq.cassandra.cluster.is-embedded", properties)));
        setLoggingLevel(loadProperty("rhq.cassandra.logging.level", properties));

        String ringDelay = loadProperty("rhq.cassandra.ring.delay", properties);
        if (ringDelay != null && !ringDelay.isEmpty()) {
            setRingDelay(Long.valueOf(ringDelay));
        }

        setNumTokens(Integer.valueOf(loadProperty("rhq.cassandra.node.num-tokens", properties)));
        setNativeTransportPort(Integer.valueOf(loadProperty("rhq.cassandra.native-transport-port", properties)));
        setNativeTransportMaxThreads(Integer.valueOf(loadProperty("rhq.casandra.native-transport-max-threads",
            properties)));
    }

    private String loadProperty(String key, Properties properties) {
        String value = System.getProperty(key);
        if (value == null || value.isEmpty()) {
            return properties.getProperty(key);
        }
        return value;
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

    public Long getRingDelay() {
        return ringDelay;
    }

    public void setRingDelay(Long ringDelay) {
        if (this.ringDelay == null) {
            this.ringDelay = ringDelay;
        }
    }

    public Integer getNumTokens() {
        return numTokens;
    }

    public void setNumTokens(int numTokens) {
        if (this.numTokens == null) {
            this.numTokens = numTokens;
        }
    }

    public Integer getNativeTransportPort() {
        return nativeTransportPort;
    }

    public void setNativeTransportPort(Integer port) {
        if (nativeTransportPort == null) {
            nativeTransportPort = port;
        }
    }

    public Integer getNativeTransportMaxThreads() {
        return nativeTransportMaxThreads;
    }

    public void setNativeTransportMaxThreads(int numThreads) {
        if (nativeTransportMaxThreads == null) {
            nativeTransportMaxThreads = numThreads;
        }
    }

}
