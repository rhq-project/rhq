/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.core.util.updater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.rhq.core.util.MessageDigestGenerator;

/**
 * This provides a hashmap that maps a filename to its hashcode value (md5).
 * The key to the map will be filenames; the values are unique hashcodes generated from
 * the content of the files.
 * 
 * @author John Mazzitelli
 */
public class FileHashcodeMap extends TreeMap<String, String> {
    public static final String UNKNOWN_DIR_HASHCODE = "?UNKNOWN_DIR_HASHCODE?";
    public static final String UNKNOWN_FILE_HASHCODE = "?UNKNOWN_FILE_HASHCODE?";

    private static final long serialVersionUID = 1L;
    private static final String COLUMN_SEPARATOR = "\t";

    public static FileHashcodeMap loadFromFile(File file) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            FileHashcodeMap map = new FileHashcodeMap();
            int lineNumber = 1;
            String line = reader.readLine();
            while (line != null) {
                String[] columns = line.split(COLUMN_SEPARATOR);
                if (columns.length != 2) {
                    throw new Exception("Format error in [" + file.getAbsolutePath() + "] at line #[" + lineNumber
                        + "]: " + line);
                }

                map.put(columns[0], columns[1]);

                lineNumber++;
                line = reader.readLine();
            }
            return map;
        } finally {
            reader.close();
        }
    }

    /**
     * Given a directory, this will recursively traverse that directory's files/subdirectories and
     * generate the hashcode map for all files it encounters and add that data to the given map.
     * If given a regular file, a single entry is added to the given map.
     * 
     * @param rootDir existing directory to scan and generate hashcodes for all its files
     * @param ignoreRegex a regular expression that indicates which files/directories should be ignored.
     *                    If a relative file/directory path matches this regex, it will be skipped.
     * @returns the map containing all files found and their generated hashcodes
     * @throws Exception if failed to generate hashcode for the directory
     */
    public static FileHashcodeMap generateFileHashcodeMap(File rootDir, Pattern ignoreRegex) throws Exception {
        FileHashcodeMap map = new FileHashcodeMap();
        generateFileHashcodeMapRecursive(map, rootDir.getAbsolutePath(), 0, rootDir, ignoreRegex);
        return map;
    }

    /**
     * Given a directory, this will recursively traverse that directory's files/subdirectories and
     * generate the hashcode map for all files it encounters and add that data to the given map.
     * If given a regular file, a single entry is added to the given map.
     * 
     * @param fileOrDir   existing directory to scan and generate hashcodes for all its files, or existing
     *                    file to generate hashcode for
     * @param rootPath    the top root directory that is being scanned
     * @param level       the level deep in the file hierarchy currently being processed (0==at top root dir)
     * @param map         the map where the hashcode data is stored
     * @param ignoreRegex a regular expression that indicates which files/directories should be ignored.
     *                    If a relative file/directory path matches this regex, it will be skipped.
     *
     * @throws Exception if failed to generate hashcode for the file/directory
     */
    private static void generateFileHashcodeMapRecursive(FileHashcodeMap map, String rootPath, int level,
        File fileOrDir, Pattern ignoreRegex) throws Exception {

        if (fileOrDir == null || !fileOrDir.exists()) {
            throw new Exception("Non-existent file/directory provided: " + fileOrDir);
        }

        // get path relative to the top root node
        String path;
        if (level == 0) {
            path = fileOrDir.getName();
        } else {
            path = fileOrDir.getAbsolutePath().substring(rootPath.length() + 1);
        }

        // if this path is one the caller wants us to ignore, then return immediately
        if (ignoreRegex != null && ignoreRegex.matcher(path).matches()) {
            return;
        }

        if (fileOrDir.isDirectory()) {
            // we never calculate hashcodes for our own install metadata
            if (fileOrDir.getName().equals(DeploymentsMetadata.METADATA_DIR)) {
                return;
            }

            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    generateFileHashcodeMapRecursive(map, rootPath, level + 1, child, ignoreRegex);
                }
            } else {
                map.put(path, UNKNOWN_DIR_HASHCODE);
            }
        } else {
            String hashcode;
            try {
                hashcode = MessageDigestGenerator.getDigestString(fileOrDir);
            } catch (Exception e) {
                hashcode = UNKNOWN_FILE_HASHCODE;
            }
            map.put(path, hashcode);
        }

        return;
    }

    /**
     * Takes all map entries in this object and writes them to the given file such that it can later
     * be loaded in via {@link #loadFromFile(File)}.
     * 
     * @param file the file to store the entries to
     * @throws Exception if failed to store the entries to the given file
     */
    public void storeToFile(File file) throws Exception {
        PrintWriter writer = new PrintWriter(file);
        try {
            for (Map.Entry<String, String> entry : entrySet()) {
                writer.println(entry.getKey() + COLUMN_SEPARATOR + entry.getValue());
            }
        } finally {
            writer.close();
        }
        return;
    }

    /**
     * If a file was not readable or its hashcode could not be generated for some reason, its path
     * will be returned as a key to the returned map. The value will be {@link #UNKNOWN_FILE_HASHCODE}.
     * If a directory was not readable or its list of files could not be retrieved for some reason, its path
     * will be returned as a key to the returned map. The value will be {@link #UNKNOWN_DIR_HASHCODE}.
     *  
     * @return map of file or directories whose hashcodes could not be determined. Will be <code>null</code> if
     *         the map is fully complete and all content was able to have its hashcodes generated.
     */
    public Map<String, String> getUnknownContent() {
        Map<String, String> unknowns = null;

        for (Map.Entry<String, String> entry : entrySet()) {
            if (entry.getValue().equals(UNKNOWN_DIR_HASHCODE) || entry.getValue().equals(UNKNOWN_FILE_HASHCODE)) {
                if (unknowns == null) {
                    unknowns = new HashMap<String, String>();
                }
                unknowns.put(entry.getKey(), entry.getValue());
            }
        }

        return unknowns;
    }
}
