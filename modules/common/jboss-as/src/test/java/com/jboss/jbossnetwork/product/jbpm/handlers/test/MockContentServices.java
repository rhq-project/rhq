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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.rhq.core.domain.content.PackageDetailsKey;
import org.rhq.core.domain.content.composite.PackageVersionMetadataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;

/**
 * Mock implementation of the plugin container's content callback. This lets us simulate how the plugin would interact
 * with the PC.
 *
 * @author Jason Dobies
 */
public class MockContentServices implements ContentServices {
    /**
     * File to return as bits to the download call.
     */
    private String filename;

    public long downloadPackageBits(ContentContext context, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, boolean resourceExists) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(filename);
        BufferedInputStream bis = new BufferedInputStream(inputStream);

        byte[] buffer = new byte[4096];
        int numBytesCopied = 0;

        try {
            for (int bytesRead = bis.read(buffer); bytesRead != -1; bytesRead = bis.read(buffer)) {
                outputStream.write(buffer, 0, bytesRead);
                numBytesCopied += bytesRead;
            }

            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            assert false : "Error reading input file: " + e;
        }

        return numBytesCopied;
    }

    public long downloadPackageBitsForChildResource(ContentContext context, String childResourceTypeName, PackageDetailsKey key, OutputStream outputStream) {
        // Stub, unused in this test
        return 0;
    }
    
    public long downloadPackageBitsRange(ContentContext context, PackageDetailsKey packageDetailsKey,
        OutputStream outputStream, long startByte, long endByte, boolean resourceExists) {
        // Stub, unused in this test
        return 0;
    }

    public long getPackageBitsLength(ContentContext context, PackageDetailsKey packageDetailsKey) {
        // Stub, unused in this test
        return 0;
    }

    public PageList<PackageVersionMetadataComposite> getPackageVersionMetadata(ContentContext context, PageControl pc) {
        // Stub, unused in this test
        return null;
    }

    public String getResourceSubscriptionMD5(ContentContext context) {
        // Stub, unused in this test
        return null;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}