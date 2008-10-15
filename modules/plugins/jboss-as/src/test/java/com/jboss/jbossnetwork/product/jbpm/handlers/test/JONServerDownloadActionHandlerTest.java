 /*
  * Jopr Management Platform
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
package com.jboss.jbossnetwork.product.jbpm.handlers.test;

import java.io.File;

import com.jboss.jbossnetwork.product.jbpm.handlers.JONServerDownloadActionHandler;

import org.testng.annotations.Test;

import org.rhq.core.domain.content.PackageDetailsKey;

/**
 * @author Jason Dobies
 */
public class JONServerDownloadActionHandlerTest {
    @Test
    public void downloadBits() throws Exception {
        // Setup
        File downloadTo = File.createTempFile("JONServerDownloadActionHandlerTest-download", null);

        PackageDetailsKey key = new PackageDetailsKey("testPackage", "1.0", "testPackageType", "noarch");
        MockContentContext contentContext = new MockContentContext();
        MockContentServices contentServices = (MockContentServices) contentContext.getContentServices();
        contentServices.setFilename("handlers/JONServerDownloadActionHandlerTest-input-1.txt");

        JONServerDownloadActionHandler handler = new JONServerDownloadActionHandler();

        // This would be set by the workflow
        handler.setDestinationFileLocation(downloadTo.getAbsolutePath());

        // Test
        handler.downloadBits(key, contentContext);

        // Verify
        assert downloadTo.exists() : "Temporary file [" + downloadTo.getAbsoluteFile() + "] was not created";
        assert downloadTo.length() > 0 : "No data written to temporary file [" + downloadTo.getAbsoluteFile() + "]";

        // Clean up
        try {
            downloadTo.delete();
        } catch (Exception e) {
            System.out.println("Could not delete temporary file [" + downloadTo.getAbsoluteFile() + "] after test");
        }
    }
}