/*
 * RHQ Management Platform
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.server.drift;

import java.io.InputStream;
import java.io.Serializable;

public class DriftUploadRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private int resourceId;

    private String driftDefName;

    private String token;

    private long dataSize;

    private InputStream dataStream;

    public DriftUploadRequest(int resourceId, long dataSize, InputStream dataStream) {
        this(resourceId, null, null, dataSize, dataStream);
    }

    public DriftUploadRequest(int resourceId, String driftDefName, String token, long dataSize,
        InputStream dataStream) {
        this.resourceId = resourceId;
        this.driftDefName = driftDefName;
        this.token = token;
        this.dataSize = dataSize;
        this.dataStream = dataStream;
    }

    public int getResourceId() {
        return resourceId;
    }

    public String getDriftDefName() {
        return driftDefName;
    }

    public String getToken() {
        return token;
    }

    public long getDataSize() {
        return dataSize;
    }

    public InputStream getDataStream() {
        return dataStream;
    }
}
