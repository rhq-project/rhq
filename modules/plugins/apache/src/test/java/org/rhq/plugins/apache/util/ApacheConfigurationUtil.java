package org.rhq.plugins.apache.util;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.rhq.augeas.node.AugeasNode;
import org.rhq.augeas.tree.AugeasTree;
import org.rhq.core.clientapi.agent.metadata.PluginMetadataManager;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.pc.PluginContainer;
import org.rhq.core.pc.plugin.PluginManager;
import org.rhq.plugins.apache.ApacheTestConstants;
import org.rhq.plugins.apache.Components;
import org.rhq.plugins.apache.UnitTestException;
import org.rhq.plugins.apache.mapping.ApacheAugeasMapping;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 *
 * @author Filip Drabek
 *
 */

public class ApacheConfigurationUtil {

    public static final String SIMPLE_TAG_NAME = "simple";
    public static final String LIST_TAG_NAME = "list";
    public static final String MAP_TAG_NAME = "map";
    public static final String ROOT_ELEMENT = "configuration";
    public static final String NAME_ATTRIBUTE = "name";

      private Document loadXML(String file) throws UnitTestException{
          Document document= null;
          try {
              File xmlFile = new File(file);
              if (!xmlFile.exists())
                  throw new UnitTestException("Configuration file not found.");

              DocumentBuilderFactory factory =
              DocumentBuilderFactory.newInstance();
              DocumentBuilder loader = factory.newDocumentBuilder();

             document = loader.parse(file);

          }catch(Exception e){
              throw new UnitTestException("Loading of xml file failed.",e);
          }

          return document;
      }

      public static void saveConfiguration(Configuration config,String fileName) throws UnitTestException{
          try {
          Collection<Property> properties = config.getProperties();
          DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
          DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
          Document doc = docBuilder.newDocument();
          Element element = doc.createElement(ROOT_ELEMENT);

          for (Property property : properties){
               element.appendChild(configurationToDom(property,doc));
          }
          doc.appendChild(element);
          saveXML(doc,fileName);

          }catch(Exception e){
              throw new UnitTestException(e);
          }
      }


      private static Element configurationToDom(Property prop,Document doc){
          Element element = null;
          if (prop instanceof PropertySimple){
              element = doc.createElement(SIMPLE_TAG_NAME);
              element.appendChild(doc.createTextNode(((PropertySimple) prop).getStringValue()));
          }

          if (prop instanceof PropertyList){
              PropertyList list = (PropertyList) prop;
              element = doc.createElement(LIST_TAG_NAME);
              List<Property> propertyList = list.getList();
              for (Property property : propertyList){
                  element.appendChild(configurationToDom(property,doc));
              }
          }

          if (prop instanceof PropertyMap){
              PropertyMap map = (PropertyMap) prop;
              element = doc.createElement(MAP_TAG_NAME);
              Map<String,Property> propertyMap = map.getMap();
              for (Property property : propertyMap.values()){
                  element.appendChild(configurationToDom(property,doc));
              }
          }

          fillElement(element,prop);
          return element;
      }

      private static void fillElement(Element element,Property prop){
          element.setAttribute(NAME_ATTRIBUTE, prop.getName());
      }

      public Configuration loadConfiguration(String fileName) throws UnitTestException{
          Document document = loadXML(fileName);
          NodeList nodeList = document.getElementsByTagName(ROOT_ELEMENT);
          if (nodeList.getLength()!=1)
              throw new UnitTestException("Configuration file's " + fileName+ " format is not valid.");

          Node rootElement = nodeList.item(0);
          Configuration configuration = new Configuration();
          NodeList childNodes =  rootElement.getChildNodes();
          for (int i=0;i<childNodes.getLength();i++){
            configuration.put(domToConfiguration(childNodes.item(i)));
            }
          return configuration;
          }

      private Property domToConfiguration(Node node){
          String nodeName = node.getNodeName();
           NamedNodeMap attrMap = node.getAttributes();
           Node attrNode = attrMap.getNamedItem(NAME_ATTRIBUTE);
          String propertyName = attrNode.getNodeValue();

          Property prop = null;

          if (nodeName.equals(SIMPLE_TAG_NAME)){
                PropertySimple propertySimple = new PropertySimple(propertyName,null);
                String value = node.getTextContent();
                propertySimple.setStringValue(value);
                prop = propertySimple;
             }

          if (nodeName.equals(LIST_TAG_NAME)){
                 PropertyList propertyList = new PropertyList(propertyName);
                 NodeList list = node.getChildNodes();
                 for (int i=0;i<list.getLength();i++) {
                      propertyList.add(domToConfiguration(list.item(i)));
                  }
                 prop = propertyList;
              }

          if (nodeName.equals(MAP_TAG_NAME)){
                PropertyMap propertyMap = new PropertyMap(propertyName);
                NodeList list = node.getChildNodes();
                for (int i=0;i<list.getLength();i++){
                  propertyMap.put(domToConfiguration(list.item(i)));
                }
                prop = propertyMap;
             }

          return prop;
      }

    @SuppressWarnings("restriction")
    private static void saveXML(Document document,String fileName) throws UnitTestException{
          try {
              File file = new File(fileName);
              if (!file.exists())
                  file.createNewFile();

            XMLSerializer serializer = new XMLSerializer();
            serializer.setOutputCharStream(
              new java.io.FileWriter(fileName));
            serializer.serialize(document);
          }catch(Exception e){
            throw new UnitTestException("Saving of xml file failed",e);
          }
      }

    public static void printConfiguration(Property prop){
        if (prop instanceof PropertySimple){
           System.out.println("    SimpleProperty name="+prop.getName()+" value="+((PropertySimple)prop).getStringValue());
        }

        if (prop instanceof PropertyList){
            PropertyList list = (PropertyList) prop;
            System.out.println("PropertyList name="+list.getName());
            for (Property property : list.getList()){
                printConfiguration(property);
            }
        }

        if (prop instanceof PropertyMap){
            PropertyMap map = (PropertyMap) prop;
            System.out.println(" PropertyMap name="+map.getName());
            Map<String,Property> propertyMap = map.getMap();
            for (Property property : propertyMap.values()){
                printConfiguration(property);
            }
        }
    }


    public static ConfigurationDefinition getConfigurationDefinition(PluginContainer container,Components component){
        PluginManager pluginManager = container.getPluginManager();
        PluginMetadataManager pluginMetadataManager = pluginManager.getMetadataManager();

        ResourceType type =  pluginMetadataManager.getType(component.getComponentName(), ApacheTestConstants.PLUGIN_NAME);
        ConfigurationDefinition configDef = type.getResourceConfigurationDefinition();
        return configDef;
      }


      public static Configuration componentToConfiguration(PluginContainer container,Components component,String key,AugeasTree tree) throws UnitTestException{
            ConfigurationDefinition def = getConfigurationDefinition(container,component);
            AugeasNode node = AugeasNodeSearch.findNodeById(tree.getRootNode(), key);
            ApacheAugeasMapping map = new ApacheAugeasMapping(tree);
            Configuration config = map.updateConfiguration(node, def);
            return config;
        }
}
