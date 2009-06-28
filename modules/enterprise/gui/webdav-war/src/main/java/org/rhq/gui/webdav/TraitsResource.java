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
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.FileResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author John Mazzitelli
 */
public class TraitsResource extends BasicResource implements FileResource {

    private Resource resource;
    private List<MeasurementDataTrait> traits;
    private String content;

    public TraitsResource(org.rhq.core.domain.resource.Resource resource, List<MeasurementDataTrait> traits) {
        this.resource = resource;
        this.traits = traits;
    }

    public String getUniqueId() {
        return "traits_" + resource.getId();
    }

    public String getName() {
        return "measurement_traits.xml";
    }

    public Date getModifiedDate() {
        long latestTimestamp = 0L;
        for (MeasurementDataTrait trait : this.traits) {
            if (latestTimestamp < trait.getTimestamp()) {
                latestTimestamp = trait.getTimestamp();
            }
        }
        return new Date(latestTimestamp);
    }

    public Long getContentLength() {
        return Long.valueOf(loadContent().length());
    }

    public String getContentType(String arg0) {
        return "text/xml";
    }

    public void sendContent(OutputStream out, Range range, Map<String, String> map, String str) throws IOException,
        NotAuthorizedException {

        byte[] bytes = loadContent().getBytes();
        long start = (range != null) ? range.getStart() : 0L;
        long length = (range != null) ? ((range.getFinish() - start) + 1) : bytes.length;

        InputStream in = new ByteArrayInputStream(bytes);
        StreamUtil.copy(in, out, start, length);
    }

    public Long getMaxAgeSeconds(Auth arg0) {
        return Long.valueOf(0L);
    }

    public Date getCreateDate() {
        return new Date(this.resource.getCtime());
    }

    public String checkRedirect(Request arg0) {
        return null; // no-op
    }

    public void copyTo(CollectionResource arg0, String arg1) {
        throw new UnsupportedOperationException("WebDAV users cannot copy traits");
    }

    public void delete() {
        throw new UnsupportedOperationException("WebDAV users cannot delete traits");
    }

    public void moveTo(CollectionResource arg0, String arg1) {
        throw new UnsupportedOperationException("WebDAV users cannot move traits");
    }

    public String processForm(Map<String, String> arg0, Map<String, FileItem> arg1) {
        return null; // no-op
    }

    private String loadContent() {
        if (this.content == null) {
            StringBuilder str = new StringBuilder();
            str.append("<?xml version=\"1.0\"?>\n");
            str.append("<traits>\n");
            for (MeasurementDataTrait trait : this.traits) {
                str.append("   <trait>\n");
                str.append("      <name>").append(trait.getName()).append("</name>\n");
                str.append("      <value>").append(trait.getValue()).append("</value>\n");
                str.append("      <last-changed>").append(new Date(trait.getTimestamp())).append("</last-changed>\n");
                str.append("      <schedule-id>").append(trait.getScheduleId()).append("</schedule-id>\n");
                str.append("   </trait>\n");
            }
            str.append("</traits>\n");
            this.content = str.toString();
        }
        return this.content;
    }
}
