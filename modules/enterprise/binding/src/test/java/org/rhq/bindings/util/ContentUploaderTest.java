/*
 * RHQ Management Platform
 * Copyright (C) 2005-2015 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.bindings.util;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.util.MessageDigestGenerator;
import org.rhq.core.util.stream.StreamUtil;
import org.rhq.enterprise.server.content.ContentManagerRemote;

/**
 * @author Thomas Segismont
 */
public class ContentUploaderTest {
    private static final String TEST_HANDLE = "calanques";

    private Subject subject;
    private ContentUploader contentUploader;
    @Mock
    private ContentManagerRemote contentManager;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        subject = new Subject(TEST_HANDLE, true, false);
        when(contentManager.createTemporaryContentHandle(eq(subject))).thenReturn(TEST_HANDLE);
        contentUploader = new ContentUploader(subject, contentManager);
    }

    @AfterMethod
    public void tearDown() throws Exception {

    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void uploadByFileNameShouldThrowIAEWhenFilenameIsNull() throws Exception {
        contentUploader.upload((String) null);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void uploadByFileNameShouldThrowIAEWhenFilenameIsBlank() throws Exception {
        contentUploader.upload("     ");
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void uploadByFileNameShouldThrowIAEWhenFileDoesNotExist() throws Exception {
        File aFileWhichDoesNotExist = aFileWhichDoesNotExist();
        assertFalse(aFileWhichDoesNotExist.exists());
        contentUploader.upload(aFileWhichDoesNotExist.getAbsolutePath());
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void uploadByFileNameShouldThrowIAEWhenFileIsADirectory() throws Exception {
        File aFileWhichIsADirectory = aFileWhichIsADirectory();
        contentUploader.upload(aFileWhichIsADirectory.getAbsolutePath());
    }

    @Test
    public void testUploadByFileName() throws Exception {
        File testFileToSend = createTestFileToSend();
        final File testFileReceived = createTestFileReceived();
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] invocationArguments = invocation.getArguments();
                int argIndex = 1;
                ByteArrayInputStream inputStream = new ByteArrayInputStream((byte[]) invocationArguments[++argIndex],
                    (Integer) invocationArguments[++argIndex], (Integer) invocationArguments[++argIndex]);
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(testFileReceived, true); // append == true
                    StreamUtil.copy(inputStream, new BufferedOutputStream(fileOutputStream, 1024 * 32));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } finally {
                    StreamUtil.safeClose(fileOutputStream);
                }
                return null;
            }
        }).when(contentManager).uploadContentFragment(eq(subject), eq(TEST_HANDLE), any(byte[].class), anyInt(),
            anyInt());
        String temporaryContentHandle = contentUploader.upload(testFileToSend.getAbsolutePath());
        assertEquals(temporaryContentHandle, TEST_HANDLE);
        // If file sizes and file hashes are equal, we can say they have the same content
        assertEquals(testFileReceived.length(), testFileToSend.length());
        assertEquals(MessageDigestGenerator.getDigestString(testFileReceived),
            MessageDigestGenerator.getDigestString(testFileToSend));
    }

    private File aFileWhichDoesNotExist() {
        File file = mock(File.class);
        when(file.exists()).thenReturn(FALSE);
        return file;
    }

    private File aFileWhichIsADirectory() {
        File file = mock(File.class);
        when(file.exists()).thenReturn(TRUE);
        when(file.isDirectory()).thenReturn(TRUE);
        return file;
    }

    private File createTestFileToSend() throws IOException {
        File file = File.createTempFile(ContentUploaderTest.class.getSimpleName(), ".content");
        file.deleteOnExit();
        StreamUtil.copy(getClass().getClassLoader().getResourceAsStream("ContentUploaderTest.content"),
            new FileOutputStream(file));
        return file;
    }

    private File createTestFileReceived() throws IOException {
        File file = File.createTempFile(ContentUploaderTest.class.getSimpleName(), ".received");
        file.deleteOnExit();
        return file;
    }
}
