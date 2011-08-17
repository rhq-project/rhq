/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.core.pc.drift;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.drift.Filter;
import org.rhq.core.util.file.FileVisitor;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.FileUtils.toFile;
import static org.rhq.core.util.file.FileUtil.forEachFile;
import static org.rhq.test.AssertUtils.assertCollectionEqualsNoOrder;
import static org.testng.Assert.assertTrue;

public class FilterFileVisitorTest {

    private File basedir;

    @BeforeMethod
    public void setUp() throws Exception {
        File root = toFile(getClass().getResource("."));
        basedir = new File(root, "basedir");
        deleteDirectory(basedir);
        basedir.mkdirs();
    }

    @Test
    public void visitAllFilesWhenNoFiltersSpecified() throws Exception {
        File libDir = mkdir(basedir, "lib");
        File server1Jar = touch(libDir, "server-1.jar");
        File server2Jar = touch(libDir, "server-2.jar");

        File nativeLibDir = mkdir(libDir, "native");
        File nativeServer1Lib = touch(nativeLibDir, "server-1.so");
        File nativeServer2Lib = touch(nativeLibDir, "server-2.so");

        List<Filter> includes = emptyList();
        List<Filter> excludes = emptyList();
        TestVisitor visitor = new TestVisitor();

        forEachFile(basedir, new FilterFileVisitor(basedir, includes, excludes, visitor));

        assertCollectionEqualsNoOrder(asList(server1Jar, server2Jar, nativeServer1Lib, nativeServer2Lib),
            visitor.visitedFiles, "Visitor should be called for every file when no filters specified");
    }

    @Test
    public void visitFilesThatMatchIncludes() throws Exception {
        File libDir = mkdir(basedir, "lib");
        File fooJar = touch(libDir, "foo.jar");
        File fooWar = touch(libDir, "foo-1.jar");
        File myapp = touch(libDir, "myapp.war");
        touch(libDir, "bar.jar");

        List<Filter> includes = asList(new Filter(libDir.getAbsolutePath(), "foo*"),
            new Filter(libDir.getAbsolutePath(), "*.war"));
        List<Filter> excludes = emptyList();
        TestVisitor visitor = new TestVisitor();

        forEachFile(basedir, new FilterFileVisitor(basedir, includes, excludes, visitor));

        assertCollectionEqualsNoOrder(asList(fooJar, fooWar, myapp), visitor.visitedFiles,
            "Filtering failed with mulitple includes and no excludes");
    }

    @Test
    public void doNotVisitFileThatDoesNotMatchInclude() throws Exception {
        touch(basedir, "server.txt");

        List<Filter> includes = asList(new Filter(basedir.getAbsolutePath(), "*.html"));
        List<Filter> excludes = emptyList();
        TestVisitor visitor = new TestVisitor();

        forEachFile(basedir, new FilterFileVisitor(basedir, includes, excludes, visitor));

        assertTrue(visitor.visitedFiles.isEmpty(), "Do not visit files that do not match an includes filter");
    }

    @Test
    public void visitFileThatDoesNotMatchExcludes() throws Exception {
        File server1Txt = touch(basedir, "server-1.txt");
        File server1Html = touch(basedir, "server-1.html");

        List<Filter> includes = emptyList();
        List<Filter> excludes = asList(new Filter(basedir.getAbsolutePath(), "*.txt"));
        TestVisitor visitor = new TestVisitor();

        forEachFile(basedir, new FilterFileVisitor(basedir, includes, excludes, visitor));

        assertCollectionEqualsNoOrder(asList(server1Html), visitor.visitedFiles, "Visit files that do not match " +
            "excludes filter and no includes are specified");
    }

    @Test
    public void doNotVisitFileThatMatchesExclude() throws Exception {
        File libDir = mkdir(basedir, "lib");
        File server1Jar = touch(libDir, "server-1.jar");
        File server2Jar = touch(libDir, "server-2.jar");

        List<Filter> includes = emptyList();
        List<Filter> excludes = asList(new Filter(libDir.getAbsolutePath(), "server-1.jar"));
        TestVisitor visitor = new TestVisitor();

        forEachFile(basedir, new FilterFileVisitor(basedir, includes, excludes, visitor));

        assertCollectionEqualsNoOrder(asList(server2Jar), visitor.visitedFiles,
            "Filtering failed when no includes and an exclude were specified.");
    }

    @Test
    public void visitFileThatMatchesIncludeAndDoesNotMatchesExclude() throws Exception {
        // This test also verifies that files that match an include and also match
        // an exclude are not visited.

        File libDir = mkdir(basedir, "lib");
        File server1Jar = touch(libDir, "server-1.jar");
        File server2Jar = touch(libDir, "server-2.jar");

        File confDir = mkdir(basedir, "conf");
        File server1Conf = touch(confDir, "server-1.conf");
        File server2Conf = touch(confDir, "server-2.conf");

        List<Filter> includes = asList(new Filter(basedir.getAbsolutePath(), "**/*.*"));
        List<Filter> excludes = asList(new Filter(confDir.getAbsolutePath(), "server-2.conf"));
        TestVisitor visitor = new TestVisitor();

        forEachFile(basedir, new FilterFileVisitor(basedir, includes, excludes, visitor));

        assertCollectionEqualsNoOrder(asList(server1Jar, server2Jar, server1Conf), visitor.visitedFiles,
            "Do not visit files that match an excludes even when the file matches an include");
    }

    @Test
    public void expandRelativePaths() throws Exception {
        File server1Html = touch(basedir, "server-1.html");
        File server2Html = touch(basedir, "server-2.html");
        File server1Txt = touch(basedir, "server-1.txt");
        File server2Txt = touch(basedir, "server-2.txt");

        File libDir = mkdir(basedir, "lib");
        File server1Jar = touch(libDir, "server-1.jar");
        File server2Jar = touch(libDir, "server-2.jar");

        List<Filter> includes = asList(new Filter(".", "server-1.*"), new Filter("./lib", "server-1.*"));
        List<Filter> excludes = emptyList();
        TestVisitor visitor = new TestVisitor();

        forEachFile(basedir, new FilterFileVisitor(basedir, includes, excludes, visitor));

        assertCollectionEqualsNoOrder(asList(server1Html, server1Txt, server1Jar), visitor.visitedFiles,
            "Relative paths should be expanded out to full paths");
    }

    @Test
    public void includeEverythingWhenPathIsADirectoryAndNoPatternSpecified() throws Exception {
        File deployDir = mkdir(basedir, "deploy");
        File serverEar = touch(deployDir, "server.ear");
        File myapp = mkdir(deployDir, "myapp.war");
        File indexHtml = touch(myapp, "index.html");

        File confDir = mkdir(basedir, "conf");
        File serverConf = touch(confDir, "server.conf");

        List<Filter> includes = asList(new Filter(deployDir.getAbsolutePath(), null));
        List<Filter> excludes = emptyList();
        TestVisitor visitor = new TestVisitor();

        forEachFile(basedir, new FilterFileVisitor(basedir, includes, excludes, visitor));

        assertCollectionEqualsNoOrder(asList(serverEar, indexHtml), visitor.visitedFiles,
            "When a filter path specifies a directory and no pattern is specified, all files under that directory, " +
            "including subdirectories should be considered a match.");
    }

    private File mkdir(File parent, String dirName) throws IOException {
        File dir = new File(parent, dirName);
        dir.mkdirs();
        return dir;
    }

    private File touch(File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        FileUtils.touch(file);
        return file;
    }

    static class TestVisitor implements FileVisitor {

        public List<File> visitedFiles = new ArrayList<File>();

        @Override
        public void visit(File file) {
            visitedFiles.add(file);
        }
    }

}
