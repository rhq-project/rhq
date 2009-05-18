/*
* Jopr Management Platform
* Copyright (C) 2005-2009 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.rhq.plugins.jbossas5.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.Nullable;
import org.rhq.plugins.jbossas5.helper.JBossProperties;
import org.xml.sax.Attributes;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import org.jboss.util.StringPropertyReplacer;

/**
 * This class will parse the passed File in the getConfig method. This file is normally the jboss-service.xml file in
 * the conf directory of the JBoss Server.
 * <p/>
 * Using a SaxParser it will look for the MBean tag for Naming, when it finds that it is in that tag, it will then look
 * for the Port attribute to determine the jnp port
 * <p/>
 * The JBossServerHandler class handles the searching, by determining when the SAX parser has started reading the
 * element that has the text that is being searched, and when that element has ended.
 * <p/>
 * Config parsing intended only for JBoss server auto discovery.
 */
public class JnpConfig
{
    private static Log log = LogFactory.getLog(JnpConfig.class);

    static final String PROPERTY_EXPRESSION_PREFIX = "${";
    private static final HashMap<File, JnpConfig> CACHE = new HashMap<File, JnpConfig>();

    private String jnpAddress;
    private Integer jnpPort;
    private long lastModified = 0;

    private String serverName;
    private File storeFile;
    private Properties systemProperties;

    private JnpConfig(Properties systemProperties)
    {
        this.systemProperties = systemProperties;
    }

    public static synchronized JnpConfig getConfig(final File distributionDirectory, File configXML,
                                                   Properties systemProperties)
    {

        JnpConfig config = CACHE.get(configXML);

        long lastModified = configXML.lastModified();

        if ((config == null) || (lastModified != config.lastModified))
        {
            config = new JnpConfig(systemProperties);
            config.lastModified = lastModified;
            CACHE.put(configXML, config);

            try
            {
                config.read(distributionDirectory, configXML);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return config;
    }

    /**
     * Returns the JNP port, or null if the port could not be determined.
     *
     * @return the JNP port, or null if the port could not be determined
     */
    @Nullable
    public Integer getJnpPort()
    {
        return this.jnpPort;
    }

    /**
     * Returns the JNP address, or null if the address could not be determined.
     *
     * @return the JNP address, or null if the address could not be determined
     */
    @Nullable
    public String getJnpAddress()
    {
        return this.jnpAddress;
    }

    private void read(File distributionDirectory, File file) throws IOException
    {
        try
        {
            parseServiceXML(distributionDirectory, file);
        }
        catch (SAXException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
        catch (ParserConfigurationException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }

        // Not in that first xml - let's try the binding service xml...
        if (this.jnpPort == null && this.storeFile != null)
        {
            parseBindingManagerXML();
        }
    }

    private void parseServiceXML(File distributionDirectory, File serviceXmlFile) throws IOException, SAXException,
            ParserConfigurationException
    {
        FileInputStream is = null;
        try
        {
            is = new FileInputStream(serviceXmlFile);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();

            EntityResolver r = new LocalEntityResolver(distributionDirectory);

            JBossServiceHandler handler = new JBossServiceHandler(serviceXmlFile);

            XMLReader reader = parser.getXMLReader();

            reader.setEntityResolver(r);

            reader.setContentHandler(handler);

            reader.parse(new InputSource(is));

            this.jnpAddress = handler.getNamingBindAddress();
            this.jnpPort = handler.getNamingPort();
            this.storeFile = handler.getStoreFile();
            this.serverName = handler.getServerName();
        }
        finally
        {
            if (is != null)
            {
                is.close();
            }
        }
    }

    private void parseBindingManagerXML() throws IOException
    {
        InputStream bindIs = null;
        try
        {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();

            if (!this.storeFile.isFile())
            {
                log.warn("Store file does not exist: " + this.storeFile);
                return;
            }

            JBossBindingManagerHandler bindingHandler = new JBossBindingManagerHandler(this.storeFile);
            bindingHandler.setServerName(this.serverName);
            XMLReader reader = parser.getXMLReader();
            bindIs = new FileInputStream(this.storeFile);
            reader.setContentHandler(bindingHandler);
            reader.parse(new InputSource(bindIs));
            this.jnpAddress = bindingHandler.getJnpAddress();
            this.jnpPort = bindingHandler.getJnpPort();
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
        finally
        {
            if (bindIs != null)
            {
                bindIs.close();
            }
        }
    }

    private class JBossBindingManagerHandler extends DefaultHandler
    {
        private File file;

        private boolean inServer = false;
        private boolean inServiceConfig = false;
        private boolean inNamingPort = false;
        private String jnpAddress;
        private Integer jnpPort;
        private String serverName;

        private JBossBindingManagerHandler(File file)
        {
            this.file = file;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if (qName.equals("server"))
            {
                String name = attributes.getValue("name");
                if ((name != null) && name.equals(serverName))
                {
                    inServer = true;
                    return;
                }
            }

            if (inServer && qName.equals("service-config"))
            {
                String name = attributes.getValue("name");
                if ((name != null) && name.equals("jboss:service=Naming"))
                {
                    inServiceConfig = true;
                    return;
                }
            }

            if (inServiceConfig && qName.equals("binding"))
            {
                jnpAddress = replaceProperties(attributes.getValue("host"));
                if (jnpAddress.substring(0, PROPERTY_EXPRESSION_PREFIX.length()).equals(PROPERTY_EXPRESSION_PREFIX))
                {
                    log.warn("Naming binding 'host' attribute has invalid value (" + jnpAddress
                            + ") in JBossAS config file " + file
                            + " - the value should be a host name, an IP address, or a resolvable property reference.");
                    jnpAddress = null;
                }
                String jnpPortString = replaceProperties(attributes.getValue("port"));
                try
                {
                    jnpPort = Integer.parseInt(jnpPortString);
                }
                catch (NumberFormatException e)
                {
                    log.warn("Naming binding 'port' attribute has invalid value (" + jnpPortString
                            + ") in JBossAS config file " + file
                            + " - the value should be a positive integer or a resolvable property reference.");
                    jnpPort = null;
                }
            }

        }

        @Override
        public void endElement(String uri, String localName, String qName)
        {
            if (inServiceConfig && qName.equals("binding"))
            {
                inServiceConfig = false;
            }

            if (inNamingPort && qName.equals("service-config"))
            {
                inNamingPort = false;
            }

            if (inServer && qName.equals("server"))
            {
                inServer = false;
            }
        }

        public String getJnpAddress()
        {
            return this.jnpAddress;
        }

        protected Integer getJnpPort()
        {
            return this.jnpPort;
        }

        protected void setServerName(String serverName)
        {
            this.serverName = serverName;
        }
    }

    private class JBossServiceHandler extends DefaultHandler
    {
        private static final String DEFAULT_JNP_ADDRESS = "0.0.0.0";
        private static final String DEFAULT_JNP_PORT = "1099";

        private File file;

        //Naming Service
        private boolean inNaming = false;
        private boolean inNamingPort = false;
        private boolean inNamingBindAddress = false;
        private StringBuilder namingPort = new StringBuilder();
        private StringBuilder namingBindAddress = new StringBuilder();

        //Binding Manager
        private boolean inBinding = false;
        private boolean inServerName = false;
        private boolean inStoreURL = false;
        private boolean isBindingManagerInUse = false;
        private StringBuilder storeURL = new StringBuilder();
        private StringBuilder serverName = new StringBuilder();

        private JBossServiceHandler(File filePath)
        {
            this.file = filePath;
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException
        {
            if (inNamingBindAddress)
            {
                namingBindAddress.append(ch, start, length);
            }
            else if (inNamingPort)
            {
                namingPort.append(ch, start, length);
            }
            else if (inServerName)
            {
                serverName.append(ch, start, length);
            }
            else if (inStoreURL)
            {
                storeURL.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName)
        {
            if (inNamingPort && qName.equals("attribute"))
            {
                inNamingPort = false;
                namingPort = processValue(namingPort, DEFAULT_JNP_PORT);
                return;
            }

            if (inNamingBindAddress && qName.equals("attribute"))
            {
                inNamingBindAddress = false;
                namingBindAddress = processValue(namingBindAddress, DEFAULT_JNP_ADDRESS);
                return;
            }

            if (inServerName && qName.equals("attribute"))
            {
                inServerName = false;
                serverName = processValue(serverName, null);
                return;
            }

            if (inStoreURL && qName.equals("attribute"))
            {
                storeURL = processValue(storeURL, null);
                inStoreURL = false;
                return;
            }

            if (inNaming && qName.equals("mbean"))
            {
                inNaming = false;
                return;
            }

            if (inBinding && qName.equals("mbean"))
            {
                inBinding = false;
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
        {
            if (inNaming && qName.equals("attribute"))
            {
                String name = attributes.getValue("name");
                if (name != null)
                {
                    if (name.equals("Port"))
                    {
                        inNamingPort = true;
                        return;
                    }
                    else if (name.equals("BindAddress"))
                    {
                        inNamingBindAddress = true;
                        return;
                    }
                }
            }

            if (inBinding && qName.equals("attribute"))
            {
                String name = attributes.getValue("name");
                if ((name != null) && name.equals("ServerName"))
                {
                    inServerName = true;
                    return;
                }
                if ((name != null) && name.equals("StoreURL"))
                {
                    inStoreURL = true;
                    return;
                }
            }

            if (qName.equals("mbean"))
            {
                String name = attributes.getValue("name");
                if ((name != null) && name.equals("jboss:service=Naming"))
                {
                    inNaming = true;
                    return;
                }
                if ((name != null) && name.equals("jboss.system:service=ServiceBindingManager"))
                {
                    inBinding = true;
                    isBindingManagerInUse = true;
                }
            }
        }

        protected Integer getNamingPort()
        {
            try
            {
                return (!isBindingManagerInUse) ? Integer.parseInt(namingPort.toString()) : null;
            }
            catch (NumberFormatException e)
            {
                log.warn("Naming 'Port' attribute has invalid value (" + namingPort + ") in JBossAS config file "
                        + file + " - the value should be a positive integer or a resolvable property reference.");
                return null;
            }
        }

        protected String getNamingBindAddress()
        {
            if (namingBindAddress.substring(0, PROPERTY_EXPRESSION_PREFIX.length()).equals(PROPERTY_EXPRESSION_PREFIX))
            {
                log.warn("Naming 'BindingAddress' attribute has invalid value (" + namingBindAddress
                        + ") in JBossAS config file " + file
                        + " - the value should be a host name, an IP address, or a resolvable property reference.");
                return null;
            }
            return (!isBindingManagerInUse) ? namingBindAddress.toString() : null;
        }

        protected String getServerName()
        {
            return serverName.toString();
        }

        protected File getStoreFile()
        {
            File homeDir = new File(systemProperties.getProperty(JBossProperties.HOME_DIR));
            try
            {
                URL url = JBossConfigurationUtility.makeURL(storeURL.toString(), homeDir);
                if (!"file".equals(url.getProtocol()))
                {
                    // TODO: Do we need to support non-file URL's too?
                    throw new MalformedURLException();
                }
                return new File(url.getPath());
            }
            catch (MalformedURLException e)
            {
                log.warn("Binding 'StoreURL' attribute has invalid value (" + storeURL + ") in JBossAS config file "
                        + file + " - the value should be a file URL or file path.");
                return null;
            }
        }

        private StringBuilder processValue(StringBuilder value, String defaultValue)
        {
            String stringValue = value.toString().trim();
            if (stringValue.equals("") && defaultValue != null)
            {
                stringValue = defaultValue;
            }
            stringValue = replaceProperties(stringValue);
            return new StringBuilder(stringValue);
        }
    }

    public static class LocalEntityResolver implements EntityResolver
    {
        static Properties resolverMap = new Properties();

        static
        {
            resolverMap.setProperty("-//JBoss//DTD JBOSS 3.2//EN", "jboss-service_3_2.dtd");
            resolverMap.setProperty("-//JBoss//DTD JBOSS 4.0//EN", "jboss-service_4_0.dtd");
        }

        private File distributionDirectory;

        public LocalEntityResolver(File distributionDirectory)
        {
            this.distributionDirectory = distributionDirectory;
        }

        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException
        {
            if (log.isDebugEnabled())
                log.debug("Resolving DTD [" + publicId + "]...");
            String dtdName = resolverMap.getProperty(publicId);
            if (dtdName != null)
            {
                File systemJar = new File(distributionDirectory + File.separator + "lib" + File.separator
                        + "jboss-system.jar");
                if (systemJar.exists())
                {
                    JarFile j = new JarFile(systemJar);
                    JarEntry entry = j.getJarEntry("dtd/" + dtdName);
                    if (entry == null)
                    {
                        entry = j.getJarEntry("org/jboss/metadata/" + dtdName);
                    }

                    if (entry != null)
                    {
                        if (log.isDebugEnabled())
                            log.debug("Found DTD locally: " + entry.getName());

                        return new InputSource(j.getInputStream(entry));
                    }
                }
            }

            return null;
        }
    }

    private String replaceProperties(String value)
    {
        return (value != null) ? StringPropertyReplacer.replaceProperties(value, this.systemProperties) : null;
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 1)
        {
            System.err.println("Usage: <config file>");
            System.exit(1);
        }

        File serviceFile = new File(args[0]);
        File distDir = serviceFile.getParentFile().getParentFile().getParentFile().getParentFile();

        // Pass in an empty set of System properties.
        JnpConfig cfg = JnpConfig.getConfig(distDir, serviceFile, new Properties());

        System.out.println("JNP address: " + cfg.getJnpAddress());
        System.out.println("JNP port: " + cfg.getJnpPort());
    }
}