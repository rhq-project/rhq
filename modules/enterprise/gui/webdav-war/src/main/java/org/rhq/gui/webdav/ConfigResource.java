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

import java.io.ByteArrayOutputStream;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.configuration.ConfigurationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Provides the resource configuration XML for a given resource.
 *
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ConfigResource extends GetableBasicResource {

    private Configuration configuration;
    private String content;

    public ConfigResource(Subject subject, Resource managedResource) {
        super(subject, managedResource);
    }

    public String getUniqueId() {
        return "config_" + getConfiguration().getId();
    }

    public String getName() {
        return "resource_configuration.xml";
    }

    /**
     * The modified date is that of the last time the configuration changed.
     */
    public Date getModifiedDate() {
        return new Date(getConfiguration().getModifiedTime());
    }

    /**
     * The creation date is that of the configuration itself, not of the managed resource.
     */
    public Date getCreateDate() {
        return new Date(getConfiguration().getCreatedTime());
    }

    protected String loadContent() {
        if (this.content == null) {
            try {
                JAXBContext context = JAXBContext.newInstance(Configuration.class, Property.class);
                Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                marshaller.marshal(getConfiguration(), baos);
                this.content = baos.toString();
            } catch (JAXBException e) {
                throw new RuntimeException("Failed to translate configuration to XML", e);
            }
        }
        return this.content;
    }

    private Configuration getConfiguration() {
        if (this.configuration == null) {
            ConfigurationManagerLocal cm = LookupUtil.getConfigurationManager();
            this.configuration = cm.getResourceConfiguration(getManagedResource().getId());
        }
        return this.configuration;
    }
}
