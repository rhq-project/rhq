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
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.rhq.core.util.collection.IntHashMap;
import org.rhq.core.util.stream.StreamUtil;

public class FileUtil {
    private static IntHashMap invalidChars = null;

    /**
     * This will check to see if file1 is newer than file2. If file1's last modified date
     * is <strong>after</strong> file2's last modified date, then <code>true</code> is returned.
     * <code>false</code> is returned if file1's date is the same or older than file2's date.
     * <p>
     * <code>null</code> is returned if any of these conditions are true:
     * <ul>
     * <li>If either file is null</li>
     * <li>If either file does not exist</li>
     * <li>If either file is not a normal file (but, say, a directory)</li>
     * </ul>
     * </p>
     *
     * @param file1
     * @param file2
     * @return indication if file1 is newer than file2
     */
    public static Boolean isNewer(File file1, File file2) {
        if (file1 == null || file2 == null) {
            return null;
        }
        if (!file1.isFile() || !file2.isFile()) {
            return null;
        }
        long file1Date = file1.lastModified();
        long file2Date = file2.lastModified();
        return file1Date > file2Date;
    }

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

        // TODO do we care to restore execute bit? For coping large number of files via copyDirectory, will this make things slow?
        // if (inFile.canExecute()) {
        //    outFile.setExecutable(true);
        // }
        // TODO do we care to restore the last mod time on the destination file?
        //outFile.setLastModified(inFile.lastModified());
    }

    public static void copyDirectory(File inDir, File outDir) throws IOException {
        if (inDir.exists()) {
            if (!inDir.isDirectory()) {
                throw new IOException("Source directory [" + inDir + "] is not a directory");
            }
        } else {
            throw new FileNotFoundException("Source directory [" + inDir + "] does not exist");
        }

        if (!outDir.mkdirs()) {
            throw new IOException("Destination directory [" + outDir + "] failed to be created");
        }

        if (!canWrite(outDir)) {
            throw new IOException("Cannot write to destination directory [" + outDir + "]");
        }

        // TODO do we care to restore the last mod time on the destination dir?
        //outDir.setLastModified(inDir.lastModified());

        File[] files = inDir.listFiles();
        if (files == null) {
            throw new IOException("Failed to get the list of files in source directory [" + inDir + "]");
        }
        for (File file : files) {
            File copiedFile = new File(outDir, file.getName());
            if (file.isDirectory()) {
                copyDirectory(file, copiedFile);
            } else {
                copyFile(file, copiedFile);
            }
        }

        files = null; // help GC
        return;
    }

    /**
     * Obtains the list of all files in the given directory and, recursively, all its subdirectories.
     * Note that the returns list is only regular files - directory names are NOT in the list. Also,
     * the names in the list are relative to the given directory.
     * @param directory the directory whose files are to be returned
     * @return list of files in the directory, not sorted in any particular order
     * @throws IOException if directory does not exist or is not a directory
     */
    public static List<File> getDirectoryFiles(File directory) throws IOException {
        ArrayList<File> files = new ArrayList<File>();
        if (!directory.isDirectory()) {
            throw new IOException("[" + directory + "] is not an existing directory");
        }
        getDirectoryFilesRecursive(directory, files, null);
        return files;
    }

    private static void getDirectoryFilesRecursive(File directory, List<File> files, String relativeTo)
        throws IOException {
        File[] children = directory.listFiles();
        if (children == null) {
            throw new IOException("Cannot obtain files from directory [" + directory + "]");
        }
        for (File child : children) {
            if (child.isDirectory()) {
                getDirectoryFilesRecursive(child, files, ((relativeTo == null) ? "" : relativeTo) + child.getName()
                    + File.separatorChar);
            } else {
                files.add(new File(relativeTo, child.getName()));
            }
        }
        return;
    }

    /**
     * Copy a stream, using a buffer.
     * @deprecated use {@link StreamUtil} for more methods like this - those are unit tested and used more
     */
    @Deprecated
    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        StreamUtil.copy(is, os, false);
    }

    /**
     * Copy a stream, using a buffer.
     * @deprecated use {@link StreamUtil} for more methods like this - those are unit tested and used more
     */
    @Deprecated
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
     * @return if there was a drive letter, it will be returned and normalized to upcase. If no drive letter
     *         was in path, null is returned
     */
    public static String stripDriveLetter(StringBuilder path) {
        String driveLetter = null;
        Pattern regex = Pattern.compile("^([a-zA-Z]):.*");
        Matcher matcher = regex.matcher(path);
        if (matcher.matches()) {
            driveLetter = matcher.group(1).toUpperCase();
            path.replace(0, 2, ""); // we know the pattern is one char drive letter plus one ':' char followed by the path
        }
        return driveLetter;
    }

    /**
     * Ensure that the path uses only forward slash.
     * @param path
     * @return forward-slashed path, or null if path is null
     */
    public static String useForwardSlash(String path) {

        return (null != path) ? path.replace('\\', '/') : null;
    }

    /**
     * Return just the filename portion (the portion right of the last path separator string)
     * @param path
     * @param separator
     * @return null if path is null, otherwise the trimmed filename
     */
    public static String getFileName(String path, String separator) {
        if (null == path) {
            return null;
        }

        int i = path.lastIndexOf(separator);

        return (i < 0) ? path.trim() : path.substring(++i).trim();
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

        while (!directories.isEmpty()) {
            File dir = directories.pop();
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        directories.push(file);
                    } else {
                        visitor.visit(file);
                    }
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
     *         that jboss/server/default/myapp.war, jboss/server/production/myapp.war and
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
            File pathFile = new File(filter.getPath());

            if (isEmpty(filter.getPattern()) && pathFile.isDirectory()) {
                regex.append(".*");
            } else if (isEmpty(filter.getPattern()) && !pathFile.isDirectory()) {
                buildPatternRegex(pathFile.getAbsolutePath(), regex);

            } else if (!isEmpty(filter.getPattern())) {
                // note that this case assumes path is a directory. We probably
                // need another if else block for when there is a pattern and
                // path is not a directory.

                // escape win separators because backslash is a regex character
                String pathString = pathFile.getAbsolutePath();
                if (!pathString.endsWith(File.separator)) {
                    pathString += File.separator;
                }
                pathString = pathString.replace("\\", "\\\\");

                // escape parens in the path, these are valid dir chars on win and also regex characters
                pathString = pathString.replace("(", "\\(");
                pathString = pathString.replace(")", "\\)");

                regex.append(pathString).append("(");
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
                // ? match any character
                regex.append('.');

            } else if (c == '*') {
                // ? match zero or more characters
                if (i + 1 < pattern.length()) {
                    char c2 = pattern.charAt(i + 1);
                    if (c2 == '*') {
                        regex.append(".*");
                        i += 2;
                        continue;
                    }
                }
                String separator = File.separator;
                if ("\\".equals(separator)) {
                    separator = "\\\\";
                }
                regex.append("[^" + separator + "]*");

            } else if (c == '.' || c == '(' || c == ')') {
                // escape file extensions because dot is a regex character
                // escape parens because they are regex characters
                regex.append("\\");
                regex.append(c);

            } else if (c == '\\') {
                // escape windows separators because backslash is a regex character
                regex.append("\\\\");

            } else {
                regex.append(c);
            }
        }
    }

    private static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    /**
     * Normalizes the path of the file by removing any ".." and "."
     * <p/>
     * This method behaves very similar to Java7's {@code Path.normalize()} method with the exception of dealing with
     * paths jumping "above" the FS root.
     * <p/>
     * Java7's normalization will normalize a path  like {@code C:\..\asdf} to {@code C:\asdf}, while this method will
     * return null, because it understands {@code C:\..\asdf} as an attempt to "go above" the file system root.
     *
     * @return the file with the normalized path or null if the ".."s would jump further up than the number of preceding
     * path elements (e.g. passing files with paths like ".." or "path/../.." will return null). On Windows the drive
     * letter will be upper-cased if present.
     */
    public static File normalizePath(File file) {
        String path = file.getPath();
        File root = null;

        // make sure driver letter on windows is upcased
        int rootLength = FileSystem.get().getPathRootLength(path);
        if (rootLength > 0) {
            StringBuilder rootPath = new StringBuilder(path.substring(0, rootLength));
            String driveLetter = stripDriveLetter(rootPath);
            root = new File((null == driveLetter) ? rootPath.toString() : (driveLetter + ":" + rootPath.toString()));
        }

        StringTokenizer tokenizer = new StringTokenizer(path.substring(rootLength), FileSystem.get()
            .getSeparatorChars(), true);
        LinkedList<String> pathStack = new LinkedList<String>();

        boolean previousWasDelimiter = false;

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();

            if (File.separator.equals(token)) {
                if (!previousWasDelimiter) {
                    pathStack.push(token);
                    previousWasDelimiter = true;
                }
            } else if ("..".equals(token)) {
                //yes, this is correct - ".." will jump up the stack to the next-previous delimiter, so we should
                //declare that we're at a delimiter position.
                previousWasDelimiter = true;
                if (pathStack.isEmpty()) {
                    return null;
                } else {
                    //pop the previous delimiter(s)
                    pathStack.pop();

                    //and pop the previous path element
                    if (pathStack.isEmpty()) {
                        return null;
                    }
                    pathStack.pop();
                }
            } else if (".".equals(token)) {
                previousWasDelimiter = true;
            } else if (token.length() > 0) {
                previousWasDelimiter = false;
                pathStack.push(token);
            } else {
                previousWasDelimiter = false;
            }
        }

        StringBuilder normalizedPath = new StringBuilder();

        for (int i = pathStack.size(); --i >= 0;) {
            normalizedPath.append(pathStack.get(i));
        }

        File ret = (root == null) ? new File(normalizedPath.toString()) : new File(root, normalizedPath.toString());

        if (file.isAbsolute() != ret.isAbsolute()) {
            // if the normalization changed the path such that it is not absolute anymore
            // (or that it wasn't absolute but now is, which shouldn't ever happen), return null.
            // The fact that the original file was absolute and the normalized path isn't can be caused by
            // the normalization "climbing past" the prefix of the absolute path which is the drive letter of Windows
            // for example.
            return null;
        } else {
            return ret;
        }
    }

    private enum FileSystem {
        UNIX {
            @Override
            public int getPathRootLength(String path) {
                if (path != null && path.charAt(0) == '/') {
                    return 1;
                } else {
                    return 0;
                }
            }

            @Override
            public String getSeparatorChars() {
                return "/";
            }
        },
        WINDOWS {
            @Override
            public int getPathRootLength(String path) {
                if (path == null || path.length() < 3) {
                    return 0;
                }

                // C:\asdf
                // C:asdf
                // \\host\share\asdf

                char c0 = path.charAt(0);
                char c1 = path.charAt(1);
                char c2 = path.charAt(2);

                switch (c0) {
                case '\\':
                case '/':
                    if (isSlash(c1)) {
                        //UNC
                        int nextSlash = nextSlash(path, 2);
                        if (nextSlash < 3) {
                            throw new IllegalArgumentException("Invalid UNC path - no host specified");
                        }

                        int hostSlash = nextSlash;
                        nextSlash = nextSlash(path, nextSlash + 1);

                        if (nextSlash <= hostSlash) {
                            throw new IllegalArgumentException("Invalid UNC path - no share specified");
                        }

                        return nextSlash;
                    } else {
                        return 0;
                    }
                default:
                    if (c1 == ':') {
                        char driveLetter = Character.toLowerCase(c0);
                        if ('a' <= driveLetter && 'z' >= driveLetter) {
                            return c2 == '\\' ? 3 : 2;
                        } else {
                            return 0;
                        }
                    } else {
                        return 0;
                    }
                }
            }

            @Override
            public String getSeparatorChars() {
                return "\\/";
            }
        };

        private static boolean isSlash(char c) {
            return c == '\\' || c == '/';
        }

        private static int nextSlash(String str, int from) {
            int len = str.length();
            for (int i = from; i < len; ++i) {
                if (isSlash(str.charAt(i))) {
                    return i;
                }
            }

            return -1;
        }

        public static FileSystem get() {
            switch (File.separatorChar) {
            case '/':
                return UNIX;
            case '\\':
                return WINDOWS;
            default:
                throw new IllegalStateException("Unsupported filesystem");
            }
        }

        public abstract int getPathRootLength(String path);

        public abstract String getSeparatorChars();
    }

    /**
     * Under certain conditions, it might be desired to consider a path that is technically a Windows relative path
     * to be absolute. For example, a relative path declared as "\opt" could be considered absolute if
     * the current working directory at runtime is C:\working\dir\here and that \opt directory is located on
     * the same drive (i.e. C:\opt).
     *
     * It is preferable that you do use real absolute paths including drive letter, but this method helps
     * work around some circumstances where that isn't possible, but yet by just implying a drive letter, you
     * do have an absolute path.
     *
     * Note that if the VM is not running on a Windows machine, this method is the same as File.isAbsolute().
     *
     * @param path the path to see if it really can be considered absolute
     *
     * @return true if the path can be considered absolute if the current working directory drive letter is implied.
     */
    public static boolean isAbsolutePath(String path) {
        File filepath = new File(path);

        if (File.separatorChar == '/') {
            return filepath.isAbsolute();
        }

        if (filepath.isAbsolute()) {
            return true; // nothing else to check, it already is technically an absolute path
        }

        String driveLetter = stripDriveLetter(new StringBuilder(path));
        if (driveLetter != null) {
            return false; // the path already had a drive letter in it, it really is a relative path that we can't consider absolute
        }

        char cwdDriveLetter = new File("\\").getAbsolutePath().charAt(0); // gets the current working directory's drive letter

        return new File(cwdDriveLetter + ":" + path).isAbsolute();
    }

    /**
     * Compressed the data found in the file. The file can only be decompressed with {@link #decompressFile(File)}.
     *
     * @param originalFile
     * @throws IOException
     */
    public static void compressFile(File originalFile) throws IOException {
        // make a copy of the original data, so we can use the original file for the compressed data
        File decompressedFile = new File(originalFile + ".d");
        try {
            copyFile(originalFile, decompressedFile);
            try {
                FileOutputStream out = new FileOutputStream(originalFile);
                out.write(new byte[] { (byte) 0 }); // write a prefix byte so people can't easily just gunzip this
                GZIPOutputStream zip = new GZIPOutputStream(out); // writes the compressed data into original file
                StreamUtil.copy(new FileInputStream(decompressedFile), zip);
            } catch (IOException e) {
                // try to restore the original file before throwing exception
                try {
                    copyFile(decompressedFile, originalFile);
                } catch (Throwable ignore) {
                }
                throw e;
            }
        } finally {
            //            System.out.println("compress: original: " + decompressedFile.length() + ", "
            //                + MessageDigestGenerator.getDigestString(decompressedFile));
            //            System.out.println("          compress: " + originalFile.length() + ", "
            //                + MessageDigestGenerator.getDigestString(originalFile));
            decompressedFile.delete();
        }
    }

    /**
     * Decompresses the compressed data found in the file that was compressed with {@link #compressFile(File)}.
     * If the file was not previously compressed, an exception is thrown.
     *
     * @param originalFile
     * @throws IOException
     */
    public static void decompressFile(File originalFile) throws IOException {
        // make a copy of the original data, so we can use the original file for the decompressed data
        File compressedFile = new File(originalFile + ".c");
        try {
            copyFile(originalFile, compressedFile);
            try {
                FileInputStream in = new FileInputStream(compressedFile);
                in.read(); // read our prefix byte
                GZIPInputStream zip = new GZIPInputStream(in);
                StreamUtil.copy(zip, new FileOutputStream(originalFile));
            } catch (IOException e) {
                // try to restore the original file before throwing exception
                try {
                    copyFile(compressedFile, originalFile);
                } catch (Throwable ignore) {
                }
                throw e;
            }
        } finally {
            //            System.out.println("decompre: original: " + compressedFile.length() + ", "
            //                + MessageDigestGenerator.getDigestString(compressedFile));
            //            System.out.println("          decompre: " + originalFile.length() + ", "
            //                + MessageDigestGenerator.getDigestString(originalFile));
            compressedFile.delete();
        }
    }

    /*
     * I was going to use this, but then decided to use the compress/decompress instead.
     * However, this might be useful in the future. We can uncomment this if we want to use something
     * like this. There is also a commented out test in FileUtilTest that should be uncommented if
     * we reintroduce these two methods.
     *
    public static void obfuscateFile(File originalFile) throws IOException {
        // make a copy of the original data, so we can use the original file for the compressed data
        File deobfuscatedFile = new File(originalFile + ".d");
        try {
            copyFile(originalFile, deobfuscatedFile);
            try {
                byte[] unobfuscatedData;
                String obfuscatedData;
                unobfuscatedData = StreamUtil.slurp(new FileInputStream(deobfuscatedFile));
                obfuscatedData = Obfuscator.encode(new String(unobfuscatedData));
                unobfuscatedData = null; // help GC
                FileUtil.writeFile(new ByteArrayInputStream(obfuscatedData.getBytes()), originalFile);
                obfuscatedData = null; // help GC
            } catch (Exception e) {
                // try to restore the original file before throwing exception
                try {
                    copyFile(deobfuscatedFile, originalFile);
                } catch (Throwable ignore) {
                }
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new IOException(e);
                }
            }
        } finally {
            //            System.out.println("obfuscat: original: " + deobfuscatedFile.length() + ", "
            //                + MessageDigestGenerator.getDigestString(deobfuscatedFile));
            //            System.out.println("          obfuscat: " + originalFile.length() + ", "
            //                + MessageDigestGenerator.getDigestString(originalFile));
            deobfuscatedFile.delete();
        }
    }

    public static void deobfuscateFile(File originalFile) throws IOException {
        // make a copy of the original data, so we can use the original file for the decompressed data
        File obfuscatedFile = new File(originalFile + ".o");
        try {
            copyFile(originalFile, obfuscatedFile);
            try {
                String unobfuscatedData;
                byte[] obfuscatedData = StreamUtil.slurp(new FileInputStream(obfuscatedFile));
                unobfuscatedData = Obfuscator.decode(new String(obfuscatedData));
                obfuscatedData = null; // help GC
                FileUtil.writeFile(new ByteArrayInputStream(unobfuscatedData.getBytes()), originalFile);
                unobfuscatedData = null; // help GC
            } catch (Exception e) {
                // try to restore the original file before throwing exception
                try {
                    copyFile(obfuscatedFile, originalFile);
                } catch (Throwable ignore) {
                }
                if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new IOException(e);
                }
            }
        } finally {
            //            System.out.println("deobfusc: original: " + obfuscatedFile.length() + ", "
            //                + MessageDigestGenerator.getDigestString(obfuscatedFile));
            //            System.out.println("          deobfusc: " + originalFile.length() + ", "
            //                + MessageDigestGenerator.getDigestString(originalFile));
            obfuscatedFile.delete();
        }
    }
    */
}
