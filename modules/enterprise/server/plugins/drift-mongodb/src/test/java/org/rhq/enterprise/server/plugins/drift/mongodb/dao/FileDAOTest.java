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

package org.rhq.enterprise.server.plugins.drift.mongodb.dao;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.BasicDBObject;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSetEntry;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBFile;

import static org.apache.commons.io.IOUtils.write;
import static org.rhq.core.util.file.FileUtil.purge;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class FileDAOTest {
    // Tests need to be disabled when committed/pushed to the remote repo until we get
    // mongodb installed on the hudson slave
    static final boolean ENABLED = false;

    Mongo connection;

    Morphia morphia;

    Datastore ds;

    FileDAO dao;

    private File dataDir;

    private GridFS gridFS;

    private Random random;

    private MessageDigestGenerator digestGenerator;

    @BeforeClass
    public void initDB() throws Exception {
        connection = new Mongo("127.0.0.1");

        morphia = new Morphia()
            .map(MongoDBChangeSet.class)
            .map(MongoDBChangeSetEntry.class)
            .map(MongoDBFile.class);

        ds = morphia.createDatastore(connection, "rhqtest");
        gridFS = new GridFS(connection.getDB("rhqtest"));

        dataDir = new File("target", getClass().getSimpleName());
        if (dataDir.exists()) {
            purge(dataDir, false);
        }
        dataDir.mkdirs();

        random = new Random();
        digestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);

        dao = new FileDAO(connection.getDB("rhqtest"));
    }

    @BeforeMethod
    public void clearCollections() {
        gridFS.remove(new BasicDBObject());
    }

    @Test(enabled = ENABLED)
    public void findFile() throws Exception {
        File file = createRandomFile();
        String hash = sha256(file);

        GridFSInputFile inputFile = gridFS.createFile(new FileInputStream(file));
        inputFile.put("_id", hash);
        inputFile.save();

        InputStream dbInputStream = dao.findOne(hash);

        assertNotNull(dbInputStream, "Expected to find file with name " + hash);
        File actualFile = new File(dataDir, "findFile.actual");
        StreamUtil.copy(dbInputStream, new FileOutputStream(actualFile), true);

        assertTrue(actualFile.exists(), "Expected to find file " + actualFile.getPath());
    }

    @Test(enabled = ENABLED)
    public void saveAndFindFile() throws Exception {
        File file = createRandomFile();
        String hash = file.getName();

        dao.save(file);
        InputStream dbInputStream = dao.findOne(hash);

        assertNotNull(dbInputStream, "Expected to find file with name " + hash);
        File actualFile = new File(dataDir, "saveAndFindFile.actual");
        StreamUtil.copy(dbInputStream, new FileOutputStream(actualFile), true);

        assertTrue(actualFile.exists(), "Expected to find file " + actualFile.getPath());
    }

    private File createRandomFile() throws Exception {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        File file = new File(dataDir, sha256(bytes));
        FileOutputStream stream = new FileOutputStream(file);
        write(bytes, stream);
        stream.close();

        return file;
    }

    protected String sha256(byte[] bytes) {
        try {
            return digestGenerator.calcDigestString(bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to calculate SHA-256 hash", e);
        }
    }

    protected String sha256(File file) {
        try {
            return digestGenerator.calcDigestString(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to calculate SHA-256 hash for " + file.getPath(), e);
        }
    }
}
