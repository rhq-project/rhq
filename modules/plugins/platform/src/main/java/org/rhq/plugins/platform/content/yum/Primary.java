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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * Represents the primary <i>yum</i> metadata file (primary.xml).
 *
 * @author jortel
 */
public class Primary extends Content {
    /**
     * file mutex.
     */
    private static ReentrantLock lock = new ReentrantLock();

    /**
     * Locally stored primary.xml MD5.
     */
    private final PrimaryMD5 mymd5;

    /**
     * Construct the object with an active request.
     *
     * @param request An active yum request.
     */
    public Primary(Request request) {
        super(request);
        mymd5 = new PrimaryMD5(context().getTemporaryDirectory());
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
     * (non-Javadoc) @see org.jboss.on.plugins.platform.content.yum.Content#delete()
     */
    @Override
    public void delete() {
        mymd5.delete();
        lock.lock();
        try {
            new File(filepath()).delete();
        } finally {
            lock.unlock();
        }
    }

    /*
     * (non-Javadoc) @see org.jboss.on.plugins.platform.content.yum.Content#length()
     */
    @Override
    public long length() throws Exception {
        lock.lock();
        try {
            return file().length();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get the primary.xml file object. The fiie is created if it does not exist.
     *
     * @return The file object.
     *
     * @throws Exception
     */
    File file() throws Exception {
        lock.lock();
        try {
            File file = new File(filepath());
            if (!file.exists()) {
                create(file);
            }

            return file;
        } catch (Exception e) {
            delete();
            throw e;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Get whether the primary metadata is stale.
     *
     * <p/>The data is stale when the md5 stored locally does not match the server's md5.
     *
     * <p/>The md5 is the checksum of all of the packages in all of the channels to which a resource is subscribed. The
     * locally stored value is compared with the server's in order to detect that package metadata has changed in one or
     * more channel which means the locally cached primary.xml no longer represents the content of the subscribed
     * channels.
     *
     * @return
     */
    boolean stale() {
        try {
            String md5 = context().getResourceSubscriptionMD5();
            if (!mymd5.matches(md5)) {
                return true;
            }
        } catch (IOException e) {
            log.error("Error validating/writing md5", e);
            return true;
        }

        return false;
    }

    /**
     * Get the object's local file path. The primary.xml is constructed locally on the filesystem.
     *
     * @return The object's local file path.
     */
    private String filepath() {
        return new File(context().getTemporaryDirectory(), "primary.xml").getAbsolutePath();
    }

    /**
     * Create the <i>local</i> primary.xml file. This consists of querying the server for all package version metadata
     * that this resource is associated with. Each package metadata blob is expected to be a yum package entry from a
     * primary.xml file. The location is replaced with an encoded list of http arguments (?,,) as needed to construct
     * the PackageDetailsKey when yum calls for the package.
     *
     * @param  file The file to create.
     *
     * @throws IOException On errors.
     */
    private void create(File file) throws IOException {
        long start = System.currentTimeMillis();
        PrintWriter writer = new PrintWriter(file);
        Pattern ptn = Pattern.compile("(href=\")([^\"]+)(\")");
        PageControl pc = new PageControl();
        pc.setPageNumber(0);
        pc.setPageSize(200);

        mymd5.write(context().getResourceSubscriptionMD5());

        while (true) {
            PageList<PackageVersionMetadataComposite> list = context().getPackageVersionMetadata(pc);
            if (pc.getPageNumber() == 0) {
                writer.append("<metadata xmlns=\"http://linux.duke.edu/metadata/common\" ");
                writer.append("xmlns:rpm=\"http://linux.duke.edu/metadata/rpm\" ");
                writer.printf("packages=\"%d\">\n", list.getTotalSize());
            }

            if (list.size() < 1) {
                break;
            }

            for (PackageVersionMetadataComposite p : list) {
                byte[] metadata = p.getMetadata();
                String pkg = new String(gunzip(metadata));
                Matcher m = ptn.matcher(pkg);
                m.find();
                StringBuilder sb = new StringBuilder(pkg);
                sb.insert(m.end(2), toArgs(p.getPackageDetailsKey()));
                writer.append(sb.toString());
            }

            pc.setPageNumber(pc.getPageNumber() + 1);
        }

        writer.append("</metadata>");
        writer.flush();
        writer.close();

        long duration = (System.currentTimeMillis() - start);
        log.info("file: " + file + " created: " + duration + " (ms)");
    }

    /**
     * Unzip the input bytes into a string. Uncompression exceptions are tolorated to support databases containing
     * uncompressed data.
     *
     * @param  input An array of gzipped bytes.
     *
     * @return A string.
     */
    private String gunzip(byte[] input) {
        try {
            InputStream zipped = new GZIPInputStream(new ByteArrayInputStream(input), bfr.length);
            ByteArrayOutputStream unzipped = new ByteArrayOutputStream();
            while (true) {
                int bytesRead = zipped.read(bfr);
                if (bytesRead != -1) {
                    unzipped.write(bfr, 0, bytesRead);
                } else {
                    break;
                }
            }

            return unzipped.toString();
        } catch (Exception e) {
            log.debug("compressed data expected, gunzip failed", e);
        }

        return new String(input);
    }

    /**
     * Convert the package key into an http arg string: <i>?type=,name=,ver=arch=</i>.
     *
     * @param  key The package key to convert.
     *
     * @return An http args representation of the key.
     */
    static String toArgs(PackageDetailsKey key) {
        StringBuilder sb = new StringBuilder("?");
        sb.append("type=");
        sb.append(key.getPackageTypeName());
        sb.append(",name=");
        sb.append(key.getName());
        sb.append(",ver=");
        sb.append(key.getVersion());
        sb.append(",arch=");
        sb.append(key.getArchitectureName());
        return sb.toString();
    }

    /**
     * Convert the http args (name, ver, type, arch) into a package details key object.
     *
     * @param  args An http arg string.
     *
     * @return A package details key.
     */
    static PackageDetailsKey toKey(Map<String, String> args) {
        return new PackageDetailsKey(args.get("name"), args.get("ver"), args.get("type"), args.get("arch"));
    }
}