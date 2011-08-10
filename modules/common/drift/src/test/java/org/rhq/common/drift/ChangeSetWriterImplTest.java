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

package org.rhq.common.drift;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.apache.commons.io.IOUtils.readLines;
import static org.rhq.common.drift.FileEntry.addedFileEntry;
import static org.rhq.common.drift.FileEntry.changedFileEntry;
import static org.rhq.common.drift.FileEntry.removedFileEntry;
import static org.rhq.core.domain.drift.DriftChangeSetCategory.COVERAGE;
import static org.testng.Assert.assertEquals;

public class ChangeSetWriterImplTest {

    File changeSetsDir;

    File resourcesDir;

    @BeforeClass
    public void setupChangesetsDir() throws Exception {
        File basedir = new File("target", getClass().getSimpleName());
        deleteDirectory(basedir);

        basedir.mkdir();

        changeSetsDir = new File(basedir, "changesets");
        changeSetsDir.mkdir();

        resourcesDir = new File(basedir, "resources");
        resourcesDir.mkdir();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writeFiledAddedEntry() throws Exception {
        File resourceDir = new File(resourcesDir, "myresource");

        File changeSetFile = new File(changeSetsDir, "added-file-test");

        Headers headers = new Headers();
        headers.setResourceId(1);
        headers.setDriftCofigurationId(2);
        headers.setDriftConfigurationName("add-file-test");
        headers.setBasedir(resourceDir.getAbsolutePath());
        headers.setType(COVERAGE);

        ChangeSetWriterImpl writer = new ChangeSetWriterImpl(changeSetFile, headers);

        writer.write(addedFileEntry("conf/myconf.conf", "a34ef6"));
        writer.close();

        File metaDataFile = writer.getChangeSetFile();
        List<String> lines = readLines(new FileInputStream(metaDataFile));

        assertEquals(lines.size(), 6, "Expected to find seven lines in " + metaDataFile.getPath() +
            " - five for the header followed by one file entry.");
        assertHeadersEquals(lines, headers);
        assertFileEntryEquals(lines.get(5), "A a34ef6 0 conf/myconf.conf");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writeFileRemovedEntry() throws Exception {
        File resourceDir = new File(resourcesDir, "myresource");

        File changeSetFile = new File(changeSetsDir, "removed-file-test");
        Headers headers = new Headers();
        headers.setResourceId(1);
        headers.setDriftCofigurationId(2);
        headers.setDriftConfigurationName("removed-file-test");
        headers.setBasedir(resourceDir.getAbsolutePath());
        headers.setType(COVERAGE);

        ChangeSetWriterImpl writer = new ChangeSetWriterImpl(changeSetFile, headers);

        writer.write(removedFileEntry("conf/myconf.conf", "a34ef6"));
        writer.close();

        File metaDataFile = writer.getChangeSetFile();
        List<String> lines = readLines(new FileInputStream(metaDataFile));

        assertEquals(lines.size(), 6, "Expected to find six lines in " + metaDataFile.getPath() +
            " - five for the header followed by one file entry.");
        assertHeadersEquals(lines, headers);
        assertFileEntryEquals(lines.get(5), "R 0 a34ef6 conf/myconf.conf");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void writeFileChangedEntry() throws Exception {
        File resourceDir = new File(resourcesDir, "myresource");

        File changeSetFile = new File(changeSetsDir, "changed-file-test");
        Headers headers = new Headers();
        headers.setResourceId(1);
        headers.setDriftCofigurationId(2);
        headers.setDriftConfigurationName("changed-file-test");
        headers.setBasedir(resourceDir.getAbsolutePath());
        headers.setType(COVERAGE);

        ChangeSetWriterImpl writer = new ChangeSetWriterImpl(changeSetFile, headers);

        writer.write(changedFileEntry("conf/myconf.conf", "a34ef6", "c2d55f"));
        writer.close();

        File metaDataFile = writer.getChangeSetFile();
        List<String> lines = readLines(new FileInputStream(metaDataFile));

        assertEquals(lines.size(), 6, "Expected to find six lines in " + metaDataFile.getPath() +
            " - five for the header followed by one file entry.");
        assertHeadersEquals(lines, headers);
        assertFileEntryEquals(lines.get(5), "C c2d55f a34ef6 conf/myconf.conf");
    }

    /**
     * Verifies that <code>lines</code> which is assumed to represent the entire cange set
     * file contains the expected headers
     *
     * @param lines The change set where each string in the list represents a line
     * @param headers The expected headers
     */
    void assertHeadersEquals(List<String> lines, Headers headers) {
        assertEquals(lines.get(0), Integer.toString(headers.getResourceId()), "The first header entry should be the " +
            "resurce id.");
        assertEquals(lines.get(1), Integer.toString(headers.getDriftCofigurationId()), "The second header entry " +
            "should be the drift configuration id.");
        assertEquals(lines.get(2), headers.getDriftConfigurationName(), "The third header entry should be the drift " +
            "configuration name.");
        assertEquals(lines.get(3), headers.getBasedir(), "The fourth header entry should be the drift configuration " +
            "base directory.");
        assertEquals(lines.get(4), headers.getType().code(), "The fifth header entry should be the change set " +
            "category code");
    }

    /**
     * Verifies that a file entry matches an expected value. A file entry consists of
     * four, space-delimited fields and is terminated by a newline character. Those fields
     * are type_code, new_sha, old_sha, file_name.
     *
     * @param actual
     * @param expected
     */
    void assertFileEntryEquals(String actual, String expected) {
        String[] expectedFields = expected.split(" ");
        String[] actualFields = actual.split(" ");

        assertEquals(4, expectedFields.length, "<" + expected + "> should contain 4 fields");
        assertEquals(4, actualFields.length, "<" + expected + "> should contain 4 fields");

        assertEquals(actualFields[0], expectedFields[0], "The first column, the SHA-256, is wrong");
        assertEquals(actualFields[1], expectedFields[1], "The second column, the old SHA-256, is wrong");
        assertEquals(actualFields[2], expectedFields[2], "The third column, the file name, is wrong");
        assertEquals(actualFields[3], expectedFields[3], "The fourth column, the type, is wrong");
    }

}
