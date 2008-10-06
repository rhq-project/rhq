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
package org.rhq.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

import org.testng.annotations.Test;

/**
 * When this class is run as part of the build, it will notify
 * developers through test failure when they forget to add the
 * appropriate copyright header to source files.
 * 
 * @author Joseph Marques
 */
@Test
public class CopyrightHeaderValidatorTest extends TestCase {

    private static final String TARGET_SEARCH_TEXT = "Copyright (C) 2005-2008 Red Hat, Inc.";
    private static final String END_SEARCH_TEXT = "package";
    private final String[] IGNORED_CLASSESS = { "ComparableVersion" };

    private class JavaSourceFileFilter implements FileFilter {
        public boolean accept(File f) {
            String fileName = f.getName().toLowerCase();
            if (f.isDirectory()) {
                // skip target dirs, such as client-api/generated-sources
                // skip resources dirs, if they contain src we don't proceed it
                return !fileName.endsWith("target") && !fileName.endsWith("resources");
            } else if (f.isFile()) {
                return fileName.endsWith(".java") && !ignored(fileName);
            } else {
                return false;
            }
        }

        private boolean ignored(String fileName) {
            for (String ignored : IGNORED_CLASSESS) {
                if (fileName.contains(ignored)) {
                    return false;
                }
            }
            return true;
        }
    }

    public void testExistenceOfHeaders() {
        String baseDir = System.getProperty("user.dir");
        JavaSourceFileFilter javaSourceFileFilter = new JavaSourceFileFilter();
        File baseDirFile = new File(baseDir);

        List<File> sourceFiles = new LinkedList<File>();
        sourceFiles.add(baseDirFile);

        int missingCopyrightCount = 0;
        while (sourceFiles.size() > 0) {
            File nextFile = sourceFiles.remove(0);
            if (nextFile.isDirectory()) {
                File[] children = nextFile.listFiles(javaSourceFileFilter);
                sourceFiles.addAll(Arrays.asList(children));
            } else {
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(nextFile));
                    String nextLine = null;
                    boolean foundCopyright = false;
                    while ((nextLine = reader.readLine()) != null) {
                        nextLine = nextLine.trim();
                        if (nextLine.endsWith(TARGET_SEARCH_TEXT)) {
                            foundCopyright = true;
                            break;
                        } else if (nextLine.startsWith(END_SEARCH_TEXT)) {
                            break;
                        }
                    }
                    if (!foundCopyright) {
                        System.out.println(nextFile + " is missing copyright");
                        missingCopyrightCount++;
                    }
                } catch (Exception e) {
                    System.out.println("Error reading " + nextFile + ", probably best to check it manually");
                }
            }
        }
        System.out.println("There were " + missingCopyrightCount + " missing copyrights");
        assert (missingCopyrightCount == 0) : "There were " + missingCopyrightCount + " missing copyrights";
    }

    public static void main(String[] args) {
        CopyrightHeaderValidatorTest validator = new CopyrightHeaderValidatorTest();
        validator.testExistenceOfHeaders();
    }
}
