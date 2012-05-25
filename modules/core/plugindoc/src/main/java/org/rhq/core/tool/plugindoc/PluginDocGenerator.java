/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

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
import org.codehaus.swizzle.confluence.Confluence;
import org.codehaus.swizzle.confluence.Page;
import org.codehaus.swizzle.confluence.PageSummary;
import org.codehaus.swizzle.confluence.SwizzleException;

import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceType;

/**
 * @author Ian Springer
 */
public class PluginDocGenerator {

    private final Log log = LogFactory.getLog(this.getClass());

    private static final String PLUGIN_DESCRIPTOR_PATH = "src/main/resources/META-INF/rhq-plugin.xml";
    private static final String OUTPUT_DIR_PATH = "target/plugindoc";
    private static final String PLUGIN_DESCRIPTOR_JAXB_CONTEXT_PATH = "org.rhq.core.clientapi.descriptor.plugin";
    private static final String CONFLUENCE_PLUGIN_TEMPLATE_RESOURCE_PATH = "plugin-doc-confluence.vm";
    private static final String CONFLUENCE_RESOURCE_TYPE_TEMPLATE_RESOURCE_PATH = "resource-type-doc-confluence.vm";
    private static final String DOCBOOK_PLUGIN_TEMPLATE_RESOURCE_PATH = "plugin-doc-docbook.vm";
    private static final String DOCBOOK_RESOURCE_TYPE_TEMPLATE_RESOURCE_PATH = "resource-type-doc-docbook.vm";
    private static final String CONFLUENCE_MACRO_LIBRARY_RESOURCE_PATH = "confluence-macros.vm";
    private static final String DOCBOOK_MACRO_LIBRARY_RESOURCE_PATH = "docbook-macros.vm";
    private static final String RHQ_VERSION = "4.4.0";

    private String confluenceUrl; // The main Confluence URL (e.g. "http://rhq-project.org/").
    private String confluenceSpace; // The Confluence space (e.g. "JOPR2").
    private String confluenceParentPageTitle; // The Confluence parent page name (e.g. "Management Plugins").
    private String confluenceUserName;
    private String confluencePassword;

    private static final Comparator<ResourceType> CHILD_RESOURCE_TYPES_SORTER = new Comparator<ResourceType>() {
        public int compare(ResourceType first, ResourceType second) {
            // platforms before servers, and servers before services
            int result = first.getCategory().compareTo(second.getCategory());
            if (result == 0) {
                result = first.getName().toLowerCase().compareTo(second.getName().toLowerCase());
            }
            return result;
        }
    };

    public void loadProperties(String propertiesFileName) {
        if (propertiesFileName == null) {
            throw new IllegalArgumentException("Argument to loadProperties must not be null");
        }

        Properties props = new Properties();
        try {
            File f = new File(propertiesFileName);
            InputStream fileInputStream = new FileInputStream(f);
            try {
                props.load(fileInputStream);
            } finally {
                fileInputStream.close();
            }
        } catch (FileNotFoundException fnfe) {
            throw new IllegalArgumentException(propertiesFileName + " file does not exist");
        } catch (IOException ioe) {
            throw new IllegalStateException("Error loading properties from " + propertiesFileName + ": "
                + ioe.getMessage());
        }

    }

    public void loadProperties(Properties props) {
        this.confluenceUrl = props.getProperty("confluenceUrl", "http://rhq-project.org/");
        this.confluenceSpace = props.getProperty("confluenceSpace", "JOPR2");
        this.confluenceParentPageTitle = props.getProperty("confluenceParentPageTitle", "Management Plugins");
        this.confluenceUserName = props.getProperty("confluenceUserName");
        this.confluencePassword = props.getProperty("confluencePassword");
    }

    public void execute(String projectBaseDirName) throws PluginDocGeneratorException {
        File projectBaseDir = new File(projectBaseDirName);

        File pluginXmlFile = new File(projectBaseDir, PLUGIN_DESCRIPTOR_PATH);
        if (!pluginXmlFile.exists()) {
            log.info("'" + pluginXmlFile + "' does not exist - nothing to do.");
            return;
        }

        PluginDescriptor pluginDescriptor = parsePluginDescriptor(pluginXmlFile);
        PluginDescriptorProcessor descriptorProcessor = new PluginDescriptorProcessor(pluginDescriptor);
        Set<ResourceType> resourceTypes;
        try {
            resourceTypes = descriptorProcessor.processPluginDescriptor();
        } catch (InvalidPluginDescriptorException e) {
            throw new PluginDocGeneratorException("Failed to process plugin descriptor.", e);
        }

        String pluginName = pluginDescriptor.getName();
        log.info("Generating plugin docs for " + pluginName + " plugin...");
        File baseOutputDir = new File(projectBaseDir.getParentFile(), OUTPUT_DIR_PATH);

        generateConfluenceContent(descriptorProcessor, resourceTypes, baseOutputDir);
        generateDocbookContent(descriptorProcessor, resourceTypes, baseOutputDir);
    }

    private static Set<ResourceType> sortResourceTypes(Set<ResourceType> resourceTypes) {
        Set<ResourceType> orderedChildTypes = new TreeSet<ResourceType>(CHILD_RESOURCE_TYPES_SORTER);
        orderedChildTypes.addAll(resourceTypes);
        return orderedChildTypes;
    }

    private void generateConfluenceContent(PluginDescriptorProcessor descriptorProcessor,
                                           Set<ResourceType> resourceTypes, File baseOutputDir)
            throws PluginDocGeneratorException {
        String pluginName = descriptorProcessor.getPluginDescriptor().getName();
        System.out.println("Generating Confluence content for " + pluginName + " plugin...");

        File confluenceBaseOutputDir = new File(baseOutputDir, "confluence");
        File confluenceOutputDir = new File(confluenceBaseOutputDir, pluginName);
        confluenceOutputDir.mkdirs();

        Plugin plugin = descriptorProcessor.getPlugin();
        File confluencePluginOutputFile = generateConfluencePluginPage(resourceTypes, confluenceOutputDir, plugin);
        generateConfluenceResourceTypePages(null, resourceTypes, confluenceOutputDir, plugin);

        boolean publishConfluencePages = (this.confluenceUserName != null) && (this.confluencePassword != null);
        Confluence confluence;
        if (publishConfluencePages) {
            String endpointURL = this.confluenceUrl + "/rpc/xmlrpc";
            log.debug("Publishing plugin docs to Confluence endpoint [" + endpointURL + "]...");
            try {
                confluence = createConfluenceEndpoint(endpointURL);
            } catch (Exception e) {
                throw new RuntimeException("Failed to connect to Confluence endpoint [" + endpointURL + "].", e);
            }

            try {
                Page basePage = publishConfluenceBasePage(confluence);
                //removeDescendantPages(confluence, basePage);
                publishPluginPage(plugin, confluencePluginOutputFile, confluence, basePage);
                publishConfluenceResourceTypePages(null, resourceTypes, plugin, confluenceOutputDir, confluence);
            } catch (Exception e) {
                throw new RuntimeException("Failed to publish plugin docs to Confluence endpoint [" + endpointURL
                        + "].", e);
            } finally {
                try {
                    confluence.logout();
                } catch (SwizzleException e) {
                    log.error("Failed to logout from Confluence endpoint.", e);
                }
            }
        }
    }

    private void removeDescendantPages(Confluence confluence, Page basePage) throws Exception {
        List<PageSummary> descendantPages = confluence.getDescendents(basePage.getId());
        Page trashbin = getPage(confluence, "TRASHBIN");
        for (PageSummary descendantPage : descendantPages) {
            Page page = confluence.getPage(descendantPage);
            page.setParentId(trashbin.getId());
            page = storePage(confluence, page);
            confluence.removePage(page.getId());
        }
    }

    private File generateConfluencePluginPage(Set<ResourceType> resourceTypes, File confluenceOutputDir, Plugin plugin) {
        if (plugin.getHelp() != null) {
            String confluenceHelp = getConfluenceHelp(plugin.getHelpContentType(), plugin.getHelp());
            plugin.setHelpContentType("confluence");
            plugin.setHelp(confluenceHelp);
        }

        VelocityTemplateProcessor confluencePluginTemplateProcessor = new VelocityTemplateProcessor(
                CONFLUENCE_PLUGIN_TEMPLATE_RESOURCE_PATH, CONFLUENCE_MACRO_LIBRARY_RESOURCE_PATH,
                EscapeConfluenceReference.class);

        confluencePluginTemplateProcessor.getContext().put("rhqVersion", RHQ_VERSION);
        confluencePluginTemplateProcessor.getContext().put("plugin", plugin);
        confluencePluginTemplateProcessor.getContext().put("resourceTypes", resourceTypes);
        String confluenceOutputFileName = escapeFileName(getPageTitle(plugin) + ".wiki");
        File confluencePluginOutputFile = new File(confluenceOutputDir, confluenceOutputFileName);
        confluencePluginTemplateProcessor.processTemplate(confluencePluginOutputFile);

        return confluencePluginOutputFile;
    }

    private static String getConfluenceHelp(String helpContentType, String help) {
        String confluenceHelp;
        if (helpContentType.equals("confluence")) {
            confluenceHelp = help;
        } else if (helpContentType.endsWith("html")) {
            confluenceHelp = DocConverter.htmlToConfluence(help);
        } else {
            confluenceHelp = null;
        }

        if (confluenceHelp != null) {
            confluenceHelp = confluenceHelp.replaceAll(" [ ]+", " ");
            confluenceHelp = confluenceHelp.replaceAll("\n ", "\n");
            confluenceHelp = confluenceHelp.replaceAll(" \n", "\n");
            confluenceHelp = confluenceHelp.replaceAll("\n\n[\n]+", "\n\n");
            // TODO: Fix the below, which is supposed to replace single newlines with spaces.
            //confluenceHelp = confluenceHelp.replaceAll("\n([^\n\\*h])", " $1");
        }

        return confluenceHelp;
    }

    private static String getDocbookHelp(String helpContentType, String help) {
        String docbookHelp;
        if (helpContentType.contains("docbook")) {
            docbookHelp = help;
        } else if (helpContentType.endsWith("html")) {
            docbookHelp = DocConverter.htmlToDocBook(help);
        } else {
            docbookHelp = null;
        }

        return docbookHelp;
    }

    private void generateConfluenceResourceTypePages(ResourceType parentType, Set<ResourceType> resourceTypes, File confluenceOutputDir,
                                                     Plugin plugin) {
        VelocityTemplateProcessor confluenceResourceTypeTemplateProcessor = new VelocityTemplateProcessor(
                        CONFLUENCE_RESOURCE_TYPE_TEMPLATE_RESOURCE_PATH, CONFLUENCE_MACRO_LIBRARY_RESOURCE_PATH, EscapeConfluenceReference.class);

        // Traverse the type tree depth first.
        for (ResourceType resourceType : resourceTypes) {
            System.out.println("Generating Confluence content for " + resourceType + " Resource type...");
            Set<ResourceType> originalChildTypes = resourceType.getChildResourceTypes();
            Set<ResourceType> sortedChildTypes = sortResourceTypes(originalChildTypes);
            resourceType.setChildResourceTypes(sortedChildTypes);

            if (resourceType.getHelpText() != null) {
                String confluenceHelp = getConfluenceHelp(resourceType.getHelpTextContentType(), resourceType.getHelpText());
                resourceType.setHelpTextContentType("confluence");
                resourceType.setHelpText(confluenceHelp);
            }

            String pageTitle = getPageTitle(resourceType, parentType);
            confluenceResourceTypeTemplateProcessor.getContext().put("pageTitle", pageTitle);
            confluenceResourceTypeTemplateProcessor.getContext().put("resourceType", resourceType);
            File confluenceResourceTypeOutputFile = getConfluenceResourceTypeOutputFile(confluenceOutputDir, plugin,
                    resourceType);
            confluenceResourceTypeTemplateProcessor.processTemplate(confluenceResourceTypeOutputFile);
            resourceType.setChildResourceTypes(originalChildTypes);

            // Recurse on child types.
            generateConfluenceResourceTypePages(resourceType, resourceType.getChildResourceTypes(), confluenceOutputDir, plugin);
        }
    }

    private File getConfluenceResourceTypeOutputFile(File confluenceOutputDir, Plugin plugin, ResourceType resourceType) {
        String pageTitle = getPageTitle(resourceType, null);
        String confluenceOutputFileName = escapeFileName(pageTitle + ".wiki");
        return new File(confluenceOutputDir, confluenceOutputFileName);
    }

    private void publishConfluenceResourceTypePages(ResourceType parentType, Set<ResourceType> resourceTypes, Plugin plugin,
                                                    File confluenceOutputDir, Confluence confluence) throws PluginDocGeneratorException {
        // Traverse the type tree depth first.
        for (ResourceType resourceType : resourceTypes) {
            publishConfluenceResourceTypePage(confluenceOutputDir, plugin, resourceType, parentType, confluence);

            // Recurse on child types.
            publishConfluenceResourceTypePages(resourceType, resourceType.getChildResourceTypes(), plugin,
                    confluenceOutputDir, confluence);
        }
    }

    private void publishPluginPage(Plugin plugin, File confluencePluginOutputFile, Confluence confluence, Page basePage)
            throws Exception {
        String pluginPageTitle = getPageTitle(plugin);
        Page pluginPage = getPage(confluence, pluginPageTitle);
        if (pluginPage == null) {
            pluginPage = createPage(pluginPageTitle);
        } else {
            log.warn("Page with title '" + pluginPageTitle + "' already exists - overwriting it...");
        }
        pluginPage.setContent(getContentAsString(confluencePluginOutputFile));
        pluginPage.setParentId(basePage.getId());
        storePage(confluence, pluginPage);
    }

    private void generateDocbookContent(PluginDescriptorProcessor descriptorProcessor, Set<ResourceType> resourceTypes,
                                        File baseOutputDir) {
        String pluginName = descriptorProcessor.getPluginDescriptor().getName();
        System.out.println("Generating DocBook content for " + pluginName + " plugin...");

        File docbookBaseOutputDir = new File(baseOutputDir, "docbook");
        File docbookOutputDir = new File(docbookBaseOutputDir, descriptorProcessor.getPlugin().getName());
        docbookOutputDir.mkdirs();

        Plugin plugin = descriptorProcessor.getPlugin();
        generateDocbookPluginPage(resourceTypes, docbookOutputDir, plugin);
        generateDocbookResourceTypePages(null, resourceTypes, docbookOutputDir, plugin);
    }

    private File generateDocbookPluginPage(Set<ResourceType> resourceTypes, File docBookOutputDir, Plugin plugin) {
        if (plugin.getHelp() != null) {
            String docBookHelp = getDocbookHelp(plugin.getHelpContentType(), plugin.getHelp());
            plugin.setHelpContentType("docbook");
            plugin.setHelp(docBookHelp);
        }

        VelocityTemplateProcessor docbookPluginTemplateProcessor = new VelocityTemplateProcessor(
                DOCBOOK_PLUGIN_TEMPLATE_RESOURCE_PATH, DOCBOOK_MACRO_LIBRARY_RESOURCE_PATH,
                EscapeDocBookReference.class);

        docbookPluginTemplateProcessor.getContext().put("rhqVersion", RHQ_VERSION);
        docbookPluginTemplateProcessor.getContext().put("plugin", plugin);
        docbookPluginTemplateProcessor.getContext().put("resourceTypes", resourceTypes);
        String docbookOutputFileName = escapeFileName(getPageTitle(plugin) + ".xml");
        File docbookPluginOutputFile = new File(docBookOutputDir, docbookOutputFileName);
        docbookPluginTemplateProcessor.processTemplate(docbookPluginOutputFile);

        return docbookPluginOutputFile;
    }

    private void generateDocbookResourceTypePages(ResourceType parentType, Set<ResourceType> resourceTypes,
                                                  File docbookOutputDir, Plugin plugin) {
        VelocityTemplateProcessor docbookTemplateProcessor = new VelocityTemplateProcessor(
                DOCBOOK_RESOURCE_TYPE_TEMPLATE_RESOURCE_PATH, DOCBOOK_MACRO_LIBRARY_RESOURCE_PATH,
                EscapeDocBookReference.class);

        // Traverse the type tree depth first.
        for (ResourceType resourceType : resourceTypes) {
            String htmlHelpText = resourceType.getHelpText();
            String docBookHelpText = DocConverter.htmlToDocBook(htmlHelpText);
            resourceType.setHelpText(docBookHelpText);

            System.out.println("Generating DocBook content for " + resourceType + " Resource type...");

            docbookTemplateProcessor.getContext().put("resourceType", resourceType);
            String docbookOutputFileName = escapeFileName(getPageTitle(resourceType, parentType) + ".xml");
            File docbookOutputFile = new File(docbookOutputDir, docbookOutputFileName);
            docbookTemplateProcessor.processTemplate(docbookOutputFile);

            // Recurse on child types.
            generateDocbookResourceTypePages(resourceType, resourceType.getChildResourceTypes(), docbookOutputDir,
                    plugin);
        }
    }

    private PluginDescriptor parsePluginDescriptor(File pluginXmlFile) throws PluginDocGeneratorException {
        JAXBContext jaxbContext;
        try {
            jaxbContext = JAXBContext.newInstance(PLUGIN_DESCRIPTOR_JAXB_CONTEXT_PATH);
        } catch (JAXBException e) {
            throw new PluginDocGeneratorException("Failed to instantiate JAXB context for context path '"
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
            throw new PluginDocGeneratorException("Could not successfully parse plugin descriptor '" + pluginXmlFile
                + "'.", e);
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

    private void publishConfluenceResourceTypePage(File outputDir, Plugin plugin, ResourceType resourceType,
                                                   ResourceType parentType, Confluence confluence)
        throws PluginDocGeneratorException {
        System.out.println("*** Publishing plugin doc page for " + resourceType + " Resource type to Confluence...");
        String pageTitle = getPageTitle(resourceType, parentType);
        try {
            Page page = getPage(confluence, pageTitle);
            if (page == null) {
                page = createPage(pageTitle);
            } else {
                log.warn("Page with title '" + pageTitle + "' already exists - overwriting it...");
            }

            String parentPageTitle;
            if (resourceType.getParentResourceTypes().isEmpty()) {
                // root platform or server
                parentPageTitle = getPageTitle(plugin);
            } else {
                if (parentType == null) {
                    parentType = resourceType.getParentResourceTypes().iterator().next();
                }
                parentPageTitle = getPageTitle(parentType, null);
            }
            Page parentPage = getPage(confluence, parentPageTitle);
            if (parentPage != null) {
                page.setParentId(parentPage.getId());
            } else {
                log.warn("Parent page [" + parentPageTitle + "] for page [" + pageTitle + "] not found - page will have no parent for now.");
            }
            File contentFile = getConfluenceResourceTypeOutputFile(outputDir, plugin, resourceType);
            page.setContent(getContentAsString(contentFile));
            page = storePage(confluence, page);
        } catch (Exception e) {
            throw new PluginDocGeneratorException("Failed to publish page [" + pageTitle + "] to Confluence.", e);
        }
    }

    private Page publishConfluenceBasePage(Confluence confluence) throws Exception {
        Page baseParentPage = getPage(confluence, this.confluenceParentPageTitle);
        if (baseParentPage == null) {
            baseParentPage = createPage(this.confluenceParentPageTitle);
        } else {
            log.warn("Page with title '" + this.confluenceParentPageTitle + "' already exists - overwriting it...");
        }
        baseParentPage.setContent("{children}\n");
        Page homePage = getPage(confluence, "Home");
        baseParentPage.setParentId(homePage.getId());
        baseParentPage = storePage(confluence, baseParentPage);

        return baseParentPage;
    }

    private Confluence createConfluenceEndpoint(String endpoint) throws MalformedURLException, SwizzleException {
        Confluence confluence = new Confluence(endpoint);
        confluence.login(this.confluenceUserName, this.confluencePassword);
        return confluence;
    }

    private Page storePage(Confluence confluence, Page page) throws Exception {
        Page parentPage = getParentPage(confluence, page);
        String parentPageTitle = (parentPage != null) ? parentPage.getTitle() : null;

        System.out.println("Storing page [" + page.getTitle() + "] with parent page [" + parentPageTitle + "]...");
        Page storedPage;
        try {
            storedPage = confluence.storePage(page);
        } catch (SwizzleException e) {
            throw new Exception("Failed to publish page [" + page.getTitle() + "] with parent page [" + parentPageTitle
                    + "]: " + e.getMessage());
        }

        parentPage = getParentPage(confluence, storedPage);
        parentPageTitle = (parentPage != null) ? parentPage.getTitle() : null;
        System.out.println("Stored page [" + storedPage.getTitle() + "] with ID [" + storedPage.getId()
                + "] and parent page [" + parentPageTitle + "].");
        return storedPage;
    }

    private Page getParentPage(Confluence confluence, Page page) throws SwizzleException {
        return (page.getParentId() != null) ? getPage(confluence, page.getParentId()) : null;
    }

    private Page createPage(String pageTitle) {
        System.out.println("Creating page [" + pageTitle + "]...");
        Page page = new Page();
        page.setSpace(this.confluenceSpace);
        page.setTitle(pageTitle);
        page.setContent("{children}\n");
        return page;
    }

    private Page getPage(Confluence confluence, String pageTitle) throws SwizzleException {
        try {
            return confluence.getPage(this.confluenceSpace, pageTitle);
        } catch (SwizzleException e) {
            // This either means the page doesn't exist or that we don't have access to view it.
            return null;
        }
    }

    private static String getPageTitle(Plugin plugin) {
        String title = (plugin.getDisplayName() != null) ? plugin.getDisplayName() : plugin.getName();
        if (!title.endsWith(" Plugin")) {
            title += " Plugin";
        }
        return escapePageTitle(title);
    }

    private static String getPageTitle(ResourceType resourceType, ResourceType parentType) {
        StringBuilder buffer = new StringBuilder();

        String pluginName = resourceType.getPlugin();
        buffer.append(pluginName).append(" - ");

        Set<ResourceType> parentTypes = resourceType.getParentResourceTypes();
        if ((parentType != null) && (parentTypes != null) && (parentTypes.size() > 1)) {
            // it has multiple parent types, so we'll need to include the parent type in the title
            String parentTypeName = parentType.getName();
            if (parentTypeName.startsWith(pluginName + " ")) {
                buffer.append(parentTypeName.substring(pluginName.length() + 1));
            } else {
                buffer.append(parentTypeName);
            }
            buffer.append(" - ");
        }

        String typeName = resourceType.getName();
        if (typeName.startsWith(pluginName + " ")) {
            buffer.append(typeName.substring(pluginName.length() + 1));
        } else {
            buffer.append(typeName);
        }

        if (!typeName.endsWith(" " + resourceType.getCategory())) {
            buffer.append(' ').append(resourceType.getCategory());
        }

        return escapePageTitle(buffer.toString());
    }

    private static String getContentAsString(File contentFile) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(contentFile)));
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } finally {
            bufferedReader.close();
        }
        return content.toString();
    }

    private static String escapeFileName(String fileName) {
        // DocBook doesn't like parentheses or dashes in filenames.
        fileName = fileName.replaceAll("\\(", "").replaceAll("\\)", "").replace('-', ':');

        // Remove other characters that are generally undesirable in filenames.
        return fileName.replace('/', '_').replace('\\', '_').replace(' ', '_');
    }

    private static String escapePageTitle(String fileName) {
        return fileName.replace('/', '-');
    }

    public static void main(String[] args) throws PluginDocGeneratorException {
        if (args.length < 1 || args.length > 2) {
            System.out.println("Usage: " + PluginDocGenerator.class.getSimpleName()
                + " projectBaseDirName [propertiesFile]");
            return;
        }

        PluginDocGenerator generator = new PluginDocGenerator();
        if (args.length == 2) {
            String propertiesFile = args[1];
            generator.loadProperties(propertiesFile);
        }

        String projectBaseDir = args[0];
        generator.execute(projectBaseDir);
    }

}
