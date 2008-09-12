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
package org.rhq.helpers.rtfilter.util;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Heiko W. Rupp
 */
public class ServletUtility {
    private static final Log LOG = LogFactory.getLog(ServletUtility.class);

    private static final String EAR_CONTENTS = ".ear-contents";

    private static final String SEPARATOR = System.getProperty("file.separator");

    private ServletUtility() {
    }

    /**
     * Get the context root for the specified servlet context.
     *
     * @param  servletContext
     *
     * @return
     */
    public static String getContextRoot(ServletContext servletContext) {
        String ctxName;

        /*
         * Get the servlet context. If we have servlet spec >=2.5, then there is a method on the servlet context for it.
         * Else we need to do some heuristics (which may be wrong at the end).
         */
        int major = servletContext.getMajorVersion();
        int minor = servletContext.getMinorVersion();
        if (((major == 2) && (minor >= 5)) || (major >= 3)) {
            ctxName = getContextRootFromSpec25(servletContext);
        } else {
            // First we check if the context-root was explicitly given in jboss-web.xml
            ctxName = getContextRootFromJbossWebXml(servletContext);

            // Still nothing? try application.xml
            if (ctxName == null) {
                ctxName = getContextRootFromApplicationXml(servletContext);
            }

            // if all else fails, take the name of the .war
            if (ctxName == null) {
                ctxName = getContextRootFromWarFileName(servletContext);
            }

            if ((ctxName == null) || "".equals(ctxName)) {
                ctxName = "__unknown";
            }
        }
        // Actually the context might always start with / even on Win* so on Unix this is / or /,
        // but this does not harm
        if (ctxName.startsWith(SEPARATOR) || ctxName.startsWith("/")) {
            ctxName = ctxName.substring(1);
        }

        return ctxName;
    }

    private static String getContextRootFromWarFileName(ServletContext servletContext) {
        String ctxName;
        ctxName = servletContext.getRealPath("/");
        if (ctxName.endsWith(SEPARATOR)) {
            ctxName = ctxName.substring(0, ctxName.length() - 1);
        }

        // the war name is from last / to end (sans .war)
        ctxName = ctxName.substring(ctxName.lastIndexOf(SEPARATOR), ctxName.length() - 4);

        // Now remove crap that might be there
        if (ctxName.endsWith("-exp")) {
            ctxName = ctxName.substring(0, ctxName.length() - 4);
            // TODO jboss sometimes has some strange 5 digits in the name
        }
        return ctxName;
    }

    /**
     * Try to read the context root from an application.xml file of a EAR archive
     *
     * @param  servletContext
     *
     * @return context root or null if not found.
     */
    private static String getContextRootFromApplicationXml(ServletContext servletContext) {
        String ctxRoot = null;

        String path = servletContext.getRealPath("/");
        if (!path.toLowerCase().contains(EAR_CONTENTS)) {
            return null;
        }

        // get the path to application.xml
        path = path.substring(0, path.lastIndexOf(EAR_CONTENTS) + EAR_CONTENTS.length());
        path += SEPARATOR + "META-INF" + SEPARATOR + "application.xml";
        File file = new File(path);
        if ((file == null) || (!file.canRead())) {
            LOG.debug(path + " is not readable");
            return null;
        }

        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.parse(file);
            Element root = doc.getDocumentElement(); // <application>
            NodeList modules = root.getChildNodes(); // <module>
            for (int i = 0; i < modules.getLength(); i++) {
                Node module = modules.item(i);
                NodeList moduleChildren = module.getChildNodes();
                for (int j = 0; j < moduleChildren.getLength(); j++) {
                    Node child = moduleChildren.item(j);
                    if ((child instanceof Element) && child.getNodeName().equals("web")) {
                        NodeList webChildren = child.getChildNodes();
                        for (int k = 0; k < webChildren.getLength(); k++) {
                            Node webChild = webChildren.item(k);
                            if (webChild.getNodeName().equals("context-root")) {
                                Node textNode = webChild.getFirstChild();
                                if (textNode != null) {
                                    ctxRoot = textNode.getNodeValue();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug(e);
            ctxRoot = null;
        }

        LOG.debug("CtxRoot from application.xml: " + ctxRoot);
        return ctxRoot;
    }

    /**
     * Try to read the contextRoot from WEB-INF/jboss-web.xml
     *
     * @param  servletContext
     *
     * @return
     */
    private static String getContextRootFromJbossWebXml(ServletContext servletContext) {
        String ctxName = null;
        try {
            String path = SEPARATOR + "WEB-INF" + SEPARATOR + "jboss-web.xml";
            InputStream jbossWeb = servletContext.getResourceAsStream(path);
            if (jbossWeb != null) {
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = db.parse(jbossWeb);
                Element root = doc.getDocumentElement(); // <jboss-web>
                NodeList children = root.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node node = children.item(i);
                    if (node instanceof Element) {
                        String name = node.getNodeName();
                        if (name.equals("context-root")) {
                            Node textNode = node.getFirstChild();
                            if (textNode != null) {
                                ctxName = textNode.getNodeValue();
                                LOG.debug("CtxName from jboss-web.xml: " + ctxName);
                            }

                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Can not get contextRoot from jboss-web.xml: " + e);
        }

        return ctxName;
    }

    /**
     * Get the contextName from the servlet context if we are in a server with spec version 2.5. We do it over
     * reflection to allow the filter to be used in a servlet 2.3 environement.
     *
     * @param  servletContext
     *
     * @return
     */
    private static String getContextRootFromSpec25(ServletContext servletContext) {
        String ret = null;
        try {
            Method meth = ServletContext.class.getMethod("getContextPath", (Class[]) null);
            Object o = meth.invoke(servletContext, (Object[]) null);
            ret = (String) o;
            if ("".equals(ret)) // default context does not return /, but empty string
            {
                ret = "ROOT";
            }

            LOG.debug("ctxPath from servlet spec 2.5: " + ret);
        } catch (Exception e) {
            LOG
                .warn("Container says it is at least servlet 2.5, but getting the contextPath failed: "
                    + e.getMessage());
        }

        return ret;
    }
}