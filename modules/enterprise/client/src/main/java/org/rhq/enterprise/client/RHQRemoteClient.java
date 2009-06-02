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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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

    //    private Client remotingClient = null;
    private Map<String, Object> allServices;
    private Subject subject = null;

    public RHQRemoteClient(String host, int port) {
        this.host = host;
        this.port = port;
        //        init();
    }

    public boolean isConnected() {
        //        return this.subject != null && this.subjectManager != null && this.subjectManager.isLoggedIn(subject.getName());
        return (this.subject != null && this.subjectManagerRemote != null && this.subjectManagerRemote
            .isLoggedIn(subject.getName()));
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
        }
        return subjectManagerRemote;
    }

    public OperationManagerRemote getOperationManagerRemote() {
        OperationManagerRemote remote = null;
        OperationManagerBeanService operationManagerService = null;
        if (operationManagerRemote == null) {
            operationManagerService = new OperationManagerBeanService();
            operationManagerRemote = operationManagerService.getOperationManagerBeanPort();
        }
        return operationManagerRemote;
    }

    public ChannelManagerRemote getChannelManagerRemote() {
        ChannelManagerRemote remote = null;
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
        ConfigurationManagerRemote remote = null;
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
}