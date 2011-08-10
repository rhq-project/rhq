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

package org.rhq.enterprise.server.plugins.drift.mongodb;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import org.bson.types.ObjectId;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GridFSTest {

    Mongo connection;

    DB db;

    @BeforeClass
    public void initDB() throws Exception {
        connection = new Mongo("localhost");
        connection.dropDatabase("rhqtest");
        db = connection.getDB("rhqtest");

    }

    @Test(enabled = false)
    public void writeFile() throws Exception {
        GridFS gridFS = new GridFS(db);
        File file = new File("/home/jsanda/clojure-plugin.tar");
        GridFSInputFile inputFile = gridFS.createFile(new FileInputStream(file), file.getName());

        inputFile.save();

        FileOutputStream stream = new FileOutputStream("/home/jsanda/gridfs-test.tar");
        GridFSDBFile gridFSDBFile = gridFS.findOne((ObjectId) inputFile.getId());
        gridFSDBFile.writeTo(stream);
    }

}
