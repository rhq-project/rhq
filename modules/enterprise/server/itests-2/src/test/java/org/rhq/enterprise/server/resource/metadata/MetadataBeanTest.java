package org.rhq.enterprise.server.resource.metadata;

import static org.rhq.core.clientapi.shared.PluginDescriptorUtil.loadPluginDescriptor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.datatype.IDataTypeFactory;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.operation.DatabaseOperation;
import org.xml.sax.InputSource;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.criteria.ServerCriteria;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.bundle.TestBundlePluginComponent;
import org.rhq.enterprise.server.bundle.TestBundleServerPluginService;
import org.rhq.enterprise.server.resource.ResourceTypeManagerLocal;
import org.rhq.enterprise.server.scheduler.jobs.PurgePluginsJob;
import org.rhq.enterprise.server.scheduler.jobs.PurgeResourceTypesJob;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

public class MetadataBeanTest extends AbstractEJB3Test {

    private final String objectFileName = "pluginIds.obj";
    private Set<Integer> pluginIds;

    /**
     * <pre>IMPORTANT NOTE FOR SUBCLASS IMPLEMENTORS
     * Arquillian (1.0.2) executes @AfterXXX after each test. The work below would normally be done in
     * an AfterClass method, but instead we'll use a "special" test that performs last for each subclass, to
     * perform this cleanup code.
     * </pre>
     * Need to delete rows from RHQ_PLUGINS because subsequent tests in server/jar would otherwise fail. Some tests look
     * at what plugins are in the database, and then look for corresponding plugin files on the file system. MetadataTest
     * however removes the generated plugin files during each test run.
     *
     * Note that using this for cleanup is discouraged in favor or beforeMethod and afterMethod overrides.  Remember
     * that tests and test classes can execute in any order. This only guarantees clean up at some point, other tests
     * [in other test classes] may encounter data that is left for cleanup by this mechanism.
     */
    protected void afterClassWork() throws Exception {
        PluginManagerLocal pluginMgr = LookupUtil.getPluginManager();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        List<Integer> doomedPlugins = new ArrayList<Integer>(pluginIds);
        pluginMgr.deletePlugins(overlord, doomedPlugins);

        //the following 3 lines ensure we truly delete the above plugins
        //from the database
        ackDeletedPlugins();
        new PurgeResourceTypesJob().executeJobCode(null);
        new PurgePluginsJob().executeJobCode(null);

        pluginIds.clear();
        deleteObjects(objectFileName);
    }

    @Override
    protected void beforeMethod() throws Exception {

        setupDB();

        TestBundleServerPluginService bundleService = new TestBundleServerPluginService(getTempDir(), new TestBundlePluginComponent());
        prepareCustomServerPluginService(bundleService);
        bundleService.startMasterPluginContainerWithoutSchedulingJobs();
        prepareScheduler();
        preparePluginScannerService();

        try {
            List<Object> objects = readObjects(objectFileName, 1);
            pluginIds = (Set<Integer>) objects.get(0);
        } catch (Throwable t) {
            pluginIds = new HashSet<Integer>();
        }
    }

    /**
     * Need to delete rows from RHQ_PLUGINS because subsequent tests in server/jar would otherwise fail. Some tests look
     * at what plugins are in the database, and then look for corresponding plugin files on the file system. MetadataTest
     * however removes the generated plugin files during each test run.
     */
    @Override
    protected void afterMethod() throws Exception {

        if (!pluginIds.isEmpty()) {
            writeObjects(objectFileName, pluginIds);
        }

        unpreparePluginScannerService();
        unprepareServerPluginService();
        unprepareScheduler();
    }

    protected void setupDB() throws Exception {
        Connection connection = null;

        try {
            connection = getConnection();
            DatabaseConnection dbunitConnection = new DatabaseConnection(connection);
            setDbType(dbunitConnection);
            // note - this info should already be in the db as part of dbsetup, but just in case
            // perform the refresh. Do not DELETE this data set as it may be assumed in other tests.
            DatabaseOperation.REFRESH.execute(dbunitConnection, getDataSet());
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private void setDbType(IDatabaseConnection connection) throws Exception {
        DatabaseConfig config = connection.getConfig();
        String name = connection.getConnection().getMetaData().getDatabaseProductName().toLowerCase();
        int major = connection.getConnection().getMetaData().getDatabaseMajorVersion();
        IDataTypeFactory type = null;
        if (name.contains("postgres")) {
            type = new org.dbunit.ext.postgresql.PostgresqlDataTypeFactory();
        } else if (name.contains("oracle")) {
            if (major >= 10) {
                type = new org.dbunit.ext.oracle.Oracle10DataTypeFactory();
            } else {
                type = new org.dbunit.ext.oracle.OracleDataTypeFactory();
            }
        }
        if (type != null) {
            config.setProperty("http://www.dbunit.org/properties/datatypeFactory", type);
        }
    }

    private IDataSet getDataSet() throws DataSetException {
        FlatXmlProducer xmlProducer = new FlatXmlProducer(new InputSource(getClass().getResourceAsStream(
            getDataSetFile())));
        xmlProducer.setColumnSensing(true);
        return new FlatXmlDataSet(xmlProducer);
    }

    protected String getDataSetFile() {
        return "MetadataTest.xml";
    }

    protected void createPlugin(String pluginFileName, String version, String descriptorFileName) throws Exception {
        URL descriptorURL = getDescriptorURL(descriptorFileName);
        PluginDescriptor pluginDescriptor = loadPluginDescriptor(descriptorURL);
        String pluginFilePath = getPluginScannerService().getAgentPluginDir() + "/" + pluginFileName + ".jar";

        Plugin plugin = new Plugin(pluginDescriptor.getName(), pluginFilePath);
        plugin.setDisplayName(pluginDescriptor.getName());
        plugin.setEnabled(true);
        plugin.setDescription(pluginDescriptor.getDescription());
        plugin.setAmpsVersion(getAmpsVersion(pluginDescriptor));
        plugin.setVersion(pluginDescriptor.getVersion());
        plugin.setMD5(MessageDigestGenerator.getDigestString(descriptorURL));

        PluginManagerLocal pluginMgr = LookupUtil.getPluginManager();

        pluginMgr.registerPlugin(plugin, pluginDescriptor, null, true);

        pluginIds.add(plugin.getId());
    }

    private URL getDescriptorURL(String descriptor) {
        String dir = getClass().getSimpleName();
        return getClass().getResource(dir + "/" + descriptor);
    }

    String getAmpsVersion(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor.getAmpsVersion() == null) {
            return "2.0";
        }

        ComparableVersion version = new ComparableVersion(pluginDescriptor.getAmpsVersion());
        ComparableVersion version2 = new ComparableVersion("2.0");

        if (version.compareTo(version2) <= 0) {
            return "2.0";
        }

        return pluginDescriptor.getAmpsVersion();
    }

    protected ResourceType assertResourceTypeAssociationEquals(String resourceTypeName, String plugin,
        String propertyName, List<String> expected) throws Exception {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal resourceTypeMgr = LookupUtil.getResourceTypeManager();

        String fetch = "fetch" + WordUtils.capitalize(propertyName);
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterName(resourceTypeName);
        criteria.addFilterPluginName(plugin);
        criteria.setStrict(true);
        criteria.fetchBundleConfiguration(true);
        criteria.fetchDriftDefinitionTemplates(true);
        MethodUtils.invokeMethod(criteria, fetch, true);

        List<ResourceType> resourceTypes = resourceTypeMgr.findResourceTypesByCriteria(subjectMgr.getOverlord(),
            criteria);
        assertEquals("too many types!", 1, resourceTypes.size());
        ResourceType resourceType = resourceTypes.get(0);
        Set<String> expectedSet = new HashSet<String>(expected);
        List<String> missing = new ArrayList<String>();
        List<String> unexpected = new ArrayList<String>();

        for (String expectedProperty : expectedSet) {
            if (!contains(resourceType, propertyName, expectedProperty)) {
                missing.add(expectedProperty);
            }
        }

        Collection<?> actualPropertyValues = (Collection<?>) PropertyUtils.getProperty(resourceType, propertyName);
        for (Object actualPropertyValue : actualPropertyValues) {
            String actualName = (String) PropertyUtils.getProperty(actualPropertyValue, "name");
            if (!expectedSet.contains(actualName)) {
                unexpected.add(actualName);
            }
        }

        String errors = "";
        if (!missing.isEmpty()) {
            errors = "Failed to find the following " + propertyName + "(s) for type " + resourceTypeName + ": "
                + missing;
        }

        if (unexpected.size() > 0) {
            errors += "\nFound unexpected " + propertyName + "(s) for type " + resourceTypeName + ": " + unexpected;
        }

        assert errors.isEmpty() : errors;

        return resourceType;
    }

    private boolean contains(ResourceType type, String propertyName, String expected) throws Exception {
        Collection<?> actualPropertyValues = (Collection<?>) PropertyUtils.getProperty(type, propertyName);
        for (Object actualPropertyValue : actualPropertyValues) {
            String actualName = (String) PropertyUtils.getProperty(actualPropertyValue, "name");
            if (actualName.equals(expected)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This actually creates a .jar file on the file system but doesn't register it.
     *
     * @param jarName the name to be given to the new jar file
     * @param descriptorXmlFilename where the descriptor XML can be found on the test classloader
     * @return the location of the new jar file
     * @throws Exception
     */
    protected File createPluginJarFile(String jarName, String descriptorXmlFilename) throws Exception {
        FileOutputStream stream = null;
        JarOutputStream out = null;
        InputStream in = null;

        try {
            String pluginDirPath = getPluginScannerService().getAgentPluginDir();
            File pluginDir = new File(pluginDirPath);
            pluginDir.mkdirs();
            File jarFile = new File(pluginDir, jarName);
            jarFile.delete(); // in case some older file is hanging around, get rid of it
            stream = new FileOutputStream(jarFile);
            out = new JarOutputStream(stream);

            // Add archive entry for the descriptor
            JarEntry jarAdd = new JarEntry("META-INF/rhq-plugin.xml");
            jarAdd.setTime(System.currentTimeMillis());
            out.putNextEntry(jarAdd);

            // Write the descriptor - note that we assume the xml file is in the test classloader
            URL descriptorURL = getDescriptorURL(descriptorXmlFilename);
            in = descriptorURL.openStream();
            StreamUtil.copy(in, out, false);

            return jarFile;
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }

    /**
     * Tests can use this to let us know that a plugin has been deployed and needs to
     * be cleaned up/removed at the end of the test.
     *
     * @param pluginName
     */
    protected void pluginDeployed(String pluginName) {
        PluginManagerLocal pluginMgr = LookupUtil.getPluginManager();
        Plugin plugin = pluginMgr.getPlugin(pluginName);
        if (plugin != null) {
            this.pluginIds.add(plugin.getId());
        }
    }

    /**
     * Use this to ignore a plugin's resource type. Useful for making sure metadata
     * updates work even for types that are ignored.
     *
     * @param typeName type to ignore
     * @param pluginName the plugin where the type is defined
     */
    protected void ignoreType(String typeName, String pluginName) {
        SubjectManagerLocal subjectMgr = LookupUtil.getSubjectManager();
        ResourceTypeManagerLocal typeMgr = LookupUtil.getResourceTypeManager();
        ResourceType rt = typeMgr.getResourceTypeByNameAndPlugin(typeName, pluginName);
        if (rt == null) {
            fail("Should have had a resource type named [" + typeName + "] from plugin [" + pluginName + "]");
        }
        typeMgr.setResourceTypeIgnoreFlagAndUninventoryResources(subjectMgr.getOverlord(), rt.getId(), true);

        // sanity check - make sure the type really got ignored
        rt = typeMgr.getResourceTypeByNameAndPlugin(typeName, pluginName);
        if (!rt.isIgnored()) {
            fail("Should have ignored resource type [" + rt + "]");
        }

        return;
    }
}
