/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.pc.drift;

import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.IOUtils.write;
import static org.apache.commons.io.IOUtils.writeLines;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.BaseDirValueContext.fileSystem;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Random;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import org.rhq.common.drift.Headers;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.util.MessageDigestGenerator;

/**
 * A base test class that provides a framework for drift related tests. DriftTest sets up
 * directories for change sets and for fake resources. Before each test method is run, a
 * uniquely named resource directory is created. Files to be included in a change set for
 * example can be placed under this directory. DriftTest also provides a number of helper
 * methods for things like generating a SHA-256 hash, accessing a change set file, and
 * obtaining a unique resource id.
 * <br/>
 * <br/>
 * DriftTest writes all output to a directory named after the test class name. Suppose your
 * test class name is MyDriftTest. DriftTest creates the following directories:
 *
 * <ul>
 *   <li><b>target/MyDriftTest</b> - the base directory to which all output will be written</li>
 *   <li><b>target/MyDriftTest/resources</b> - directory in which fake resources are created for
 *   each test method.</li>
 *   <li><b>target/MyDriftTest/changesets</b> - directory to which change set files are written</li>
 * </ul>
 */
public class DriftTest {

    /**
     * The base directory to which change sets are written to and read from. This directory
     * is deleted and recreated before any test methods are run. It is not deleted after
     * individual test methods are run so that output is available for inspection after tests
     * run.
     */
    protected File changeSetsDir;

    /**
     * The base directory to which resources are written/stored. This directory is deleted
     * and recreated before any test methods are run. It is not deleted after individual
     * test methods are run so that output is available for inspection after tests run.
     */
    protected File resourcesDir;

    /**
     * A {@link ChangeSetManager} to use in tests for reading, writing, and finding change
     * sets.
     */
    protected ChangeSetManager changeSetMgr;

    /**
     * This is basically a counter used to generate unique resource ids across test methods.
     * The current id is obtained from {@link #resourceId()}. The next (or a new) id is
     * obtained from {@link #nextResourceId()}.
     */
    private int resourceId;

    /**
     * Resource files for a given tests are to be written to this directory (or
     * subdirectories). This directory is created before each test method runs. Its name is
     * of the form:
     * <br/>
     * <pre>
     *         &lt;test_method_name&gt;-id
     * </pre>
     * where test_method_name is the name of the current test method, and id is unique
     * integer obtained from {@link #nextResourceId()}.
     */
    protected File resourceDir;

    /**
     * The default interval assigned to drift definitions created using
     * {@link #driftDefinition(String, String)}
     */
    protected long defaultInterval = 1800L; // 30 minutes;

    private MessageDigestGenerator digestGenerator;

    /**
     * Used for creating random files in {@link #createRandomFile(java.io.File, String)}
     */
    private Random random = new Random();

    /**
     * Deletes the base output directory (which is the name of the test class), removing
     * output from a previous run. The output directories (i.e., change sets and resources)
     * are then recreated.
     *
     * @throws Exception
     */
    @BeforeClass
    public void initResourcesAndChangeSetsDirs() throws Exception {
        File basedir = basedir();
        deleteDirectory(basedir);
        basedir.mkdir();

        changeSetsDir = mkdir(basedir, "changesets");
        resourcesDir = mkdir(basedir, "resources");

        digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
    }

    /**
     * Generates a uniquely named resource directory for the test method about to run. The
     * directory name is &lt;test_method_name&gt;-&lt;id&gt; where id is an integer id
     * generated from {@link #nextResourceId()}. The member variable, {@link #resourceDir},
     * is initialized to this directory and is accessible to subclasses.
     *
     * @param test
     */
    @BeforeMethod
    public void setUp(Method test) {
        resourceDir = mkdir(resourcesDir, test.getName() + "-" + nextResourceId());
        changeSetMgr = new ChangeSetManagerImpl(changeSetsDir);
    }

    protected File basedir() {
        return new File("target", getClass().getSimpleName());
    }

    /** @return The current or last resource id generated */
    protected int resourceId() {
        return resourceId;
    }

    /** @return Generates and returns the next resource id */
    protected int nextResourceId() {
        return ++resourceId;
    }

    /**
     * Creates and returns the specified directory. Any nonexistent parent directories are
     * created as well.
     *
     * @param parent The parent directory
     * @param name The name of the directory to be created
     * @return The directory
     */
    protected File mkdir(File parent, String name) {
        File dir = new File(parent, name);
        dir.mkdirs();
        return dir;
    }

    /**
     * Returns the change set file for the specified drift definition for the resource
     * with the id that can be obtained from {@link #resourceId}. The type argument
     * determines whether a coverage or drift change set file is returned.
     *
     * @param config The drift definition name
     * @param type Determines whether a coverage or drift change set file is to be returned
     * @return The change set file
     * @throws IOException
     */
    protected File changeSet(String config, DriftChangeSetCategory type) throws IOException {
        return changeSetMgr.findChangeSet(resourceId(), config, type);
    }

    protected File pinnedSnapshot(String definitionName) throws Exception {
        return new File(changeSetDir(definitionName), "snapshot.pinned");
    }

    /**
     * Returns the previous version snapshot file. This file is generated when an initial
     * snapshot has already been generated and drift is subsequently detected.
     *
     * @param driftDefinitionName The drift definition name
     * @return The previous version snapshot file
     * @throws Exception
     */
    protected File previousSnapshot(String driftDefinitionName) throws Exception {
        File driftDefDir = changeSetDir(driftDefinitionName);
        return new File(driftDefDir, "changeset.txt.previous");
    }

    protected File changeSetDir(String driftDefName) throws Exception {
        File dir = new File(new File(changeSetsDir, Integer.toString(resourceId)), driftDefName);
        dir.mkdirs();
        return dir;
    }

    /**
     * Generates a SHA-256 hash
     * @param file The file for which the hash will be generated
     * @return The SHA-256 hash as a string
     * @throws IOException
     */
    protected String sha256(File file) {
        try {
            return digestGenerator.calcDigestString(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to calculate SHA-256 hash for " + file.getPath(), e);
        }
    }

    protected File createRandomFile(File dir, String fileName) throws Exception {
        return createRandomFile(dir, fileName, 32);
    }

    protected File createRandomFile(File dir, String fileName, int numBytes) throws Exception {
        File file = new File(dir, fileName);
        FileOutputStream stream = new FileOutputStream(file);
        byte[] bytes = new byte[numBytes];
        random.nextBytes(bytes);
        write(bytes, stream);
        stream.close();

        return file;
    }

    protected void writeChangeSet(File changeSetDir, String... changeSet) throws Exception {
        File changeSetFile = new File(changeSetDir, "changeset.txt");
        BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(changeSetFile));
        writeLines(asList(changeSet), "\n", stream);
        stream.close();
    }

    /**
     * Creates a {@link DriftDefinition} with the specified basedir. The file system is
     * used as the context for the basedir which means the path specified is used as is.
     * The interval property is set to {@link #defaultInterval}.
     *
     * @param name The definition name
     * @param basedir An absolute path of the base directory
     * @return The drift definition object
     */
    protected DriftDefinition driftDefinition(String name, String basedir) {
        DriftDefinition config = new DriftDefinition(new Configuration());
        config.setName(name);
        config.setBasedir(new DriftDefinition.BaseDirectory(fileSystem, basedir));
        config.setEnabled(true);
        config.setInterval(defaultInterval);

        return config;
    }

    protected Headers createHeaders(DriftDefinition driftDef, DriftChangeSetCategory type) {
        return createHeaders(driftDef, type, 0);
    }

    protected Headers createHeaders(DriftDefinition driftDef, DriftChangeSetCategory type, int version) {
        Headers headers = new Headers();
        headers.setResourceId(resourceId());
        headers.setDriftDefinitionId(driftDef.getId());
        headers.setDriftDefinitionName(driftDef.getName());
        headers.setBasedir(resourceDir.getAbsolutePath());
        headers.setType(type);
        headers.setVersion(version);

        return headers;
    }

}
