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
package org.rhq.enterprise.server.plugins.drift.mongodb;

import org.jmock.Expectations;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.ChangeSetWriterImpl;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.dto.DriftFileDTO;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.enterprise.server.plugin.pc.drift.DriftChangeSetSummary;
import org.rhq.enterprise.server.plugins.drift.mongodb.dao.ChangeSetDAO;
import org.rhq.enterprise.server.plugins.drift.mongodb.dao.FileDAO;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSetEntry;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;
import static org.apache.commons.io.IOUtils.write;
import static org.rhq.core.domain.drift.DriftCategory.FILE_ADDED;
import static org.rhq.core.domain.drift.DriftCategory.FILE_CHANGED;
import static org.rhq.core.domain.drift.DriftCategory.FILE_REMOVED;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.DRIFT;
import static org.rhq.core.domain.drift.DriftConfigurationDefinition.DriftHandlingMode.normal;
import static org.rhq.test.AssertUtils.assertPropertiesMatch;
import static org.testng.Assert.assertEquals;

public class MongoDBDriftServerTest extends MongoDBTest {

    private MessageDigestGenerator digestGenerator;

    private Random random;

    private TestMongoDBDriftServer driftServer;
    
    @BeforeClass
    public void initClass() throws Exception {
        digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
        random = new Random();
    }
    
    @BeforeMethod
    public void initTest() {
        clearCollections("changesets", "fs.files", "fs.chunks");

        File basedir = getBaseDir();
        basedir.delete();
        getBaseDir().mkdirs();

        driftServer = new TestMongoDBDriftServer();
        driftServer.setConnection(connection);
        driftServer.setMorphia(morphia);
        driftServer.setDatastore(ds);
        driftServer.setChangeSetDAO(new ChangeSetDAO(morphia, connection, "rhqtest"));
        driftServer.setFileDAO(new FileDAO(ds.getDB()));
    }

    @Test
    public void persistInitialChangeSetWithContentNotInDB() throws Exception {
        int driftDefId = 1;
        final String driftDefName = "saveInitialChangeSetWithContentNotInDB";
        final int resourceId = 1;

        final Headers headers = new Headers();
        headers.setBasedir(getBaseDir().getAbsolutePath());
        headers.setDriftDefinitionId(driftDefId);
        headers.setDriftDefinitionName(driftDefName);
        headers.setResourceId(resourceId);
        headers.setType(COVERAGE);
        headers.setVersion(0);

        String file1SHA = sha256("1a2b3c4d");
        String file2SHA = sha256("1a2b3c4d");

        File changeSetZip = createChangeSetZipFile(headers,
                addedFileEntry("1.bin", file1SHA),
                addedFileEntry("2.bin", file2SHA));

        final List<? extends DriftFile> missingContent = asList(new TestDriftFile(file1SHA),
                new TestDriftFile(file2SHA));

        final DriftAgentService driftAgentService = context.mock(DriftAgentService.class);
        context.checking(new Expectations() {{
            exactly(1).of(driftAgentService).ackChangeSet(resourceId, driftDefName);
            // TODO Need to verify that we send the correct headers to the agent
            exactly(1).of(driftAgentService).requestDriftFiles(with(resourceId), with(any(Headers.class)),
                    with(missingContent));
        }});
        driftServer.setDriftAgentService(driftAgentService);

        // We can pass null for the subject because MongoDBDriftServer currently does not
        // use the subject argument.
        DriftChangeSetSummary actualSummary = driftServer.saveChangeSet(null, resourceId, changeSetZip);

        // verify that the change set was persisted
        ChangeSetDAO changeSetDAO = new ChangeSetDAO(morphia, connection, "rhqtest");
        List<MongoDBChangeSet> changeSets = changeSetDAO.find().asList();

        assertEquals(changeSets.size(), 1, "Expected to find one change set in the database.");
        MongoDBChangeSet actual = changeSets.get(0);

        MongoDBChangeSet expected = new MongoDBChangeSet();
        // Need to set the id to actual.id. Since ids are random, we cannot use a canned
        // value. We have to set it the same value that is in the database.
        expected.setId(actual.getId());
        expected.setDriftDefinitionId(driftDefId);
        expected.setResourceId(resourceId);
        expected.setDriftDefinitionName(driftDefName);
        expected.setCategory(COVERAGE);
        expected.setVersion(0);
        expected.setDriftHandlingMode(normal);

        MongoDBChangeSetEntry entry1 = new MongoDBChangeSetEntry("1.bin", FILE_ADDED);
        entry1.setNewFileHash(file1SHA);
        expected.add(entry1);

        MongoDBChangeSetEntry entry2 = new MongoDBChangeSetEntry("2.bin", FILE_ADDED);
        entry2.setNewFileHash(file2SHA);
        expected.add(entry2);

        String[] ignore = new String[] {"id", "objectId", "ctime"};
        assertChangeSetMatches("Failed to persist change set", expected, actual, ignore);

        DriftChangeSetSummary expectedSummary = new DriftChangeSetSummary();
        expectedSummary.setCategory(COVERAGE);
        expectedSummary.setResourceId(resourceId);
        expectedSummary.setDriftDefinitionName(driftDefName);
        expectedSummary.setCreatedTime(actual.getCtime());

        assertPropertiesMatch("The change set summary is wrong", expectedSummary, actualSummary);
    }

    @Test
    public void persistInitialChangeSetWithContentInDB() throws Exception {
        int driftDefId = 1;
        final String driftDefName = "saveInitialChangeSetWithContentInDB";
        final int resourceId = 1;

        final Headers headers = new Headers();
        headers.setBasedir(getBaseDir().getAbsolutePath());
        headers.setDriftDefinitionId(driftDefId);
        headers.setDriftDefinitionName(driftDefName);
        headers.setResourceId(resourceId);
        headers.setType(COVERAGE);
        headers.setVersion(0);

        String file1SHA = sha256("1a2b3c4d");
        String file2SHA = sha256("1a2b3c4d");

        // store content in the database
        File file1 = createRandomFile(getBaseDir(), file1SHA, 1024);
        File file2 = createRandomFile(getBaseDir(), file2SHA, 1024);

        FileDAO fileDAO = new FileDAO(ds.getDB());
        fileDAO.save(file1);
        fileDAO.save(file2);

        File changeSetZip = createChangeSetZipFile(headers,
                addedFileEntry("1.bin", file1SHA),
                addedFileEntry("2.bin", file2SHA));


        final DriftAgentService driftAgentService = context.mock(DriftAgentService.class);
        context.checking(new Expectations() {{
            exactly(1).of(driftAgentService).ackChangeSet(resourceId, driftDefName);
        }});
        driftServer.setDriftAgentService(driftAgentService);

        // We can pass null for the subject because MongoDBDriftServer currently does not
        // use the subject argument.
        DriftChangeSetSummary actualSummary = driftServer.saveChangeSet(null, resourceId, changeSetZip);

        // verify that the change set was persisted
        ChangeSetDAO changeSetDAO = new ChangeSetDAO(morphia, connection, "rhqtest");
        List<MongoDBChangeSet> changeSets = changeSetDAO.find().asList();

        assertEquals(changeSets.size(), 1, "Expected to find one change set in the database.");
        MongoDBChangeSet actual = changeSets.get(0);

        MongoDBChangeSet expected = new MongoDBChangeSet();
        // Need to set the id to actual.id. Since ids are random, we cannot use a canned
        // value. We have to set it the same value that is in the database.
        expected.setId(actual.getId());
        expected.setDriftDefinitionId(driftDefId);
        expected.setResourceId(resourceId);
        expected.setDriftDefinitionName(driftDefName);
        expected.setCategory(COVERAGE);
        expected.setVersion(0);
        expected.setDriftHandlingMode(normal);

        MongoDBChangeSetEntry entry1 = new MongoDBChangeSetEntry("1.bin", FILE_ADDED);
        entry1.setNewFileHash(file1SHA);
        expected.add(entry1);

        MongoDBChangeSetEntry entry2 = new MongoDBChangeSetEntry("2.bin", FILE_ADDED);
        entry2.setNewFileHash(file2SHA);
        expected.add(entry2);

        String[] ignore = new String[] {"id", "objectId", "ctime"};
        assertChangeSetMatches("Failed to persist change set", expected, actual, ignore);

        DriftChangeSetSummary expectedSummary = new DriftChangeSetSummary();
        expectedSummary.setCategory(COVERAGE);
        expectedSummary.setResourceId(resourceId);
        expectedSummary.setDriftDefinitionName(driftDefName);
        expectedSummary.setCreatedTime(actual.getCtime());

        assertPropertiesMatch("The change set summary is wrong", expectedSummary, actualSummary);
    }

    @Test
    public void persistChangeSetWithSomeContentInDB() throws Exception {
        int driftDefId = 1;
        final String driftDefName = "saveChangeSetWithSomeContentInDB";
        final int resourceId = 1;

        final Headers headers = new Headers();
        headers.setBasedir(getBaseDir().getAbsolutePath());
        headers.setDriftDefinitionId(driftDefId);
        headers.setDriftDefinitionName(driftDefName);
        headers.setResourceId(resourceId);
        headers.setType(DRIFT);
        headers.setVersion(1);

        String oldFile1SHA = sha256("1a2b3c4d");
        String newFile1SHA = sha256("2a3b4c5d");
        String file2SHA = sha256("1a2b3c4d");

        // store content in the database
        File oldFile1 = createRandomFile(getBaseDir(), oldFile1SHA, 1024);
        File file2 = createRandomFile(getBaseDir(), file2SHA, 1024);

        FileDAO fileDAO = new FileDAO(ds.getDB());
        fileDAO.save(oldFile1);
        fileDAO.save(file2);

        File changeSetZip = createChangeSetZipFile(headers,
                changedFileEntry("1.bin", oldFile1SHA, newFile1SHA),
                removedFileEntry("2.bin", file2SHA));

        final List<? extends DriftFile> missingContent = asList(new TestDriftFile(newFile1SHA));
        final DriftAgentService driftAgentService = context.mock(DriftAgentService.class);
        context.checking(new Expectations() {{
            exactly(1).of(driftAgentService).ackChangeSet(resourceId, driftDefName);
            exactly(1).of(driftAgentService).requestDriftFiles(with(resourceId), with(any(Headers.class)),
                    with(missingContent));
        }});
        driftServer.setDriftAgentService(driftAgentService);

        // We can pass null for the subject because MongoDBDriftServer currently does not
        // use the subject argument.
        DriftChangeSetSummary actualSummary = driftServer.saveChangeSet(null, resourceId, changeSetZip);

        // verify that the change set was persisted
        ChangeSetDAO changeSetDAO = new ChangeSetDAO(morphia, connection, "rhqtest");
        List<MongoDBChangeSet> changeSets = changeSetDAO.find().asList();

        assertEquals(changeSets.size(), 1, "Expected to find one change set in the database.");
        MongoDBChangeSet actual = changeSets.get(0);

        MongoDBChangeSet expected = new MongoDBChangeSet();
        // Need to set the id to actual.id. Since ids are random, we cannot use a canned
        // value. We have to set it the same value that is in the database.
        expected.setId(actual.getId());
        expected.setDriftDefinitionId(driftDefId);
        expected.setResourceId(resourceId);
        expected.setDriftDefinitionName(driftDefName);
        expected.setCategory(DRIFT);
        expected.setVersion(1);
        expected.setDriftHandlingMode(normal);

        MongoDBChangeSetEntry entry1 = new MongoDBChangeSetEntry("1.bin", FILE_CHANGED);
        entry1.setOldFileHash(oldFile1SHA);
        entry1.setNewFileHash(newFile1SHA);
        expected.add(entry1);

        MongoDBChangeSetEntry entry2 = new MongoDBChangeSetEntry("2.bin", FILE_REMOVED);
        entry2.setOldFileHash(file2SHA);
        expected.add(entry2);

        String[] ignore = new String[] {"id", "objectId", "ctime"};
        assertChangeSetMatches("Failed to persist change set", expected, actual, ignore);

        DriftChangeSetSummary expectedSummary = new DriftChangeSetSummary();
        expectedSummary.setCategory(DRIFT);
        expectedSummary.setResourceId(resourceId);
        expectedSummary.setDriftDefinitionName(driftDefName);
        expectedSummary.setCreatedTime(actual.getCtime());
        expectedSummary.addDriftPathname("1.bin");
        expectedSummary.addDriftPathname("2.bin");

        assertPropertiesMatch("The change set summary is wrong", expectedSummary, actualSummary);
    }

    protected File createChangeSetZipFile(Headers headers, FileEntry... fileEntries) throws Exception {
        ChangeSetWriter writer = newChangeSetWriter(headers);
        for (FileEntry entry : fileEntries) {
            writer.write(entry);
        }
        writer.close();
        
        File zipFile = new File(getBaseDir(), "changeset.zip");
        ZipUtil.zipFileOrDirectory(getChangeSetFile(), zipFile);
        
        return zipFile;
    }

    /**
     * Initializes a {@link ChangeSetWriter} that will write to a file returned from
     * {@link #getChangeSetFile()}
     *
     * @return A new {@link ChangeSetWriter}
     */
    protected ChangeSetWriter newChangeSetWriter(Headers headers) throws Exception {
        return new ChangeSetWriterImpl(getChangeSetFile(), headers);
    }

    /**
     * @return A file named changeset.txt located in {@link #getBaseDir() basedir} 
     */
    protected File getChangeSetFile() {
        return new File(getBaseDir(), "changeset.txt");
    }

    /**
     * Returns a File object that denotes the base directory for this test class. That 
     * directory by default is &lt;project_basedir&gt;target/&lt;test_class_name&gt;
     * 
     * @return The base directory for this test class
     */
    protected File getBaseDir() {
        return new File("target", getClass().getSimpleName());
    }

    protected FileEntry addedFileEntry(String path, String sha256) {
        long timestamp = System.currentTimeMillis();
        long size = 1024;
        return FileEntry.addedFileEntry(path, sha256, timestamp, size);
    }

    protected FileEntry changedFileEntry(String path, String oldSHA, String newSHA) {
        long timestamp = System.currentTimeMillis();
        long size = 1024;
        return FileEntry.changedFileEntry(path, oldSHA, newSHA, timestamp, size);
    }

    protected FileEntry removedFileEntry(String path, String oldSHA) {
        return FileEntry.removedFileEntry(path, oldSHA);
    }

    protected String sha256(String string) {
        return digestGenerator.calcDigestString(string);
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

    private static class TestMongoDBDriftServer extends MongoDBDriftServer {
        
        public DriftAgentService driftAgentService;
        
        @Override
        public DriftAgentService getDriftAgentService(int resourceId) {
            return driftAgentService;
        }
        
        public void setDriftAgentService(DriftAgentService driftAgentService) {
            this.driftAgentService = driftAgentService;
        }
    }

    /**
     * {@link DriftFileDTO} does not implement equals/hashCode which makes some of the 
     * verification a little tricky in situations where collections of DriftFile objects
     * are getting passed around. This subclass implements equals and hashCode.
     */
    private static class TestDriftFile extends DriftFileDTO {

        public TestDriftFile(String hash) {
            super();
            setHashId(hash);
        }

        /**
         * Equality is based soley on the {@link #getHashId() hashId} property.
         *
         * @param o The object to compare against
         * @return true if the object is a TestDriftFile and has the same hashId.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof DriftFileDTO)) return false;

            DriftFileDTO that = (DriftFileDTO) o;

            if (!getHashId().equals(that.getHashId())) return false;

            return true;
        }

        /**
         * @return A hash code based on the {@link #getHashId() hashId} property.
         */
        @Override
        public int hashCode() {
            return getHashId().hashCode();
        }
    }
         
}
