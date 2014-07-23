/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.ejb.EJB;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.transaction.TransactionManager;

import org.testng.AssertJUnit;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.asset.ClassAsset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.jboss.shrinkwrap.resolver.api.Resolvers;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolverSystem;

import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.shared.BuilderException;
import org.rhq.core.domain.shared.ResourceBuilder;
import org.rhq.core.domain.shared.ResourceTypeBuilder;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.exception.ThrowableUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.core.comm.ServerCommunicationsServiceMBean;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScanner;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScannerMBean;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.plugin.pc.ServerPluginServiceMBean;
import org.rhq.enterprise.server.scheduler.SchedulerService;
import org.rhq.enterprise.server.scheduler.SchedulerServiceMBean;
import org.rhq.enterprise.server.storage.FakeStorageClusterSettingsManagerBean;
import org.rhq.enterprise.server.storage.StorageClientManagerBean;
import org.rhq.enterprise.server.util.LookupUtil;
import org.rhq.test.AssertUtils;
import org.rhq.test.MatchResult;
import org.rhq.test.PropertyMatchException;
import org.rhq.test.PropertyMatcher;

public abstract class AbstractEJB3Test extends Arquillian {

    // this value should correspond with the value passed in the default constructor in
    // org.rhq.core.domain.criteria.Criteria class
    public static final int DEFAULT_CRITERIA_PAGE_SIZE = 200;

    protected static final String JNDI_RHQDS = "java:jboss/datasources/RHQDS";

    protected static File tmpdirRoot = new File("./target/test-tmpdir");

    private TestServerCommunicationsService agentService;
    private SchedulerService schedulerService;
    private ServerPluginService serverPluginService;
    private PluginDeploymentScannerMBean pluginScannerService;

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    protected EntityManager em;

    @ArquillianResource
    protected InitialContext initialContext;

    @EJB
    private StorageClientManagerBean storageClientManager;

    // We originally (in 4.2.3 days) ran these tests as "unit" tests in the server/jar module using
    // the embedded container.  With Arquillian it makes sense to actually deploy an EAR because
    // we need a way to deploy dependent ears needed to support the server/jar classes. But
    // building this jar up (as is done in core/domain) was too difficult due to the huge number
    // of dependencies. It was easier, and made sense, to use the already built rhq.ear
    // and run as true integration tests.  We do thin rhq.ear by removing all of the WAR files, and
    // deploy only the EJB jars, and the services, which are really the objects under test.

    @Deployment
    protected static EnterpriseArchive getBaseDeployment() {

        // Ensure the test working dir exists
        tmpdirRoot.mkdirs();

        // deploy the test classes in their own jar, under /lib
        JavaArchive testClassesJar = ShrinkWrap.create(JavaArchive.class, "test-classes.jar");
        testClassesJar = addClasses(testClassesJar, new File("target/test-classes/org"), null);

        // add non itests-2 RHQ classes used by the test classes, as well as needed resources
        testClassesJar.addClass(ThrowableUtil.class);
        testClassesJar.addClass(MessageDigestGenerator.class);
        testClassesJar.addClass(StreamUtil.class);
        testClassesJar.addClass(AssertUtils.class);
        testClassesJar.addClass(ResourceBuilder.class);
        testClassesJar.addClass(ResourceTypeBuilder.class);
        testClassesJar.addClass(BuilderException.class);
        testClassesJar.addClasses(PropertyMatcher.class, MatchResult.class, PropertyMatchException.class);
        testClassesJar.addAsManifestResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml")); // add CDI injection (needed by arquillian injection);
        //TOD0 (jshaughn): Once we have identified all of the necessary test resource files, see if we can just add the
        //      entire /resources dir in one statement
        testClassesJar.addAsResource("binary-blob-sample.jar");
        testClassesJar.addAsResource("test-alert-sender-serverplugin.xml");
        testClassesJar.addAsResource("test-assist-color-number.txt");
        testClassesJar.addAsResource("test-ldap.properties");
        testClassesJar.addAsResource("test-scheduler.properties");
        testClassesJar.addAsResource("cassandra-test.properties");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/configuration/metadata/configuration_metadata_manager_bean_test_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/configuration/metadata/configuration_metadata_manager_bean_test_v2.xml");
        testClassesJar.addAsResource("org/rhq/enterprise/server/discovery/DiscoveryBossBeanTest.xml");
        testClassesJar.addAsResource("org/rhq/enterprise/server/inventory/InventoryManagerBeanTest.xml");
        testClassesJar.addAsResource("org/rhq/enterprise/server/resource/metadata/MetadataTest.xml");

        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/AlertMetadataManagerBeanTest/plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/AlertMetadataManagerBeanTest/plugin_v2.xml");

        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/ContentMetadataManagerBeanTest/plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/ContentMetadataManagerBeanTest/plugin_v2.xml");

        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/EventMetadataManagerBeanTest/plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/EventMetadataManagerBeanTest/plugin_v2.xml");

        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/MeasurementMetadataManagerBeanTest/plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/MeasurementMetadataManagerBeanTest/plugin_v2.xml");

        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/MultiplePluginExtensionMetadataTest/child1_plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/MultiplePluginExtensionMetadataTest/child2_plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/MultiplePluginExtensionMetadataTest/parent_plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/MultiplePluginExtensionMetadataTest/parent_plugin_v2.xml");

        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/MultiplePluginExtensionSinglePluginDescriptorMetadataTest/child_plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/MultiplePluginExtensionSinglePluginDescriptorMetadataTest/parent_plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/MultiplePluginExtensionSinglePluginDescriptorMetadataTest/parent_plugin_v2.xml");

        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/OperationMetadataManagerBeanTest/plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/OperationMetadataManagerBeanTest/plugin_v2.xml");

        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/PluginExtensionMetadataTest/child_plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/PluginExtensionMetadataTest/parent_plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/PluginExtensionMetadataTest/parent_plugin_v2.xml");

        testClassesJar.addAsResource("org/rhq/enterprise/server/resource/metadata/PluginManagerBeanTest/plugin_1.xml");
        testClassesJar.addAsResource("org/rhq/enterprise/server/resource/metadata/PluginManagerBeanTest/plugin_2.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/PluginManagerBeanTest/plugin_3.1.xml");
        testClassesJar.addAsResource("org/rhq/enterprise/server/resource/metadata/PluginManagerBeanTest/plugin_3.xml");

        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/PluginScanningExtensionMetadataTest/child1_plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/PluginScanningExtensionMetadataTest/child2_plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/PluginScanningExtensionMetadataTest/parent_plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/PluginScanningExtensionMetadataTest/parent_plugin_v2.xml");

        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/ResourceMetadataManagerBeanTest/dup_drift.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/ResourceMetadataManagerBeanTest/parent_resource_type-plugin.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/ResourceMetadataManagerBeanTest/plugin_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/ResourceMetadataManagerBeanTest/plugin_v2.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/ResourceMetadataManagerBeanTest/remove_bundle_drift_config_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/ResourceMetadataManagerBeanTest/remove_bundle_drift_config_v2.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/ResourceMetadataManagerBeanTest/remove_types_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/ResourceMetadataManagerBeanTest/remove_types_v2.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/MetadataUpdateWithIgnoredTypesTest/remove_types_v1.xml");
        testClassesJar
            .addAsResource("org/rhq/enterprise/server/resource/metadata/MetadataUpdateWithIgnoredTypesTest/remove_types_v2.xml");

        testClassesJar.addAsResource("perftest/AvailabilityInsertPurgeTest-testOne-data.xml.zip");
        testClassesJar.addAsResource("serverplugins/simple-generic-serverplugin.xml");
        testClassesJar.addAsResource("test/deployment/1.0-feb-2.xml");
        testClassesJar.addAsResource("test/deployment/1.0-feb.xml");
        testClassesJar.addAsResource("test/deployment/1.0-june.xml");
        testClassesJar.addAsResource("test/deployment/1.1-feb.xml");
        testClassesJar.addAsResource("test/deployment/1.1-june.xml");
        testClassesJar.addAsResource("test/metadata/content-source-update-v1.xml");
        testClassesJar.addAsResource("test/metadata/content-source-update-v2.xml");
        testClassesJar.addAsResource("test/metadata/noTypes.xml");
        testClassesJar.addAsResource("test/metadata/alerts/type-with-metric.xml");
        testClassesJar.addAsResource("test/metadata/configuration/addDeleteTemplate1.xml");
        testClassesJar.addAsResource("test/metadata/configuration/addDeleteTemplate2.xml");
        testClassesJar.addAsResource("test/metadata/configuration/addDeleteTemplate3.xml");
        testClassesJar.addAsResource("test/metadata/configuration/constraint.xml");
        testClassesJar.addAsResource("test/metadata/configuration/constraintMinMax.xml");
        testClassesJar.addAsResource("test/metadata/configuration/propertyChanging-v1.xml");
        testClassesJar.addAsResource("test/metadata/configuration/propertyChanging-v2.xml");
        testClassesJar.addAsResource("test/metadata/configuration/propertyList-v1.xml");
        testClassesJar.addAsResource("test/metadata/configuration/propertyList-v2.xml");
        testClassesJar.addAsResource("test/metadata/configuration/propertyList-simple.xml");
        testClassesJar.addAsResource("test/metadata/configuration/propertyMap-v1.xml");
        testClassesJar.addAsResource("test/metadata/configuration/propertyMap-v2.xml");
        testClassesJar.addAsResource("test/metadata/configuration/groupDeleted-v1.xml");
        testClassesJar.addAsResource("test/metadata/configuration/groupDeleted-v2.xml");
        testClassesJar.addAsResource("test/metadata/configuration/groupPropDeleted-v1.xml");
        testClassesJar.addAsResource("test/metadata/configuration/groupPropDeleted-v2.xml");
        testClassesJar.addAsResource("test/metadata/configuration/groupPropDeleted-v3.xml");
        testClassesJar.addAsResource("test/metadata/configuration/groupPropDeleted-v4.xml");
        testClassesJar.addAsResource("test/metadata/configuration/groupPropMoved-v1.xml");
        testClassesJar.addAsResource("test/metadata/configuration/groupPropMoved-v2.xml");
        testClassesJar.addAsResource("test/metadata/configuration/update5-v1_0.xml");
        testClassesJar.addAsResource("test/metadata/configuration/update5-v2_0.xml");
        testClassesJar.addAsResource("test/metadata/configuration/updateDefaultTemplate1.xml");
        testClassesJar.addAsResource("test/metadata/configuration/updateDefaultTemplate2.xml");
        testClassesJar.addAsResource("test/metadata/events/event1-1.xml");
        testClassesJar.addAsResource("test/metadata/events/event1-2.xml");
        testClassesJar.addAsResource("test/metadata/measurement/measurementDeletion-v1_0.xml");
        testClassesJar.addAsResource("test/metadata/measurement/measurementDeletion-v2_0.xml");
        testClassesJar.addAsResource("test/metadata/measurement/update-v1_0.xml");
        testClassesJar.addAsResource("test/metadata/measurement/update-v2_0.xml");
        testClassesJar.addAsResource("test/metadata/measurement/update6-1.xml");
        testClassesJar.addAsResource("test/metadata/measurement/update6-2.xml");
        testClassesJar.addAsResource("test/metadata/measurement/update7-1.xml");
        testClassesJar.addAsResource("test/metadata/measurement/update7-2.xml");
        testClassesJar.addAsResource("test/metadata/natives/update5-v1_0.xml");
        testClassesJar.addAsResource("test/metadata/natives/update5-v2_0.xml");
        testClassesJar.addAsResource("test/metadata/operation/operation1-1.xml");
        testClassesJar.addAsResource("test/metadata/operation/operation1-2.xml");
        testClassesJar.addAsResource("test/metadata/operation/operation2-1.xml");
        testClassesJar.addAsResource("test/metadata/operation/operation2-2.xml");
        testClassesJar.addAsResource("test/metadata/operation/operation3-1.xml");
        testClassesJar.addAsResource("test/metadata/operation/operation3-2.xml");
        testClassesJar.addAsResource("test/metadata/operation/update3-v1_0.xml");
        testClassesJar.addAsResource("test/metadata/operation/update3-v2_0.xml");
        testClassesJar.addAsResource("test/metadata/resource/illegal-subcat-1.xml");
        testClassesJar.addAsResource("test/metadata/resource/nested-subcat-2children.xml");
        testClassesJar.addAsResource("test/metadata/resource/nested-subcat-grandchild.xml");
        testClassesJar.addAsResource("test/metadata/resource/nested-subcat-services-v1_0.xml");
        testClassesJar.addAsResource("test/metadata/resource/nested-subcat-services-v2_0.xml");
        testClassesJar.addAsResource("test/metadata/resource/nested-subcat-v1_0.xml");
        testClassesJar.addAsResource("test/metadata/resource/nested-subcat-v1_1.xml");
        testClassesJar.addAsResource("test/metadata/resource/nested-subcat-v2_0.xml");
        testClassesJar.addAsResource("test/metadata/resource/no-subcat.xml");
        testClassesJar.addAsResource("test/metadata/resource/one-subcat-v1_0.xml");
        testClassesJar.addAsResource("test/metadata/resource/one-subcat-v1_1.xml");
        testClassesJar.addAsResource("test/metadata/resource/one-subcat-v2_0.xml");
        testClassesJar.addAsResource("test/metadata/resource/one-subcat-v3_0.xml");
        testClassesJar.addAsResource("test/metadata/resource/services-v1_0.xml");
        testClassesJar.addAsResource("test/metadata/resource/services-v2_0.xml");
        testClassesJar.addAsResource("test/metadata/resource/test-subcategories.xml");
        testClassesJar.addAsResource("test/metadata/resource/test-subcategories2.xml");
        testClassesJar.addAsResource("test/metadata/resource/test-subcategories3.xml");
        testClassesJar.addAsResource("test/metadata/resource/two-subcat.xml");
        testClassesJar.addAsResource("test/metadata/resource/undefined-child-subcat-1.xml");
        testClassesJar.addAsResource("test/metadata/resource-type/duplicateResourceType.xml");
        testClassesJar.addAsResource("test/metadata/resource-type/update2-v1_0.xml");
        testClassesJar.addAsResource("test/metadata/resource-type/update2-v2_0.xml");
        testClassesJar.addAsResource("test/metadata/resource-type/update4-v1_0.xml");
        testClassesJar.addAsResource("test/metadata/resource-type/update4-v2_0.xml");
        testClassesJar.addAsResource("test/metadata/resource-type/updateResourceTypeBundleTarget-v1.xml");
        testClassesJar.addAsResource("test/metadata/resource-type/updateResourceTypeBundleTarget-v2.xml");

        testClassesJar.addAsResource("org/rhq/enterprise/server/plugins/ant/recipe-no-manageRootDir.xml");

        // create test ear by starting with rhq.ear and thinning it
        String projectVersion = System.getProperty("project.version");
        String rhqCoreClientApiVersion = System.getProperty("rhq-core-client-api.version");
        if ((rhqCoreClientApiVersion == null) || rhqCoreClientApiVersion.trim().isEmpty()) {
            rhqCoreClientApiVersion = projectVersion;
        }
        String rhqPlatformPluginVersion = System.getProperty("rhq-platform-plugin.version");
        if ((rhqPlatformPluginVersion == null) || rhqPlatformPluginVersion.trim().isEmpty()) {
            rhqPlatformPluginVersion = projectVersion;
        }
        MavenResolverSystem earResolver = Resolvers.use(MavenResolverSystem.class);
        // this must be named rhq.ear because the "rhq" portion is used in the jndi names
        earResolver.offline();
        EnterpriseArchive testEar = ShrinkWrap.create(EnterpriseArchive.class, "rhq.ear");
        EnterpriseArchive rhqEar = earResolver.resolve("org.rhq:rhq-enterprise-server-ear:ear:" + projectVersion)
            .withoutTransitivity().asSingle(EnterpriseArchive.class);
        // merge rhq.ear into testEar but include only the EJB jars and the supporting libraries. Note that we
        // don't include the services sar because tests are responsible for prepare/unprepare of all required services,
        // we don't want the production services performing any unexpected work.
        testEar = testEar.merge(rhqEar, Filters.include("/lib.*|/rhq.*ejb3\\.jar.*|/rhq-server.jar.*"));
        // remove startup beans and shutdown listeners, we don't want this to be a full server deployment. The tests
        // start/stop what they need, typically with test services or mocks.
        testEar.delete(ArchivePaths
            .create("/rhq-server.jar/org/rhq/enterprise/server/core/StartupBean.class"));
        testEar.delete(ArchivePaths
            .create("/rhq-server.jar/org/rhq/enterprise/server/core/StartupBean$1.class"));
        testEar.delete(ArchivePaths
            .create("/rhq-server.jar/org/rhq/enterprise/server/core/ShutdownListener.class"));
        testEar.delete(ArchivePaths
            .create("/rhq-server.jar/org/rhq/enterprise/server/storage/StorageClusterSettingsManagerBean.class"));

        //replace the above startup beans with stripped down versions
        testEar.add(new ClassAsset(StrippedDownStartupBean.class), ArchivePaths
            .create("/rhq-server.jar/org/rhq/enterprise/server/test/StrippedDownStartupBean.class"));
        testEar.add(new ClassAsset(StrippedDownStartupBeanPreparation.class), ArchivePaths
            .create("/rhq-server.jar/org/rhq/enterprise/server/test/"
                + "StrippedDownStartupBeanPreparation.class"));
        testEar.add(new ClassAsset(FakeStorageClusterSettingsManagerBean.class), ArchivePaths
            .create("/rhq-server.jar/org/rhq/enterprise/server/storage/FakeStorageClusterSettingsManagerBean.class"));
        testEar.addAsManifestResource(new ByteArrayAsset("<beans/>".getBytes()), ArchivePaths.create("beans.xml"));

        // add the test classes to the deployment
        testEar.addAsLibrary(testClassesJar);

        // add the necessary AS7 dependency modules
        testEar.addAsManifestResource("jboss-deployment-structure.xml");

        // add the application xml declaring the ejb jars
        testEar.setApplicationXML("application.xml");

        // add additional 3rd party dependent jars needed to support test classes
        Collection thirdPartyDeps = new ArrayList();
        thirdPartyDeps.add("joda-time:joda-time");
        thirdPartyDeps.add("org.jboss.shrinkwrap:shrinkwrap-impl-base");
        thirdPartyDeps.add("org.liquibase:liquibase-core");
        thirdPartyDeps.add("org.powermock:powermock-api-mockito");
        thirdPartyDeps.add("org.rhq.helpers:perftest-support:" + projectVersion);
        thirdPartyDeps.add("org.rhq:rhq-core-client-api:jar:" + rhqCoreClientApiVersion);
        thirdPartyDeps.add("org.rhq:rhq-platform-plugin:jar:" + rhqPlatformPluginVersion);

        thirdPartyDeps.add("org.rhq:test-utils:" + projectVersion);

        MavenResolverSystem resolver = Maven.resolver();

        Collection<JavaArchive> dependencies = new HashSet<JavaArchive>();
        dependencies.addAll(Arrays.asList(resolver.loadPomFromFile("pom.xml").resolve(thirdPartyDeps)
            .withTransitivity().as(JavaArchive.class)));

        // If we're running oracle we need to include the OJDBC driver because dbunit needs it. Note that we need
        // add it explicitly even though it is a provided module used by the datasource.
        if (!Boolean.valueOf(System.getProperty("rhq.skip.oracle"))) {
            // in proxy situations (like Jenkins) shrinkwrap won't be able to find repositories defined in
            // settings.xml profiles.  We know at this point the driver is in the local repo, try going offline
            // at this point to force local repo resolution since the oracle driver is not in public repos.
            // see http://stackoverflow.com/questions/6291146/arquillian-shrinkwrap-mavendependencyresolver-behind-proxy
            // Last verified this problem using: Arquillian 1.0.3 bom
            resolver.offline();
            dependencies.addAll(Arrays.asList(resolver
                .resolve("com.oracle:ojdbc6:jar:" + System.getProperty("rhq.ojdbc.version")).withTransitivity()
                .as(JavaArchive.class)));
        }

        // Transitive test dep required by rhq-core-client-api above and for some reason not sucked in.
        // TODO: pass in version from pom property
        dependencies.addAll(Arrays.asList(resolver.resolve("commons-jxpath:commons-jxpath:1.3").withTransitivity()
            .as(JavaArchive.class)));

        // exclude any transitive deps we don't want
        String[] excludeFilters = { "testng.*jdk", "rhq-core-domain.*jar" };
        dependencies = exclude(dependencies, excludeFilters);

        testEar.addAsLibraries(dependencies);

        // Print out the test EAR structure
        //System.out.println("** The Deployment EAR: " + testEar.toString(true) + "\n");

        // Save the test EAR to a zip file for inspection (set file explicitly)
        //String tmpDir = System.getProperty("java.io.tmpdir");
        //exportZip(testEar, new File(tmpDir, "test.ear"));

        return testEar;
    }

    @SuppressWarnings("unused")
    static private void exportZip(Archive<?> zipArchive, File outFile) {
        try {
            ZipExporter exporter = new ZipExporterImpl(zipArchive);
            exporter.exportTo(outFile, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param dependencies The current set of deps - THIS IS FILTERED, not copied
     * @param filters regex filters
     * @return the filtered set of deps
     */
    public static Collection<JavaArchive> exclude(Collection<JavaArchive> dependencies, String... filters) {

        for (String filter : filters) {
            Pattern p = Pattern.compile(filter);

            for (Iterator<JavaArchive> i = dependencies.iterator(); i.hasNext();) {
                JavaArchive dependency = i.next();
                if (p.matcher(dependency.getName()).find()) {
                    i.remove();
                }
            }
        }

        return dependencies;
    }

    /**
     * @param dependencies The current set of deps - this is not changed
     * @param filters regex filters
     * @return set set of JavaArchives in dependencies that matched an include filter
     */
    public static Collection<JavaArchive> include(Collection<JavaArchive> dependencies, String... filters) {

        Collection<JavaArchive> result = new HashSet<JavaArchive>();

        for (String filter : filters) {
            Pattern p = Pattern.compile(filter);

            for (Iterator<JavaArchive> i = dependencies.iterator(); i.hasNext();) {
                JavaArchive dependency = i.next();
                if (p.matcher(dependency.getName()).find()) {
                    result.add(dependency);
                }
            }
        }

        return result;
    }

    public static JavaArchive addClasses(JavaArchive archive, File dir, String packageName) {

        packageName = (null == packageName) ? "" + dir.getName() : packageName + "." + dir.getName();

        for (File file : dir.listFiles()) {
            String fileName = file.getName();
            if (file.isDirectory()) {
                archive = addClasses(archive, file, packageName);
            } else if (fileName.endsWith(".class")) {
                int dot = fileName.indexOf('.');
                try {
                    archive.addClass(packageName + "." + fileName.substring(0, dot));
                } catch (Exception e) {
                    System.out.println("WARN: Could not add class:" + e);
                }
            }
        }

        return archive;
    }

    /**
     * <p>DO NOT OVERRIDE.</p>
     * <p>DO NOT DEFINE AN @BeforeMethod</p>
     *
     * Instead, override {@link #beforeMethod()}.  If you must override, for example, if you
     * need to use special attributes on your annotation, then ensure you protect the code with
     * and {@link #inContainer()} call.
     */
    @BeforeMethod
    protected void __beforeMethod(Method method) throws Throwable {
        // Note that Arquillian calls the testng BeforeMethod twice (as of 1.0.2.Final, once
        // out of container and once in container. In general the expectation is to execute it
        // one time, and doing it in container allows for the expected injections and context.
        if (inContainer()) {
            try {
                // Make sure we set the db type for tests that may need it (normally done in StartupBean)
                if (null == DatabaseTypeFactory.getDefaultDatabaseType()) {
                    Connection conn = null;
                    try {
                        conn = getConnection();
                        DatabaseTypeFactory.setDefaultDatabaseType(DatabaseTypeFactory.getDatabaseType(conn));
                    } catch (Exception e) {
                        System.err.println("!!! WARNING !!! cannot set default database type, some tests may fail");
                        e.printStackTrace();
                    } finally {
                        if (null != conn) {
                            conn.close();
                        }
                    }
                }
                storageClientManager.init();
                beforeMethod();
                beforeMethod(method);

            } catch (Throwable t) {
                // Arquillian is eating these, make sure they show up in some way
                System.out.println("BEFORE METHOD FAILURE, TEST DID NOT RUN!!! [" + method.getName() + "]");
                t.printStackTrace();
                throw t;
            }
        }
    }

    /**
     * <p>DO NOT OVERRIDE.</p>
     * <p>DO NOT DEFINE AN @AfterMethod</p>
     *
     * Instead, override {@link #afterMethod()}.
     */
    @AfterMethod(alwaysRun = true)
    protected void __afterMethod(ITestResult result, Method method) throws Throwable {
        try {
            if (inContainer()) {
                afterMethod();
                afterMethod(result, method);
            }
        } catch (Throwable t) {
            System.out
                .println("AFTER METHOD FAILURE, TEST CLEAN UP FAILED!!! MAY NEED TO CLEAN DB BEFORE RUNNING MORE TESTS! ["
                    + method.getName() + "]");
            t.printStackTrace();
            throw t;
        }
    }

    protected boolean inContainer() {
        // If the injection is done we're running in the container.
        return (null != initialContext);
    }

    /**
     * Override Point!  Do not implement a @BeforeMethod, instead override this method.
     */
    protected void beforeMethod() throws Exception {
        // do nothing if we're not overridden
    }

    /**
     * Override Point!  Do not implement a @BeforeMethod, instead override this method.
     */
    protected void beforeMethod(Method method) throws Exception {
        // do nothing if we're not overridden
    }

    /**
     * Override Point!  Do not implement an @AfterMethod, instead override this method. note: alwaysRun=true
     */
    protected void afterMethod() throws Exception {
        // do nothing if we're not overridden
    }

    /**
     * Override Point!  Do not implement an @AfterMethod, instead override this method. note: alwaysRun=true
     */
    protected void afterMethod(ITestResult result, Method meth) throws Exception {
        // do nothing if we're not overridden
    }

    protected void startTransaction() throws Exception {
        getTransactionManager().begin();
    }

    protected void commitTransaction() throws Exception {
        getTransactionManager().commit();
    }

    protected void rollbackTransaction() throws Exception {
        getTransactionManager().rollback();
    }

    protected InitialContext getInitialContext() {
        // may be null if not yet injected
        if (null != initialContext) {
            return initialContext;
        }

        InitialContext result = null;

        try {
            Properties jndiProperties = new Properties();
            jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.as.naming.InitialContextFactory");
            result = new InitialContext(jndiProperties);

        } catch (Exception e) {
            throw new RuntimeException("Failed to get InitialContext", e);
        }

        return result;
    }

    public TransactionManager getTransactionManager() {
        TransactionManager result = null;

        try {
            result = (TransactionManager) getInitialContext().lookup("java:jboss/TransactionManager");
            if (null == result) {
                result = (TransactionManager) getInitialContext().lookup("TransactionManager");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to get TransactionManager", e);
        }

        return result;
    }

    protected EntityManager getEntityManager() {
        // may be null if not yet injected (as of 1.0.1.Final, only injected inside @Test)
        if (null != em) {
            return em;
        }

        EntityManager result = null;

        try {
            EntityManagerFactory emf = (EntityManagerFactory) getInitialContext().lookup(
                "java:jboss/RHQEntityManagerFactory");
            result = emf.createEntityManager();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get EntityManagerFactory", e);
        }

        return result;
    }

    /**
     * Equivalent to <pre>executeInTransaction(true, callback)</pre>.
     * @param callback
     */
    protected void executeInTransaction(TransactionCallback callback) {
        executeInTransaction(true, callback);
    }

    protected void executeInTransaction(boolean rollback, final TransactionCallback callback) {
        executeInTransaction(rollback, new TransactionCallbackReturnable<Void>() {

            public Void execute() throws Exception {
                callback.execute();
                return null;
            }
        });
    }

    protected <T> T executeInTransaction(TransactionCallbackReturnable<T> callback) {
        return executeInTransaction(true, callback);
    }

    protected <T> T executeInTransaction(boolean rollback, TransactionCallbackReturnable<T> callback) {
        try {
            startTransaction();
            T result = callback.execute();
            return result;

        } catch (Throwable t) {
            rollback = true;
            RuntimeException re = new RuntimeException(ThrowableUtil.getAllMessages(t), t);
            throw re;

        } finally {
            try {
                if (!rollback) {
                    commitTransaction();
                } else {
                    rollbackTransaction();
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to " + (rollback ? "rollback" : "commit") + " transaction", e);
            }
        }
    }

    /* The old AbstractEJB3Test impl extended AssertJUnit. Continue to support the used methods
     * with various call-thru methods.
     */

    protected void assertNotNull(Object o) {
        AssertJUnit.assertNotNull(o);
    }

    protected void assertNotNull(String msg, Object o) {
        AssertJUnit.assertNotNull(msg, o);
    }

    protected void assertNull(Object o) {
        AssertJUnit.assertNull(o);
    }

    protected void assertNull(String msg, Object o) {
        AssertJUnit.assertNull(msg, o);
    }

    protected void assertFalse(boolean b) {
        AssertJUnit.assertFalse(b);
    }

    protected void assertFalse(String msg, boolean b) {
        AssertJUnit.assertFalse(msg, b);
    }

    protected void assertTrue(boolean b) {
        AssertJUnit.assertTrue(b);
    }

    protected void assertTrue(String msg, boolean b) {
        AssertJUnit.assertTrue(msg, b);
    }

    protected void assertEquals(int expected, int actual) {
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void assertEquals(String msg, int expected, int actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void assertEquals(long expected, long actual) {
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void assertEquals(String msg, long expected, long actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void assertEquals(String expected, String actual) {
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void assertEquals(String msg, String expected, String actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void assertEquals(boolean expected, boolean actual) {
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void assertEquals(String msg, boolean expected, boolean actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void assertEquals(Object expected, Object actual) {
        AssertJUnit.assertEquals(expected, actual);
    }

    protected void assertEquals(String msg, Object expected, Object actual) {
        AssertJUnit.assertEquals(msg, expected, actual);
    }

    protected void fail() {
        AssertJUnit.fail();
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

    /**
     * This will register any custom service, replacing any service with the same objectName.
     * <br/>
     * It does nothing more than registration, any calls to the service (e.g. start) are up to the caller.
     *
     * @param testServiceMBean the test service MBean to register
     * @param objectNameStr the name of the service, which will be converted to an ObjectName
     *
     * @throws RuntimeException
     */
    public void prepareCustomServerService(Object testServiceMBean, String objectNameStr) {
        try {
            ObjectName objectName = new ObjectName(objectNameStr);
            prepareCustomServerService(testServiceMBean, objectName);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This will register any custom service, replacing any service with the same objectName.
     * <br/>
     * It does nothing more than registration, any calls to the service (e.g. start) are up to the caller.
     *
     * @param testServiceMBean the test service MBean to register
     * @param objectName the name of the service
     *
     * @throws RuntimeException
     */
    public void prepareCustomServerService(Object testServiceMBean, ObjectName objectName) {
        try {
            // first, unregister the real service...
            MBeanServer mbs = getPlatformMBeanServer();
            if (mbs.isRegistered(objectName)) {
                mbs.unregisterMBean(objectName);
            }

            // Now replace with the test service...
            mbs.registerMBean(testServiceMBean, objectName);
            return;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unprepareCustomServerService(String objectNameStr) throws Exception {
        ObjectName objectName = new ObjectName(objectNameStr);
        unprepareCustomServerService(objectName);
    }

    public void unprepareCustomServerService(ObjectName objectName) throws Exception {
        MBeanServer mbs = getPlatformMBeanServer();
        if (mbs.isRegistered(objectName)) {
            mbs.unregisterMBean(objectName);
        }
    }

    /**
     * If you need to test server plugins, you must first prepare the server plugin service.
     * After this returns, the caller must explicitly start the PC by using the appropriate API
     * on the given mbean; this method will only start the service, it will NOT start the master PC.
     *
     * @param testServiceMBean the object that will house your test server plugins
     *
     * @throws RuntimeException
     */
    public void prepareCustomServerPluginService(ServerPluginService testServiceMBean) {
        try {
            // first, unregister the real service...
            MBeanServer mbs = getPlatformMBeanServer();
            if (mbs.isRegistered(ServerPluginService.OBJECT_NAME)) {
                mbs.unregisterMBean(ServerPluginService.OBJECT_NAME);
            }

            // Now replace with the test service...
            testServiceMBean.start();
            mbs.registerMBean(testServiceMBean, ServerPluginServiceMBean.OBJECT_NAME);
            serverPluginService = testServiceMBean;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void unprepareServerPluginService() throws Exception {
        if (serverPluginService != null) {
            serverPluginService.stopMasterPluginContainer();
            serverPluginService.stop();
            serverPluginService = null;
        }

        MBeanServer mbs = getPlatformMBeanServer();
        if (mbs.isRegistered(ServerPluginService.OBJECT_NAME)) {
            mbs.unregisterMBean(ServerPluginService.OBJECT_NAME);
        }
    }

    public SchedulerService getSchedulerService() {
        return schedulerService;
    }

    public void prepareScheduler() {
        try {
            if (schedulerService != null) {
                return;
            }

            // first, unregister the real service...
            MBeanServer mbs = getPlatformMBeanServer();
            if (mbs.isRegistered(SchedulerService.SCHEDULER_MBEAN_NAME)) {
                mbs.unregisterMBean(SchedulerService.SCHEDULER_MBEAN_NAME);
            }

            // Now replace with the test service...
            Properties quartzProps = new Properties();
            quartzProps.load(this.getClass().getClassLoader().getResourceAsStream("test-scheduler.properties"));

            schedulerService = new SchedulerService();
            schedulerService.setQuartzProperties(quartzProps);
            schedulerService.start();
            mbs.registerMBean(schedulerService, SchedulerServiceMBean.SCHEDULER_MBEAN_NAME);
            schedulerService.startQuartzScheduler();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void unprepareScheduler() throws Exception {
        if (schedulerService != null) {
            schedulerService.stop();
            schedulerService = null;
        }

        MBeanServer mbs = getPlatformMBeanServer();
        if (mbs.isRegistered(SchedulerService.SCHEDULER_MBEAN_NAME)) {
            mbs.unregisterMBean(SchedulerService.SCHEDULER_MBEAN_NAME);
        }
    }

    /**
     * If you need to test round trips from server to agent and back, you first must install the server communications
     * service that houses all the agent clients. Call this method and add your test agent services to the public fields
     * in the returned object.
     *
     * @return the object that will house your test agent service impls and the agent clients.
     *
     * @throws RuntimeException
     */
    public TestServerCommunicationsService prepareForTestAgents() {
        if (agentService != null) {
            return agentService;
        }

        return prepareForTestAgents(new TestServerCommunicationsService());
    }

    /**
     * If you need to test round trips from server to agent and back, you first must install the server communications
     * service that houses all the agent clients. Call this method and add your test agent services to the public fields
     * in the returned object.
     *
     * @return the object that will house your test agent service impls and the agent clients.
     *
     * @throws RuntimeException
     */
    public TestServerCommunicationsService prepareForTestAgents(TestServerCommunicationsServiceMBean customAgentService) {
        try {
            if (agentService != null) {
                return agentService;
            }

            // first, unregister the real service...
            MBeanServer mbs = getPlatformMBeanServer();
            if (mbs.isRegistered(ServerCommunicationsServiceMBean.OBJECT_NAME)) {
                mbs.unregisterMBean(ServerCommunicationsServiceMBean.OBJECT_NAME);
            }

            // Now replace with the test service...
            agentService = (TestServerCommunicationsService) customAgentService;
            mbs.registerMBean(agentService, ServerCommunicationsServiceMBean.OBJECT_NAME);
            return agentService;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Call this after your tests have finished. You only need to call this if your test previously called
     * {@link #prepareForTestAgents()}.
     */
    public void unprepareForTestAgents() {
        try {
            if (agentService != null) {
                agentService.stop();
                agentService = null;
            }

            MBeanServer mbs = getPlatformMBeanServer();
            if (mbs.isRegistered(ServerCommunicationsServiceMBean.OBJECT_NAME)) {
                mbs.unregisterMBean(ServerCommunicationsServiceMBean.OBJECT_NAME);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PluginDeploymentScannerMBean getPluginScannerService() {
        return pluginScannerService;
    }

    /**
     * Prepares a test deployment scanner with the following characteristics<br/>.
     * -  start() is called but startDeployment() is not called.<br/>
     * - agentPluginDir is set to getTempDir() + "/plugins"<br/>
     * - serverPluginDir is set to null (no scanning for server plugins)<br/>
     * - scanPeriod is set to 9999999 (basically prevent autoscan)<br/>
     */
    protected PluginDeploymentScannerMBean preparePluginScannerService() {
        if (null != pluginScannerService) {
            return pluginScannerService;
        }

        PluginDeploymentScanner scanner = new PluginDeploymentScanner();
        String pluginDirPath = getTempDir() + "/plugins";
        scanner.setAgentPluginDir(pluginDirPath); // we don't want to scan for these
        scanner.setServerPluginDir("ignore no plugins here"); // we don't want to scan for these
        scanner.setUserPluginDir("ignore no plugins here"); // we don't want to scan for these
        scanner.setScanPeriod("9999999"); // we want to manually scan - don't allow for auto-scan to happen

        return preparePluginScannerService(scanner);
    }

    /**
     * Note that the standard plugin scanner service is deployed automatically with the test rhq ear,
     * this is only necessary if you want a custom service.
     *
     * @param scannerService
     */
    public PluginDeploymentScannerMBean preparePluginScannerService(PluginDeploymentScannerMBean scannerService) {
        try {
            MBeanServer mbs = getPlatformMBeanServer();
            if (mbs.isRegistered(PluginDeploymentScannerMBean.OBJECT_NAME)) {
                mbs.unregisterMBean(PluginDeploymentScannerMBean.OBJECT_NAME);
            }

            // Now replace with the test service...
            mbs.registerMBean(scannerService, PluginDeploymentScannerMBean.OBJECT_NAME);
            pluginScannerService = scannerService;
            pluginScannerService.start();

            return pluginScannerService;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void unpreparePluginScannerService() throws Exception {
        if (pluginScannerService != null) {
            pluginScannerService.stop();
            pluginScannerService = null;
        }

        MBeanServer mbs = getPlatformMBeanServer();
        if (mbs.isRegistered(PluginDeploymentScannerMBean.OBJECT_NAME)) {
            mbs.unregisterMBean(PluginDeploymentScannerMBean.OBJECT_NAME);
        }
    }

    public MBeanServer getPlatformMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }

    /**
     * This creates a session for the given user and associates that session with the subject. You can test the security
     * annotations by creating sessions for different users with different permissions.
     *
     * @param subject a JON subject
     * @return the session activated subject, a copy of the subject passed in.
     */
    public Subject createSession(Subject subject) {
        return SessionManager.getInstance().put(subject);
    }

    public static Connection getConnection() throws SQLException {
        return LookupUtil.getDataSource().getConnection();
    }

    /**
     * A utility for writing out various objects that need to be persisted for use between
     * tests.  Arquillian (1.0.2) basically "new"s the testng test class on each test, so instance
     * variables can not be used between tests. Instead, the db or this mechanism needs to be used.
     *
     * The file will be placed in the standard temp dir.  If it already exists it will be replaced.
     *
     * @param filename Do not include the directory. The value will be prepended with the class name.
     * @param objects
     * @throws Exception
     */
    protected void writeObjects(String filename, Object... objects) throws Exception {
        File file = new File(getTempDir(), filename);
        file.delete();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        for (Object o : objects) {
            oos.writeObject(o);
        }
        oos.close();
    }

    /**
     * A utility for reading in objects written with {@link #writeObjects(String, Object...). They are
     * placed in the result List in the same order they were written.
     *
     * @param filename The same filename used in the write. Do not include the directory.
     * @param numObjects the number of objects to read out. Can be less than total written, not greater.
     * @throws Exception
     */
    protected List<Object> readObjects(String filename, int numObjects) throws Exception {
        List<Object> result = new ArrayList<Object>();
        ObjectInputStream ois = null;

        try {
            File file = new File(getTempDir(), filename);
            ois = new ObjectInputStream(new FileInputStream(file));
            for (int i = 0; i < numObjects; ++i) {
                result.add(ois.readObject());
            }
        } finally {
            ois.close();
        }

        return result;
    }

    /**
     * A utility for cleaning up files created with {@link #writeObjects(String, Object...).
     *
     * @param filename The same filename used in the write. Do not include the directory.
     * @return true if deleted, false otherwise.
     */
    protected boolean deleteObjects(String filename) {
        File file = new File(getTempDir(), filename);
        return file.delete();
    }

    /**
     * @return a temp directory for testing that is specific to this test class. Specifically tmpdirRoot/this.getClass().getSimpleName().
     */
    public File getTempDir() {
        return new File(tmpdirRoot, this.getClass().getSimpleName());
    }

}
