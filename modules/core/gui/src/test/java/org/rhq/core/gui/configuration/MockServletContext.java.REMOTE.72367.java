/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.core.gui.configuration;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import com.sun.faces.config.WebConfiguration;

final class MockServletContext implements ServletContext {
    public void setAttribute(String arg0, Object arg1) {
        this.attributeMap.put(arg0, arg1);

    }

    public void removeAttribute(String arg0) {
        attributeMap.remove(arg0);

    }

    public void log(String arg0, Throwable arg1) {
        throw new RuntimeException("Function not implemented");

    }

    public void log(Exception arg0, String arg1) {
        throw new RuntimeException("Function not implemented");

    }

    public void log(String arg0) {
        throw new RuntimeException("Function not implemented");

    }

    public Enumeration getServlets() {
        throw new RuntimeException("Function not implemented");

    }

    public Enumeration getServletNames() {
        throw new RuntimeException("Function not implemented");

    }

    public String getServletContextName() {
        return MockServletContext.class.getSimpleName();

    }

    public Servlet getServlet(String arg0) throws ServletException {
        throw new RuntimeException("Function not implemented");

    }

    public String getServerInfo() {
        throw new RuntimeException("Function not implemented");

    }

    public Set getResourcePaths(String arg0) {
        throw new RuntimeException("Function not implemented");

    }

    public InputStream getResourceAsStream(String arg0) {
        throw new RuntimeException("Function not implemented");

    }

    public URL getResource(String arg0) throws MalformedURLException {
        throw new RuntimeException("Function not implemented");

    }

    public RequestDispatcher getRequestDispatcher(String arg0) {
        throw new RuntimeException("Function not implemented");

    }

    public String getRealPath(String arg0) {
        throw new RuntimeException("Function not implemented");

    }

    public RequestDispatcher getNamedDispatcher(String arg0) {
        throw new RuntimeException("Function not implemented");

    }

    public int getMinorVersion() {
        return 4;
    }

    public String getMimeType(String arg0) {
        throw new RuntimeException("Function not implemented");

    }

    public int getMajorVersion() {
        return 2;

    }

    Properties initParams = new Properties();

    public Enumeration getInitParameterNames() {
        return initParams.elements();

    }

    public String getInitParameter(String key) {
        return initParams.getProperty(key);

    }

    public ServletContext getContext(String arg0) {
        throw new RuntimeException("Function not implemented");

    }

    public Enumeration getAttributeNames() {
        throw new RuntimeException("Function not implemented");

    }

    Map<String, Object> attributeMap;

    public Object getAttribute(String key) {
        if (null == attributeMap) {
            attributeMap = new HashMap<String, Object>();
            WebConfiguration wc = WebConfiguration.getInstance(this);

            attributeMap.put("com.sun.faces.config.WebConfiguration", wc);
        }

        return attributeMap.get(key);
    }
}