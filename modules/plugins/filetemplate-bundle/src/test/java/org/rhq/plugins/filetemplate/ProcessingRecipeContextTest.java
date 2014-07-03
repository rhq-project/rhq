/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.plugins.filetemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.bundle.filetemplate.recipe.RecipeContext;
import org.rhq.bundle.filetemplate.recipe.RecipeParser;
import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeploymentHistory;
import org.rhq.core.domain.bundle.BundleType;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.pluginapi.bundle.BundleHandoverRequest;
import org.rhq.core.pluginapi.bundle.BundleHandoverResponse;
import org.rhq.core.pluginapi.bundle.BundleManagerProvider;
import org.rhq.core.pluginapi.util.FileUtils;
import org.rhq.core.system.OperatingSystemType;
import org.rhq.core.system.SystemInfo;
import org.rhq.core.system.SystemInfoFactory;
import org.rhq.core.util.stream.StreamUtil;

@Test
public class ProcessingRecipeContextTest {
    private StringBuilder recipe;

    @BeforeMethod
    public void cleanRecipeBeforeMethod() {
        cleanRecipe();
    }

    @AfterClass
    public void afterClass() throws IOException {
        FileUtils.purge(getTmpDir(), true);
    }

    public void testRealizeRecipe() throws Exception {
        File testDir = getTestDir("testrealize");
        File file = writeFile("a b c\nhello@@ var @@world\na b c\n", testDir, "config.txt");

        addRecipeCommand("realize --file=" + file.getPath());

        ProcessingRecipeContext context = createRecipeContext(testDir);
        context.addReplacementVariableValue("var", "BOO");
        context = parseRecipeNow(context);

        Set<String> files = context.getRealizedFiles();
        assert files.size() == 1 : files;
        assert files.contains(file.getPath()) : files;

        String realizedContent = readFile(file);
        assert realizedContent.equals("a b c\nhelloBOOworld\na b c\n") : realizedContent;
    }

    public void testFileRecipe() throws Exception {
        File testDir = getTestDir("testfile");
        File testDestDir = getTestDir("testfile-dest");
        File runme = writeFile("runme script", testDir, "run-me.sh");
        File runmeDest = new File(testDestDir, "run-me.sh");
        File another = writeFile("<wotgorilla/>", testDir, "META-INF/another.xml");
        File anotherDest = new File(testDestDir, "/OTHER/another.xml");

        addRecipeCommand("file --source=run-me.sh \"--destination=" + runmeDest + "\"");
        addRecipeCommand("file -s META-INF/another.xml -d \"" + anotherDest + "\"");

        RecipeContext context = parseRecipeNow(testDir);

        Map<String, String> files = context.getFiles();
        assert files.size() == 2 : files;
        assert files.get("run-me.sh") != null : files;
        assert files.get("run-me.sh").equals(runmeDest.getPath()) : files;
        assert files.get("META-INF/another.xml") != null : files;
        assert files.get("META-INF/another.xml").equals(anotherDest.getPath()) : files;

        String runmeContent = readFile(runme);
        String runmeDestContent = readFile(runmeDest);
        assert runmeContent.equals(runmeDestContent) : runmeDestContent;
        String anotherContent = readFile(another);
        String anotherDestContent = readFile(anotherDest);
        assert anotherContent.equals(anotherDestContent) : anotherDestContent;
    }

    public void testDeployRecipe() throws Exception {
        File testDir = getTestDir("testdeploy");
        File testDestDir = getTestDir("testdeploy-dest");
        File unzippedFile = new File(testDestDir, "zipped-file.txt");
        File zipFile = createZip("zipped file content", testDir, "deploy-test.zip", unzippedFile.getName());

        addRecipeCommand("deploy -f " + zipFile.getName() + " -d \"" + testDestDir + "\"");

        RecipeContext context = parseRecipeNow(testDir);

        Map<String, String> files = context.getDeployFiles();
        assert files.size() == 1 : files;
        assert files.get(zipFile.getName()) != null;

        String zippedFileContent = readFile(unzippedFile);
        assert zippedFileContent.equals("zipped file content");
    }

    public void testScriptRecipe() throws Exception {
        File testDir = getTestDir("testscript");
        File testOutDir = getTestDir("testscript-out");
        File echoOutput = new File(testOutDir, "out.txt");
        echoOutput.getParentFile().mkdirs();
        File script = writeFile("echo hello > \"" + echoOutput.getAbsolutePath() + "\"\n", testDir, "bin/test.sh");

        addRecipeCommand("script bin/test.sh");

        RecipeContext context = parseRecipeNow(testDir);

        Set<String> scripts = context.getScriptFiles();
        assert scripts.size() == 1 : scripts;
        assert scripts.contains("bin/test.sh") : scripts;
        String echoOutputContent = readFile(echoOutput);
        assert echoOutputContent.trim().equals("hello");
    }

    public void testCommandRecipe() throws Exception {
        File testDir = getTestDir("testcommand");
        File echoOutput = new File(testDir, "out.txt");
        echoOutput.getParentFile().mkdirs();

        SystemInfo sysinfo = SystemInfoFactory.createSystemInfo();
        if (sysinfo.getOperatingSystemType() == OperatingSystemType.WINDOWS) {
            addRecipeCommand("command cmd /C \"echo helloWorld > '" + echoOutput.getAbsolutePath() + "'\"");
        } else {
            addRecipeCommand("command sh -c \"echo helloWorld > '" + echoOutput.getAbsolutePath() + "'\"");
        }

        RecipeContext context = parseRecipeNow(testDir);

        String echoOutputContent = readFile(echoOutput);
        assert echoOutputContent.trim().equals("helloWorld");
    }

    private ProcessingRecipeContext parseRecipeNow(File testDir) throws Exception {
        ProcessingRecipeContext context = createRecipeContext(testDir);
        return parseRecipeNow(context);
    }

    private ProcessingRecipeContext createRecipeContext(File testDir) {
        Map<PackageVersion, File> packageVersionFiles = null; // TODO not used yet
        SystemInfo sysinfo = SystemInfoFactory.createSystemInfo();
        String cwd = testDir.getAbsolutePath();

        // need a dummy deployment

        ResourceType resourceType = new ResourceType("name", "plugin", ResourceCategory.PLATFORM, null);
        Resource resource = new Resource("key", "name", resourceType);
        BundleType bundleType = new BundleType("name", resourceType);
        Bundle bundle = new Bundle("name", bundleType, null, null);
        BundleVersion bundleVersion = new BundleVersion("bname", "bversion", bundle, "");
        String name = "name";
        String destBaseDirName = "destBaseDirName";
        String installDir = "installDir";
        BundleDestination bundleDestination = new BundleDestination(bundle, "destName", new ResourceGroup("groupName"),
            destBaseDirName, installDir);
        BundleDeployment bundleDeployment = new BundleDeployment(bundleVersion, bundleDestination, name);
        BundleResourceDeployment deployment = new BundleResourceDeployment(bundleDeployment, resource);

        ProcessingRecipeContext context = new ProcessingRecipeContext(getRecipe(), packageVersionFiles, sysinfo, cwd,
            deployment, new DummyBundleManagerProvider());
        return context;
    }

    private ProcessingRecipeContext parseRecipeNow(ProcessingRecipeContext context) throws Exception {
        RecipeParser parser = new RecipeParser();
        parser.setReplaceReplacementVariables(true);
        parser.parseRecipe(context);
        return context;
    }

    private File getTestDir(String subdir) {
        return new File(getTmpDir(), subdir);
    }

    private File getTmpDir() {
        File tmpdir = new File(System.getProperty("java.io.tmpdir"), "processing-recipe-test");
        tmpdir.mkdirs();
        return tmpdir;
    }

    private void cleanRecipe() {
        this.recipe = new StringBuilder();
    }

    private void addRecipeCommand(String commandLine) {
        this.recipe.append(commandLine).append("\n");
    }

    private String getRecipe() {
        return this.recipe.toString();
    }

    private String readFile(File file) throws Exception {
        return new String(StreamUtil.slurp(new FileInputStream(file)));
    }

    private File writeFile(String content, File destDir, String fileName) throws Exception {
        FileOutputStream out = null;

        try {
            File destFile = new File(destDir, fileName);
            destFile.getParentFile().mkdirs();
            out = new FileOutputStream(destFile);
            out.write(content.getBytes());
            return destFile;
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private File createZip(String content, File destDir, String zipName, String entryName) throws Exception {
        FileOutputStream stream = null;
        ZipOutputStream out = null;

        try {
            destDir.mkdirs();
            File zipFile = new File(destDir, zipName);
            stream = new FileOutputStream(zipFile);
            out = new ZipOutputStream(stream);

            ZipEntry zipAdd = new ZipEntry(entryName);
            zipAdd.setTime(System.currentTimeMillis());
            out.putNextEntry(zipAdd);
            out.write(content.getBytes());
            return zipFile;
        } finally {
            if (out != null) {
                out.close();
            }
            if (stream != null) {
                stream.close();
            }
        }
    }

    private class DummyBundleManagerProvider implements BundleManagerProvider {
        public void auditDeployment(BundleResourceDeployment deployment, String action, String info,
            BundleResourceDeploymentHistory.Category category, BundleResourceDeploymentHistory.Status status,
            String message, String attachment) throws Exception {
            System.out.println("audit: action=[" + action + "], status=[" + status + "], message: " + message);
        }

        public List<PackageVersion> getAllBundleVersionPackageVersions(BundleVersion bundleVersion) throws Exception {
            return null;
        }

        public long getFileContent(PackageVersion packageVersion, OutputStream outputStream) throws Exception {
            return 0;
        }

        @Override
        public BundleHandoverResponse handoverContent(Resource bundleTarget, BundleHandoverRequest handoverRequest) {
            return null;
        }
    }
}
