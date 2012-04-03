package org.rhq.core.tool.plugindoc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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
import org.codehaus.swizzle.confluence.SwizzleException;

import org.rhq.core.clientapi.agent.metadata.InvalidPluginDescriptorException;
import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;

/**
 * @author Ian Springer
 */
public class PluginDocGenerator {

    private final Log log = LogFactory.getLog(this.getClass());

    private static final String PLUGIN_DESCRIPTOR_PATH = "src/main/resources/META-INF/rhq-plugin.xml";
    private static final String OUTPUT_DIR_PATH = "target/plugindoc";
    private static final String PLUGIN_DESCRIPTOR_JAXB_CONTEXT_PATH = "org.rhq.core.clientapi.descriptor.plugin";
    private static final String CONFLUENCE_TEMPLATE_RESOURCE_PATH = "resource-type-doc-confluence.vm";
    private static final String DOCBOOK_TEMPLATE_RESOURCE_PATH = "resource-type-doc-docbook.vm";
    private static final String CONFLUENCE_MACRO_LIBRARY_RESOURCE_PATH = "confluence-macros.vm";
    private static final String DOCBOOK_MACRO_LIBRARY_RESOURCE_PATH = "docbook-macros.vm";

    private String confluenceUrl; // The main Confluence URL (e.g. "http://rhq-project.org/").
    private String confluenceSpace; // The Confluence space (e.g. "JOPR2").
    private String confluenceParentPageTitle; // The Confluence parent page name (e.g. "Managed Resources").
    private String confluenceUserName;
    private String confluencePassword;

    private static final Comparator<ResourceType> CHILD_RESOURCE_TYPES_SORTER = new Comparator<ResourceType>() {
        public int compare(ResourceType first, ResourceType second) {
            return first.getName().toLowerCase().compareTo(second.getName().toLowerCase());
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

        this.confluenceUrl = props.getProperty("confluenceUrl", "http://rhq-project.org/");
        this.confluenceSpace = props.getProperty("confluenceSpace", "JOPR2");
        this.confluenceParentPageTitle = props.getProperty("confluenceParentPageTitle", "Managed Resources");
        this.confluenceUserName = props.getProperty("confluenceUserName");
        this.confluencePassword = props.getProperty("confluencePassword");
    }

    public void setConfluenceProperties(String url, String space, String parentPageTitle, String userName,
        String password) {
        this.confluenceUrl = url;
        this.confluenceSpace = space;
        this.confluenceParentPageTitle = parentPageTitle;
        this.confluenceUserName = userName;
        this.confluencePassword = password;
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

        File confluenceBaseOutputDir = new File(baseOutputDir, "confluence");
        File confluenceOutputDir = new File(confluenceBaseOutputDir, pluginName);
        confluenceOutputDir.mkdirs();

        File docbookBaseOutputDir = new File(baseOutputDir, "docbook");
        File docbookOutputDir = new File(docbookBaseOutputDir, pluginName);
        docbookOutputDir.mkdirs();

        VelocityTemplateProcessor confluenceTemplateProcessor = new VelocityTemplateProcessor(
            CONFLUENCE_TEMPLATE_RESOURCE_PATH, CONFLUENCE_MACRO_LIBRARY_RESOURCE_PATH, EscapeConfluenceReference.class);
        VelocityTemplateProcessor docbookTemplateProcessor = new VelocityTemplateProcessor(
            DOCBOOK_TEMPLATE_RESOURCE_PATH, DOCBOOK_MACRO_LIBRARY_RESOURCE_PATH, EscapeDocBookReference.class);

        // fixes for documentation generation
        for (ResourceType resourceType : resourceTypes) {
            // alphasort the childResourceTypes
            Set<ResourceType> orderedChildTypes = new LinkedHashSet<ResourceType>();
            List<ResourceType> childTypes = new ArrayList<ResourceType>(resourceType.getChildResourceTypes());
            Collections.sort(childTypes, CHILD_RESOURCE_TYPES_SORTER);
            for (ResourceType sortedType : childTypes) {
                orderedChildTypes.add(sortedType);
            }
            resourceType.setChildResourceTypes(orderedChildTypes);
        }

        // generate content for Confluence
        String endpoint;
        if ((this.confluenceUserName != null) && (this.confluencePassword != null)) {
            log.debug("Using Confluence URL: " + this.confluenceUrl);
            endpoint = this.confluenceUrl + "/rpc/xmlrpc";
        } else {
            endpoint = null;
        }

        for (ResourceType resourceType : resourceTypes) {
            String htmlHelpText = resourceType.getHelpText();
            String confluenceHelpText = DocConverter.htmlToConfluence(htmlHelpText);
            resourceType.setHelpText(confluenceHelpText);

            log.info("Generating Confluence content for " + resourceType + " Resource type...");

            confluenceTemplateProcessor.getContext().put("resourceType", resourceType);
            String confluenceOutputFileName = escapeFileName(resourceType.getName() + ".wiki");
            File confluenceOutputFile = new File(confluenceOutputDir, confluenceOutputFileName);
            confluenceTemplateProcessor.processTemplate(confluenceOutputFile);

            if ((this.confluenceUserName != null) && (this.confluencePassword != null)) {
                publishPage(confluenceOutputFile, resourceType, endpoint);
            }
        }

        // generate content for docbook
        for (ResourceType resourceType : resourceTypes) {
            String htmlHelpText = resourceType.getHelpText();
            String docBookHelpText = DocConverter.htmlToDocBook(htmlHelpText);
            resourceType.setHelpText(docBookHelpText);

            log.info("Generating Docbook content for " + resourceType + " Resource type...");

            docbookTemplateProcessor.getContext().put("resourceType", resourceType);
            String docbookOutputFileName = escapeFileName(resourceType.getName() + ".xml");
            File docbookOutputFile = new File(docbookOutputDir, docbookOutputFileName);
            docbookTemplateProcessor.processTemplate(docbookOutputFile);
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

    private void publishPage(File contentFile, ResourceType resourceType, String endpoint)
        throws PluginDocGeneratorException {
        System.out.println("*** Publishing plugin doc page for " + resourceType + " Resource type to Confluence...");
        String pageTitle = getPageTitle(resourceType);
        try {
            Confluence confluence = new Confluence(endpoint);
            confluence.login(this.confluenceUserName, this.confluencePassword);
            Page page = getPage(confluence, pageTitle);
            if (page == null) {
                page = createPage(pageTitle);
            } else {
                log.warn("Page with title '" + pageTitle + "' already exists - overwriting it...");
            }
            if (this.confluenceParentPageTitle != null) {
                Page baseParentPage = getPage(confluence, this.confluenceParentPageTitle);
                if (baseParentPage == null) {
                    log.error("Specified parent page ('" + this.confluenceParentPageTitle
                            + "') does not exist - creating it...");
                    baseParentPage = createPage(this.confluenceParentPageTitle);
                    baseParentPage.setContent("{children:depth=3}\n");
                    baseParentPage = storePage(confluence, baseParentPage);
                }
                String parentPageTitle;
                boolean topLevelParentPage;
                if (resourceType.getCategory() == ResourceCategory.PLATFORM) {
                    parentPageTitle = "Managed Platforms";
                    topLevelParentPage = true;
                } else if (resourceType.getParentResourceTypes().isEmpty()) {
                    if (resourceType.getCategory() == ResourceCategory.SERVER) {
                        parentPageTitle = "Managed Servers";
                    } else {
                        throw new IllegalStateException("Service type " + resourceType + " has no parent types.");
                    }
                    topLevelParentPage = true;
                } else {
                    ResourceType parentType = resourceType.getParentResourceTypes().iterator().next();
                    if (resourceType.getPlugin().equals("Platforms") &&
                            (parentType.getCategory() == ResourceCategory.PLATFORM)) {
                        parentPageTitle = "Managed Platform Services";
                        topLevelParentPage = true;
                    } else {
                        parentPageTitle = getPageTitle(parentType);
                        topLevelParentPage = false;
                    }
                }
                Page parentPage = getPage(confluence, parentPageTitle);
                if (parentPage == null) {
                    parentPage = createPage(parentPageTitle);
                }
                if (topLevelParentPage) {
                    parentPage.setParentId(baseParentPage.getId());
                }
                parentPage = storePage(confluence, parentPage);
                System.out.println("Using parent page [" + parentPage.getTitle() + "] with ID [" + parentPage.getId()
                        + "] for page [" + page.getTitle() + "]...");
                page.setParentId(parentPage.getId());
            }
            page.setContent(getContentAsString(contentFile));
            page = storePage(confluence, page);
            confluence.logout();
        } catch (Exception e) {
            throw new PluginDocGeneratorException("Failed to publish page [" + pageTitle + "] to Confluence.", e);
        }
    }

    private Page storePage(Confluence confluence, Page page) throws Exception {
        System.out.println("Storing page [" + page.getTitle() + "] with parent ID [" + page.getParentId() + "]...");
        Page storedPage;
        try {
            storedPage = confluence.storePage(page);
        } catch (SwizzleException e) {
            throw new Exception("Failed to publish page [" + page.getTitle() + "] with parent ID [" + page.getParentId()
                    + "]: " + e.getMessage());
        }
        System.out.println("Stored page [" + storedPage.getTitle() + "] with ID [" + storedPage.getId()
                + "] and parent ID [" + storedPage.getParentId() + "].");
        return storedPage;
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

    private static String getPageTitle(ResourceType resourceType) {
        String title = resourceType.getName();
        if (!resourceType.getName().endsWith(resourceType.getCategory().toString())) {
            title += " " + resourceType.getCategory();
        }
        title += " (" + resourceType.getPlugin() + ")";
        return escapePageTitle(title);
    }

    private static String getContentAsString(File contentFile) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(contentFile)));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            content.append(line).append("\n");
        }
        bufferedReader.close();
        return content.toString();
    }

    private static String escapeFileName(String fileName) {
        // DocBook doesn't like parentheses or dashes in filenames.
        fileName = fileName.replaceAll("\\(", "").replaceAll("\\)", "").replace('-', '_');

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
