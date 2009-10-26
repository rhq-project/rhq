/*
 * @(#)XMLTree.java  1.0  23. Juni 2008
 *
 * Copyright (c) 2007 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Werner Randelshofer. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Werner Randelshofer.
 */

package ch.randelshofer.tree.demo;

import ch.randelshofer.gui.ProgressObserver;
import ch.randelshofer.gui.ProgressView;
import ch.randelshofer.io.*;
import ch.randelshofer.tree.TreeNode;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XMLTree produces a general purpose tree over a XML file.
 * See {@link XMLNodeInfo} on how the XML data structure is being interpreted.
 *
 * @author  Werner Randelshofer
 * @version 1.0 23. Juni 2008 Created.
 */
public class XMLTree implements DemoTree {
    private XMLNode root;
    private XMLNodeInfo info;
       private ProgressObserver p;

    /** Creates a new instance. */
    public XMLTree(File xmlFile) throws IOException {
        p = null;
        if (p == null) {
            p = new ProgressView("Opening "+xmlFile.getName(), "", 0, 1);
            p.setIndeterminate(true);
        }
        try {
        root = new XMLNode();
        info = new XMLNodeInfo();

        SAXParserFactory factory = SAXParserFactory.newInstance();
            InputStream in = null;
        try {
            SAXParser saxParser = factory.newSAXParser();
            BoundedRangeInputStream bris;
            if (xmlFile.getName().endsWith(".zip")) {
                bris = null;
                ZipInputStream zis = new ZipInputStream(in = new FileInputStream(xmlFile));
                for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry() ) {
                    if (! entry.isDirectory() && entry.getName().endsWith(".xml")) {
                        bris = new BoundedRangeInputStream(zis);
                        if (entry.getSize() != -1) {
                            bris.setMaximum((int) entry.getSize() + 1);
                        }
                        break;
                    }
                }
              if (bris == null) {
                  throw new IOException("No XML file found inside of "+xmlFile+".");
              }

            } else {
              bris = new BoundedRangeInputStream(in = new FileInputStream(xmlFile));
            bris.setMaximum((int) xmlFile.length()+1);
            }

            final SuspendableInputStream sis = new SuspendableInputStream(bris);
            p.setModel(bris);
            p.setIndeterminate(false);
            p.setDoCancel(new Runnable() { public void run() { sis.abort(); } });

            saxParser.parse( sis, new DefaultHandler() {

                private Stack<XMLNode> stack = new Stack<XMLNode>();
                {
                    stack.push(root);
                }
                public void startElement(String uri, String localName,
                        String qName, Attributes attributes)
                        throws SAXException {
                    XMLNode node = new XMLNode();
                    node.setName(qName);
                    for (int i=0, n=attributes.getLength(); i<n; i++) {
                        // internalizing attribute names considerably saves memory
                        String name = attributes.getLocalName(i).intern();
                        node.putAttribute(name,attributes.getValue(i));
                        //info.putAttribute(name,attributes.getValue(i));
                    }

                    stack.peek().addChild(node);
                    stack.push(node);
                }
                public void endElement(String uri, String localName, String qName)
                throws SAXException {
                    stack.pop();
                }
            });
        } catch (ParserConfigurationException ex) {
            IOException iex = new IOException("XML Parser configuration error");
            iex.initCause(ex);
            throw iex;
        } catch (SAXException ex) {
            IOException iex = new IOException("XML Error");
            iex.initCause(ex);
            throw iex;
        } finally {
            if (in != null) {
                in.close();
            }
        }
        p.setNote("Calculating statistics");
            p.setIndeterminate(true);
        info.init(root);
        } finally {
p.close();
        }
    }

    public XMLNode getRoot() {
        return root;
    }
    public XMLNodeInfo getInfo() {
        return info;
    }
}
