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
package org.rhq.core.tool.plugindoc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Set;
import java.util.HashMap;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.util.ValidationEventCollector;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.swizzle.confluence.Confluence;
import org.codehaus.swizzle.confluence.Page;
import org.codehaus.swizzle.confluence.SwizzleException;

import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.resource.ResourceType;

/**
 * Generates both Confluence and Docbook format documentation for an RHQ plugin based on the plugin's descriptor (i.e.
 * rhq-plugin.xml). Invoke from a RHQ plugin module directory as follows:
 * <code>mvn org.rhq:rhq-core-plugindoc:plugindoc</code>
 *
 * @author                       Ian Springer
 * @goal                         plugindoc
 * @requiresProject
 * @requiresDependencyResolution
 */
@SuppressWarnings({"UnusedDeclaration"})
public class PluginDocMojo extends AbstractMojo {
    private static final String PLUGIN_DESCRIPTOR_PATH = "src/main/resources/META-INF/rhq-plugin.xml";
    private static final String OUTPUT_DIR_PATH = "target/plugindoc";
    private static final String PLUGIN_DESCRIPTOR_JAXB_CONTEXT_PATH = "org.rhq.core.clientapi.descriptor.plugin";
    private static final String CONFLUENCE_TEMPLATE_RESOURCE_PATH = "resource-type-doc-confluence.vm";
    private static final String DOCBOOK_TEMPLATE_RESOURCE_PATH = "resource-type-doc-docbook.vm";

    private final Log log = LogFactory.getLog(PluginDocMojo.class);

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    @SuppressWarnings( { "UnusedDeclaration" })
    private MavenProject project;

    /**
     * The main Confluence URL (e.g. "http://support.rhq-project.org/").
     *
     * @parameter expression="${confluenceUrl}"
     */
    private String confluenceUrl;

    /**
     * The Confluence space (e.g. "RHQ").
     *
     * @parameter expression="${confluenceSpace}"
     */
    private String confluenceSpace;

    /**
     * The Confluence parent page name (e.g. "Managed Resources").
     *
     * @parameter expression="${confluenceParentPageTitle}"
     */
    private String confluenceParentPageTitle;

    /**
     * @parameter expression="${confluenceUserName}"
     */
    private String confluenceUserName;

    /**
     * @parameter expression="${confluencePassword}"
     */
    private String confluencePassword;

    private String endpoint;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File pluginXmlFile = new File(this.project.getBasedir(), PLUGIN_DESCRIPTOR_PATH);
        if (!pluginXmlFile.exists()) {
            log.info("'" + pluginXmlFile + "' does not exist - nothing to do.");
            return;
        }

        PluginDescriptorProcessor descriptorProcessor = new PluginDescriptorProcessor(
            parsePluginDescriptor(pluginXmlFile));
        Set<ResourceType> resourceTypes;
        try {
            resourceTypes = descriptorProcessor.processPluginDescriptor();
        } catch (InvalidPluginDescriptorException e) {
            throw new MojoExecutionException("Failed to process plugin descriptor.", e);
        }

        File outputDir = new File(this.project.getBasedir(), OUTPUT_DIR_PATH);
        outputDir.mkdirs();

        VelocityTemplateProcessor confluenceTemplateProcessor = new VelocityTemplateProcessor(CONFLUENCE_TEMPLATE_RESOURCE_PATH);
        VelocityTemplateProcessor docbookTemplateProcessor = new VelocityTemplateProcessor(DOCBOOK_TEMPLATE_RESOURCE_PATH);

        if (this.confluenceUrl != null) {
            log.debug("Using Confluence URL: " + this.confluenceUrl);
            this.endpoint = this.confluenceUrl + "/rpc/xmlrpc";
        }
        for (ResourceType resourceType : resourceTypes) {
            log.info("Generating plugin doc for '" + resourceType.getName() + "' Resource type...");            

            confluenceTemplateProcessor.getContext().put("resourceType", resourceType);
            String confluenceOutputFileName = escapeFileName(resourceType.getName() + ".wiki");
            File confluenceOutputFile = new File(outputDir, confluenceOutputFileName);
            confluenceTemplateProcessor.processTemplate(confluenceOutputFile);
            if (this.endpoint != null) {
                publishPage(confluenceOutputFile, resourceType);
            }

            docbookTemplateProcessor.getContext().put("resourceType", resourceType);
            String docbookOutputFileName = escapeFileName(resourceType.getName() + ".xml");
            File docbookOutputFile = new File(outputDir, docbookOutputFileName);
            docbookTemplateProcessor.processTemplate(docbookOutputFile);
        }
    }

    private PluginDescriptor parsePluginDescriptor(File pluginXmlFile) throws MojoExecutionException,
        MojoFailureException {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(PLUGIN_DESCRIPTOR_JAXB_CONTEXT_PATH);
        } catch (JAXBException e) {
            throw new MojoExecutionException("Failed to instantiate JAXB context for context path '"
                + PLUGIN_DESCRIPTOR_JAXB_CONTEXT_PATH + "'.", e);
        }

        InputStream is = null;
        try {
            is = new FileInputStream(pluginXmlFile);

            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            // Enable schema validation. (see http://jira.jboss.com/jira/browse/JBNADM-1539)
            URL pluginSchemaURL = getClass().getClassLoader().getResource("rhq-plugin.xsd");
            Schema pluginSchema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(
                pluginSchemaURL);
            unmarshaller.setSchema(pluginSchema);

            ValidationEventCollector validationEventCollector = new ValidationEventCollector();
            unmarshaller.setEventHandler(validationEventCollector);

            PluginDescriptor pluginDescriptor = (PluginDescriptor) unmarshaller.unmarshal(is);

            for (ValidationEvent event : validationEventCollector.getEvents()) {
                log.debug("Plugin [" + pluginDescriptor.getName() + "] descriptor messages {Severity: "
                    + event.getSeverity() + ", Message: " + event.getMessage() + ", Exception: "
                    + event.getLinkedException() + "}");
            }

            return pluginDescriptor;
        } catch (Exception e) {
            throw new MojoExecutionException("Could not successfully parse plugin descriptor '" + pluginXmlFile + "'.",
                e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Nothing more we can do here
                }
            }
        }
    }

    private void publishPage(File contentFile, ResourceType resourceType) throws MojoExecutionException {
        log.info("Publishing plugin doc page for '" + resourceType.getName() + "' Resource type to Confluence...");
        String title = getPageTitle(resourceType);
        try {
            Confluence confluence = new Confluence(endpoint);
            confluence.login(this.confluenceUserName, this.confluencePassword);
            Page page;
            try {
                page = confluence.getPage(this.confluenceSpace, title);
                log.warn("Page with title '" + title + "' already exists - overwriting it...");
            }
            catch (SwizzleException e) {
                page = new Page(new HashMap());
                page.setSpace(this.confluenceSpace);
                if (this.confluenceParentPageTitle != null) {
                    Page parentPage = confluence.getPage(this.confluenceSpace, this.confluenceParentPageTitle);
                    if (parentPage != null)
                        page.setParentId(parentPage.getId());
                    else
                        log.error("Specified parent page ('" + this.confluenceParentPageTitle + "') does not exist.");
                }
                page.setTitle(title);
            }
            page.setContent(getContentAsString(contentFile));
            confluence.storePage(page);
            confluence.logout();
        }
        catch (Exception e) {
            throw new MojoExecutionException("Failed to publish plugin doc page to Confluence.", e);
        }
    }

    private static String getPageTitle(ResourceType resourceType) {
        String title = resourceType.getName();
        if (!resourceType.getName().endsWith(resourceType.getCategory().toString())) {
           title += " " + resourceType.getCategory();
        }
        return escapePageTitle(title);
    }

    private static String getContentAsString(File contentFile) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(contentFile)));
        String line;
        while((line = bufferedReader.readLine()) != null) {
            content.append(line).append("\n");
        }
        bufferedReader.close();
        return content.toString();
    }

    private static String escapeFileName(String fileName)
    {
        return fileName.replace('/', '-').replace('\\', '-');
    }

    private static String escapePageTitle(String fileName)
    {
        return fileName.replace('/', '-');
    }
}