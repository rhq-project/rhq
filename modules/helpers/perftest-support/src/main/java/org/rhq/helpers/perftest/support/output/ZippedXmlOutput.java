/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.output;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Represents a zipped XML output.
 * 
 * @author Lukas Krejci
 */
public class ZippedXmlOutput extends XmlOutput {

    /**
     * 
     * @param stream the stream to write the data to.
     * @param doClose
     * @throws IOException 
     */
    public ZippedXmlOutput(OutputStream stream, boolean doClose) throws IOException {
        super(getNewZipStream(stream), doClose);
    }
    
    private static ZipOutputStream getNewZipStream(OutputStream stream) throws IOException {
        ZipOutputStream zipStream = new ZipOutputStream(stream);
        zipStream.putNextEntry(new ZipEntry("data"));
        
        return zipStream;
    }
}
