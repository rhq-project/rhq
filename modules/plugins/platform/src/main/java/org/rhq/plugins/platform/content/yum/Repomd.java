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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represends the yum <i>repomd.xml</i> file. This file contains information about the other metadata files.
 *
 * @author jortel
 */
public class Repomd extends Content {
    /**
     * file mutex.
     */
    private static ReentrantLock lock = new ReentrantLock();

    /**
     * Construct with an active yum request.
     *
     * @param request An active yum request.
     */
    public Repomd(Request request) {
        super(request);
    }

    /*
     * (non-Javadoc) @see org.jboss.on.plugins.platform.content.yum.Content#openStream()
     */
    @Override
    public InputStream openStream() throws Exception {
        return new FileInputStream(file());
    }

    /*
     * (non-Javadoc) @see org.jboss.on.plugins.platform.content.yum.Content#writeHeader(java.io.OutputStream)
     */
    @Override
    public void writeHeader(OutputStream ostr) throws Exception {
        PrintWriter writer = new PrintWriter(ostr);
        writer.printf("HTTP/1.1 200\n");
        writer.println("Server: Ackbar (Red Hat)");
        writer.println("Content-Type: text/xml; charset=utf-8");
        writer.printf("Content-Length: %d\n\n", length());
        writer.flush();
    }

    /*
     * (non-Javadoc) @see org.jboss.on.plugins.platform.content.yum.Content#writeContent(java.io.OutputStream)
     */
    @Override
    public void writeContent(OutputStream ostr) throws Exception {
        InputStream istr = openStream();
        transfer(istr, ostr);
        istr.close();
    }

    /*
     * (non-Javadoc) @see org.jboss.on.plugins.platform.content.yum.Content#length()
     */
    @Override
    public long length() throws Exception {
        return file().length();
    }

    /*
     * (non-Javadoc) @see org.jboss.on.plugins.platform.content.yum.Content#delete()
     */
    @Override
    public void delete() {
        lock.lock();
        try {
            new File(filepath()).delete();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the <i>local</i> file object. The file is created if it doesn't already exist.
     *
     * @return The <i>local</i> file object.
     *
     * @throws Exception On all errors.
     */
    File file() throws Exception {
        lock.lock();
        try {
            File file = new File(filepath());
            if (!file.exists()) {
                create(file);
            }

            return file;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Build and return the <i>local</i> file path.
     *
     * @return The <i>local</i> file path.
     */
    String filepath() {
        return new File(context().getTemporaryDirectory(), "repomd.xml").getAbsolutePath();
    }

    /**
     * Create the <i>local</i> file representation of this metadata file. Since this file needs to contain the checksum
     * of the primary.xml file, this method ensures that the Primary object is created and up to date.
     *
     * @param  file The repomd.xml file to be created.
     *
     * @throws Exception On all errors.
     */
    private void create(File file) throws Exception {
        PrintWriter writer = new PrintWriter(file);
        writer.append("<repomd xmlns=\"http://linux.duke.edu/metadata/repo\">");
        writeEntry(writer, "other", 0, "0");
        writeEntry(writer, "filelists", 0, "0");
        Primary primary = new Primary(request);
        long ts = primary.file().lastModified();
        String checksum = sha(primary.openStream());
        writeEntry(writer, "primary", ts, checksum);
        writer.append("</repomd>");
        writer.flush();
        writer.close();
    }

    /**
     * Writes and XML entry for the specified metadata file.
     *
     * @param writer An open writer.
     * @param type   The type of entry.
     * @param ts     The metadata file timestamp.
     * @param sum    The metadata file checksum.
     */
    private void writeEntry(PrintWriter writer, String type, long ts, String sum) {
        writer.printf("\t<data type=\"%s\">", type);
        writer.printf("\t<location href=\"repodata/%s.xml\"/>", type);
        writer.printf("\t<checksum type=\"sha\">%s</checksum>", sum);
        writer.printf("\t<timestamp>%d</timestamp>", ts);
        writer.append("\t</data>");
    }

    /**
     * Get the SHA checksum for the metadata file using the provided input stream.
     *
     * @param  istr An input stream opened on the metadata file.
     *
     * @return The SHA checksum in hex digits for the file.
     *
     * @throws Exception On all errors.
     */
    String sha(InputStream istr) throws Exception {
        byte[] bfr = new byte[10240];
        MessageDigest md = MessageDigest.getInstance("SHA");
        DigestInputStream mdistr = new DigestInputStream(istr, md);
        while (mdistr.read(bfr) != -1) {
            ;
        }

        mdistr.close();
        byte[] b = md.digest();
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }

        return result;
    }
}