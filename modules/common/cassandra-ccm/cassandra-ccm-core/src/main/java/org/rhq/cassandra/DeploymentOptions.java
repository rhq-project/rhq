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
 * <p>
 * A container for deployment options and Cassandra configuration settings. A
 * DeploymentOptions object represents the merger of properties defined in
 * cassandra.properties or defined as system properties. System properties take precedence
 * over corresponding properties in cassandra.properties.
 * </p>
 * <p>
 * Properties are "sticky". Like Ant properties, once set a property's value cannot be
 * changed. This means that if you set a property by calling its setter method prior to
 * invoking {@link #load()}, that value will be retained even if that property is also
 * defined as a system property and in cassandra.properties.
 * </p>
 *
 * @author John Sanda
 */
public class DeploymentOptions {

    private final Log log = LogFactory.getLog(DeploymentOptions.class);

    private boolean loaded;

    // If you add a new field make sure that it is exposed as a "sticky" property. In
    // other words, once set the property's value does not change again. See
    // setClusterDir below for an example. If the property corresponds to an input-property
    // in deploy.xml, then annotate the property's getter method with @BundleProperty and
    // set the name attribute to the name of the corresponding input-property in deploy.xml.

    private String bundleFileName;
    private String bundleName;
    private String bundleVersion;
    private String clusterDir;
    private String basedir;
    private Integer numNodes;
    private Boolean embedded;
    private String loggingLevel;
    private Long ringDelay;
    private Integer numTokens;
    private Integer nativeTransportPort;
    private Integer rpcPort;
    private Integer nativeTransportMaxThreads;
    private String username;
    private String password;
    private String authenticator;
    private String authorizer;
    private String dataDir;
    private String commitLogDir;
    private String savedCachesDir;
    private String logDir;
    private String listenAddress;
    private String rpcAddress;
    private String passwordPropertiesFile;
    private String accessPropertiesFile;
    private Integer jmxPort;
    private Integer storagePort;
    private Integer sslStoragePort;
    private String seeds;
    private String heapSize;
    private String heapNewSize;
    private String logFileName;

    public DeploymentOptions() {
    }

    /**
     * Initializes any properties that are not already set. Values are assigned from
     * system properties and from the cassandra.properties file that is expected to
     * be on the classpath. System properties are given precedence over corresponding
     * properties in cassandra.properties.
     *
     * @throws IOException If an error occurs loading cassandra.properties
     */
    public void load() throws IOException {
        if (loaded) {
            return;
        }
        InputStream stream = null;
        try {
            stream = getClass().getResourceAsStream("/cassandra.properties");
            Properties props = new Properties();
            props.load(stream);

            init(props);
            loaded = true;
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
        setEmbedded(Boolean.valueOf(loadProperty("rhq.cassandra.cluster.is-embedded", properties)));
        setLoggingLevel(loadProperty("rhq.cassandra.logging.level", properties));

        String ringDelay = loadProperty("rhq.cassandra.ring.delay", properties);
        if (ringDelay != null && !ringDelay.isEmpty()) {
            setRingDelay(Long.valueOf(ringDelay));
        }

        setNumTokens(Integer.valueOf(loadProperty("rhq.cassandra.num-tokens", properties)));
        setNativeTransportPort(Integer.valueOf(loadProperty("rhq.cassandra.native-transport-port", properties)));
        setRpcPort(Integer.valueOf(loadProperty("rhq.cassandra.rpc-port", properties)));
        setNativeTransportMaxThreads(Integer.valueOf(loadProperty("rhq.casandra.native-transport-max-threads",
            properties)));
        setUsername(loadProperty("rhq.cassandra.username", properties));
        setPassword(loadProperty("rhq.cassandra.password", properties));
        setAuthenticator(loadProperty("rhq.cassandra.authenticator", properties));
        setAuthorizer(loadProperty("rhq.cassandra.authorizer", properties));
        setDataDir(loadProperty("rhq.cassandra.data.dir", properties));
        setCommitLogDir(loadProperty("rhq.cassandra.commitlog.dir", properties));
        setSavedCachesDir(loadProperty("rhq.cassandra.saved.caches.dir", properties));
        setLogDir(loadProperty("rhq.cassandra.log.dir", properties));
        setLogFileName(loadProperty("rhq.cassandra.log.file.name", properties));
        setListenAddress(loadProperty("rhq.cassandra.listen.address", properties));
        setRpcAddress(loadProperty("rhq.cassandra.rpc.address", properties));
        setPasswordPropertiesFile(loadProperty("rhq.cassandra.password.properties.file", properties));
        setAccessPropertiesFile(loadProperty("rhq.cassandra.access.properties.file", properties));
        setJmxPort(Integer.valueOf(loadProperty("rhq.cassandra.jmx.port", properties)));
        setStoragePort(Integer.valueOf(loadProperty("rhq.cassandra.storage.port", properties)));
        setSslStoragePort(Integer.valueOf(loadProperty("rhq.cassandra.ssl.storage.port", properties)));
        setSeeds(loadProperty("rhq.cassandra.seeds", properties));
        setBasedir(loadProperty("rhq.cassandra.basedir", properties));
        setHeapSize(loadProperty("rhq.cassandra.max.heap.size", properties));
        setHeapNewSize(loadProperty("rhq.cassandra.heap.new.size", properties));
    }

    private String loadProperty(String key, Properties properties) {
        String value = System.getProperty(key);
        if (value == null || value.isEmpty()) {
            return properties.getProperty(key);
        }
        return value;
    }

    public void merge(DeploymentOptions other) {
        setBundleFileName(other.bundleFileName);
        setBundleName(other.bundleName);
        setBundleVersion(other.bundleVersion);
        setClusterDir(other.clusterDir);
        setNumNodes(other.numNodes);
        setEmbedded(other.embedded);
        setLoggingLevel(other.loggingLevel);
        setRingDelay(other.ringDelay);
        setNumTokens(other.numTokens);
        setNativeTransportPort(other.nativeTransportPort);
        setNativeTransportMaxThreads(other.nativeTransportMaxThreads);
        setUsername(other.username);
        setPassword(other.password);
        setAuthenticator(other.authenticator);
        setAuthorizer(other.authorizer);
        setPasswordPropertiesFile(other.passwordPropertiesFile);
        setAccessPropertiesFile(other.accessPropertiesFile);
        setDataDir(other.dataDir);
        setCommitLogDir(other.commitLogDir);
        setSavedCachesDir(other.savedCachesDir);
        setLogDir(other.logDir);
        setLogFileName(other.logFileName);
        setListenAddress(other.listenAddress);
        setRpcAddress(other.rpcAddress);
        setRpcPort(other.rpcPort);
        setJmxPort(other.jmxPort);
        setStoragePort(other.storagePort);
        setSslStoragePort(other.sslStoragePort);
        setSeeds(other.seeds);
        setBasedir(other.basedir);
        setHeapSize(other.heapSize);
        setHeapNewSize(other.heapNewSize);
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

    @BundleProperty(name = "cluster.dir")
    public String getClusterDir() {
        return clusterDir;
    }

    public void setClusterDir(String dir) {
        if (clusterDir == null) {
            clusterDir = dir;
        }
    }

    @BundleProperty(name = "rhq.deploy.dir")
    public String getBasedir() {
        return basedir;
    }

    public void setBasedir(String dir) {
        if (basedir == null) {
            basedir = dir;
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

    public boolean isEmbedded() {
        return embedded;
    }

    public void setEmbedded(boolean embedded) {
        if (this.embedded == null) {
            this.embedded = embedded;
        }
    }

    @BundleProperty(name = "logging.level")
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

    @BundleProperty(name = "rhq.cassandra.num_tokens")
    public Integer getNumTokens() {
        return numTokens;
    }

    public void setNumTokens(int numTokens) {
        if (this.numTokens == null) {
            this.numTokens = numTokens;
        }
    }

    @BundleProperty(name = "rhq.cassandra.native_transport_port")
    public Integer getNativeTransportPort() {
        return nativeTransportPort;
    }

    public void setNativeTransportPort(Integer port) {
        if (nativeTransportPort == null) {
            nativeTransportPort = port;
        }
    }

    @BundleProperty(name = "rhq.cassandra.rpc_port")
    public Integer getRpcPort() {
        return rpcPort;
    }

    public void setRpcPort(Integer port) {
        if (rpcPort == null) {
            rpcPort = port;
        }
    }

    @BundleProperty(name = "rhq.casandra.native_transport_max_threads")
    public Integer getNativeTransportMaxThreads() {
        return nativeTransportMaxThreads;
    }

    public void setNativeTransportMaxThreads(int numThreads) {
        if (nativeTransportMaxThreads == null) {
            nativeTransportMaxThreads = numThreads;
        }
    }

    @BundleProperty(name = "rhq.cassandra.username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        if (this.username == null) {
            this.username = username;
        }
    }

    @BundleProperty(name = "rhq.cassandra.password")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (this.password == null) {
            this.password = password;
        }
    }

    @BundleProperty(name = "rhq.cassandra.authenticator")
    public String getAuthenticator() {
        return authenticator;
    }

    public void setAuthenticator(String authenticator) {
        if (this.authenticator == null) {
            this.authenticator = authenticator;
        }
    }

    @BundleProperty(name = "rhq.cassandra.authorizer")
    public String getAuthorizer() {
        return authorizer;
    }

    public void setAuthorizer(String authorizer) {
        if (this.authorizer == null) {
            this.authorizer = authorizer;
        }
    }

    @BundleProperty(name = "data.dir")
    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dir) {
        if (dataDir == null) {
            dataDir = dir;
        }
    }

    @BundleProperty(name = "commitlog.dir")
    public String getCommitLogDir() {
        return commitLogDir;
    }

    public void setCommitLogDir(String dir) {
        if (commitLogDir == null) {
            commitLogDir = dir;
        }
    }

    @BundleProperty(name = "saved.caches.dir")
    public String getSavedCachesDir() {
        return savedCachesDir;
    }

    public void setSavedCachesDir(String dir) {
        if (savedCachesDir == null) {
            savedCachesDir = dir;
        }
    }

    @BundleProperty(name = "log.dir")
    public String getLogDir() {
        return logDir;
    }

    public void setLogDir(String dir) {
        if (logDir == null) {
            logDir = dir;
        }
    }

    @BundleProperty(name = "rhq.cassandra.log.file.name")
    public String getLogFileName() {
        return logFileName;
    }

    public void setLogFileName(String name) {
        if (logFileName == null) {
            logFileName = name;
        }
    }

    @BundleProperty(name = "listen.address")
    public String getListenAddress() {
        return listenAddress;
    }

    public void setListenAddress(String address) {
        if (listenAddress == null) {
            listenAddress = address;
        }
    }

    @BundleProperty(name = "rpc.address")
    public String getRpcAddress() {
        return rpcAddress;
    }

    public void setRpcAddress(String address) {
        if (rpcAddress == null) {
            rpcAddress = address;
        }
    }

    @BundleProperty(name = "rhq.cassandra.password.properties.file")
    public String getPasswordPropertiesFile() {
        return passwordPropertiesFile;
    }

    public void setPasswordPropertiesFile(String file) {
        if (passwordPropertiesFile == null) {
            passwordPropertiesFile = file;
        }
    }

    @BundleProperty(name = "rhq.cassandra.access.properties.file")
    public String getAccessPropertiesFile() {
        return accessPropertiesFile;
    }

    public void setAccessPropertiesFile(String file) {
        if (accessPropertiesFile == null) {
            accessPropertiesFile = file;
        }
    }

    @BundleProperty(name = "jmx.port")
    public Integer getJmxPort() {
        return jmxPort;
    }

    public void setJmxPort(Integer port) {
        if (jmxPort == null) {
            jmxPort = port;
        }
    }

    @BundleProperty(name = "rhq.cassandra.storage.port")
    public Integer getStoragePort() {
        return storagePort;
    }

    public void setStoragePort(Integer port) {
        if (storagePort == null) {
            storagePort = port;
        }
    }

    @BundleProperty(name = "rhq.cassandra.ssl.storage.port")
    public Integer getSslStoragePort() {
        return sslStoragePort;
    }

    public void setSslStoragePort(Integer port) {
        if (sslStoragePort == null) {
            sslStoragePort = port;
        }
    }

    @BundleProperty(name = "seeds")
    public String getSeeds() {
        return seeds;
    }

    public void setSeeds(String seeds) {
        if (this.seeds == null) {
            this.seeds = seeds;
        }
    }

    @BundleProperty(name = "rhq.cassandra.max.heap.size")
    public String getHeapSize() {
        return heapSize;
    }

    public void setHeapSize(String heapSize) {
        if (this.heapSize == null) {
            this.heapSize = heapSize;
        }
    }

    @BundleProperty(name = "rhq.cassandra.heap.new.size")
    public String getHeapNewSize() {
        return heapNewSize;
    }

    public void setHeapNewSize(String heapNewSize) {
        if (this.heapNewSize == null) {
            this.heapNewSize = heapNewSize;
        }
    }

}
