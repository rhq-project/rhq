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

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;

import org.jmock.Expectations;
import org.rhq.common.drift.ChangeSetWriter;
import org.rhq.common.drift.ChangeSetWriterImpl;
import org.rhq.common.drift.FileEntry;
import org.rhq.common.drift.Headers;
import org.rhq.core.clientapi.agent.drift.DriftAgentService;
import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.core.domain.drift.DriftFile;
import org.rhq.core.domain.drift.dto.DriftFileDTO;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.ZipUtil;
import org.rhq.enterprise.server.plugins.drift.mongodb.dao.ChangeSetDAO;
import org.rhq.enterprise.server.plugins.drift.mongodb.dao.FileDAO;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSetEntry;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBFile;
import org.rhq.test.JMockTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;

public class MongoDBDriftServerTest extends JMockTest {

    Mongo connection;

    Morphia morphia;

    Datastore ds;
    
    String db = "rhqtest";

    MessageDigestGenerator digestGenerator;
    
    @BeforeClass
    public void initDB() throws Exception {
        connection = new Mongo("127.0.0.1");
        morphia = new Morphia().map(MongoDBChangeSet.class).map(MongoDBChangeSetEntry.class).map(MongoDBFile.class);
        ds = morphia.createDatastore(connection, db);

        digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
    }
    
    @BeforeMethod
    public void initTest() {
        File basedir = getBaseDir();
        basedir.delete();
        getBaseDir().mkdirs();
    }

    @Test
    public void persistChangeSetWithContentNotInDB() throws Exception {
        int driftDefId = 1;
        final String driftDefName = "saveInitialChangeSet";        
        final int resourceId = 1;
        
        final Headers headers = new Headers();
        headers.setBasedir(getBaseDir().getAbsolutePath());
        headers.setDriftDefinitionId(driftDefId);
        headers.setDriftDefinitionName(driftDefName);
        headers.setResourceId(resourceId);
        headers.setType(COVERAGE);
        headers.setVersion(0);
        
        long timestamp = System.currentTimeMillis();
        long size = 1024;
        
        File changeSetZip = createChangeSetZipFile(headers,
                addedFileEntry("1.txt", sha256("1a2b3c4d"), timestamp, size));
                                
        TestMongoDBDriftServer driftServer = new TestMongoDBDriftServer();
        driftServer.setConnection(connection);
        driftServer.setMorphia(morphia);
        driftServer.setDatastore(ds);
        driftServer.setChangeSetDAO(new ChangeSetDAO(morphia, connection, db));
        driftServer.setFileDAO(new FileDAO(ds.getDB()));
        
        final List<? extends DriftFile> missingContent = asList(new TestDriftFile(sha256("1a2b3c4d")));

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
        driftServer.saveChangeSet(null, resourceId, changeSetZip);
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

    protected String sha256(String string) {
        return digestGenerator.calcDigestString(string);
    }

    protected DriftFileDTO newDriftFile(String hash) {
        DriftFileDTO file = new DriftFileDTO();
        file.setHashId(hash);
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
