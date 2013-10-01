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

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.TokenReplacingProperties;
import org.rhq.core.util.file.FileUtil;

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
    // setClusterDir below for an example.

    private String clusterDir;
    private String basedir;
    private Integer numNodes;
    private Boolean embedded;
    private String loggingLevel;
    private Integer numTokens;
    private Integer cqlPort;
    private Boolean startRpc;
    private Integer rpcPort;
    private Integer nativeTransportMaxThreads;
    private String username;
    private String password;
    private String authenticator;
    private String authorizer;
    private String dataDir;
    private String commitLogDir;
    private String savedCachesDir;
    private String listenAddress;
    private String rpcAddress;
    private Integer jmxPort;
    private Integer gossipPort;
    private String seeds;
    private String heapSize;
    private String heapNewSize;
    private String logFileName;
    private String stackSize;

    DeploymentOptions() {
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
        setUsername(loadProperty("rhq.storage.username", properties));
        setPassword(loadProperty("rhq.storage.password", properties));

        setBasedir(loadProperty("rhq.storage.basedir", properties));
        setClusterDir(loadProperty("rhq.storage.cluster.dir", properties));
        setNumNodes(Integer.parseInt(loadProperty("rhq.storage.cluster.num-nodes", properties)));
        setEmbedded(Boolean.valueOf(loadProperty("rhq.storage.cluster.is-embedded", properties)));

        setLoggingLevel(loadProperty("rhq.storage.logging.level", properties));
        setLogFileName(loadProperty("rhq.storage.log.file", properties));

        setRpcPort(Integer.valueOf(loadProperty("rhq.storage.rpc-port", properties)));
        setCqlPort(Integer.valueOf(loadProperty("rhq.storage.cql-port", properties)));
        setJmxPort(Integer.valueOf(loadProperty("rhq.storage.jmx-port", properties)));
        setGossipPort(Integer.valueOf(loadProperty("rhq.storage.gossip-port", properties)));

        setNumTokens(Integer.valueOf(loadProperty("rhq.storage.num-tokens", properties)));
        setNativeTransportMaxThreads(Integer.valueOf(loadProperty("rhq.storage.native-transport-max-threads",
            properties)));

        setAuthenticator(loadProperty("rhq.storage.authenticator", properties));
        setAuthorizer(loadProperty("rhq.storage.authorizer", properties));

        setDataDir(loadProperty("rhq.storage.data", properties));
        setCommitLogDir(loadProperty("rhq.storage.commitlog", properties));
        setSavedCachesDir(loadProperty("rhq.storage.saved-caches", properties));

        setSeeds(loadProperty("rhq.storage.seeds", properties));
        setListenAddress(loadProperty("rhq.storage.listen.address", properties));
        setStartRpc(Boolean.valueOf(loadProperty("rhq.storage.start_rpc", properties)));
        setRpcAddress(loadProperty("rhq.storage.rpc.address", properties));

        setHeapSize(loadProperty("rhq.storage.heap-size", properties));
        setHeapNewSize(loadProperty("rhq.storage.heap-new-size", properties));
        setStackSize(loadProperty("rhq.storage.stack-size", properties));
    }

    private String loadProperty(String key, Properties properties) {
        String value = System.getProperty(key);
        if (value == null || value.isEmpty()) {
            return properties.getProperty(key);
        }
        return value;
    }

    public void merge(DeploymentOptions other) {
        setClusterDir(other.clusterDir);
        setNumNodes(other.numNodes);
        setEmbedded(other.embedded);
        setLoggingLevel(other.loggingLevel);
        setNumTokens(other.numTokens);
        setCqlPort(cqlPort);
        setNativeTransportMaxThreads(other.nativeTransportMaxThreads);
        setUsername(other.username);
        setPassword(other.password);
        setAuthenticator(other.authenticator);
        setAuthorizer(other.authorizer);
        setDataDir(other.dataDir);
        setCommitLogDir(other.commitLogDir);
        setSavedCachesDir(other.savedCachesDir);
        setLogFileName(other.logFileName);
        setListenAddress(other.listenAddress);
        setRpcAddress(other.rpcAddress);
        setStartRpc(other.startRpc);
        setRpcPort(other.rpcPort);
        setJmxPort(other.jmxPort);
        setGossipPort(other.gossipPort);
        setSeeds(other.seeds);
        setBasedir(other.basedir);
        setHeapSize(other.heapSize);
        setHeapNewSize(other.heapNewSize);
        setStackSize(other.stackSize);
    }

    public TokenReplacingProperties toMap()  {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(DeploymentOptions.class);
            Map<String, String> properties = new TreeMap<String, String>();

            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (pd.getReadMethod() == null) {
                    throw new RuntimeException("The [" + pd.getName() + "] property must define a getter method");
                }
                Method method = pd.getReadMethod();
                DeploymentProperty deploymentProperty = method.getAnnotation(DeploymentProperty.class);
                if (deploymentProperty != null) {
                    Object value = method.invoke(this, null);
                    if (value != null) {
                        properties.put(deploymentProperty.name(), value.toString());
                    }
                }
            }
            return new TokenReplacingProperties(properties);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert " + DeploymentOptions.class.getName() + " to a map", e);
        }
    }

    /**
     * @return The directory in which nodes will be installed. This only applies to
     * embedded clusters.
     */
    @DeploymentProperty(name = "cluster.dir")
    public String getClusterDir() {
        return clusterDir;
    }

    /**
     * @param dir The directory in which nodes will be installed. This only applies to
     *            embedded clusters.
     */
    public void setClusterDir(String dir) {
        if (clusterDir == null) {
            clusterDir = FileUtil.useForwardSlash(dir);
        }
    }

    /**
     * @return The directory in which the node will be installed.
     */
    @DeploymentProperty(name = "rhq.storage.basedir")
    public String getBasedir() {
        return basedir;
    }

    /**
     * @param dir The directory in which the node will be installed.
     */
    public void setBasedir(String dir) {
        if (basedir == null) {
            basedir = FileUtil.useForwardSlash(dir);
        }
    }

    /**
     * @return The number of nodes in the cluster. This only applies to embedded clusters.
     */
    @DeploymentProperty(name = "rhq.storage.cluster.num-nodes")
    public int getNumNodes() {
        return numNodes;
    }

    /**
     * @param numNodes The number of nodes in the cluster. This only applies to embedded
     *                 clusters.
     */
    public void setNumNodes(int numNodes) {
        if (this.numNodes == null) {
            this.numNodes = numNodes;
        }
    }

    /**
     * @return true is this is an embedded deployment, false otherwise. Note that an
     * embedded cluster is one in which all nodes run on a single host and can only accept
     * requests from that same host.
     */
    @DeploymentProperty(name = "rhq.storage.cluster.is-embedded")
    public boolean isEmbedded() {
        return embedded;
    }

    /**
     * @param embedded A flag that indicates whether or not this is an embedded deployment.
     * Note than embedded cluster is one in which all nodes run on a single host and can
     * only accept requests from that same host.
     */
    public void setEmbedded(boolean embedded) {
        if (this.embedded == null) {
            this.embedded = embedded;
        }
    }

    /**
     * @return The log4j logging level that Cassandra uses
     */
    @DeploymentProperty(name = "rhq.storage.logging.level")
    public String getLoggingLevel() {
        return loggingLevel;
    }

    /**
     * @param loggingLevel The log4j logging level that Cassandra uses
     */
    public void setLoggingLevel(String loggingLevel) {
        if (this.loggingLevel == null) {
            this.loggingLevel = loggingLevel;
        }
    }

    /**
     * @return The number of tokens assigned to this the node on the ring. Defaults to 256.
     */
    @DeploymentProperty(name = "rhq.storage.num_tokens")
    public Integer getNumTokens() {
        return numTokens;
    }

    /**
     * @param numTokens The number of tokens assigned to this node on the ring. Defaults to
     * 256.
     */
    public void setNumTokens(int numTokens) {
        if (this.numTokens == null) {
            this.numTokens = numTokens;
        }
    }

    /**
     * @return The port on which Cassandra listens for client requests.
     */
    @DeploymentProperty(name = "rhq.storage.cql-port")
    public Integer getCqlPort() {
        return cqlPort;
    }

    /**
     * @param port The port on which Cassandra listens for client requests.
     */
    public void setCqlPort(Integer port) {
        if (cqlPort == null) {
            cqlPort = port;
        }
    }

    /**
     * @return true whether the Thrift-based RPC should be started
     */
    @DeploymentProperty(name = "rhq.storage.start_rpc")
    public Boolean getStartRpc() {
        return startRpc;
    }

    /**
     * @param startRpc whether the Thrift-based RPC should be started
     */
    public void setStartRpc(Boolean startRpc) {
        if (this.startRpc == null) {
            this.startRpc = startRpc;
        }
    }

    @DeploymentProperty(name = "rhq.storage.rpc_port")
    public Integer getRpcPort() {
        return rpcPort;
    }

    public void setRpcPort(Integer port) {
        if (rpcPort == null) {
            rpcPort = port;
        }
    }

    /**
     * @return The max number of threads to handle CQL requests
     */
    @DeploymentProperty(name = "rhq.storage.native_transport_max_threads")
    public Integer getNativeTransportMaxThreads() {
        return nativeTransportMaxThreads;
    }

    /**
     * @param numThreads The max number of threads to handle CQL requests
     */
    public void setNativeTransportMaxThreads(Integer numThreads) {
        if (nativeTransportMaxThreads == null) {
            nativeTransportMaxThreads = numThreads;
        }
    }

    /**
     * @return The username RHQ will use to make client connections to Cassandra. This is
     * <strong>not</strong> a Cassandra configuration property. This deployment property is
     * written to rhq-server.properties at build time by the rhq-container.build.xml script.
     */
    @DeploymentProperty(name = "rhq.storage.username")
    public String getUsername() {
        return username;
    }

    /**
     * @param username The username RHQ will use to make client connections to Cassandra.
     * This is <strong>not</strong> a Cassandra configuration property. This deployment
     * property is written to rhq-server.properties at build time by the
     * rhq-container.build.xml script.
     */
    public void setUsername(String username) {
        if (this.username == null) {
            this.username = username;
        }
    }

    /**
     * @return The password RHQ will use to make client connections to Cassandra. This is
     * <strong>not</strong> a Cassandra configuration property. This deployment property is
     * written to rhq-server.properties at build time by the rhq-container.build.xml script.
     */
    @DeploymentProperty(name = "rhq.storage.password")
    public String getPassword() {
        return password;
    }

    /**
     * @param password The password RHQ will use to make client connections to Cassandra.
     * This is <strong>not</strong> a Cassandra configuration property. This deployment
     * property is written to rhq-server.properties at build time by the
     * rhq-container.build.xml script.
     */
    public void setPassword(String password) {
        if (this.password == null) {
            this.password = password;
        }
    }

    /**
     * @return The FQCN of the class that handles Cassandra authentication
     */
    @DeploymentProperty(name = "rhq.storage.authenticator")
    public String getAuthenticator() {
        return authenticator;
    }

    /**
     * @param authenticator The FQCN of the class that handles Cassandra authentication
     */
    public void setAuthenticator(String authenticator) {
        if (this.authenticator == null) {
            this.authenticator = authenticator;
        }
    }

    /**
     * @return The FQCN of the class that handles Cassandra authorization
     */
    @DeploymentProperty(name = "rhq.storage.authorizer")
    public String getAuthorizer() {
        return authorizer;
    }

    /**
     * @param authorizer The FQCN of the class that handles Cassandra authorization
     */
    public void setAuthorizer(String authorizer) {
        if (this.authorizer == null) {
            this.authorizer = authorizer;
        }
    }

    /**
     * @return The directory where Cassandra stores data on disk
     */
    @DeploymentProperty(name = "rhq.storage.data")
    public String getDataDir() {
        return dataDir;
    }

    /**
     * @param dir The directory where Cassandra stores data on disk
     */
    public void setDataDir(String dir) {
        if (dataDir == null) {
            dataDir = FileUtil.useForwardSlash(dir);
        }
    }

    /**
     * @return The directory where Cassandra stores commit log files
     */
    @DeploymentProperty(name = "rhq.storage.commitlog")
    public String getCommitLogDir() {
        return commitLogDir;
    }

    /**
     * @param dir The directory where Cassandra stores commit log files
     */
    public void setCommitLogDir(String dir) {
        if (commitLogDir == null) {
            commitLogDir = FileUtil.useForwardSlash(dir);
        }
    }

    /**
     * @return The directory where Cassandra stores saved caches on disk
     */
    @DeploymentProperty(name = "rhq.storage.saved-caches")
    public String getSavedCachesDir() {
        return savedCachesDir;
    }

    /**
     * @param dir The direcotry where Cassandra stores saved caches on disk
     */
    public void setSavedCachesDir(String dir) {
        if (savedCachesDir == null) {
            savedCachesDir = FileUtil.useForwardSlash(dir);
        }
    }

    /**
     * @return The full path of the Log4J log file to which Cassandra writes.
     */
    @DeploymentProperty(name = "rhq.storage.log.file")
    public String getLogFileName() {
        return logFileName;
    }

    /**
     * @param name The full path of the Log4J log file to which Cassandra writes.
     */
    public void setLogFileName(String name) {
        if (logFileName == null) {
            logFileName = FileUtil.useForwardSlash(name);
        }
    }

    /**
     * @return The address to which Cassandra binds and tells other node to connect to
     */
    @DeploymentProperty(name = "rhq.storage.listen.address")
    public String getListenAddress() {
        return listenAddress;
    }

    /**
     * @param address The address to which Cassandra binds and tells other nodes to connect to
     */
    public void setListenAddress(String address) {
        if (listenAddress == null) {
            listenAddress = address;
        }
    }

    @DeploymentProperty(name = "rpc.address")
    public String getRpcAddress() {
        return rpcAddress;
    }

    public void setRpcAddress(String address) {
        if (rpcAddress == null) {
            rpcAddress = address;
        }
    }

    /**
     * @return The port on which Cassandra listens for JMX connections
     */
    @DeploymentProperty(name = "rhq.storage.jmx-port")
    public Integer getJmxPort() {
        return jmxPort;
    }

    /**
     * @param port The port on which Cassandra listens for JMX connections
     */
    public void setJmxPort(Integer port) {
        if (jmxPort == null) {
            jmxPort = port;
        }
    }

    /**
     * @return The port on which Cassandra listens for gossip requests
     */
    @DeploymentProperty(name = "rhq.storage.gossip-port")
    public Integer getGossipPort() {
        return gossipPort;
    }

    /**
     * @param port The port on which Cassandra listens for gossip requests
     */
    public void setGossipPort(Integer port) {
        if (gossipPort == null) {
            gossipPort = port;
        }
    }

    /**
     * @return A comma-delimited list of IP addresses/host names that are deemed contact
     * points during node start up to learn about the ring topology.
     */
    @DeploymentProperty(name = "rhq.storage.seeds")
    public String getSeeds() {
        return seeds;
    }

    /**
     * @param seeds A comma-delimited list of IP addresses/host names that are deemed
     * contact points during node start up to learn about the ring topology.
     */
    public void setSeeds(String seeds) {
        if (this.seeds == null) {
            this.seeds = seeds;
        }
    }

    /**
     * @return The value to use for both the max and min heap sizes. Defaults to
     * ${MAX_HEAP_SIZE} which allows the cassandra-env.sh script to determine the value.
     */
    @DeploymentProperty(name = "rhq.storage.heap-size")
    public String getHeapSize() {
        return heapSize;
    }

    /**
     * @param heapSize The value to use for both the max and min heap sizes. This needs to
     * be a value value recognized by the -Xmx and -Xms options such as 512M.
     */
    public void setHeapSize(String heapSize) {
        if (this.heapSize == null) {
            this.heapSize = heapSize;
        }
    }

    /**
     * @return The value to use for the size of the new generation. Defaults to
     * ${HEAP_NEWSIZE} which allows the cassandra-env.sh script to determine the value.
     */
    @DeploymentProperty(name = "rhq.storage.heap-new-size")
    public String getHeapNewSize() {
        return heapNewSize;
    }

    /**
     * @param heapNewSize The value to use for the size of the new generation. This needs
     * to be a valid value recognized by the -Xmn option such as 256M.
     * is passed directly to the -Xmn option so it
     */
    public void setHeapNewSize(String heapNewSize) {
        if (this.heapNewSize == null) {
            this.heapNewSize = heapNewSize;
        }
    }

    /**
     * @return The value to use for the JVM stack size. This is passed directly to the -Xss
     * JVM start up option.
     */
    @DeploymentProperty(name = "rhq.storage.stack-size")
    public String getStackSize() {
        return stackSize;
    }

    /**
     * @param size The value to use for the JVM stack size which is passed directly to the
     * -Xss JVM start up option.
     */
    public void setStackSize(String size) {
        if (stackSize == null) {
            stackSize = size;
        }
    }

}
