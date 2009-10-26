/*
 * @(#)XMLNode.java  1.0  23. Juni 2008
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
import java.util.zip.ZipInputStream;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * TreevizFileSystemXMLTree reads an XML file with the root element named
 * TreevizFileSystem and the top-level child elements Users and Files.
 * <p>
 * The children of the Users element are interpreted as user account data.
 * Each element should have the following attributes: id(XML-ID), name(Text),
 * created(ISO-Date), isActive(boolean).<br>
 * Nesting of elements is not allowed. Other than that, there are no
 * more restrictions on the elements.
 * <p>
 * The children of the Files element are interpreted as a directory tree.
 * Each element should have the following attributes: name(Text),
 * created(ISO-Date), size(Number), and "ownerRef"(id of a user), "creatorRef".
 * Nesting of elements is allowed to form a directory structure. Other than that,
 * there are no more restrictions on the elements.
 *
 *
 * See {@link XMLNodeInfo} on how the XML data structure is being interpreted.
 *
 * @author  Werner Randelshofer
 * @version 1.0 25. Juni 2008 Created.
 */
public class TreevizFileSystemXMLTree implements DemoTree {
    private XMLNode root;
    private XMLNode filesRoot;
    private XMLNode usersRoot;
    private TreevizFileSystemXMLNodeInfo info;
        ProgressObserver p;

    /** Creates a new instance. */
    public TreevizFileSystemXMLTree(File xmlFile) throws IOException {
        p = null;
        if (p == null) {
            p = new ProgressView("Opening "+xmlFile.getName(), "", 0, 1);
            p.setIndeterminate(true);
        }
        try {
        root = new XMLNode();
        info = new TreevizFileSystemXMLNodeInfo(this);

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
                private boolean isFirstElement = true;

                private Stack<XMLNode> stack = new Stack<XMLNode>();
                {
                    stack.push(root);
                }
                public void startElement(String uri, String localName,
                        String qName, Attributes attributes)
                        throws SAXException {
                    if (isFirstElement) {
                        isFirstElement = false;
                        if (! qName.equals("TreevizFileSystem")) {
                            throw new SAXException("Illegal root element: \""+qName+"\" must be \"TreevizFileSystem\"");
                        }
                    }

                    XMLNode node = new XMLNode();
                    node.setName(qName);
                    for (int i=0, n=attributes.getLength(); i<n; i++) {
                        // internalizing attribute names considerably saves memory
                        String name = attributes.getLocalName(i).intern();
                        node.putAttribute(name,attributes.getValue(i));
                      //  info.putAttribute(name,attributes.getValue(i));
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

        // the first child of the root must be named Users the second one named Files
        if (root.children().size() != 1) {
               throw new IOException("XML File is empty");
        }

        TreeNode rootElement = root.children().get(0);
        if (rootElement.children().size() != 2) {
            throw new IOException("TreevizFileSystem element must have two children");
        }
         usersRoot = (XMLNode) rootElement.children().get(0);
         if (! usersRoot.getName().equals("Users")) {
            throw new IOException("First child of TreevizFileSystem element \""+usersRoot.getName()+"\" must be named \"Users\"");
         }
         filesRoot = (XMLNode) rootElement.children().get(1);
         if (! filesRoot.getName().equals("Files")) {
            throw new IOException("Second child of TreevizFileSystem element \""+filesRoot.getName()+"\" must be named \"Files\"");
         }

        p.setNote("Calculating statistics");
            p.setIndeterminate(true);
        info.init(filesRoot);
        } finally {
p.close();
        }
    }

    public XMLNode getRoot() {
        return filesRoot;
    }
    public XMLNode getUsersRoot() {
        return usersRoot;
    }
    public TreevizFileSystemXMLNodeInfo getInfo() {
        return info;
    }
}
