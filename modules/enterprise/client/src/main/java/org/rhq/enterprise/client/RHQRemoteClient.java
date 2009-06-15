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
package org.rhq.enterprise.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceClient;

import org.rhq.enterprise.server.ws.ChannelManagerBeanService;
import org.rhq.enterprise.server.ws.ChannelManagerRemote;
import org.rhq.enterprise.server.ws.ConfigurationManagerBeanService;
import org.rhq.enterprise.server.ws.ConfigurationManagerRemote;
import org.rhq.enterprise.server.ws.ContentManagerBeanService;
import org.rhq.enterprise.server.ws.ContentManagerRemote;
import org.rhq.enterprise.server.ws.OperationManagerBeanService;
import org.rhq.enterprise.server.ws.OperationManagerRemote;
import org.rhq.enterprise.server.ws.ResourceManagerBeanService;
import org.rhq.enterprise.server.ws.ResourceManagerRemote;
import org.rhq.enterprise.server.ws.RoleManagerBeanService;
import org.rhq.enterprise.server.ws.RoleManagerRemote;
import org.rhq.enterprise.server.ws.Subject;
import org.rhq.enterprise.server.ws.SubjectManagerBeanService;
import org.rhq.enterprise.server.ws.SubjectManagerRemote;

/**
 * A remote access client with transparent proxies to RHQ servers.
 *
 * @author Greg Hinkle, Simeon Pinder
 */
public class RHQRemoteClient {
    // Default locator values
    private String transport = "servlet";
    private String host = "localhost";
    private int port = 7080;
    private boolean loggedIn = false;

    public void setLoggedIn(boolean value) {
        this.loggedIn = value;
    }

    public SubjectManagerBeanService sbms = null;

    private Map<String, Object> allServices;
    private Subject subject = null;

    public RHQRemoteClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean isConnected() {
        return this.loggedIn;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    private RoleManagerRemote roleManagerRemote = null;
    private ContentManagerRemote contentManagerRemote = null;
    private SubjectManagerRemote subjectManagerRemote = null;
    private OperationManagerRemote operationManagerRemote = null;
    private ChannelManagerRemote channelManagerRemote = null;
    private ConfigurationManagerRemote configurationManagerRemote = null;
    private ResourceManagerRemote resourceManagerRemote = null;
    private ClientMain clireference;

    public RoleManagerRemote getRoleManagerRemote() {
        RoleManagerBeanService roleManagerService = null;
        if (roleManagerRemote == null) {
            roleManagerService = new RoleManagerBeanService();
            roleManagerRemote = roleManagerService.getRoleManagerBeanPort();
        }
        return roleManagerRemote;
    }

    public ContentManagerRemote getContentManagerRemote() {
        ContentManagerBeanService service = null;
        if (contentManagerRemote == null) {
            service = new ContentManagerBeanService();
            contentManagerRemote = service.getContentManagerBeanPort();
        }
        return contentManagerRemote;
    }

    public SubjectManagerRemote getSubjectManagerRemote() {
        SubjectManagerBeanService subjectManagerService = null;
        if (subjectManagerRemote == null) {
            subjectManagerService = new SubjectManagerBeanService();
            subjectManagerRemote = subjectManagerService.getSubjectManagerBeanPort();
            sbms = subjectManagerService;// store ref
        }
        return subjectManagerRemote;
    }

    public OperationManagerRemote getOperationManagerRemote() {
        OperationManagerBeanService operationManagerService = null;
        if (operationManagerRemote == null) {
            operationManagerService = new OperationManagerBeanService();
            operationManagerRemote = operationManagerService.getOperationManagerBeanPort();
        }
        return operationManagerRemote;
    }

    public ChannelManagerRemote getChannelManagerRemote() {
        ChannelManagerBeanService channelManagerService = null;
        if (channelManagerRemote == null) {
            channelManagerService = new ChannelManagerBeanService();
            channelManagerRemote = channelManagerService.getChannelManagerBeanPort();
        }
        return channelManagerRemote;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public void setChannelManagerRemote(ChannelManagerRemote channelManagerRemote) {
        this.channelManagerRemote = channelManagerRemote;
    }

    public ConfigurationManagerRemote getConfigurationManagerRemote() {
        ConfigurationManagerBeanService configurationManagerService = null;
        if (configurationManagerRemote == null) {
            configurationManagerService = new ConfigurationManagerBeanService();
            configurationManagerRemote = configurationManagerService.getConfigurationManagerBeanPort();
        }
        return configurationManagerRemote;
    }

    public ResourceManagerRemote getResourceManagerRemote() {
        ResourceManagerBeanService resourceManagerService = null;
        if (resourceManagerRemote == null) {
            resourceManagerService = new ResourceManagerBeanService();
            resourceManagerRemote = resourceManagerService.getResourceManagerBeanPort();
        }
        return resourceManagerRemote;
    }

    public static final String[] SERVICE_NAMES = new String[] { "RoleManagerRemote", "ContentManagerRemote",
        "SubjectManagerRemote", "OperationManagerRemote", "ChannelManagerRemote", "ConfigurationManagerRemote",
        "ResourceManagerRemote" };

    public Map<String, Object> getAllServices() {
        if (this.allServices == null) {

            this.allServices = new HashMap<String, Object>();

            for (String serviceName : SERVICE_NAMES) {
                try {
                    Method m = this.getClass().getMethod("get" + serviceName);
                    this.allServices.put(serviceName, m.invoke(this));
                } catch (Throwable e) {
                    System.out.println("Couldn't load service " + serviceName + " due to missing class "
                        + e.getMessage());
                }
            }
        }

        return allServices;
    }

    public void reinitialize() {
        try {
            // RoleManager
            RoleManagerBeanService roleManagerService = new RoleManagerBeanService(
                getClientConnectionWsdl(RoleManagerBeanService.class), getServiceQName(RoleManagerBeanService.class));
            roleManagerRemote = roleManagerService.getRoleManagerBeanPort();
            // "SubjectManagerRemote",
            SubjectManagerBeanService subjectManagerService = new SubjectManagerBeanService(
                getClientConnectionWsdl(SubjectManagerBeanService.class),
                getServiceQName(SubjectManagerBeanService.class));
            subjectManagerRemote = subjectManagerService.getSubjectManagerBeanPort();
            sbms = subjectManagerService;
            // "OperationManagerRemote",
            OperationManagerBeanService operationManagerService = new OperationManagerBeanService(
                getClientConnectionWsdl(OperationManagerBeanService.class),
                getServiceQName(OperationManagerBeanService.class));
            operationManagerRemote = operationManagerService.getOperationManagerBeanPort();

            // "ChannelManagerRemote",
            ChannelManagerBeanService channelManagerService = new ChannelManagerBeanService(
                getClientConnectionWsdl(ChannelManagerBeanService.class),
                getServiceQName(ChannelManagerBeanService.class));
            channelManagerRemote = channelManagerService.getChannelManagerBeanPort();

            // "ConfigurationManagerRemote",
            ConfigurationManagerBeanService configurationManagerService = new ConfigurationManagerBeanService(
                getClientConnectionWsdl(ConfigurationManagerBeanService.class),
                getServiceQName(ConfigurationManagerBeanService.class));
            configurationManagerRemote = configurationManagerService.getConfigurationManagerBeanPort();

            // "ResourceManagerRemote"
            ResourceManagerBeanService resourceManagerService = new ResourceManagerBeanService(
                getClientConnectionWsdl(ResourceManagerBeanService.class),
                getServiceQName(ResourceManagerBeanService.class));
            resourceManagerRemote = resourceManagerService.getResourceManagerBeanPort();
            // "ContentManagerRemote"
            ContentManagerBeanService contentManagerService = new ContentManagerBeanService(
                getClientConnectionWsdl(ContentManagerBeanService.class),
                getServiceQName(ContentManagerBeanService.class));
            contentManagerRemote = contentManagerService.getContentManagerBeanPort();

        } catch (Exception ex) {
            System.out.println("Exception reinitalizing with new host :" + ex.getMessage());
        }

    }

    private QName getServiceQName(Class remote) {
        QName generated = null;
        // check for reference to ClientEngine/Client && that class passed in has right annotation
        if ((remote != null) && (remote.isAnnotationPresent(WebServiceClient.class))) {
            String annotatedQnameValue = "";
            Annotation annot = remote.getAnnotation(WebServiceClient.class);
            WebServiceClient annotated = (WebServiceClient) annot;
            annotatedQnameValue = annotated.targetNamespace();
            String beanName = remote.getSimpleName();

            generated = new QName(annotatedQnameValue, beanName);
        }
        return generated;
    }

    /**
     * Dynamically builds the WSDL URL to connect to a remote server.
     *
     * @param remote
     *            class correctly annotated with Webservice reference.
     * @return valid URL
     * @throws MalformedURLException
     */
    private URL getClientConnectionWsdl(Class remote) throws MalformedURLException {
        URL wsdlLocation = null;
        // check for reference to ClientEngine/Client && that class passed in has right annotation
        if ((remote != null) && remote.isAnnotationPresent(WebServiceClient.class)) {
            String beanName = remote.getSimpleName();
            String protocol = "https://";
            if ((this.clireference != null) && (!this.clireference.isHttps())) {
                protocol = "http://";
            }
            wsdlLocation = new URL(protocol + getHost() + ":" + getPort() + "/rhq-rhq-enterprise-server-ejb3/"
                + beanName.substring(0, beanName.length() - "Service".length()) + "?wsdl");
        }
        return wsdlLocation;
    }

    public void setCliReference(ClientMain main) {
        this.clireference = main;
    }

}