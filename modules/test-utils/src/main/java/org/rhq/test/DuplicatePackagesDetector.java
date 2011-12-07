package org.rhq.test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This will walk a set of jar files and directories and print out
 * packages that are found in more than one jar file.
 *
 * @author John Mazzitelli
 */
public class DuplicatePackagesDetector {

    static Visitor visitor;

    static boolean verbose = Boolean.getBoolean("rhq.verbose");

    private static void debug(String msg) {
        if (verbose) {
            System.out.println(msg);
        }
    }

    /**
     * Each argument is the name of a .jar file or a directory that will
     * be walked looking for jar files.
     */
    public static void main(String[] args) {
        visitor = new Visitor();

        if (args.length == 0) {
            // no arguments given, try to use the user's local maven repo of all the RHQ libraries
            String homeEnv = System.getenv("HOME");
            if (homeEnv != null && homeEnv.length() > 0) {
                String s = File.separator;
                args = new String[] { homeEnv + s + ".m2" + s + "repository" + s + "org" + s + "rhq" };
            }
        }

        for (String arg : args) {
            File file = new File(arg);
            debug("Processing [" + file.getAbsolutePath() + "]...");
            if (file.exists()) {
                if (file.isDirectory()) {
                    processDirectory(file);
                } else {
                    processFile(file);
                }
            } else {
                System.err.println("File does not exist: " + arg);
            }

        }

        // print out what we've found
        System.out.println("======================================================================");
        Map<String, Set<String>> map = visitor.getMap(); // key=dir (pkg) name; value=list of jars where dir is
        for (Entry<String, Set<String>> entry : map.entrySet()) {
            String packageName = entry.getKey();
            Set<String> jarFiles = entry.getValue();
            if (jarFiles.size() > 1 || verbose) {
                System.out.println(packageName);
                for (String jarFile : jarFiles) {
                    System.out.println("\t" + jarFile);
                }
            }
        }
    }

    public static void processDirectory(File dir) {
        File[] dirEntries = dir.listFiles();
        if (dirEntries != null) {
            for (File dirEntry : dirEntries) {
                if (dirEntry.isDirectory()) {
                    processDirectory(dirEntry);
                } else {
                    processFile(dirEntry);
                }
            }
        }
    }

    public static void processFile(File file) {
        // if its not a .jar file, ignore it
        if (!file.getName().endsWith(".jar")) {
            debug("Not a jar file, skipping [" + file.getAbsolutePath() + "]");
            return;
        }

        try {
            walkZipFile(file, visitor);
        } catch (Exception e) {
            System.err.println("Cannot process jar file [" + file + "]:" + e.toString());
        }
    }

    /**
     * Walks the entries of a zip file, allowing a listener to "visit" each node and perform tasks on
     * the zip entry.
     *
     * @param zipFile the zip file to walk
     * @param visitor the object that will be notified for each entry in the zip file
     *
     * @throws Exception if any errors occur during the reading or visiting
     */
    public static void walkZipFile(File zipFile, Visitor visitor) throws Exception {
        FileInputStream fis = new FileInputStream(zipFile);
        try {
            InputStream zipContent = new BufferedInputStream(fis);
            try {
                ZipInputStream zis = new ZipInputStream(zipContent);

                ZipEntry e;
                while ((e = zis.getNextEntry()) != null) {
                    boolean keepGoing = visitor
                        .visit(e, zis, (verbose) ? zipFile.getAbsolutePath() : zipFile.getName());
                    if (!keepGoing) {
                        break; // visitor told us to stop
                    }
                }
            } finally {
                zipContent.close();
            }
        } finally {
            fis.close();
        }
    }

    public static class Visitor {
        // keyed on directory name - the value is all the jar files that had that directory in it
        private Map<String, Set<String>> map = new HashMap<String, Set<String>>();

        public Map<String, Set<String>> getMap() {
            return map;
        }

        /**
         * Visits a specific zip file entry. Implementations can read the entry content from the given stream but
         * must <b>not</b> close the stream - the caller of this method will handle the lifecycle of the stream.
         *
         * @param entry the entry being visited
         * @param stream the stream containing the zip content
         * @param zipFilePath the actual zip file that is being walked
         * @return the visitor should return <code>true</code> if everything is OK and processing of the zip content
         *         should continue; returning <code>false</code> will tell the walker to abort further traversing
         *         of the zip content.
         * @throws Exception if the visitation failed for some reason - this will abort further walking of the zip content
         */
        public boolean visit(ZipEntry entry, ZipInputStream stream, String zipFilePath) throws Exception {
            if (!entry.isDirectory()) {
                String[] dirAndName = splitPathName(entry.getName());
                if (dirAndName != null) {
                    String dirName = dirAndName[0];
                    Set<String> zipFilePaths = map.get(dirName);
                    if (zipFilePaths == null) {
                        zipFilePaths = new HashSet<String>();
                        map.put(dirName, zipFilePaths);
                    }
                    zipFilePaths.add(zipFilePath);
                }
            }
            return true;
        }

        /**
         * Splits the path from the filename and returns as a 2-dimension array.
         * If the full name does NOT end with .class, <code>null</code> is returned.
         * Thus, this only processes Java classes.
         * @param fullname the name to split
         * @return first element is the path, second element is the filename
         */
        private String[] splitPathName(String fullname) {
            if (fullname.endsWith(".class")) {
                int lastSlash = fullname.lastIndexOf('/');
                if (lastSlash >= 0) {
                    return new String[] { fullname.substring(0, lastSlash), fullname.substring(lastSlash + 1) };
                } else {
                    return new String[] { "", fullname };
                }
            } else {
                return null;
            }
        }
    }
}
