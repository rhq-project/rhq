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

import java.util.Date;
import java.util.Map;
import java.io.OutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.bradmcevoy.http.FileResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

/**
 *
 *
 * @author Greg Hinkle
 */
public class ConfigResource extends BasicResource implements FileResource {

    private org.rhq.core.domain.resource.Resource resource;
    private Configuration configuration;

    private String content;

    public ConfigResource(org.rhq.core.domain.resource.Resource resource, Configuration configuration) {
        this.resource = resource;
        this.configuration = configuration;
    }

    private void loadContent() {
        JAXBContext context = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            context = JAXBContext.newInstance(Configuration.class, Property.class);
            context.createMarshaller().marshal(configuration, baos);
            this.content = baos.toString();

        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }

    public String getUniqueId() {
        return "config_" + configuration.getId();
    }

    public String getName() {
        return "resource_configuration.xml";
    }


    public Date getModifiedDate() {
        return new Date(configuration.getModifiedTime());
    }

    public Long getContentLength() {
        if (content == null) {
            loadContent();
        }
        return new Long(content.length());
    }

    public String getContentType(String s) {
        return "text/xml";
    }

    public String checkRedirect(Request request) {
        return null;
    }


    public void sendContent(OutputStream out) throws IOException {
        out.write( "hi".getBytes() );
    }


    public void sendContent(OutputStream outputStream, Range range, Map<String, String> stringStringMap, String s) throws IOException {

        JAXBContext context;
        try {
            context = JAXBContext.newInstance(Configuration.class, Property.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); //JMMarshallerImpl.JAXME_INDENTATION_STRING, "\r\n");
            marshaller.marshal(configuration, outputStream);

        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    public Long getMaxAgeSeconds(Auth auth) {
        return new Long(0);
    }

    public void copyTo(CollectionResource collectionResource, String s) {
    }

    public void delete() {
    }

    public void moveTo(CollectionResource collectionResource, String s) {
    }

    public String processForm(Map<String, String> stringStringMap, Map<String, FileItem> stringFileItemMap) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date getCreateDate() {
        return new Date(configuration.getCreatedTime());
    }
}
