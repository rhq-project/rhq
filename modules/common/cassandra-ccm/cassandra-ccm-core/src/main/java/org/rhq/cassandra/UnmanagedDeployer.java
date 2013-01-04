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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.bundle.ant.AntLauncher;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * Performs unmanaged deployments of Cassandra nodes. The deployment is unmanaged in that
 * it happens outside of the agent. Although it is unmanaged, the same Ant-based bundle API
 * is used to execute the deployment.
 *
 * @author John Sanda
 */
public class UnmanagedDeployer {

    private final Log log = LogFactory.getLog(UnmanagedDeployer.class);

    private DeploymentOptions deploymentOptions;

    private File bundleDir;

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

    public void unpackBundle() throws CassandraException {
        try {
            File bundleZipFile = unpackBundleZipFile();
            bundleDir = unpackBundle(bundleZipFile);
        } catch (IOException e) {
            log.error("An error occurred while unpacking the bundle", e);
            throw new CassandraException("An error occurred while unpacking the bundle", e);
        }
    }

    public void cleanUpBundle() {
        if (bundleDir != null && bundleDir.exists()) {
            FileUtil.purge(bundleDir, true);
        }
    }

    public void deploy(DeploymentOptions options, int deploymentId) throws CassandraException {
        Properties bundleProperties = createBundleProperties(options, deploymentId);
        runAnt(bundleProperties, bundleDir);
    }

    private Properties createBundleProperties(DeploymentOptions options, int deploymentId) throws CassandraException {
        try {
            Properties properties = new Properties();
            properties.put("cluster.name", "rhq");
            properties.put("rhq.deploy.id", deploymentId);
            properties.put("rhq.deploy.phase", "install");

            BeanInfo beanInfo = Introspector.getBeanInfo(DeploymentOptions.class);

            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                if (pd.getReadMethod() == null) {
                    continue;
                }
                Method method = pd.getReadMethod();
                BundleProperty bundleProperty = method.getAnnotation(BundleProperty.class);
                if (bundleProperty != null) {
                    properties.put(bundleProperty.name(), method.invoke(options, null));
                }
            }
            return properties;
        } catch (Exception e) {
            throw new CassandraException("Failed to create bundle deployment properties", e);
        }
    }

    private void runAnt(Properties deployProps, File bundleDir) throws CassandraException {
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

    private String getLocalIPAddress(int i) {
        return "127.0.0." + i;
    }

}
