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
package org.rhq.enterprise.server.plugins.url;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.rhq.core.util.MessageDigestGenerator;

/**
 * Parses the simple index txt file whose format is simply "location|sha256" where location
 * is a relative filepath where the file is found on the content source.
 * 
 * @author John Mazzitelli
 */
public class SimpleIndexParser implements IndexParser {

    public Map<String, RemotePackageInfo> parse(InputStream indexStream, UrlProvider contentSource) throws Exception {
        Map<String, RemotePackageInfo> fileList = new HashMap<String, RemotePackageInfo>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(indexStream));

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            while (line.startsWith("/")) {
                line = (line.length() > 1) ? line.substring(1) : ""; // we don't want a leading slash
            }
            if (line.length() > 0) {
                String[] locationAndSHA256 = line.split("\\|");
                // if the line doesn't have location|sha256, we silently skip it
                if (locationAndSHA256.length == 2) {
                    URL locationUrl = new URL(contentSource.getRootUrl().toString() + locationAndSHA256[0]);
                    RemotePackageInfo rpi = new RemotePackageInfo(locationAndSHA256[0], locationUrl, locationAndSHA256[1]);
                    fileList.put(rpi.getLocation(), rpi);
                }
            }
        }

        return fileList;
    }

    /**
     * A utility that can build an index file that contains metadata for content found
     * in a given directory.
     * 
     * @param args directory where the content is found and where the content-index.txt file will be written
     */
    public static void main(String args[]) {
        try {
            if (args.length == 0) {
                System.err.println("You must provide the directory name where the content can be found.");
                System.exit(1);
            }

            String directoryString = args[0];
            File directory = new File(directoryString);
            if (!directory.isDirectory()) {
                System.err.println("You did not supply a valid directory name: " + directoryString);
                System.exit(1);
            }

            File index = new File(directory, "content-index.txt");
            PrintWriter writer = new PrintWriter(index);
            try {
                generatePackageIndex(directory, writer, directory);
            } finally {
                writer.close();
            }
        } catch (Throwable t) {
            System.err.println("Failed to generate content index file. Cause: " + t);
            t.printStackTrace(System.err);
            System.exit(1);
        }
    }

    protected static void generatePackageIndex(File file, PrintWriter indexWriter, File root) throws Exception {
        if (file.isDirectory()) {
            File[] childFiles = file.listFiles();
            for (File childFile : childFiles) {
                generatePackageIndex(childFile, indexWriter, root);
            }
        } else if (!file.getCanonicalPath().equals(new File(root, "content-index.txt").getCanonicalPath())) {
            // determine the relative location of the file
            String location = file.getCanonicalPath().substring(root.getCanonicalPath().length() + 1);
            MessageDigestGenerator messageDigestGenerator = new MessageDigestGenerator(MessageDigestGenerator.SHA_256);
            String sha256 = messageDigestGenerator.calcDigestString(file);

            // write the line in the index file
            indexWriter.println(location.replace('\\', '/') + '|' + sha256);
        }
    }
}
