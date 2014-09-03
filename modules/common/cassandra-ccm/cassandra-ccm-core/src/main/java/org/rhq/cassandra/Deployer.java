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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.StringUtil;
import org.rhq.core.util.TokenReplacingReader;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * Deployment consists of a few steps.
 *
 * <ol>
 *   <li>Unzip Cassandra to disk.</li>
 *   <li>Update configuration files like casssandra.yaml. This involves performing variable substitution.</li>
 *   <li>Update file permissions to make scripts in the bin directory executable.</li>
 * </ol>
 *
 * The values used in the variable substitution are supplied by an instance of {@link DeploymentOptions}.
 *
 * @author John Sanda
 */
public class Deployer {

    private final Log log = LogFactory.getLog(Deployer.class);

    private DeploymentOptions deploymentOptions;

    public void setDeploymentOptions(DeploymentOptions deploymentOptions) {
        this.deploymentOptions = deploymentOptions;
    }

    public void unzipDistro() throws DeploymentException {
        InputStream inputStream = getClass().getResourceAsStream("/cassandra.zip");
        File deployDir = new File(deploymentOptions.getBasedir());
        deployDir.mkdir();
        try {
            log.info("Unzipping storage node to " + deployDir);
            ZipUtil.unzipFile(inputStream, deployDir);
        } catch (IOException e) {
            log.error("An error occurred while unzipping the storage zip file", e);
            throw new DeploymentException("An error occurred while unzipping the storage zip file", e);
        }
    }

    public void applyConfigChanges() throws DeploymentException {
        File deployDir = new File(deploymentOptions.getBasedir());
        File confDir = new File(deployDir, "conf");
        Map<String, String> tokens = deploymentOptions.toMap();
        tokens.put("cluster.name", "rhq");

        applyConfigChanges(confDir, "cassandra.yaml", tokens);
        applyConfigChanges(confDir, "log4j-server.properties", tokens);
        applyChangesToCassandraJvmProps(confDir, deploymentOptions);

        // For windows, update the service wrapper env. It may not ne necessary to have updated cassandra-jvm.properties
        // as well as this file, but for now we'll update both, leaving the former as a dependably set file.
        if (File.separatorChar == '\\') {
            applyChangesToWindowsServiceWrapper(deployDir);
        }

        //        applyConfigChanges(confDir, "cassandra-env.sh", tokens);
    }

    private void applyConfigChanges(File confDir, String fileName, Map<String, String> tokens)
        throws DeploymentException {

        File filteredFile = new File(confDir, fileName);
        try {
            if (log.isInfoEnabled()) {
                log.info("Applying configuration changes to " + filteredFile);
            }
            File rhqFile = new File(confDir, "rhq." + fileName);
            TokenReplacingReader reader = new TokenReplacingReader(new FileReader(rhqFile), tokens);

            StreamUtil.copy(reader, new FileWriter(filteredFile));
            rhqFile.delete();
        } catch (IOException e) {
            log.error("An unexpected error occurred while apply configuration changes to " + filteredFile, e);
            throw new DeploymentException("An unexpected error occurred while apply configuration changes to "
                + filteredFile, e);
        }
    }

    private void applyChangesToCassandraJvmProps(File confDir, DeploymentOptions deploymentOptions)
        throws DeploymentException {

        File jvmPropsFile = new File(confDir, "cassandra-jvm.properties");
        try {
            log.info("Applying configuration changes to " + jvmPropsFile);

            PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(jvmPropsFile.getAbsolutePath());
            Properties properties = propertiesUpdater.loadExistingProperties();

            properties.setProperty("heap_min", "-Xms" + deploymentOptions.getHeapSize());
            properties.setProperty("heap_max", "-Xmx" + deploymentOptions.getHeapSize());
            properties.setProperty("heap_new", "-Xmn" + deploymentOptions.getHeapNewSize());
            properties.setProperty("thread_stack_size", "-Xss" + deploymentOptions.getStackSize());
            properties.setProperty("jmx_port", deploymentOptions.getJmxPort().toString());

            String javaVersion = System.getProperty("java.version");
            // The check here is taken right from cassandra-env.sh
            if ((!isOpenJDK() || javaVersion.compareTo("1.6.0") > 0)
                || (javaVersion.equals("1.6.0") && getJavaPatchVersion() > 23)) {
                properties.put("java_agent", "-javaagent:$CASSANDRA_HOME/lib/jamm-0.2.5.jar");
            }

            propertiesUpdater.update(properties);
        } catch (IOException e) {
            log.error("An error occurred while updating " + jvmPropsFile, e);
            throw new DeploymentException("An error occurred while updating " + jvmPropsFile, e);
        }
    }

    private boolean isOpenJDK() {
        String javaVMName = System.getProperty("java.vm.name");
        return javaVMName.startsWith("OpenJDK");
    }

    private boolean isJava1_6() {
        String javaVersion = System.getProperty("java.version");
        return javaVersion.startsWith("1.6.0");
    }

    private int getJavaPatchVersion() {
        String javaVersion = System.getProperty("java.version");
        int startIndex = javaVersion.indexOf('_');

        if (startIndex == -1) {
            return 0;
        }

        return Integer.parseInt(javaVersion.substring(startIndex + 1, javaVersion.length()));
    }

    public void applyChangesToWindowsServiceWrapper(File deployDir) throws DeploymentException {
        File wrapperDir = new File(deployDir, "../bin/wrapper");
        File wrapperEnvFile = new File(wrapperDir, "rhq-storage-wrapper.env");

        try {
            log.info("Applying configuration changes to " + wrapperEnvFile);

            PropertiesFileUpdate propertiesUpdater = new PropertiesFileUpdate(wrapperEnvFile.getAbsolutePath());
            Properties wrapperEnvProps = propertiesUpdater.loadExistingProperties();

            wrapperEnvProps.setProperty("set.heap_min", "-Xms" + deploymentOptions.getHeapSize());
            wrapperEnvProps.setProperty("set.heap_max", "-Xmx" + deploymentOptions.getHeapSize());
            wrapperEnvProps.setProperty("set.heap_new", "-Xmn" + deploymentOptions.getHeapNewSize());
            wrapperEnvProps.setProperty("set.thread_stack_size", "-Xss" + deploymentOptions.getStackSize());
            wrapperEnvProps.setProperty("set.jmx_port", deploymentOptions.getJmxPort().toString());
            // This is always on by default, just set it literally
            wrapperEnvProps.setProperty("set.heap_dump_on_OOMError", "-XX:+HeapDumpOnOutOfMemoryError");
            // This is always set to the bin directory initially
            wrapperEnvProps.setProperty("set.heap_dump_dir", "-XX:HeapDumpPath="
                + useForwardSlash(new File(deployDir, "bin").getAbsolutePath()));

            propertiesUpdater.update(wrapperEnvProps);
        } catch (IOException e) {
            log.error("An error occurred while updating " + wrapperEnvFile, e);
            throw new DeploymentException("An error occurred while updating " + wrapperEnvFile, e);
        }
    }

    /**
     * Ensure that the path uses only forward slash.
     * @param path
     * @return forward-slashed path, or null if path is null
     */
    private static String useForwardSlash(String path) {
        return (null != path) ? path.replace('\\', '/') : null;
    }

    public void updateFilePerms() {
        File deployDir = new File(deploymentOptions.getBasedir());
        File binDir = new File(deployDir, "bin");

        log.info("Updating file permissions in " + binDir);

        for (File f : binDir.listFiles()) {
            f.setExecutable(true);
        }
    }

    public void updateStorageAuthConf(Set<String> addresses) {
        File confDir = new File(deploymentOptions.getBasedir(), "conf");
        File authFile = new File(confDir, "rhq-storage-auth.conf");

        try {
            authFile.delete();
            StreamUtil.copy(new StringReader(StringUtil.collectionToString(addresses, "\n")), new FileWriter(authFile),
                true);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update " + authFile);
        }
    }

}
