/*
 * RHQ Management Platform
 * Copyright (C) 2013 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugins.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.Query;
import javax.transaction.TransactionManager;

import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.util.file.FileUtil;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.bundle.BundleManagerLocal;
import org.rhq.enterprise.server.core.CoreServer;
import org.rhq.enterprise.server.core.CoreServerMBean;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScanner;
import org.rhq.enterprise.server.core.plugin.PluginDeploymentScannerMBean;
import org.rhq.enterprise.server.plugin.pc.MasterServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.ServerPluginService;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.test.TestAgentClient;
import org.rhq.enterprise.server.test.TestServerCommunicationsService;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Lukas Krejci
 */
public class RecipeValidationTest extends AbstractEJB3Test {

    public static final String ITESTS = "itests";
    private static final String ENTITY_NAME_PREFIX = "recipeValidationTest";
    private static final String FAKE_RESOURCE_TYPE_NAME = "recipeValidationTest-antbundle-resourcetype";
    private static final String ANT_BUNDLE_TYPE_NAME = "Ant Bundle";

    private TestServerCommunicationsService agentServiceContainer;
    private MasterServerPluginContainer pc;
    private ServerPluginService ps;
    private BundleManagerLocal bundleManager;
    private File serverPluginsDir;

    @Override
    protected void beforeMethod() throws Exception {
        // try and clean up any junk that may be lying around from a failed run
        cleanupDatabase();

        bundleManager = LookupUtil.getBundleManager();
        createAntBundleType();

        prepareCustomServerService(new CoreServer(), CoreServerMBean.OBJECT_NAME);
        prepareCustomServerService(new PluginDeploymentScanner(), PluginDeploymentScannerMBean.OBJECT_NAME);

        prepareCustomServerPluginService(new ServerPluginService());

        agentServiceContainer = prepareForTestAgents();
        agentServiceContainer.bundleService = new TestAgentClient(null, agentServiceContainer);

        ps = new ServerPluginService();
        serverPluginsDir = ps.getServerPluginsDirectory();
        serverPluginsDir.mkdirs();
        File agentPluginsDir = new File(serverPluginsDir.getParentFile(), "agentplugins");
        agentPluginsDir.mkdirs();

        File antBundlePlugin = new File(System.getProperty("rhq.ant-bundle.serverplugin.path"));

        PluginDeploymentScannerMBean scanner = LookupUtil.getPluginDeploymentScanner();

        //needed by server plugin lifecycle
        prepareScheduler();

        File targetFile = new File(serverPluginsDir, antBundlePlugin.getName());
        //touch the file so that the plugin scanner picks it up again
        targetFile.setLastModified(System.currentTimeMillis());

        FileUtil.copyFile(antBundlePlugin, targetFile);

        scanner.setAgentPluginDir(agentPluginsDir.getAbsolutePath());
        scanner.setServerPluginDir(serverPluginsDir.getAbsolutePath());

        //actually, this is resetting the plugin service to the real thing, because we need to deploy the
        //real ant bundle server plugin
        prepareCustomServerPluginService(ps);
//        resourceManager = LookupUtil.getResourceManager();
//        overlord = c
        ps.startMasterPluginContainer();

        LookupUtil.getPluginDeploymentScanner().startDeployment();
        LookupUtil.getPluginDeploymentScanner().scanAndRegister();
    }

    @Override
    protected void afterMethod() throws Exception {
        FileUtil.purge(serverPluginsDir.getParentFile(), true);

        unprepareForTestAgents();
        unprepareScheduler();

        try {
            cleanupDatabase();
        } finally {
            unprepareServerPluginService();
            unprepareCustomServerService(CoreServerMBean.OBJECT_NAME);
            unprepareCustomServerService(PluginDeploymentScannerMBean.OBJECT_NAME);
        }
    }

    private void cleanupDatabase() throws Exception {
        try {
            getTransactionManager().begin();

            Query q;
            List<?> doomed;

            // remove ResourceTypes which cascade remove BundleTypes
            q = em.createQuery("SELECT rt FROM ResourceType rt WHERE rt.deleted = false and rt.name = '"
                + FAKE_RESOURCE_TYPE_NAME + "'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(ResourceType.class, ((ResourceType) removeMe).getId()));
            }
            em.flush();
            //  remove any orphaned BundleTypes
            q = em.createQuery("SELECT bt FROM BundleType bt WHERE bt.name = '" + ANT_BUNDLE_TYPE_NAME + "'");
            doomed = q.getResultList();
            for (Object removeMe : doomed) {
                em.remove(em.getReference(BundleType.class, ((BundleType) removeMe).getId()));
            }
            getTransactionManager().commit();
        } catch (Exception e) {
            try {
                System.out.println("CANNOT CLEAN UP TEST: Cause: " + e);
                getTransactionManager().rollback();
            } catch (Exception ignore) {
            }
        }
    }

    @Test(groups = RecipeValidationTest.ITESTS)
    public void testManageRootDirMandatoryOnBundleVersionCreation() throws Exception {
        File root = FileUtil.createTempDirectory(getClass().getName(), null, null);
        copyFromClasspath("recipe-no-manageRootDir.xml", "deploy.xml", root);

        File bundleZip = createDistributionZip(root);

        try {
            bundleManager.createBundleVersionViaURL(getFreshOverlord(), bundleZip.toURI().toURL().toString());
            fail("A recipe without explicit managerRootDir should not have been created");
        } catch (Exception e) {
            //expected
            checkForExpectedException(e, "org.rhq.bundle.ant.InvalidBuildFileException", "The deployment unit must specifically declare compliance mode of the destination directory.");
        } finally {
            FileUtil.purge(root, true);
        }
    }

    @Test(groups = RecipeValidationTest.ITESTS)
    public void testManageRootDirAbsenceToleratedDuringUpdate() {
        // TODO does this even make sense?
    }

    @Test(groups = RecipeValidationTest.ITESTS)
    public void testManageRootDirAbsenceToleratedDuringRevert() {
        // TODO implement
    }

    private Subject getFreshOverlord() {
        return LookupUtil.getSubjectManager().getOverlord();
    }

    private File createDistributionZip(File root) throws IOException {
        File ret = File.createTempFile(getClass().getName(),
            "zip");
        ZipOutputStream distribFile = new ZipOutputStream(new FileOutputStream(ret));

        for (File f : getAllFilesRelativeToRoot(root, root)) {
            distribFile.putNextEntry(new ZipEntry(f.getPath()));

            File resultFile = new File(root, f.getPath());

            FileInputStream in = new FileInputStream(resultFile);

            try {
                StreamUtil.copy(in, distribFile, false);
            } finally {
                in.close();
            }

            distribFile.closeEntry();
        }

        distribFile.close();

        return ret;
    }

    private static Set<File> getAllFilesRelativeToRoot(File parent, File root) {
        HashSet<File> ret = new HashSet<File>();
        getAllFilesRelativeToRoot(parent, root, ret);
        return ret;
    }

    private static void getAllFilesRelativeToRoot(File parent, File root, Set<File> out) {
        for (File f : parent.listFiles()) {
            if (f.isDirectory()) {
                getAllFilesRelativeToRoot(f, root, out);
            } else {
                //getRelativePath always prefixes the path with './'. We don't need that.
                String path = FileUtil.getRelativePath(f, root).substring(2);
                out.add(new File(path));
            }
        }
    }

    private void copyFromClasspath(String resourceUrl, String filename, File target)
        throws FileNotFoundException, URISyntaxException {
        target.getParentFile().mkdirs();
        InputStream content = getClass().getResourceAsStream(resourceUrl);

        FileOutputStream out = new FileOutputStream(new File(target, filename));

        StreamUtil.copy(content, out, true);
    }

    private void createAntBundleType() throws Exception {
        ResourceType rt = createResourceTypeForBundleType();
        Subject overlord = LookupUtil.getSubjectManager().getOverlord();
        BundleType bt = bundleManager.createBundleType(overlord, ANT_BUNDLE_TYPE_NAME, rt.getId());

        assert bt.getId() > 0;
    }

    private ResourceType createResourceTypeForBundleType() throws Exception {
        final String fullName = FAKE_RESOURCE_TYPE_NAME;
        ResourceType rt = new ResourceType(fullName, RecipeValidationTest.class.getSimpleName(),
            ResourceCategory.PLATFORM, null);

        TransactionManager txMgr = getTransactionManager();
        txMgr.begin();
        em.persist(rt);
        txMgr.commit();
        return rt;
    }

    private void checkForExpectedException(Throwable t, String expectedExceptionClassName,
        String expectedMessage) {
        Throwable test = t;
        do {
            if (expectedExceptionClassName.equals(test.getClass().getName()) &&
                (expectedMessage == null || expectedMessage.equals(test.getMessage()))) {
                return;
            }

            test = test.getCause();
        } while (test != null);

        fail("Exception " + expectedExceptionClassName +
            (expectedMessage == null ? "" : " with message [" + expectedMessage + "]") +
            " not detected in the thrown exception " + t);
    }
}
