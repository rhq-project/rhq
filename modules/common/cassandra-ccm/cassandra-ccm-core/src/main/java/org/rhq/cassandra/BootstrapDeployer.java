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

import static java.util.Arrays.asList;
import static org.rhq.core.util.StringUtil.collectionToString;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;

import org.rhq.bundle.ant.AntLauncher;
import org.rhq.core.pluginapi.util.ProcessExecutionUtility;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.ProcessExecution;
import org.rhq.core.system.ProcessExecutionResults;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class BootstrapDeployer {

    private final Log log = LogFactory.getLog(BootstrapDeployer.class);

    private DeploymentOptions deploymentOptions;

    public void setDeploymentOptions(DeploymentOptions deploymentOptions) {
        this.deploymentOptions = deploymentOptions;
    }

    public String getCassandraHosts() {
        StringBuilder hosts = new StringBuilder();
        for (int i = 0; i < deploymentOptions.getNumNodes(); ++i) {
            hosts.append(getLocalIPAddress(i + 1)).append(":9160,");
        }
        hosts.deleteCharAt(hosts.length() - 1);
        return hosts.toString();
    }

    public List<File> deploy() throws CassandraException {
        Set<String> ipAddresses = calculateLocalIPAddresses(deploymentOptions.getNumNodes());
        File clusterDir = new File(deploymentOptions.getClusterDir());
        File installedMarker = new File(clusterDir, ".installed");

        if (isClusterInstalled()) {
            return getNodeDirs(clusterDir);
        }

        FileUtil.purge(clusterDir, false);

        File bundleZipeFile = null;
        File bundleDir = null;
        List<File> nodeDirs = new LinkedList<File>();

        try {
            deploymentOptions.load();
            bundleZipeFile = unpackBundleZipFile();
            bundleDir = unpackBundle(bundleZipeFile);

            for (int i = 0; i < deploymentOptions.getNumNodes(); ++i) {
                Set<String> seeds = getSeeds(ipAddresses, i + 1);
                int jmxPort = 7200 + i;
                String address = getLocalIPAddress(i + 1);
                File nodeBasedir = new File(clusterDir, "node" + i);
                nodeDirs.add(nodeBasedir);

                Properties props = new Properties();
                props.put("cluster.name", "rhq");
                props.put("cluster.dir", clusterDir.getAbsolutePath());
                props.put("auto.bootstrap", deploymentOptions.isAutoDeploy());
                props.put("data.dir", "data");
                props.put("commitlog.dir", "commit_log");
                props.put("log.dir", "logs");
                props.put("saved.caches.dir", "saved_caches");
                props.put("hostname", address);
                props.put("seeds", collectionToString(ipAddresses));
                props.put("jmx.port", Integer.toString(jmxPort));
                props.put("initial.token", generateToken(i, deploymentOptions.getNumNodes()));
                props.put("rhq.deploy.dir", nodeBasedir.getAbsolutePath());
                props.put("rhq.deploy.id", i);
                props.put("rhq.deploy.phase", "install");
                props.put("listen.address", address);
                props.put("rpc.address", address);
                props.put("logging.level", deploymentOptions.getLoggingLevel());
                props.put("rhq.cassandra.username", deploymentOptions.getUsername());
                props.put("rhq.cassandra.password", deploymentOptions.getPassword());

                if (deploymentOptions.getRingDelay() != null) {
                    props.put("cassandra.ring.delay.property", "-Dcassandra.ring_delay_ms=");
                    props.put("cassandra.ring.delay", deploymentOptions.getRingDelay());
                }

                props.put("rhq.cassandra.node.num_tokens", deploymentOptions.getNumTokens());
                props.put("rhq.cassandra.authenticator", deploymentOptions.getAuthenticator());
                props.put("rhq.cassandra.authorizer", deploymentOptions.getAuthorizer());

                doLocalDeploy(props, bundleDir);
//                startNode(nodeBasedir);
//                if (i == 0) {
//                    waitForNodeToStart(10, address);
//                }
            }
            FileUtil.writeFile(new ByteArrayInputStream(new byte[] {0}), installedMarker);
        } catch (IOException e) {
            throw new CassandraException("Failed to deploy embedded cluster", e);
        } finally {
            if (bundleZipeFile != null) {
                bundleZipeFile.delete();
            }

            if (bundleDir != null) {
                FileUtil.purge(bundleDir, true);
            }
        }

        return nodeDirs;
    }

    public List<File> getNodeDirs(final File clusterDir) {
        return asList(clusterDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return dir.equals(clusterDir) && name.startsWith("node");
            }
        }));
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        BootstrapDeployer deployer = new BootstrapDeployer();

        DeploymentOptions deploymentOptions = new DeploymentOptions();
        try {
            deploymentOptions.setNumNodes(2);
            deploymentOptions.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load deployment options.", e);
        }
        deployer.setDeploymentOptions(deploymentOptions);
        try {
            deployer.deploy();
            PropertiesFileUpdate serverPropertiesUpdater = getServerProperties();

            String[] hostNames = getHostNames(deployer.getCassandraHosts());
            serverPropertiesUpdater.update("rhq.cassandra.cluster.seeds", StringUtil.arrayToString(hostNames));

            long end = System.currentTimeMillis();
            deployer.log.info("Finished installing embedded cluster in " + (end - start) + " ms");
        } catch (CassandraException e) {
            throw new RuntimeException("A deployment error occurred.", e);
        } catch (IOException e) {
            throw new RuntimeException("An error occurred while trying to update RHQ server properties", e);
        }
    }

    private static PropertiesFileUpdate getServerProperties() {
        String sysprop = System.getProperty("rhq.server.properties-file");
        if (sysprop == null) {
            throw new RuntimeException("The required system property [rhq.server.properties] is not defined.");
        }

        File file = new File(sysprop);
        if (!(file.exists() && file.isFile())) {
            throw new RuntimeException("System property [" + sysprop + "] points to in invalid file.");
        }

        return new PropertiesFileUpdate(file.getAbsolutePath());
    }

    private static String[] getHostNames(String hosts) {
        List<String> hostNames = new ArrayList<String>();
        for (String s : hosts.split(",")) {
            String[] params = s.split(":");
            hostNames.add(params[0]);
        }
        return hostNames.toArray(new String[hostNames.size()]);
    }

    private boolean isClusterInstalled() {
        File clusterDir = new File(deploymentOptions.getClusterDir());
        File installedMarker = new File(clusterDir, ".installed");

        if (installedMarker.exists()) {
            return true;
        }
        return false;
    }

    private void doLocalDeploy(Properties deployProps, File bundleDir) throws CassandraException {
        AntLauncher launcher = new AntLauncher();
        try {
            File recipeFile = new File(bundleDir, "deploy.xml");
            launcher.executeBundleDeployFile(recipeFile, deployProps, null);
        } catch (Exception e) {
            String msg = "Failed to execute local rhq cassandra bundle deployment";
            //logException(msg, e);
            throw new CassandraException(msg, e);
        }
    }

    private void startNode(File basedir) {
        File binDir = new File(basedir, "bin");
        File startScript;
        SystemInfo systemInfo = SystemInfoFactory.createSystemInfo();

        if (systemInfo.getOperatingSystemType() == OperatingSystemType.WINDOWS) {
            startScript = new File(binDir, "cassandra.bat");
        } else {
            startScript = new File(binDir, "cassandra");
        }

        ProcessExecution startScriptExe = ProcessExecutionUtility.createProcessExecution(startScript);
        startScriptExe.setArguments(asList("-p", "cassandra.pid"));

        ProcessExecutionResults results = systemInfo.executeProcess(startScriptExe);
    }

    private void waitForNodeToStart(int maxRetries, String host) throws CassandraException {
        int port = 9160;
        int timeout = 50;
        for (int i = 0; i < maxRetries; ++i) {
            TSocket socket = new TSocket(host, port, timeout);
            try {
                socket.open();
                return;
            } catch (TTransportException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                }
            }
        }
        Date timestamp = new Date();
        throw new CassandraException("[" + timestamp + "] Could not connect to " + host + " after " + maxRetries +
            " tries");
    }

    private File unpackBundleZipFile() throws IOException {
        InputStream bundleInputStream = getClass().getResourceAsStream("/cassandra-bundle.zip");
        File bundleZipFile = File.createTempFile("cassandra-bundle.zip", null);
        StreamUtil.copy(bundleInputStream, new FileOutputStream(bundleZipFile));

        return bundleZipFile;
    }

    private File unpackBundle(File bundleZipFile) throws IOException {
        File bundleDir = new File(System.getProperty("java.io.tmpdir"), "rhq-cassandra-bundle");
        bundleDir.mkdir();
        ZipUtil.unzipFile(bundleZipFile, bundleDir);

        return bundleDir;
    }

    private Set<String> calculateLocalIPAddresses(int numNodes) {
        Set<String> addresses = new HashSet<String>();
        for (int i = 1; i <= numNodes; ++i) {
            addresses.add(getLocalIPAddress(i));
        }
        return addresses;
    }

    private String getLocalIPAddress(int i) {
        return "127.0.0." + i;
    }

    private String generateToken(int i, int numNodes) {
        BigInteger num = new BigInteger("2").pow(127).divide(new BigInteger(Integer.toString(numNodes)));
        return num.multiply(new BigInteger(Integer.toString(i))).toString();
    }

    private Set<String> getSeeds(Set<String> addresses, int i) {
        Set<String> seeds = new HashSet<String>();
        String address = getLocalIPAddress(i);

        for (String nodeAddress : addresses) {
            if (nodeAddress.equals(address)) {
                continue;
            } else {
                seeds.add(nodeAddress);
            }
        }

        return seeds;
    }

}
