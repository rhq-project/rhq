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
package org.rhq.enterprise.server.content.metadata.test;

import java.net.URL;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.testng.annotations.BeforeClass;
import org.rhq.core.clientapi.descriptor.DescriptorPackages;
import org.rhq.core.clientapi.descriptor.serverplugin.content.ContentSourcePluginDescriptor;
import org.rhq.core.clientapi.server.plugin.content.metadata.ContentSourcePluginMetadataManager;
import org.rhq.core.domain.content.ContentSourceType;
import org.rhq.enterprise.server.content.metadata.ContentSourceMetadataManagerLocal;
import org.rhq.enterprise.server.test.AbstractEJB3Test;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Base class for content metadata tests.
 */
public class TestBase extends AbstractEJB3Test {
    protected ContentSourceMetadataManagerLocal metadataManager;

    @BeforeClass
    protected void beforeClass() {
        try {
            metadataManager = LookupUtil.getContentSourceMetadataManager();
        } catch (Throwable t) {
            // Catch RuntimeExceptions and Errors and dump their stack trace, because Surefire will completely swallow them
            // and throw a cryptic NPE (see http://jira.codehaus.org/browse/SUREFIRE-157)!
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    protected ContentSourceType getContentSourceType(String typeName) throws Exception {
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            Query q = em.createNamedQuery(ContentSourceType.QUERY_FIND_BY_NAME_WITH_CONFIG_DEF);
            ContentSourceType type = (ContentSourceType) q.setParameter("name", typeName).getSingleResult();
            return type;
        } catch (NoResultException nre) {
            return null;
        } finally {
            getTransactionManager().rollback();
            em.close();
        }
    }

    protected void registerPlugin(String pathToDescriptor) throws Exception {
        ContentSourcePluginDescriptor descriptor = loadPluginDescriptor(pathToDescriptor);
        ContentSourcePluginMetadataManager mm = new ContentSourcePluginMetadataManager();
        mm.loadPlugin(descriptor);
        metadataManager.registerTypes(mm.getAllContentSourceTypes());
    }

    public ContentSourcePluginDescriptor loadPluginDescriptor(String descriptorFile) throws Exception {
        URL descriptorUrl = this.getClass().getClassLoader().getResource(descriptorFile);

        JAXBContext jaxbContext = JAXBContext.newInstance(DescriptorPackages.CONTENT);
        URL pluginSchemaURL = getClass().getClassLoader().getResource("rhq-serverplugin-content.xsd");
        Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(pluginSchemaURL);

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        ValidationEventCollector vec = new ValidationEventCollector();

        unmarshaller.setEventHandler(vec);
        unmarshaller.setSchema(pluginSchema);

        return (ContentSourcePluginDescriptor) unmarshaller.unmarshal(descriptorUrl.openStream());
    }
}