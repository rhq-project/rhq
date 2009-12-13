package org.rhq.core.gui.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;

import com.sun.faces.application.ApplicationImpl;

public class MockExternalContext extends ExternalContext {

    @Override
    public void dispatch(String path) throws IOException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String encodeActionURL(String url) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String encodeNamespace(String name) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String encodeResourceURL(String url) {

        return "encodedUrl:" + url;

    }

    public Map<String, Object> applicationMap;

    @Override
    public Map<String, Object> getApplicationMap() {
        if (null == applicationMap) {
            applicationMap = new HashMap<String, Object>();
            applicationMap.put("com.sun.faces.ApplicationImpl", new ApplicationImpl());
            applicationMap.put("com.sun.faces.sunJsfJs", "Javascript goes here".toCharArray());
        }

        return applicationMap;
    }

    @Override
    public String getAuthType() {
        throw new RuntimeException("Function not implemented");

    }

    MockServletContext servletContext = new MockServletContext();

    @Override
    public Object getContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Map getInitParameterMap() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String getRemoteUser() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Object getRequest() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String getRequestContextPath() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Map<String, Object> getRequestCookieMap() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Map<String, String> getRequestHeaderMap() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Map<String, String[]> getRequestHeaderValuesMap() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Locale getRequestLocale() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Iterator<Locale> getRequestLocales() {
        throw new RuntimeException("Function not implemented");

    }

    Map<String, Object> requestMap = new HashMap<String, Object>();

    @Override
    public Map<String, Object> getRequestMap() {
        return requestMap;
    }

    public Map<String, String> requestParameterMap = new HashMap<String, String>();

    @Override
    public Map<String, String> getRequestParameterMap() {
        return requestParameterMap;
    }

    @Override
    public Iterator<String> getRequestParameterNames() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Map<String, String[]> getRequestParameterValuesMap() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String getRequestPathInfo() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public String getRequestServletPath() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public InputStream getResourceAsStream(String path) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Set<String> getResourcePaths(String path) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Object getResponse() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Object getSession(boolean create) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Map<String, Object> getSessionMap() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public Principal getUserPrincipal() {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public boolean isUserInRole(String role) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void log(String message) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void log(String message, Throwable exception) {
        throw new RuntimeException("Function not implemented");

    }

    @Override
    public void redirect(String url) throws IOException {
        throw new RuntimeException("Function not implemented");

    }

}
