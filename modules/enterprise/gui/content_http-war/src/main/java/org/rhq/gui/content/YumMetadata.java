/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.gui.content;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.content.PackageVersion;
import org.rhq.core.domain.content.Repo;
import org.rhq.core.util.stream.StreamUtil;

public class YumMetadata {
    final private static Log log = LogFactory.getLog(YumMetadata.class);

    final public static String PRIMARY_XML = "primary.xml";
    final public static String REPOMD_XML = "repomd.xml";

    /*
     
     This class is implementing a temporary way to grab yum metadata.
     It's following the approach done by: org.rhq.plugins.platform.content.yum.Primary
     In a future sprint this topic will be revisited and this approach should change.
     
     Each package has the ability to store it's metadata in the database.
     We are assuming the metadata was stored as a xml string which has been gzipped. 
     We'll grab this from the DB, uncompress, and adjust the "location" element.
     Note:  The location element is what yum will use to form the GET request to fetch the package.
     
     All of these entries are added together to form the primary.xml
     */

    public static boolean generate(StringBuffer sb, Repo repo, List<PackageVersion> pvs, String filename) {
        if (StringUtils.equalsIgnoreCase(filename, PRIMARY_XML)) {
            generatePrimary(sb, repo, pvs);
            return true;
        }
        if (StringUtils.equalsIgnoreCase(filename, REPOMD_XML)) {
            return generateRepoMD(sb, repo, pvs);
        }
        return false;
    }

    protected static void generatePrimary(StringBuffer sb, Repo repo, List<PackageVersion> pvs) {
        sb.append("<metadata xmlns=\"http://linux.duke.edu/metadata/common\" ");
        sb.append("packages=\"" + pvs.size() + "\">\n");
        sb.append("xmlns:rpm=\"http://linux.duke.edu/metadata/rpm\" ");
        for (PackageVersion p : pvs) {
            String metadata = unzip(p.getMetadata());
            metadata = modifyLocation(metadata, repo.getName(), p.getFileName());
            if (StringUtils.isBlank(metadata)) {
                log.warn("No metadata for package : " + p.getDisplayName() + " not adding this to primary.xml");
                continue;
            }
            sb.append(metadata + "\n");
        }
        sb.append("</metadata>");
    }

    protected static boolean generateRepoMD(StringBuffer sb, Repo repo, List<PackageVersion> pvs) {
        StringBuffer primaryXML = new StringBuffer();
        generatePrimary(primaryXML, repo, pvs);
        sb.append("<repomd xmlns=\"http://linux.duke.edu/metadata/repo\">");
        try {
            writeRepoMDEntry(sb, "primary", getSHA(primaryXML), "0");
        } catch (NoSuchAlgorithmException e) {
            log.info(e);
            return false;
        } catch (IOException e) {
            log.info(e);
            return false;
        }
        sb.append("</repomd>");
        return true;
    }

    protected static String getSHA(StringBuffer primaryXML) throws NoSuchAlgorithmException, IOException {
        // Note:  For RHEL6 this may need to change to "SHA-256", 
        // going for SHA for now, as it's a lowest common denominator so it can work with older clients.
        MessageDigest md = MessageDigest.getInstance("SHA");
        ByteArrayInputStream bis = new ByteArrayInputStream(primaryXML.toString().getBytes());
        DigestInputStream mdistr = new DigestInputStream(bis, md);
        while (mdistr.read() != -1) {
            ;
        }
        mdistr.close();
        return Hex.encodeHexString(md.digest());
    }

    protected static void writeRepoMDEntry(StringBuffer sb, String type, String sum, String ts) {
        sb.append("\t<data type=\"" + type + "\">\n");
        sb.append("\t<location href=\"repodata/" + type + ".xml\"/>\n");
        sb.append("\t<checksum type=\"sha\">" + sum + "</checksum>\n");
        sb.append("\t<timestamp>" + ts + "</timestamp>\n");
        sb.append("\t</data>\n\n");
    }

    protected static String unzip(byte[] zippedMetadata) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(zippedMetadata);
            ByteArrayOutputStream unzipped = new ByteArrayOutputStream();
            GZIPInputStream gis = new GZIPInputStream(bis);
            StreamUtil.copy(gis, unzipped);
            return unzipped.toString();
        } catch (IOException e) {
            log.warn(e);
            return "";
        }
    }

    protected static String modifyLocation(String metadata, String repoName, String filename) {
        /**
         * We will wipe out the current <location href=""> and insert our own which has the package filename.
         * 
         * Note:  this might be a redundant step for content synced from RHN Hosted, yet to avoid cases where the 
         * location might not be what we want, we'll always drop it and put the filename in so we are guaranteed it's 
         * correct.
         */
        final String START_STR = "<location ";
        final String END_STR = "/>";

        int startIndex = metadata.indexOf(START_STR);
        if (startIndex == -1) {
            log.info("Unable to find " + START_STR + " in metadata.");
            return metadata;
        }
        // adjust startIndex to point to after START_STR
        startIndex = startIndex + START_STR.length();
        int endIndex = metadata.indexOf(END_STR, startIndex);
        if (endIndex == -1) {
            log.info("Unable to find " + END_STR + " in metadata.");
            return metadata;
        }

        String partA = metadata.substring(0, startIndex);
        String partB = metadata.substring(endIndex);

        String href = " href=\"" + filename + "\"";
        return partA + href + partB;
    }
}
