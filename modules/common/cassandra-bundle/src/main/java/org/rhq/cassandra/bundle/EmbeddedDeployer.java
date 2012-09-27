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

import static org.rhq.core.util.StringUtil.collectionToString;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.rhq.bundle.ant.AntLauncher;
import org.rhq.cassandra.CassandraException;
import org.rhq.core.util.ZipUtil;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Sanda
 */
public class EmbeddedDeployer {

    public void deploy(DeploymentOptions deploymentOptions) throws CassandraException {
        Set<String> ipAddresses = calculateLocalIPAddresses(deploymentOptions.getNumNodes());
        File clusterDir = new File(deploymentOptions.getClusterDir());
        File installedMarker = new File(clusterDir, ".installed");

        if (installedMarker.exists()) {
            return;
        }

        FileUtil.purge(clusterDir, false);

        File bundleZipeFile = null;
        File bundleDir = null;

        try {
            bundleZipeFile = unpackBundleZipFile();
            bundleDir = unpackBundle(bundleZipeFile);

            for (int i = 0; i < deploymentOptions.getNumNodes(); ++i) {
                Set<String> seeds = getSeeds(ipAddresses, i + 1);
                int jmxPort = 7200 + i;

                Properties props = new Properties();
                props.put("cluster.name", "rhq");
                props.put("cluster.dir", clusterDir.getAbsolutePath());
                props.put("auto.bootstrap", "false");
                props.put("data.dir", "data");
                props.put("commitlog.dir", "commit_log");
                props.put("log.dir", "logs");
                props.put("saved.caches.dir", "saved_caches");
                props.put("hostname", getLocalIPAddress(i + 1));
                props.put("seeds", collectionToString(seeds));
                props.put("jmx.port", Integer.toString(jmxPort));
                props.put("initial.token", generateToken(i, deploymentOptions.getNumNodes()));
                props.put("rhq.deploy.dir", new File(clusterDir, "node" + i).getAbsolutePath());
                props.put("rhq.deploy.id", i);
                props.put("rhq.deploy.phase", "install");

                doLocalDeploy(props, bundleDir);
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
