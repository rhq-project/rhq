/*
 * JBoss, a division of Red Hat.
 * Copyright 2006-2007, Red Hat Middleware, LLC. All rights reserved.
 */
package org.rhq.core.pluginapi.util;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

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
     * <p>If <code>dir</code> is <code>null</code>, this method does nothing.</p>
     * 
     * @param dir the directory to purge (may also be just a simple file)
     * @param deleteIt if <code>true</code>, <code>file</code> will be deleted after all of its contents are
     *                 deleted
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
