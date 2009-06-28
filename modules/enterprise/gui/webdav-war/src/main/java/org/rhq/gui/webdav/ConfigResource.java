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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.FileResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.util.stream.StreamUtil;

/**
 * Provides the resource configuration XML for a given resource.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ConfigResource extends BasicResource implements FileResource {

    private org.rhq.core.domain.resource.Resource resource;
    private Configuration configuration;
    private String content;

    public ConfigResource(org.rhq.core.domain.resource.Resource resource, Configuration configuration) {
        this.resource = resource;
        this.configuration = configuration;
    }

    private String loadContent() {
        if (this.content == null) {
            try {
                JAXBContext context = JAXBContext.newInstance(Configuration.class, Property.class);
                Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                marshaller.marshal(this.configuration, baos);
                this.content = baos.toString();
            } catch (JAXBException e) {
                throw new RuntimeException("Failed to translate configuration to XML", e);
            }
        }
        return this.content;
    }

    public String getUniqueId() {
        return "config_" + configuration.getId();
    }

    public String getName() {
        return "resource_configuration.xml";
    }

    public Date getModifiedDate() {
        return new Date(this.configuration.getModifiedTime());
    }

    public Long getContentLength() {
        return new Long(loadContent().length());
    }

    public String getContentType(String s) {
        return "text/xml";
    }

    public String checkRedirect(Request request) {
        return null; // unsupported
    }

    public void sendContent(OutputStream out, Range range, Map<String, String> map, String str) throws IOException,
        NotAuthorizedException {

        byte[] bytes = loadContent().getBytes();
        long start = (range != null) ? range.getStart() : 0L;
        long length = (range != null) ? ((range.getFinish() - start) + 1) : bytes.length;

        InputStream in = new ByteArrayInputStream(bytes);
        StreamUtil.copy(in, out, start, length);
    }

    public Long getMaxAgeSeconds(Auth auth) {
        return new Long(0);
    }

    public void copyTo(CollectionResource collectionResource, String s) {
        return; // unsupported
    }

    public void delete() {
        return; // unsupported
    }

    public void moveTo(CollectionResource collectionResource, String s) {
        return; // unsupported
    }

    public String processForm(Map<String, String> stringStringMap, Map<String, FileItem> stringFileItemMap) {
        return null; // unsupported
    }

    public Date getCreateDate() {
        return new Date(this.configuration.getCreatedTime());
    }
}
