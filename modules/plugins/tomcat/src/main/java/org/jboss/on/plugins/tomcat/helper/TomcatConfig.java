/*
 * Jopr Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.jboss.on.plugins.tomcat.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Config parsing intended only for Tomcat server auto discovery. Copied over from the 1.4 source tree.
 */
public class TomcatConfig {
    private static HashMap<File, TomcatConfig> cache = null;
    private String address;
    private String port;
    private long lastModified = 0;

    private TomcatConfig() {
    }

    /**
     * This constructor is intended to provide TomcatConfig instances not
     * based on an XML file (as opposed to the (cached) instances obtained from {@link #getConfig(File)} method).
     * 
     * @param port
     * @param address
     */
    public TomcatConfig(String port, String address) {
        this.port = port;
        this.address = address;
    }
    
    public static synchronized TomcatConfig getConfig(File configXML) {
        if (cache == null) {
            cache = new HashMap<File, TomcatConfig>();
        }

        TomcatConfig cfg = cache.get(configXML);

        long lastModified = configXML.lastModified();

        if ((cfg == null) || (lastModified != cfg.lastModified)) {
            cfg = new TomcatConfig();
            cfg.lastModified = lastModified;
            cache.put(configXML, cfg);

            try {
                cfg.read(configXML);
            } catch (IOException e) {
            }
        }

        return cfg;
    }

    public String getPort() {
        return port;
    }

    public String getAddress() {
        return address;
    }

    private void read(File file) throws IOException {
        FileInputStream is = null;

        try {
            is = new FileInputStream(file);
            parse(is);
        } catch (SAXException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void parse(InputStream is) throws IOException, SAXException, ParserConfigurationException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        TomcatConnectorHandler handler = new TomcatConnectorHandler();
        parser.parse(is, handler);

        port = handler.getPort();
        address = handler.getAddress();
    }

    static class TomcatConnectorHandler extends DefaultHandler {
        private String address;
        private String port;

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (!qName.equals("Connector")) {
                return;
            }

            if (attributes.getValue("protocolHandlerClassName") != null) {
                //e.g. org.apache.jk.server.JkCoyoteHandler
                return;
            }

            if (attributes.getValue("protocol") != null) {
                // JbossAS 4.2 now has Tomcat6 and it explicitly defines the protocol
                if (!attributes.getValue("protocol").toLowerCase().contains("http")) {
                    //e.g. probably AJP/1.3
                    return;
                }
            }

            String className = attributes.getValue("className");
            if (className != null) {
                if (className.endsWith("WarpConnector") //e.g. 4.0.x
                    || className.endsWith("Ajp13Connector")) {
                    return;
                }
            }

            this.port = attributes.getValue("port");
            this.address = attributes.getValue("address");
        }

        protected String getPort() {
            return port;
        }

        protected String getAddress() {
            return address;
        }
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            TomcatConfig cfg = TomcatConfig.getConfig(new File(args[i]));

            System.out.println("Port=" + cfg.getPort() + " [" + args[i] + "]");
        }
    }
}
