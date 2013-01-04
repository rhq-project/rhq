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

package org.rhq.enterprise.server.cassandra;

import static javax.ejb.TransactionAttributeType.NEVER;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobDataMap;
import org.quartz.SchedulerException;

import org.rhq.cassandra.UnmanagedDeployer;
import org.rhq.cassandra.CassandraException;
import org.rhq.cassandra.DeploymentOptions;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.util.PropertiesFileUpdate;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.core.CoreServerMBean;
import org.rhq.enterprise.server.resource.ResourceManagerLocal;
import org.rhq.enterprise.server.resource.ResourceNotFoundException;
import org.rhq.enterprise.server.resource.group.ResourceGroupManagerLocal;
import org.rhq.enterprise.server.scheduler.SchedulerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author John Sanda
 */
@Stateless
public class CassandraClusterManagerBean implements CassandraClusterManagerLocal {

    private final Log log = LogFactory.getLog(CassandraClusterManagerBean.class);

    @EJB
    private BundleManagerLocal bundleManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @EJB
    private ResourceManagerLocal resourceManager;

    @EJB
    private ResourceGroupManagerLocal resourceGroupManager;

    @EJB
    private SchedulerLocal scheduler;

    @Override
    @TransactionAttribute(NEVER)
    public void installBundle() throws CassandraException {
        Subject overlord = subjectManager.getOverlord();
        DeploymentOptions deploymentOptions = new DeploymentOptions();
        String bundleName = null;
        String bundleVersionString = null;
        String bundleFileName = null;
        Bundle bundle = null;
        try {
            deploymentOptions.load();
            File deployBaseDir = getDeployBaseDir();

            bundleName = deploymentOptions.getBundleName();
            bundleVersionString = deploymentOptions.getBundleVersion();
            bundleFileName = deploymentOptions.getBundleFileName();
            bundle = getBundle(overlord, bundleName);
        } catch (Exception e) {
            String msg = "Failed to create bundle [" + bundleName + "]";
            logException(msg, e);
            throw new CassandraException(msg, e);
        }

        BundleVersion bundleVersion = null;
        try {
            bundleVersion = getBundleVersion(overlord, bundle, bundleVersionString, bundleFileName);
        } catch (Exception e) {
            String msg = "Failed to create bundle version [" + bundleVersionString + "]";
            logException(msg, e);
            throw new CassandraException(msg, e);
        }

        String seeds;
        if (deploymentOptions.isEmbedded()) {
            UnmanagedDeployer deployer = new UnmanagedDeployer();
            deployer.setDeploymentOptions(deploymentOptions);
            seeds = deployer.getCassandraHosts();

            // need to save the seeds list back to rhq-server.properties.
            CoreServerMBean coreServer = LookupUtil.getCoreServer();
            File installDir = coreServer.getInstallDir();
            File binDir = new File(installDir, "bin");
            File serverProperties = new File(binDir, "rhq-server.properties");
            PropertiesFileUpdate propsFile = new PropertiesFileUpdate(serverProperties.getAbsolutePath());

            try {
                propsFile.update("rhq.cassandra.cluster.seeds", seeds);
            } catch (IOException e) {
                log.warn("Failed to update rhq.cassandra.cluster.seeds property in " + serverProperties, e);
            }

            // Commenting out the call to deploy as this was here only to support dev
            // environments. Now that the new AS 7 installer is in place, the embedded
            // cluster will be deployed during the auto installation that happens with the
            // dev-container and a separate script will be made available for managing the
            // embedded cluster.
            //
            //deployer.deploy();

        } else {
            seeds = System.getProperty("rhq.cassandra.cluster.seeds");
        }

        String jobTrigger = "CassandraClusterHeartBeatTrigger - " + UUID.randomUUID().toString();
        String jobGroup = CassandraClusterHeartBeatJob.JOB_NAME + "Group";

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(CassandraClusterHeartBeatJob.KEY_CONNECTION_TIMEOUT, "100");
        jobDataMap.put(CassandraClusterHeartBeatJob.KEY_CASSANDRA_HOSTS, seeds);

        try {
            scheduler.scheduleRepeatingJob(CassandraClusterHeartBeatJob.JOB_NAME, jobGroup, jobDataMap,
                CassandraClusterHeartBeatJob.class, true, true, 3000, 5000);
        } catch (SchedulerException e) {
            String msg = "Unable to schedule " + CassandraClusterHeartBeatJob.class.getSimpleName() + " job. The " +
                "server will reamin in maintenance mode without a manual override.";
            log.error(msg, e);
        }

//        Resource platform = getPlatform(overlord, hostname);
//        ResourceGroup group = getPlatformGroup(overlord, platform, hostname);
    }

    private Properties loadDeploymentProps() throws IOException {
        InputStream stream = null;
        try {
            stream = getClass().getResourceAsStream("/cassandra.properties");
            Properties props = new Properties();
            props.load(stream);

            return props;
        }  finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    String msg = "An error occurred while closing input stream on cassandra.properties";
                    logException(msg, e);
                }
            }
        }
    }

    private void logException(String msg, Exception e) {
        if (log.isDebugEnabled()) {
            log.debug(msg, e);
        } else if (log.isInfoEnabled()) {
            log.info(msg + ": " + e.getMessage());
        } else {
            log.warn(msg);
        }
    }

    private File getDeployBaseDir() {
        File jbossHomeDir = new File(System.getProperty("jboss.home.dir"));
        File basedir = jbossHomeDir.getParentFile().getParentFile();
        return new File(basedir, "cassandra/dev");
    }

    private Bundle getBundle(Subject overlord, String bundleName) throws Exception {
        BundleType bundleType = bundleManager.getBundleType(overlord, "Ant Bundle");

        BundleCriteria criteria = new BundleCriteria();
        criteria.addFilterBundleTypeName(bundleType.getName());
        criteria.addFilterName(bundleName);

        PageList<org.rhq.core.domain.bundle.Bundle> bundles = bundleManager.findBundlesByCriteria(overlord, criteria);

        if (bundles.isEmpty()) {
            return bundleManager.createBundle(overlord, bundleName, bundleName, bundleType.getId());
        }

        return bundles.get(0);
    }

    private BundleVersion getBundleVersion(Subject overlord, Bundle bundle, String bundleVersion, String bundleFileName)
        throws Exception {
        BundleVersionCriteria criteria = new BundleVersionCriteria();
        criteria.addFilterBundleId(bundle.getId());
        criteria.addFilterVersion(bundleVersion);
        criteria.fetchBundle(true);
        criteria.fetchBundleDeployments(true);

        PageList<BundleVersion> bundleVersions = bundleManager.findBundleVersionsByCriteria(overlord, criteria);

        if (bundleVersions.isEmpty()) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            StreamUtil.copy(getClass().getResourceAsStream(bundleFileName), outputStream);

            File tmpFile = File.createTempFile("rhq-cassandra-bundle.zip", null);
            try {
                InputStream stream = getClass().getResourceAsStream(bundleFileName);
                StreamUtil.copy(stream, new FileOutputStream(tmpFile));

                return bundleManager.createBundleVersionViaFile(overlord, tmpFile);
            } finally {
                if (tmpFile != null) {
                    tmpFile.delete();
                }
            }
        }
        return bundleVersions.get(0);
    }

    private BundleDestination getBundleDestination(Subject overlord, BundleVersion bundleVersion,
        String destinationName, ResourceGroup group, String deployDir) throws Exception {
        BundleDestinationCriteria criteria = new BundleDestinationCriteria();
        criteria.addFilterBundleId(bundleVersion.getBundle().getId());
        //criteria.addFilterBundleVersionId(bundleVersion.getId());
        criteria.addFilterGroupId(group.getId());

        PageList<BundleDestination> bundleDestinations = bundleManager.findBundleDestinationsByCriteria(overlord,
            criteria);

        if (bundleDestinations.isEmpty()) {
            return bundleManager.createBundleDestination(overlord, bundleVersion.getBundle().getId(),
                destinationName, destinationName, "Root File System", deployDir, group.getId());
        }

        for (BundleDestination destination : bundleDestinations) {
            if (destination.getDeployDir().equals(deployDir)) {
                return destination;
            }
        }

        throw new Exception("Unable to get bundle destination for [bundleId: " + bundleVersion.getBundle().getId() +
            ", bunldleVersionId: " + bundleVersion.getId() + ", destination: " +  destinationName + ", deployDir: " +
            deployDir + "]");
    }

    private File getClusterDir() {
        File jbossDir = new File(System.getProperty("jboss.home.dir"));
        File baseDir = jbossDir.getParentFile().getParentFile();
        File clusterDir = new File(baseDir, "cassandra");
        clusterDir.mkdir();

        return clusterDir;
    }

    private Resource getPlatform(Subject overlord, String hostname) {
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.setFiltersOptional(true);
        criteria.addFilterResourceKey(hostname);
        criteria.addFilterName(hostname);
        criteria.addFilterResourceCategories(ResourceCategory.PLATFORM);
        criteria.fetchResourceType(true);
        criteria.fetchExplicitGroups(true);

        PageList<Resource> resources = resourceManager.findResourcesByCriteria(overlord, criteria);

        if (resources.isEmpty()) {
            String msg = "Could not find platform with hostname " + hostname + ". The value that you specify for the " +
                "host argument should match either a platform's resource name and/or its resource key.";
            throw new ResourceNotFoundException(msg);
        }

        return resources.get(0);
    }

    private ResourceGroup getPlatformGroup(Subject overlord, Resource platform, String hostname) {
        String groupName = "RHQ Cassandra Cluster";

        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterExplicitResourceCategory(ResourceCategory.PLATFORM);
        criteria.addFilterName(groupName);

        PageList<ResourceGroup> groups = resourceGroupManager.findResourceGroupsByCriteria(overlord, criteria);

        if (groups.isEmpty()) {
            return createPlatformGroup(overlord, groupName, platform);
        }

        return groups.get(0);
    }

    private ResourceGroup createPlatformGroup(Subject overlord, String groupName, Resource resource) {
        ResourceGroup group = new ResourceGroup(groupName, resource.getResourceType());
        group.addExplicitResource(resource);

        return resourceGroupManager.createResourceGroup(overlord, group);
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
