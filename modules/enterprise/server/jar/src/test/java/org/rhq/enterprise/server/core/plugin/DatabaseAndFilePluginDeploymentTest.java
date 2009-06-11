/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.core.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.clientapi.descriptor.AgentPluginDescriptorUtil;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.util.MD5Generator;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.resource.metadata.ResourceMetadataManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

@Test
public class DatabaseAndFilePluginDeploymentTest extends AbstractEJB3Test {

    private static final String PLUGIN_NAME = "DeployTest"; // as defined in our test descriptors
    private static final String DESCRIPTORS_LOCATION = "./target/test-classes/test/deployment/";
    private static final String JARS_LOCATION = DESCRIPTORS_LOCATION + "jars";
    private static final String DEPLOY_LOCATION = DESCRIPTORS_LOCATION + "deploy";
    private static final String TESTPLUGIN_1_0_FEB = "1.0-feb";
    private static final String TESTPLUGIN_1_0_JUN = "1.0-june";
    private static final String TESTPLUGIN_1_1_FEB = "1.1-feb";
    private static final String TESTPLUGIN_1_1_JUN = "1.1-june";
    private static final String TESTPLUGIN_1_0_FEB2 = "1.0-feb-2";
    private final Map<String, File> testPluginFiles = new HashMap<String, File>();
    private final Map<String, Plugin> testPlugins = new HashMap<String, Plugin>();
    private final Map<String, Date> testTimestamps = new HashMap<String, Date>();
    private final Map<String, PluginDescriptor> testPluginDescriptors = new HashMap<String, PluginDescriptor>();
    private ResourceMetadataManagerLocal metadataManager;
    private SubjectManagerLocal subjectManager;

    // Here is a matrix of scenarios we are going to test.
    // "Winning Plugin" means the plugin considered the most up-to-date (not obsolete).
    // "Reason" == "exists" means the winning plugin was the only one that existed
    // "Reason" == "version" means the winning plugin had the newer version
    // "Reason" == "time" means the winning plugin had the newer/later timestamp
    // ==========================================================================
    //   | Plugins Deployed In:  | Winning    | Reason  | Side
    // # | Filesystem | Database | Plugin     |         | Effects
    // ==========================================================================
    // 0 | 1.0-feb    | 1.0-feb  | N/A        | N/A     | steady state - all up-to-date
    // 1 | 1.0-feb    | N/A      | Filesystem | exists  | DB row created
    // 2 | N/A        | 1.0-feb  | Database   | exists  | file created
    // 3 | 1.0-jun    | 1.0-feb  | Filesystem | time    | DB row updated
    // 4 | 1.0-feb    | 1.0-jun  | Database   | time    | file 1.0-feb deleted, file 1.0-jun created
    // 5 | 1.1-jun    | 1.0-jun  | Filesystem | version | DB row updated
    // 6 | 1.0-jun    | 1.1-jun  | Database   | version | file 1.0-jun deleted, file 1.1-jun created
    // 7 | 1.1-feb    | 1.0-jun  | Filesystem | version | DB row updated
    // 8 | 1.0-jun    | 1.1-feb  | Database   | version | file 1.0-jun deleted, file 1.1-feb created
    // ------ the tests below have two plugins deployed on the file system ------
    // 9 | 1.0-feb    | 1.0-feb  | Filesystem | time    | file 1.0-feb deleted,
    //   | 1.0-jun    |          |            |         | DB row updated
    // --------------------------------------------------------------------------
    // 10| 1.0-feb    | 1.0-jun  | None       | N/A     | file 1.0-feb deleted,
    //   | 1.0-jun    |          |            |         |
    // --------------------------------------------------------------------------
    // 11| 1.0-feb    | 1.1-feb  | Database   | version | files 1.0-feb/jun deleted,
    //   | 1.0-jun    |          |            |         | file 1.1-feb created
    // --------------------------------------------------------------------------
    // 12| 1.0-feb    | 1.0-feb  | Filesystem | version | file 1.0-feb deleted,
    //   | 1.1-jun    |          |            |         | DB row updated
    // --------------------------------------------------------------------------
    // 13| 1.0-feb    | 1.1-feb  | Filesystem | time    | file 1.0-feb deleted,
    //   | 1.1-jun    |          |            |         | DB row updated
    // --------------------------------------------------------------------------
    // 14| 1.0-feb    | 1.0-feb  | None       | N/A     | all files are the same but
    //   | 1.0-feb-2  |          |            |         | one of the files gets deleted
    // --------------------------------------------------------------------------

    public void test0() throws Exception {
        Plugin plugin10feb = deployPluginJarToFilesystem(TESTPLUGIN_1_0_FEB);
        deployPluginJarToDatabase(TESTPLUGIN_1_0_FEB);

        createScannerAndScan(TESTPLUGIN_1_0_FEB);
        assertPluginInDb(plugin10feb);
        assertPluginOnFilesystem(plugin10feb);
        return;
    }

    public void test1() throws Exception {
        Plugin plugin10feb = deployPluginJarToFilesystem(TESTPLUGIN_1_0_FEB);

        createScannerAndScan(TESTPLUGIN_1_0_FEB);
        assertPluginInDb(plugin10feb);
        assertPluginOnFilesystem(plugin10feb);
        return;
    }

    public void test2() throws Exception {
        Plugin plugin10feb = deployPluginJarToDatabase(TESTPLUGIN_1_0_FEB);

        AgentPluginURLDeploymentScanner scanner = createScanner();
        scan(scanner, null);
        assertPluginInDb(plugin10feb);
        assertPluginOnFilesystem(plugin10feb);

        scan(null, TESTPLUGIN_1_0_FEB);
        assertPluginInDb(plugin10feb);
        assertPluginOnFilesystem(plugin10feb);
        return;
    }

    public void test3() throws Exception {
        Plugin plugin10jun = deployPluginJarToFilesystem(TESTPLUGIN_1_0_JUN);
        deployPluginJarToDatabase(TESTPLUGIN_1_0_FEB);

        createScannerAndScan(TESTPLUGIN_1_0_JUN);
        assertPluginInDb(plugin10jun);
        assertPluginOnFilesystem(plugin10jun);
        return;
    }

    public void test4() throws Exception {
        Plugin plugin10feb = deployPluginJarToFilesystem(TESTPLUGIN_1_0_FEB);
        Plugin plugin10jun = deployPluginJarToDatabase(TESTPLUGIN_1_0_JUN);

        createScannerAndScan(null);
        assertPluginInDb(plugin10jun);
        assertPluginOnFilesystem(plugin10jun);
        assertPluginNotOnFilesystem(plugin10feb);

        scan(null, TESTPLUGIN_1_0_JUN);
        assertPluginInDb(plugin10jun);
        assertPluginOnFilesystem(plugin10jun);
        assertPluginNotOnFilesystem(plugin10feb);
        return;
    }

    public void test5() throws Exception {
        Plugin plugin11jun = deployPluginJarToFilesystem(TESTPLUGIN_1_1_JUN);
        deployPluginJarToDatabase(TESTPLUGIN_1_0_JUN);

        createScannerAndScan(TESTPLUGIN_1_1_JUN);
        assertPluginInDb(plugin11jun);
        assertPluginOnFilesystem(plugin11jun);
        return;
    }

    public void test6() throws Exception {
        Plugin plugin10jun = deployPluginJarToFilesystem(TESTPLUGIN_1_0_JUN);
        Plugin plugin11jun = deployPluginJarToDatabase(TESTPLUGIN_1_1_JUN);

        createScannerAndScan(null);
        assertPluginInDb(plugin11jun);
        assertPluginOnFilesystem(plugin11jun);
        assertPluginNotOnFilesystem(plugin10jun);

        scan(null, TESTPLUGIN_1_1_JUN);
        assertPluginInDb(plugin11jun);
        assertPluginOnFilesystem(plugin11jun);
        assertPluginNotOnFilesystem(plugin10jun);
        return;
    }

    public void test7() throws Exception {
        Plugin plugin11feb = deployPluginJarToFilesystem(TESTPLUGIN_1_1_FEB);
        deployPluginJarToDatabase(TESTPLUGIN_1_0_JUN);

        createScannerAndScan(TESTPLUGIN_1_1_FEB);
        assertPluginInDb(plugin11feb);
        assertPluginOnFilesystem(plugin11feb);
        return;
    }

    public void test8() throws Exception {
        Plugin plugin10jun = deployPluginJarToFilesystem(TESTPLUGIN_1_0_JUN);
        Plugin plugin11feb = deployPluginJarToDatabase(TESTPLUGIN_1_1_FEB);

        createScannerAndScan(null);
        assertPluginInDb(plugin11feb);
        assertPluginOnFilesystem(plugin11feb);
        assertPluginNotOnFilesystem(plugin10jun);

        scan(null, TESTPLUGIN_1_1_FEB);
        assertPluginInDb(plugin11feb);
        assertPluginOnFilesystem(plugin11feb);
        assertPluginNotOnFilesystem(plugin10jun);
        return;
    }

    public void test9() throws Exception {
        Plugin plugin10feb = deployPluginJarToFilesystem(TESTPLUGIN_1_0_FEB);
        Plugin plugin10jun = deployPluginJarToFilesystem(TESTPLUGIN_1_0_JUN);
        deployPluginJarToDatabase(TESTPLUGIN_1_0_FEB);

        createScannerAndScan(null);
        assertPluginInDb(plugin10feb); // still the old one, will get updated when the file is deployed
        assertPluginOnFilesystem(plugin10jun);
        assertPluginNotOnFilesystem(plugin10feb);

        scan(null, TESTPLUGIN_1_0_JUN);
        assertPluginInDb(plugin10jun); // bingo - file scan brought the plugin on filesystem into db
        assertPluginOnFilesystem(plugin10jun);
        assertPluginNotOnFilesystem(plugin10feb);
        return;
    }

    public void test10() throws Exception {
        Plugin plugin10feb = deployPluginJarToFilesystem(TESTPLUGIN_1_0_FEB);
        Plugin plugin10jun = deployPluginJarToFilesystem(TESTPLUGIN_1_0_JUN);
        deployPluginJarToDatabase(TESTPLUGIN_1_0_JUN);

        createScannerAndScan(null);
        assertPluginInDb(plugin10jun); // no change
        assertPluginOnFilesystem(plugin10jun); // no change
        assertPluginNotOnFilesystem(plugin10feb); // shows that the old duplicate is gone now

        scan(null, TESTPLUGIN_1_0_JUN);
        assertPluginInDb(plugin10jun); // bingo - file scan brought the plugin on filesystem into db
        assertPluginOnFilesystem(plugin10jun);
        assertPluginNotOnFilesystem(plugin10feb);
        return;
    }

    public void test11() throws Exception {
        Plugin plugin10feb = deployPluginJarToFilesystem(TESTPLUGIN_1_0_FEB);
        Plugin plugin10jun = deployPluginJarToFilesystem(TESTPLUGIN_1_0_JUN);
        Plugin plugin11feb = deployPluginJarToDatabase(TESTPLUGIN_1_1_FEB);

        createScannerAndScan(null);
        assertPluginInDb(plugin11feb);
        assertPluginOnFilesystem(plugin11feb);
        assertPluginNotOnFilesystem(plugin10feb);
        assertPluginNotOnFilesystem(plugin10jun);

        scan(null, TESTPLUGIN_1_1_FEB);
        assertPluginInDb(plugin11feb);
        assertPluginOnFilesystem(plugin11feb);
        assertPluginNotOnFilesystem(plugin10feb);
        assertPluginNotOnFilesystem(plugin10jun);
        return;
    }

    public void test12() throws Exception {
        Plugin plugin10feb = deployPluginJarToFilesystem(TESTPLUGIN_1_0_FEB);
        Plugin plugin11jun = deployPluginJarToFilesystem(TESTPLUGIN_1_1_JUN);
        deployPluginJarToDatabase(TESTPLUGIN_1_0_FEB);

        createScannerAndScan(null);
        assertPluginInDb(plugin10feb); // still the old one, will get updated when the file is deployed
        assertPluginOnFilesystem(plugin11jun);
        assertPluginNotOnFilesystem(plugin10feb);

        scan(null, TESTPLUGIN_1_1_JUN);
        assertPluginInDb(plugin11jun);
        assertPluginOnFilesystem(plugin11jun);
        assertPluginNotOnFilesystem(plugin10feb);
        return;
    }

    public void test13() throws Exception {
        Plugin plugin10feb = deployPluginJarToFilesystem(TESTPLUGIN_1_0_FEB);
        Plugin plugin11jun = deployPluginJarToFilesystem(TESTPLUGIN_1_1_JUN);
        Plugin plugin11feb = deployPluginJarToDatabase(TESTPLUGIN_1_1_FEB);

        createScannerAndScan(null);
        assertPluginInDb(plugin11feb); // still the old one, will get updated when the file is deployed
        assertPluginOnFilesystem(plugin11jun);
        assertPluginNotOnFilesystem(plugin10feb);

        scan(null, TESTPLUGIN_1_1_JUN);
        assertPluginInDb(plugin11jun);
        assertPluginOnFilesystem(plugin11jun);
        assertPluginNotOnFilesystem(plugin10feb);
        return;
    }

    public void test14() throws Exception {
        Plugin plugin10feb = deployPluginJarToFilesystem(TESTPLUGIN_1_0_FEB);
        Plugin plugin10feb2 = deployPluginJarToFilesystem(TESTPLUGIN_1_0_FEB2);
        deployPluginJarToDatabase(TESTPLUGIN_1_0_FEB);

        createScannerAndScan(null);
        assertPluginInDb(plugin10feb);
        boolean plugin10febExists = !isPluginNotOnFilesystem(plugin10feb);
        boolean plugin10feb2Exists = !isPluginNotOnFilesystem(plugin10feb2);
        assert plugin10febExists ^ plugin10feb2Exists; // one must exist, but only one (we aren't guaranteed which, they are identical)

        scan(null, TESTPLUGIN_1_0_FEB);
        assertPluginInDb(plugin10feb);
        plugin10febExists = !isPluginNotOnFilesystem(plugin10feb);
        plugin10feb2Exists = !isPluginNotOnFilesystem(plugin10feb2);
        assert plugin10febExists ^ plugin10feb2Exists; // one must exist, but only one (we aren't guaranteed which, they are identical)
        return;
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.set(2009, Calendar.FEBRUARY, 1, 1, 0, 0);
        Date febDate = cal.getTime();
        cal.set(2009, Calendar.JUNE, 1, 1, 0, 0);
        Date juneDate = cal.getTime();
        testTimestamps.put(TESTPLUGIN_1_0_FEB, febDate);
        testTimestamps.put(TESTPLUGIN_1_0_JUN, juneDate);
        testTimestamps.put(TESTPLUGIN_1_1_FEB, febDate);
        testTimestamps.put(TESTPLUGIN_1_1_JUN, juneDate);
        testTimestamps.put(TESTPLUGIN_1_0_FEB2, febDate);

        metadataManager = LookupUtil.getResourceMetadataManager();
        subjectManager = LookupUtil.getSubjectManager();

        File deployDir = new File(DEPLOY_LOCATION);
        deployDir.mkdirs();
        assert deployDir.isDirectory();

        File jarsDir = new File(JARS_LOCATION);
        jarsDir.mkdirs();
        assert jarsDir.isDirectory();

        testPluginFiles.put(TESTPLUGIN_1_0_FEB, new File(jarsDir, TESTPLUGIN_1_0_FEB + ".jar"));
        testPluginFiles.put(TESTPLUGIN_1_0_JUN, new File(jarsDir, TESTPLUGIN_1_0_JUN + ".jar"));
        testPluginFiles.put(TESTPLUGIN_1_1_FEB, new File(jarsDir, TESTPLUGIN_1_1_FEB + ".jar"));
        testPluginFiles.put(TESTPLUGIN_1_1_JUN, new File(jarsDir, TESTPLUGIN_1_1_JUN + ".jar"));
        testPluginFiles.put(TESTPLUGIN_1_0_FEB2, new File(jarsDir, TESTPLUGIN_1_0_FEB2 + ".jar"));

        for (Map.Entry<String, File> entry : testPluginFiles.entrySet()) {
            File descriptorFile = new File(DESCRIPTORS_LOCATION, entry.getKey() + ".xml");
            File file = entry.getValue();
            buildPluginJar(descriptorFile, file);
            assert file.exists();

            PluginDescriptor descriptor = AgentPluginDescriptorUtil.loadPluginDescriptorFromUrl(file.toURI().toURL());
            testPluginDescriptors.put(entry.getKey(), descriptor);

            Plugin pluginPojo = new Plugin(PLUGIN_NAME, file.getName());
            pluginPojo.setVersion(descriptor.getVersion());
            pluginPojo.setMd5(MD5Generator.getDigestString(file));
            pluginPojo.setMtime(testTimestamps.get(entry.getKey()).getTime());
            testPlugins.put(entry.getKey(), pluginPojo);
        }

        return;
    }

    @AfterClass
    public void afterClass() throws Exception {
        for (Map.Entry<String, File> entry : testPluginFiles.entrySet()) {
            File doomed = entry.getValue();
            doomed.delete();
        }
        File jarsDir = new File(JARS_LOCATION);
        jarsDir.delete();

        File deployDir = new File(DEPLOY_LOCATION);
        emptyDirectory(deployDir);
        deployDir.delete();

        return;
    }

    @BeforeMethod
    public void beforeMethod() throws Exception {
        afterMethod(); // we clean up before and after, just to be sure we're clean
    }

    @AfterMethod
    @SuppressWarnings("unchecked")
    public void afterMethod() throws Exception {
        emptyDirectory(new File(DEPLOY_LOCATION));

        TransactionManager tm = getTransactionManager();
        tm.begin();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createNamedQuery(Plugin.QUERY_FIND_BY_NAME);
            q.setParameter("name", PLUGIN_NAME);
            List<Plugin> doomedPlugins = q.getResultList();
            for (Plugin doomedPlugin : doomedPlugins) {
                em.remove(em.find(Plugin.class, doomedPlugin.getId()));
            }
        } catch (NoResultException ignore) {
        } finally {
            tm.commit();
            em.close();
        }

        return;
    }

    private void assertSamePlugin(Plugin p1, Plugin p2) throws Exception {
        assert p1.getName().equals(p2.getName()) : "NAME: " + p1 + "!=" + p2;
        assert p1.getMd5().equals(p2.getMd5()) : "MD5: " + p1 + "!=" + p2;
        assert p1.getMtime() == p2.getMtime() : "MTIME: " + p1 + "!=" + p2;
        assert p1.getVersion().equals(p2.getVersion()) : "VERSION: " + p1 + "!=" + p2;
    }

    private void assertPluginOnFilesystem(Plugin plugin) throws Exception {
        File file = new File(DEPLOY_LOCATION, plugin.getPath());
        String version = AgentPluginDescriptorUtil.getPluginVersion(file, null).toString();
        String md5 = MD5Generator.getDigestString(file);
        long mtime = file.lastModified();
        Plugin filePlugin = new Plugin(PLUGIN_NAME, file.getName());
        filePlugin.setMd5(md5);
        filePlugin.setVersion(version);
        filePlugin.setMtime(mtime);
        assertSamePlugin(plugin, filePlugin);
    }

    private void assertPluginInDb(Plugin plugin) throws Exception {
        Plugin dbPlugin;

        TransactionManager tm = getTransactionManager();
        tm.begin();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createNamedQuery(Plugin.QUERY_FIND_BY_NAME);
            q.setParameter("name", PLUGIN_NAME);
            dbPlugin = (Plugin) q.getSingleResult();
        } finally {
            tm.rollback();
            em.close();
        }

        assertSamePlugin(plugin, dbPlugin);
    }

    private void assertPluginNotOnFilesystem(Plugin plugin) throws Exception {
        assert isPluginNotOnFilesystem(plugin);
    }

    private boolean isPluginNotOnFilesystem(Plugin plugin) {
        File file = new File(DEPLOY_LOCATION, plugin.getPath());
        return !file.exists();
    }

    private AgentPluginURLDeploymentScanner createScannerAndScan(String pluginId) throws Exception {
        AgentPluginURLDeploymentScanner scanner = createScanner();
        scan(scanner, pluginId); // see comments in scan() for what pluginId is
        return scanner;
    }

    private AgentPluginURLDeploymentScanner createScanner() throws Exception {
        AgentPluginURLDeploymentScanner scanner = new AgentPluginURLDeploymentScanner();
        File pluginDirectoryFile = new File(DEPLOY_LOCATION);
        scanner.setPluginDirectory(pluginDirectoryFile);
        List<URL> url = new ArrayList<URL>();
        url.add(pluginDirectoryFile.toURI().toURL());
        scanner.setURLList(url);
        return scanner;
    }

    /**
     * Scans in both directions - first asks the db scanner to scan the database.
     * Then it tries to register the plugin found on the filesystem.
     * pluginId identifies the plugin on the filesystem that is to be scanned up to DB (if appropriate).
     * 
     * @param scanner if not-null, uses this to perform a db scan first
     * @param pluginId if not-null, this is registered (i.e. a file scan registering this plugin)
     * @throws Exception
     */
    private void scan(AgentPluginURLDeploymentScanner scanner, String pluginId) throws Exception {
        if (scanner != null) {
            scanner.scan();
        }

        if (pluginId != null) {
            Plugin plugin = this.testPlugins.get(pluginId);
            Plugin pluginDup = new Plugin(plugin.getName(), plugin.getPath(), plugin.getMd5());
            pluginDup.setMtime(plugin.getMtime());
            pluginDup.setVersion(plugin.getVersion());
            PluginDescriptor pluginDescriptor = this.testPluginDescriptors.get(pluginId);
            File localPluginFile = this.testPluginFiles.get(pluginId);
            metadataManager.registerPlugin(subjectManager.getOverlord(), pluginDup, pluginDescriptor, localPluginFile);
        }
        return;
    }

    private Plugin deployPluginJarToDatabase(String pluginId) throws Exception {
        Plugin plugin = testPlugins.get(pluginId);

        // make our own copy since we will be persisting it and populating content in it
        Plugin pluginPojo = new Plugin(plugin.getName(), plugin.getPath(), plugin.getMd5());
        pluginPojo.setId(0);
        pluginPojo.setDisplayName(plugin.getName());
        pluginPojo.setVersion(plugin.getVersion());
        pluginPojo.setMtime(plugin.getMtime());
        pluginPojo.setContent(StreamUtil.slurp(new FileInputStream(testPluginFiles.get(pluginId))));

        TransactionManager tm = getTransactionManager();
        tm.begin();
        EntityManager em = getEntityManager();
        try {
            em.persist(pluginPojo);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            tm.commit();
            em.close();
        }

        return plugin; // do not return the persisted pojo, let GC collect the larger pojo with the file content
    }

    private Plugin deployPluginJarToFilesystem(String pluginId) throws Exception {
        File pluginJar = testPluginFiles.get(pluginId);
        File deployedPluginJar = new File(DEPLOY_LOCATION, pluginJar.getName());
        FileOutputStream out = new FileOutputStream(deployedPluginJar);
        FileInputStream in = new FileInputStream(pluginJar);
        StreamUtil.copy(in, out);
        timestampPluginJar(deployedPluginJar, pluginId); // make sure its last-mod time is correct

        return testPlugins.get(pluginId);
    }

    private void timestampPluginJar(File pluginJar, String pluginId) throws Exception {
        pluginJar.setLastModified(testTimestamps.get(pluginId).getTime());
    }

    private void buildPluginJar(File descriptor, File pluginJar) throws Exception {
        FileOutputStream fos = new FileOutputStream(pluginJar);
        ZipOutputStream zip = new ZipOutputStream(fos);
        try {
            ZipEntry zipEntry = new ZipEntry("META-INF/rhq-plugin.xml");
            zip.putNextEntry(zipEntry);
            InputStream input = new FileInputStream(descriptor);
            try {
                StreamUtil.copy(input, zip, false);
            } finally {
                input.close();
            }
        } finally {
            zip.close();
        }
    }

    private void emptyDirectory(File dirToEmpty) {
        File[] doomedFiles = dirToEmpty.listFiles();
        for (File doomedFile : doomedFiles) {
            doomedFile.delete();
        }
    }
}
