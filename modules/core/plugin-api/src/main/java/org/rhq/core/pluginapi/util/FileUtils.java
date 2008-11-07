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
package org.rhq.core.pluginapi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * A set of utilities for working with files and file paths.
 *
 * @author Ian Springer
 */
public abstract class FileUtils {

    /**
     * The purpose of this method is to purge a directory of all of its contents, but it can also
     * be used to simply delete a given file.  If <code>dir</code> is a directory, this method will
     * attempt to delete all of its files and all of its subdirectories and their child files/subdirectories.
     * <code>dir</code> itself will then be deleted, but only if <code>deleteIt</code> is <code>true</code>.
     * If <code>dir</code> is not a directory, but rather a simple file, it will be deleted only if
     * <code>deleteIt</code> is <code>true</code>.
     * 
     * Note - This method does not protect against symbolic links and will follow them on UNIX/Linux.
     * 
     * <p>If <code>dir</code> is <code>null</code>, this method does nothing.</p>
     * 
     * @param fileOrDir the file or directory to purge
     * @param deleteIt if <code>true</code>, <code>dir</code> will be deleted after all of its contents are
     *                 deleted
     * @throws IOException if the purge fails
     */
    public static void purge(File fileOrDir, boolean deleteIt) throws IOException {
        if (fileOrDir != null) {
            if (fileOrDir.isDirectory()) {
                File[] doomedFiles = fileOrDir.listFiles();
                if (doomedFiles != null) {
                    for (File doomedFile : doomedFiles) {
                        purge(doomedFile, true); // recurse
                    }
                }
            }
            if (deleteIt) {
                if (!fileOrDir.delete())
                    throw new IOException("Failed to delete file or directory: " + fileOrDir);
            } else {
                if (fileOrDir.isDirectory() && fileOrDir.list().length != 0)
                    throw new IOException("Failed to delete contents of directory: " + fileOrDir);
            }
        }
    }

    // I *think* this returns the entire line where stringToFind is found
    public static String findString(String filePath, String stringToFind) throws IOException {
        StringBuilder result = null;

        BufferedReader in = new BufferedReader(new FileReader(filePath));

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
                     * character matches the current char in the target string
                     * then we just add the current character to our result
                     * string and move on.
                     */
                    if (toFindIndex >= stringToFind.length() || data[i] == stringToFind.charAt(toFindIndex)) {
                        if (result == null) {
                            result = new StringBuilder();
                        }
                        if (Character.isISOControl(data[i])) {
                            return result.toString();
                        }
                        result.append(data[i]);
                        toFindIndex++;
                    } else {
                        /* Otherwise things can get complex.  If we haven't
                         * started to match, then just keep going.  If we have
                         * started to match, then we need to move backwards
                         * to make sure we don't miss a match.  For example:
                         * looking for HI in HHI.  If the current character
                         * isn't the same as the last character, then we aren't
                         * going to match, so null everything out and keep
                         * going.  Otherwise, decrment everything by one,
                         * because we didn't match the first character, and
                         * go through the loop on this character again.
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
                } catch (Exception e) {
                }
            }
        }

        if (result != null) {
            return result.toString();
        }

        return null;
    }

    /**
     * Canonicalize the specified file path according to the current platform's rules:
     * <ul>
     *   <li>Condense multiple consecutive path separators into a single path separator.</li>
     *   <li>Remove "." path elements.</li>
     *   <li>Resolve ".." path elements.</li>
     *   <li>On Windows, normalize capitalization.</li>
     *   <li>On Windows, expand 8.3-abbreviated path elements (e.g. "DOCUME~1" -> "Documents and Settings").</li>
     * </ul>
     * <p/>
     * Unlike {@link File#getCanonicalPath()}, this method does <b>not</b> resolve symlinks.</li>
     * <p/>
     * The path may or may not reference an existing file.
     *
     * @param path the file path to be canonicalized
     *
     * @return the canonicalized file path
     */
    public static String getCanonicalPath(String path) {
        File file = new File(path);
        if (isUnix()) {
            // UNIX - Do *not* use File#getCanonicalFile, since it will resolve symlinks.
            file = new File(file.toURI().normalize()).getAbsoluteFile();
        } else {
            // Windows - Use File#getCanonicalFile(), since it will normalize
            // capitalization and will not resolve junctions (i.e. the NTFS
            // equivalent of symlinks).
            try {
                file = file.getCanonicalFile();
            } catch (IOException e) {
                // best we can do...
                file = new File(file.toURI().normalize()).getAbsoluteFile();
            }
        }
        return file.getPath();
    }

    private FileUtils() {
    }

    private static boolean isUnix() {
        return File.separatorChar == '/';
    }

}
