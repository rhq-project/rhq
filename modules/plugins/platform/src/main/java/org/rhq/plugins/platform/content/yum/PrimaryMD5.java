 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.plugins.platform.content.yum;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class PrimaryMD5 {
    private final File directory;
    private static final ReentrantLock lock = new ReentrantLock();

    private final Log log = LogFactory.getLog(PrimaryMD5.class);

    PrimaryMD5(File directory) {
        this.directory = directory;
    }

    String read() throws IOException {
        String content = null;
        BufferedReader reader = null;
        File file = file();
        lock.lock();
        try {
            if (file.exists()) {
                reader = new BufferedReader(new FileReader(file));
                content = reader.readLine();
            }
        } catch (Exception e) {
            log.error("primary.md5 - read failed\n" + e);
        } finally {
            lock.unlock();
            if (reader != null) {
                reader.close();
            }
        }

        return content;
    }

    void write(String content) throws IOException {
        BufferedWriter writer = null;
        lock.lock();
        try {
            writer = new BufferedWriter(new FileWriter(file()));
            writer.write(content);
            writer.flush();
            log.info("primary.md5 updated: " + content);
        } catch (IOException e) {
            log.error("primary.md5 - update failed\n" + e);
            throw e;
        } finally {
            lock.unlock();
            if (writer != null) {
                writer.close();
            }
        }
    }

    void delete() {
        file().delete();
    }

    long lastModified() {
        return file().lastModified();
    }

    boolean matches(String content) throws IOException {
        return content.equals(read());
    }

    private File file() {
        return new File(directory, "primary.md5");
    }
}