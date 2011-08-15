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
package org.rhq.core.util.file;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rhq.core.util.collection.IntHashMap;
import org.rhq.core.util.stream.StreamUtil;

public class FileUtil {
    private static IntHashMap invalidChars = null;

    /**
     * Creates a temporary directory using the same algorithm as JDK's File.createTempFile.
     */
    public static File createTempDirectory(String prefix, String suffix, File parentDirectory) throws IOException {
        // Let's reuse the algorithm the JDK uses to determine a unique name:
        // 1) create a temp file to get a unique name using JDK createTempFile
        // 2) then quickly delete the file and...
        // 3) convert it to a directory

        File tmpDir = File.createTempFile(prefix, suffix, parentDirectory); // create file with unique name
        boolean deleteOk = tmpDir.delete(); // delete the tmp file and...
        boolean mkdirsOk = tmpDir.mkdirs(); // ...convert it to a directory

        if (!deleteOk || !mkdirsOk) {
            throw new IOException("Failed to create temp directory named [" + tmpDir + "]");
        }

        return tmpDir;
    }

    /**
     * Given a directory, this will recursively purge all child directories and files.
     * If dir is actually a normal file, it will be deleted but only if deleteIt is true.
     * 
     * If deleteIt is true, the directory itself will be deleted, otherwise it will remain (albeit empty).
     * 
     * @param dir the directory to purge (if <code>null</code>, this method does nothing and returns normally)
     * @param deleteIt if <code>true</code> delete the directory itself, otherwise leave it but purge its children
     */
    public static void purge(File dir, boolean deleteIt) {
        if (dir != null) {
            if (dir.isDirectory()) {
                File[] doomedFiles = dir.listFiles();
                if (doomedFiles != null) {
                    for (File doomedFile : doomedFiles) {
                        purge(doomedFile, true); // call this method recursively
                    }
                }
            }

            if (deleteIt) {
                dir.delete();
            }
        }

        return;
    }

    /**
     * Copy a file from one file to another
     */
    public static void copyFile(File inFile, File outFile) throws FileNotFoundException, IOException {
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(inFile));
        BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));
        StreamUtil.copy(is, os);
    }

    /**
     * Copy a stream, using a buffer.
     * @deprecated use {@link StreamUtil} for more methods like this - those are unit tested and used more
     */
    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        StreamUtil.copy(is, os, false);
    }

    /**
     * Copy a stream, using a buffer.
     * @deprecated use {@link StreamUtil} for more methods like this - those are unit tested and used more
     */
    public static void copyStream(InputStream is, OutputStream os, byte[] buf) throws IOException {
        int bytesRead = 0;
        while (true) {
            bytesRead = is.read(buf);
            if (bytesRead == -1) {
                break;
            }

            os.write(buf, 0, bytesRead);
        }
    }

    /**
     * Writes the content in the input stream to the specified file.
     * NOTE: inputStream will be closed by this.
     *
     * @param  inputStream stream containing the content to write
     * @param  outputFile file to which the content will be written
     *
     * @throws IOException if any errors occur during the reading or writing
     */
    public static void writeFile(InputStream inputStream, File outputFile) throws IOException {
        FileOutputStream fos = new FileOutputStream(outputFile);
        copyStream(inputStream, fos);
        inputStream.close();
        fos.close();
    }

    public static String findString(String fname, String toFind) throws IOException {
        StringBuffer result = null;

        BufferedReader in = new BufferedReader(new FileReader(fname));

        try {
            char[] data = new char[8096];

            int numread;
            int toFindIndex = 0;

            /* Just need to initialize this, because the compiler doesn't
             * realize that it can't be used before it is assigned a value
             */
            char lastchar = 'a';
            while ((numread = in.read(data, 0, 8096)) != -1) {
                for (int i = 0; i < numread; i++) {
                    /* If we have found the string already or if we our current
                     * character matches the current char in the target string then we just add the current character to
                     * our result string and move on.
                     */
                    if ((toFindIndex >= toFind.length()) || (data[i] == toFind.charAt(toFindIndex))) {
                        if (result == null) {
                            result = new StringBuffer();
                        }

                        if (Character.isISOControl(data[i])) {
                            return result.toString();
                        }

                        result.append(data[i]);
                        toFindIndex++;
                    } else {
                        /* Otherwise things can get complex.  If we haven't
                         * started to match, then just keep going.  If we have started to match, then we need to move
                         * backwards to make sure we don't miss a match.  For example: looking for HI in HHI.  If the
                         * current character isn't the same as the last character, then we aren't going to match, so
                         * null everything out and keep going.  Otherwise, decrment everything by one, because we didn't
                         * match the first character, and go through the loop on this character again.
                         */
                        if (toFindIndex > 0) {
                            if (data[i] != lastchar) {
                                result = null;
                                toFindIndex = 0;
                                continue;
                            }

                            toFindIndex--;
                            i--;
                            result.deleteCharAt(result.length() - 1);
                            continue;
                        }
                    }

                    lastchar = data[i];
                }
            }
        } catch (IOException e) {
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }

        if (result != null) {
            return result.toString();
        }

        return null;
    }

    /**
     * The base attribute specifies what the directory base the relative path should be considered relative to. The base
     * must be part of the absolute path specified by the path attribute.
     */
    public static String getRelativePath(File path, File base) {
        String path_abs = path.getAbsolutePath();
        String base_abs = base.getAbsolutePath();
        int idx = path_abs.indexOf(base_abs);
        if (idx == -1) {
            throw new IllegalArgumentException("Path (" + path_abs + ") " + "does not contain " + "base (" + base_abs
                + ")");
        }

        String relativePath = "." + path_abs.substring(idx + base_abs.length());
        return relativePath;
    }

    private static void initInvalidChars() {
        if (invalidChars != null) {
            return;
        }

        invalidChars = new IntHashMap();

        char[] invalid = { '\\', '/', ':', '*', '?', '\'', '"', '~', '<', '>', '|', '#', '{', '}', '%', '&', ' ' };

        for (int i = 0; i < invalid.length; i++) {
            invalidChars.put(invalid[i], Boolean.TRUE);
        }
    }

    /**
     * Escape invalid characters in a filename, replacing with "_"
     */
    public static String escape(String name) {
        initInvalidChars();

        int len = name.length();
        StringBuffer buf = new StringBuffer(len);
        char[] chars = name.toCharArray();

        for (int i = 0; i < len; i++) {
            char c = chars[i];
            if (invalidChars.get(c) == Boolean.TRUE) {
                buf.append("_");
            } else {
                buf.append(c);
            }
        }

        return buf.toString();
    }

    /**
     * Test if a directory is writable java.io.File#canWrite() has problems on windows for properly detecting if a
     * directory is writable by the current user. For example, C:\Program Files is set to read-only, however the
     * Administrator user is able to write to that directory
     *
     * @throws IOException If the File is not a directory
     */
    public static boolean canWrite(File dir) throws IOException {
        if (!dir.isDirectory()) {
            throw new IOException(dir.getPath() + " is not a directory");
        }

        File tmp = null;
        try {
            tmp = File.createTempFile("rhq", null, dir);
            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (tmp != null) {
                tmp.delete();
            }
        }
    }

    /**
     * Strips the drive letter from the given Windows path. The drive letter is returned
     * or <code>null</code> is returned if there was no drive letter in the path.
     *  
     * @param path the path string that will be altered to have its drive letter stripped.
     * @return if there was a drive letter, it will be returned. If no drive letter was in path, null is returned
     */
    public static String stripDriveLetter(StringBuilder path) {
        String driveLetter = null;
        Pattern regex = Pattern.compile("^([a-zA-Z]):.*");
        Matcher matcher = regex.matcher(path);
        if (matcher.matches()) {
            driveLetter = matcher.group(1);
            path.replace(0, 2, ""); // we know the pattern is one char drive letter plus one ':' char followed by the path
        }
        return driveLetter;
    }

    /**
     * Ensure that the path uses only forward slash
     * Like java.io.File(String,String) but just 
     * @param path
     * @return
     */
    public static String useForwardSlash(String path) {
        return path.replace('\\', '/');
    }

    /**
     * Performs a breadth-first scan, calling <code>visitor</code> for each file in
     * <code>directory</code>. Sub directories are scanned as well. Note that if
     * <code>visitor</code> throws a RuntimeException it will not be called again as this
     * method does not provide any exception handling.
     *
     * @param directory The directory over which to iterate
     * @param visitor The callback to invoke for each file
     */
    public static void forEachFile(File directory, FileVisitor visitor) {
        Deque<File> directories = new LinkedList<File>();
        directories.push(directory);

        while(!directories.isEmpty()) {
            File dir = directories.pop();
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    directories.push(file);
                } else {
                    visitor.visit(file);
                }
            }
        }
    }

    /**
     * Takes a list of filters and compiles them into a regular expression that can be used
     * for matching or filtering paths. The pattern syntax supports regular expressions as
     * well as the syntax used in Ant's file/path selectors. Here are some example patterns
     * that can be specified in a filter:
     * <p/>
     * <table border="1">
     *     <tr>
     *         <td>Pattern</td>
     *         <td>Description</td>
     *     </tr>
     *     <tr>
     *         <td>/etc/yum.conf</td>
     *         <td>exact match of the path</td>
     *     </tr>
     *     <tr>
     *         <td>/etc/*.conf</td>
     *         <td>match any file /etc that has a .conf suffix</td>
     *     </tr>
     *     <tr>
     *         <td>deploy/myapp-?.war</td>
     *         <td>Match any file in the deploy directory that starts with myapp- followed any one character and
     *         ending with a suffix of .war</td>
     *     </tr>
     *     <tr>
     *         <td>jboss/server/**&#047;*.war</td>
     *         <td>Matches all .war files under the server directory. Sub directories are included as well such
     *         that jboss/server/default/myapp.war jboss/server/production/myapp.war and
     *         jboss/server/default/myapp.ear/myapp.war all match</td>
     *     </tr>
     * </table>
     *
     * @param filters Compiled into a regular expression
     * @return A Pattern object that is a compilation of regular expressions built from
     * the specified path filters
     */
    public static Pattern generateRegex(List<PathFilter> filters) {
        boolean first = true;
        StringBuilder regex = new StringBuilder();
        for (PathFilter filter : filters) {
            if (!first) {
                regex.append("|");
            } else {
                first = false;
            }
            regex.append("(");
            File path = new File(filter.getPath());

            if (isEmpty(filter.getPattern()) && path.isDirectory()) {
                regex.append(".*");
            } else if (isEmpty(filter.getPattern()) && !path.isDirectory()) {
                buildPatternRegex(path.getAbsolutePath(), regex);
            } else if (!isEmpty(filter.getPattern())) {
                // note that this case assumes path is a directory. We probably
                // need another if else block for when there is a pattern and
                // path is not a directory.
                regex.append(path).append("/").append("(");
                buildPatternRegex(filter.getPattern(), regex);
                regex.append(")");
            }
            regex.append(")");
        }
        return Pattern.compile(regex.toString());
    }

    private static void buildPatternRegex(String pattern, StringBuilder regex) {
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '?') {
                regex.append('.');
            } else if (c == '*') {
                if (i + 1 < pattern.length()) {
                    char c2 = pattern.charAt(i + 1);
                    if (c2 == '*') {
                        regex.append(".*");
                        i += 2;
                        continue;
                    }
                }
                regex.append("[^/]*");
            } else if (c == '.') {
                regex.append("\\.");
            } else {
                regex.append(c);
            }
            // TODO: Escape backslashes.
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

}