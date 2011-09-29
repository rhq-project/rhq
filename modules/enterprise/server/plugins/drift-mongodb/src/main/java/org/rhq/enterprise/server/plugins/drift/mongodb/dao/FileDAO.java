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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

public class FileDAO {

    private DB db;

    private GridFS gridFS;

    public FileDAO(DB db) {
        this.db = db;
        gridFS = new GridFS(this.db);
    }

    public InputStream findOne(String hash) {
        GridFSDBFile dbFile = gridFS.findOne(new BasicDBObject("_id", hash));
        if (dbFile == null) {
            return null;
        }
        return dbFile.getInputStream();
    }

    public void save(File file) throws IOException {
        GridFSInputFile inputFile = gridFS.createFile(new BufferedInputStream(new FileInputStream(file)));
        inputFile.put("_id", file.getName());
        inputFile.save();
    }

}
