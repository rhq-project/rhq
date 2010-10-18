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

package org.rhq.helpers.perftest.support.input;

import java.io.IOException;
import java.util.zip.ZipInputStream;

/**
 * This is a wrapper around another {@link InputStreamProvider} that returns
 * a {@link ZipInputStream} wrapped around the stream provided by the decorated provider.
 * 
 * @author Lukas Krejci
 */
public class ZipInputStreamProviderDecorator implements InputStreamProvider {

    private InputStreamProvider inner;
    private boolean openEntry;
    
    public ZipInputStreamProviderDecorator(InputStreamProvider inner, boolean openEntry) {
        this.inner = inner;
        this.openEntry = openEntry;
    }
    
    public ZipInputStream createInputStream() throws IOException {
        ZipInputStream stream = new ZipInputStream(inner.createInputStream());
        if (openEntry) {
            stream.getNextEntry();
        }
        return stream;
    }

}
