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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.rhq.core.util.MessageDigestGenerator;

/**
 * This provides a hashmap that maps a filename to its hashcode value (md5).
 * The key to the map will be filenames; the values are unique hashcodes generated from
 * the content of the files.
 * Note that the keys can be either relative or absolute paths. If relative, some outside
 * entity will need to know how to resolve those relative paths (i.e. some outside
 * entity would need to know the top level root directory for all relative paths).
 * 
 * @author John Mazzitelli
 */
public class FileHashcodeMap extends TreeMap<String, String> {
    public static final String UNKNOWN_DIR_HASHCODE = "?UNKNOWN_DIR_HASHCODE?";
    public static final String UNKNOWN_FILE_HASHCODE = "?UNKNOWN_FILE_HASHCODE?";
    public static final String DELETED_FILE_HASHCODE = "?DELETED_FILE_HASHCODE?";
    public static final String DIRECTORY_HASHCODE ="?DIRECTORY?";

    private static final long serialVersionUID = 1L;
    private static final String COLUMN_SEPARATOR = "\t";

    /**
     * Given a directory, this will recursively traverse that directory's files/subdirectories and
     * generate the hashcode map for all files it encounters and add that data to the given map.
     * If given a regular file, a single entry is added to the given map.
     * Note that the returned map will have all relative paths as keys (relative to <code>rootDir</code>).
     * Also note that <code>ignoreRegex</code> is matched against relative paths.
     * 
     * @param rootDir existing directory to scan and generate hashcodes for all its files
     * @param ignoreRegex a regular expression that indicates which files/directories should be ignored.
     *                    If a relative file/directory path matches this regex, it will be skipped.
     * @param ignored a set that will contain those files/directories that were ignored while scanning the root dir 
     * @returns the map containing all files found and their generated hashcodes
     * @throws Exception if failed to generate hashcode for the directory
     */
    public static FileHashcodeMap generateFileHashcodeMap(File rootDir, Pattern ignoreRegex, Set<String> ignored)
        throws Exception {

        if (ignored == null) {
            ignored = new HashSet<String>();
        } else {
            ignored.clear(); // start fresh, in case caller left some old data around
        }

        FileHashcodeMap map = new FileHashcodeMap();
        generateFileHashcodeMapRecursive(map, rootDir.getAbsolutePath(), 0, rootDir, ignoreRegex, ignored);
        return map;
    }

    /**
     * Given a directory, this will recursively traverse that directory's files/subdirectories and
     * generate the hashcode map for all files it encounters and add that data to the given map.
     * If given a regular file, a single entry is added to the given map.
     * 
     * @param map         the map where the hashcode data is stored
     * @param rootPath    the top root directory that is being scanned - all files in the returned map will be relative to this
     * @param level       the level deep in the file hierarchy currently being processed (0==at top root dir)
     * @param fileOrDir   existing directory to scan and generate hashcodes for all its files, or existing
     *                    file to generate hashcode for
     * @param ignoreRegex a regular expression that indicates which files/directories should be ignored.
     *                    If a relative file/directory path matches this regex, it will be skipped.
     * @param ignored a set that will contain those files/directories that were ignored while scanning the root dir 
     *
     * @throws Exception if failed to generate hashcode for the file/directory
     */
    private static void generateFileHashcodeMapRecursive(FileHashcodeMap map, String rootPath, int level,
        File fileOrDir, Pattern ignoreRegex, Set<String> ignored) throws Exception {

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
            ignored.add(path);
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
                    generateFileHashcodeMapRecursive(map, rootPath, level + 1, child, ignoreRegex, ignored);
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
     * Loads in the file that contains file/hashcode map data.
     *
     * @param file the file to load
     * @return map of files/hashcodes found in the file
     * @throws Exception
     */
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
     * This rescans a set of files (found in this map) and returns a map with an updated, current set of hashcodes.
     *
     * If this original map has relative paths, they will be considered relative to the given
     * root directory. If a file is not found, it will still be in the returned map but its
     * hashcode will be {@link #DELETED_FILE_HASHCODE}.
     * 
     * The root directory is also scanned for new files that are not in this original
     * map - if new files are found (and they do not match the ignoreRegex), they are added to the
     * returned map. Note that if <code>reportNewRootFilesAsNew</code> is false, and if new files
     * are found in the top root directory and they are not related to the deployment fileset,
     * they will not be added to the returned map.
     *
     * @param rootDir directory where the relative paths are expected to be
     * @param ignoreRegex if relative paths of files under rootDir match this, they will be ignored.
     *                    This will eliminate files/directories from being considered "new" because
     *                    they aren't in original.
     * @param reportNewRootFilesAsNew do not report as new any unrelated files found in the root dir
     * @return a map with current files/hashcodes, including files that were not found in original. 
     *         the returned object also has additional info such as those files that were added,
     *         deleted, changed from this original. It also indicates what was ignored during the rescan. 
     * @throws Exception
     */
    public ChangesFileHashcodeMap rescan(File rootDir, Pattern ignoreRegex, boolean reportNewRootFilesAsNew)
        throws Exception {
        ChangesFileHashcodeMap current = new ChangesFileHashcodeMap(this);

        // go through our original files and recalculate their hashcodes
        for (Map.Entry<String, String> entry : entrySet()) {
            String originalFileString = entry.getKey();

            // if we are now to ignore this file, don't put it in our current map and skip to the next file
            if (ignoreRegex != null && ignoreRegex.matcher(originalFileString).matches()) {
                current.remove(originalFileString);
                current.getIgnored().add(originalFileString);
                continue;
            }

            File originalFile = new File(originalFileString);
            if (!originalFile.isAbsolute()) {
                originalFile = new File(rootDir, originalFileString);
            }

            if (originalFile.exists()) {
                String currentHashcode = FileHashcodeMap.DIRECTORY_HASHCODE;
                if (!originalFile.isDirectory()) {
                    currentHashcode = MessageDigestGenerator.getDigestString(originalFile);
                }

                current.put(originalFileString, currentHashcode);

                // if file has been changed, mark it as such in our return map
                String originalHashcode = entry.getValue();
                if (!currentHashcode.equals(originalHashcode)) {
                    current.getChanges().put(originalFileString, currentHashcode);
                }
            } else {
                // file has been deleted! still put an entry in our returned map but mark it as deleted
                current.put(originalFileString, DELETED_FILE_HASHCODE);
                current.getDeletions().put(originalFileString, DELETED_FILE_HASHCODE);
            }
        }

        // now recursively traverse the root directory and look for new files that aren't in our original map
        // files that have been added need to be put into our returned map and also marked as added
        FileHashcodeMap newFiles = new FileHashcodeMap();
        Set<String> skippedFiles = new HashSet<String>();
        lookForNewFilesRecursive(newFiles, skippedFiles, rootDir.getAbsolutePath(), 0, rootDir, ignoreRegex, current
            .getIgnored(), reportNewRootFilesAsNew);
        current.putAll(newFiles);
        current.getAdditions().putAll(newFiles);
        current.getSkipped().addAll(skippedFiles);

        return current;
    }

    /**
     * This looks for new files under the given fileOrDir and adds them to <code>newFiles</code>.
     * 
     * @param newFiles    the map where the new, current file/hashcode data will be stored
     * @param skippedFiles a set where names of unrelated files/directories are stored. The names
     *                     found here after this method returns are those files/dirs that were found
     *                     in the top level root dir, but were skipped over and not processed. This will not
     *                     contain paths with subdirectories - they will only be a filename with no paths
     *                     because they are located directly under the root dir.
     * @param rootPath the top root directory that is being scanned
     * @param level the level deep in the file hierarchy currently being processed (0==at top root dir)
     * @param fileOrDir existing directory/file to rescan
     * @param ignoreRegex a regular expression that indicates which files/directories should be ignored.
     *                    If a relative file/directory path matches this regex, it will be skipped.
     * @param ignored a set that will contain those files/directories that were ignored while scanning the root dir
     * @param reportNewRootFilesAsNew if false, ignore unrelated files at the root dir location (level=0)
     *
     * @throws Exception 
     */
    private void lookForNewFilesRecursive(FileHashcodeMap newFiles, Set<String> skippedFiles, String rootPath,
        int level, File fileOrDir, Pattern ignoreRegex, Set<String> ignored, boolean reportNewRootFilesAsNew)
        throws Exception {

        if (fileOrDir == null || !fileOrDir.exists()) {
            throw new Exception("Non-existent file/directory provided: " + fileOrDir);
        }

        // get path relative to the top root node
        String path;
        if (level == 0) {
            path = fileOrDir.getName();
        } else {
            int skipLength = rootPath.length() + 1;
            if(rootPath.endsWith(File.separator)) {
                // If the last char is File.separator (such as C:\ deploy path), then we need to
                // reduce the length by one, otherwise the deployment will remove parts of the correct path
                skipLength--;
            }
            path = fileOrDir.getAbsolutePath().substring(skipLength);
        }

        path = convertPath(path);

        // if this path is one the caller wants us to ignore, then return immediately
        if (ignoreRegex != null) {
            String matchPath = path;
            // directory wouldn't match pattern unless i has trailing slash
            if (fileOrDir.isDirectory()) {
                matchPath = convertPath(path+File.separator);
            }
            if (ignoreRegex.matcher(matchPath).matches()) {
                ignored.add(path);
                return;
            }
        }

        if (fileOrDir.isDirectory()) {
            // we never calculate hashcodes for our own install metadata
            if (fileOrDir.getName().equals(DeploymentsMetadata.METADATA_DIR)) {
                return;
            }

            // If we are currently at the top level and we are not to report unrelated files as new
            // then we need to find out which files ARE related first. Related files are those files
            // already in our Map at the root level (i.e. they are just a filename, no parent paths)
            // plus those in our Map with a top parent dir name that match a child directory of
            // fileOrDir (which is the root dir if we are at level=0).
            // Note that we use File API to manipulate filenames/parents to ensure we do this right
            // on Windows (taking care of relative paths with drive letters, e.g. C:subdir/file.txt).
            HashSet<String> relatedTopLevelFiles = null;
            if (level == 0 && !reportNewRootFilesAsNew) {
                relatedTopLevelFiles = new HashSet<String>();
                for (String relatedFilePath : keySet()) { // loop through our Map key entries, these are the "related" files
                    File relatedFile = new File(relatedFilePath);
                    if (!relatedFile.isAbsolute()) {
                        String topLevelName = relatedFile.getName(); // prime the pump; if we have no parent, this is our file already at the top root dir
                        File parent = relatedFile.getParentFile();
                        // walk up the file hierarchy until we hit the top parent - this is the related dir found in our top root
                        while (parent != null) {
                            topLevelName = parent.getName();
                            parent = parent.getParentFile();
                        }
                        relatedTopLevelFiles.add(topLevelName); // this is a file or dir at the top root dir                             
                    }
                }
            }
            // we also care about directories and we're reporting them as newFiles
            if (level > 0 && reportNewRootFilesAsNew) {
             // if the file is not yet known to us, add it to the map of new files
                if (!containsKey(path)) {
                    // just insert directory with non-important hash value
                    newFiles.put(path, DIRECTORY_HASHCODE);
                }
            }

            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children) {
                    // skip this child if we are at the top root dir and it is not related to our fileset
                    if (relatedTopLevelFiles == null || relatedTopLevelFiles.contains(child.getName())) {
                        lookForNewFilesRecursive(newFiles, skippedFiles, rootPath, level + 1, child, ignoreRegex,
                            ignored, reportNewRootFilesAsNew);
                    } else {
                        skippedFiles.add(child.getName());
                    }
                }
            }
        } else {
            // if the file is not yet known to us, add it to the map of new files
            if (!containsKey(path)) {
                String hashcode;
                try {
                    hashcode = MessageDigestGenerator.getDigestString(fileOrDir);
                } catch (Exception e) {
                    hashcode = UNKNOWN_FILE_HASHCODE;
                }
                newFiles.put(path, hashcode);
            }
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

    @Override
    public String put(String key, String value) {
        return super.put(convertPath(key), value);
    }

    @Override
    public String get(Object key) {
        return super.get(convertPath((String) key));
    }

    @Override
    public String remove(Object key) {
        return super.remove(convertPath((String) key));
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(convertPath((String) key));
    }

    /**
     * Converts the path to the form that will be stored internally.
     * 
     * @param path a filepath to be converted
     * 
     * @return the converted path that is to be used to store in the internal sets.
     */
    public String convertPath(String path) {
        if (File.separatorChar != '/') {
            if (path != null) {
                path = path.replace(File.separatorChar, '/');
            }
        }

        return path;
    }
}
