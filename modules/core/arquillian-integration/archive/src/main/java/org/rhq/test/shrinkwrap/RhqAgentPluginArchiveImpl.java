/**
 * 
 */
package org.rhq.test.shrinkwrap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.EventFilter;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.asset.ArchiveAsset;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.ClassLoaderAsset;
import org.jboss.shrinkwrap.api.asset.FileAsset;
import org.jboss.shrinkwrap.api.asset.UrlAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.impl.base.Validate;
import org.jboss.shrinkwrap.impl.base.container.ContainerBase;
import org.jboss.shrinkwrap.impl.base.path.BasicPath;

import org.rhq.core.clientapi.agent.metadata.PluginDependencyGraph;
import org.rhq.core.util.stream.StreamUtil;

/**
 * @author Lukas Krejci
 */
public class RhqAgentPluginArchiveImpl extends ContainerBase<RhqAgentPluginArchive> implements RhqAgentPluginArchive {

    private static final Log LOG = LogFactory.getLog(RhqAgentPluginArchiveImpl.class);
    
    /**
     * Path to the manifests inside of the Archive.
     */
    private static final ArchivePath PATH_MANIFEST = new BasicPath("META-INF");

    /**
     * Path to the resources inside of the Archive.
     */
    private static final ArchivePath PATH_RESOURCE = new BasicPath("/");

    /**
     * Path to the classes inside of the Archive.
     */
    private static final ArchivePath PATH_CLASSES = new BasicPath("/");

    /**
     * Path to the libraries inside the RHQ agent plugin archive
     */
    private static final ArchivePath LIBRARY_PATH = new BasicPath("/lib");

    private static final ArchivePath REQUIRED_PLUGINS_PATH = new BasicPath(PATH_MANIFEST, "######REQUIRED_PLUGINS");
    
    private static final ArchivePath PLUGIN_DESCRIPTOR_PATH = new BasicPath(PATH_MANIFEST, "rhq-plugin.xml");
    
    private static class PluginDep extends PluginDependencyGraph.PluginDependency {

        public String name;
        
        /**
         * @param name
         */
        public PluginDep(String name) {
            super(name);
            this.name = name;
        }

        public PluginDep(String name, boolean useClasses, boolean required) {
            super(name, useClasses, required);
            this.name = name;
        }
        
    }
    
    public RhqAgentPluginArchiveImpl(Archive<?> delegate) {
        super(RhqAgentPluginArchive.class, delegate);
    }

    @Override
    public ArchivePath getRequiredPluginsPath() {
        return REQUIRED_PLUGINS_PATH;
    }
    
    @Override
    protected ArchivePath getManifestPath() {
        return PATH_MANIFEST;
    }

    @Override
    protected ArchivePath getClassesPath() {
        return PATH_CLASSES;
    }

    @Override
    protected ArchivePath getResourcePath() {
        return PATH_RESOURCE;
    }

    @Override
    public ArchivePath getLibraryPath() {
        return LIBRARY_PATH;
    }

    @Override
    public RhqAgentPluginArchive setPluginDescriptorFromTemplate(String resourceName,
        Map<String, String> replacementValues) throws IllegalArgumentException {

        Validate.notNull(resourceName, "resourceName should be specified");
        Validate.notNull(replacementValues, "replacementValues should be specified");

        // get the template text and replace all tokens with their replacement values
        String templateXml = new String(StreamUtil.slurp(new ClassLoaderAsset(resourceName).openStream()));
        for (Map.Entry<String, String> entry : replacementValues.entrySet()) {
            templateXml = templateXml.replace(entry.getKey(), entry.getValue());
        }

        // Make a new descriptor file with the new template content (with variables replaced) in a tmp directory.
        File newDescriptorFile;
        try {
            newDescriptorFile = File.createTempFile(resourceName.replace(".xml", ""), ".xml");
            StreamUtil.copy(new ByteArrayInputStream(templateXml.getBytes()), new FileOutputStream(newDescriptorFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return setPluginDescriptor(newDescriptorFile);
    }

    @Override
    public RhqAgentPluginArchive setPluginDescriptor(String resourceName) throws IllegalArgumentException {
        Validate.notNull(resourceName, "ResourceName should be specified");
        return setPluginDescriptor(new ClassLoaderAsset(resourceName));
    }

    @Override
    public RhqAgentPluginArchive setPluginDescriptor(File file) throws IllegalArgumentException {
        Validate.notNull(file, "File should be specified");
        return setPluginDescriptor(new FileAsset(file));
    }

    @Override
    public RhqAgentPluginArchive setPluginDescriptor(URL url) throws IllegalArgumentException {
        Validate.notNull(url, "URL should be specified");
        return setPluginDescriptor(new UrlAsset(url));
    }

    @Override
    public RhqAgentPluginArchive setPluginDescriptor(Asset asset) throws IllegalArgumentException {
        Validate.notNull(asset, "Asset should be specified");
        return add(asset, PLUGIN_DESCRIPTOR_PATH);
    }

    @Override
    public RhqAgentPluginArchive withRequiredPluginsFrom(Collection<? extends Archive<?>> archives)
        throws IllegalArgumentException {
        
        delete(REQUIRED_PLUGINS_PATH);
        
        Node descriptor = get(PLUGIN_DESCRIPTOR_PATH);
        if (descriptor == null) {
            return this;
        }
        
        Map<String, Archive<?>> plugins = new HashMap<String, Archive<?>>();
                
        String pluginName = getPluginName(this);    
        String myPluginName = pluginName;
        plugins.put(pluginName, this);
        
        for(Archive<?> ar : archives) {
            if (isPlugin(ar)) {
                pluginName = getPluginName(ar);                
                plugins.put(pluginName, ar);
            }
        }
        
        PluginDependencyGraph pdg = new PluginDependencyGraph();
        
        buildRequiredPlugins(this, plugins, pdg);
        
        for(String pn : pdg.getAllDependencies(myPluginName)) {
            Archive<?> p = plugins.get(pn);
            add(p, REQUIRED_PLUGINS_PATH, ZipExporter.class);
        }
        
        return this;
    }
    
    @Override
    public RhqAgentPluginArchive withRequiredPluginsFrom(Archive<?>... archives) throws IllegalArgumentException {
        return withRequiredPluginsFrom(Arrays.asList(archives));
    }

    @Override
    public List<Archive<?>> getRequiredPlugins() {      
        Node requiredPluginsRoot = get(REQUIRED_PLUGINS_PATH);
        if (requiredPluginsRoot == null) {
            return null;
        }
        
        List<Archive<?>> ret = new ArrayList<Archive<?>>();
        for(Node plugin : requiredPluginsRoot.getChildren()) {
           Asset a = plugin.getAsset();
           if (a instanceof ArchiveAsset) {
               ret.add(((ArchiveAsset) a).getArchive());
           }
        }
        
        return ret;
    }    

    //use this only in readonly scenarios
    @SuppressWarnings("unchecked")
    private List<PluginDependencyGraph.PluginDependency> asPdgPd(List<? extends PluginDependencyGraph.PluginDependency> source) {
        return (List<PluginDependencyGraph.PluginDependency>) source;
    }
    
    private void buildRequiredPlugins(Archive<?> archive, Map<String, Archive<?>> allAvailableArchives, PluginDependencyGraph graph) {
        List<PluginDep> requiredPlugins = getRequiredPlugins(archive);
        String pluginName = getPluginName(archive);
        
        graph.addPlugin(pluginName, asPdgPd(requiredPlugins));
        
        for(PluginDep pd : requiredPlugins) {
            Archive<?> a = allAvailableArchives.get(pd.name);
            buildRequiredPlugins(a, allAvailableArchives, graph);
        }
    }
    
    private static void closeReaderAndStream(XMLEventReader rdr, InputStream str, Archive<?> archive) {
        if (rdr != null) {
            try {
                rdr.close();
            } catch (XMLStreamException e) {
                LOG.error("Failed to close the XML reader of the plugin descriptor in archive [" + archive + "]", e);
            }
        }
        
        try {
            str.close();
        } catch (IOException e) {
            LOG.error("Failed to close the input stream of the plugin descriptor in archive [" + archive + "]", e);
        }
    }
    
    private static List<PluginDep> getRequiredPlugins(Archive<?> archive) {
        List<PluginDep> ret = new ArrayList<PluginDep>();
        
        InputStream is = archive.get(PLUGIN_DESCRIPTOR_PATH).getAsset().openStream();
        XMLEventReader rdr = null;
        try {
            XMLInputFactory f = XMLInputFactory.newInstance();
            XMLEventReader r = f.createXMLEventReader(is);
            
            rdr = f.createFilteredReader(r, new EventFilter() {                
                @Override
                public boolean accept(XMLEvent event) {
                    switch (event.getEventType()) {
                    case XMLEvent.START_ELEMENT:
                    case XMLEvent.END_DOCUMENT:
                        return true;
                    default:
                        return false;
                    }                    
                }
            });
            
            XMLEvent e;
            while ((e = rdr.nextEvent()) != null) {
                if (e.getEventType() == XMLEvent.END_DOCUMENT) {
                    break;
                }
                
                StartElement el = e.asStartElement();
                
                if (el.getName().getLocalPart().equals("depends")) {
                    Attribute pluginAttr = el.getAttributeByName(new QName("plugin"));
                    if (pluginAttr == null) {
                        throw new IllegalArgumentException("Couldn't find the 'plugin' attribute on a 'depends' element in the plugin descriptor of plugin '" + archive + "'.");
                    }
                    
                    String pluginName = pluginAttr.getValue();
                    boolean required = true;
                    boolean useClasses = false;
                    
                    Attribute requiredAttr = el.getAttributeByName(new QName("required"));
                    if (requiredAttr != null) {
                        required = Boolean.parseBoolean(requiredAttr.getValue());
                    }
                    
                    Attribute useClassesAttr = el.getAttributeByName(new QName("useClasses"));
                    if (useClassesAttr != null) {
                        useClasses = Boolean.parseBoolean(useClassesAttr.getValue());
                    }
                    
                    ret.add(new PluginDep(pluginName, useClasses, required));
                }
            }
            
            return ret;
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Failed to extract the required plugin names out of the RHQ plugin archive [" + archive + "]", e);
        } catch (FactoryConfigurationError e) {
            throw new IllegalArgumentException("Failed to extract the required plugin name out of the RHQ plugin archive [" + archive + "]", e);
        } finally {
            closeReaderAndStream(rdr, is, archive);
        }
    }
    
    private static boolean isPlugin(Archive<?> archive) {
        return archive.contains(PLUGIN_DESCRIPTOR_PATH);
    }
    
    private static String getPluginName(Archive<?> archive) {
        InputStream is = archive.get(PLUGIN_DESCRIPTOR_PATH).getAsset().openStream();
        XMLEventReader rdr = null;
        try {
            rdr = XMLInputFactory.newInstance().createXMLEventReader(is);

            XMLEvent event = null;
            while (rdr.hasNext()) {
                event = rdr.nextEvent();
                if (event.getEventType() == XMLEvent.START_ELEMENT) {
                    break;
                }
            }

            StartElement startElement = event.asStartElement();
            String tagName = startElement.getName().getLocalPart();
            if (!"plugin".equals(tagName)) {
                throw new IllegalArgumentException("Illegal start tag found in the plugin descriptor. Expected 'plugin' but found '" + tagName + "' in the plugin '" + archive + "'.");
            }
            
            Attribute nameAttr = startElement.getAttributeByName(new QName("name"));
            
            if (nameAttr == null) {
                throw new IllegalArgumentException("Couldn't find the name attribute on the plugin tag in the plugin descriptor of plugin '" + archive + "'.");
            }
            
            return nameAttr.getValue();
        } catch (XMLStreamException e) {
            throw new IllegalArgumentException("Failed to extract the plugin name out of the RHQ plugin archive [" + archive + "]", e);
        } catch (FactoryConfigurationError e) {
            throw new IllegalArgumentException("Failed to extract the plugin name out of the RHQ plugin archive [" + archive + "]", e);
        } finally {
            closeReaderAndStream(rdr, is, archive);
        }
    }
}
