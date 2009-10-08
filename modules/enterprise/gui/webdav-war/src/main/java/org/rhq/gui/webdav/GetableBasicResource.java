/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.gui.webdav;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.util.stream.StreamUtil;

/**
 * Extends the {@link BasicResource} by providing hooks to stream file content to the user.
 * 
 * @author John Mazzitelli
 */
public abstract class GetableBasicResource extends BasicResource implements GetableResource {

    public GetableBasicResource(Subject subject, Resource managedResource) {
        super(subject, managedResource);
    }

    /**
     * This implementation assumes the content will be of type "text/xml".
     * Subclasses are free to override this behavior if they are returning content
     * other than XML data.
     */
    public String getContentType(String accepts) {
        return "text/xml";
    }

    public Long getContentLength() {
        return Long.valueOf(loadContent().length());
    }

    public void sendContent(OutputStream out, Range range, Map<String, String> params, String str) throws IOException,
        NotAuthorizedException {

        byte[] bytes = loadContent().getBytes();
        long start = (range != null) ? range.getStart() : 0L;
        long length = (range != null) ? ((range.getFinish() - start) + 1) : bytes.length;

        InputStream in = new ByteArrayInputStream(bytes);
        StreamUtil.copy(in, out, start, length);
    }

    /**
     * This implementation assumes the user client should not cache the content.
     * Subclasses are free to override this if they allow the content to be cached
     * on the client side.
     */
    public Long getMaxAgeSeconds(Auth auth) {
        return Long.valueOf(0L);
    }

    /**
     * Subclasses must implement this to return the content as a String. Note that this really
     * only supports small content since you don't want to load large files in memory. If you need
     * content that is large, implement {@link GetableResource} directly; do not use this abstract class.
     * 
     * @return the content as a string, must never be <code>null</code>
     */
    protected abstract String loadContent();
}
