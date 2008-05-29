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
package org.rhq.plugins.jira;

import org.rhq.plugins.jira.soapclient.jira.JiraSoapService;
import org.rhq.plugins.jira.soapclient.jira.JiraSoapServiceServiceLocator;
import org.rhq.plugins.jira.soapclient.jira.RemoteFilter;
import org.rhq.plugins.jira.soapclient.jira.RemoteProject;
import org.rhq.plugins.jira.soapclient.jira.RemoteServerInfo;
import org.rhq.plugins.jira.soapclient.jira.RemoteIssue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages cached access to jira. Just some very basic stuff right now... Projects, filters and their counts. The
 * Jira remote api's are not terribly robust.
 *
 * @author Greg Hinkle
 */
public class JiraClient {

    public static final String WS_ENDPOINT = "/rpc/soap/jirasoapservice-v2";

    private String user;
    private String password;
    private String url;

    private String token;
    private JiraSoapService jira;
    private Map<String, RemoteProject> projectMap = new HashMap<String, RemoteProject>();
    private Map<String, List<RemoteFilter>> filters = new HashMap<String, List<RemoteFilter>>();


    public JiraClient(org.rhq.core.domain.configuration.Configuration config) {
        user = config.getSimple("user").getStringValue();
        password = config.getSimple("password").getStringValue();
        url = config.getSimple("url").getStringValue();
    }

    public JiraClient(String user, String password, String url) {
        this.user = user;
        this.password = password;
        this.url = url;
    }

    public RemoteServerInfo getServerInfo() {
        getToken();
        try {
            return jira.getServerInfo(token);
        } catch (RemoteException e) {
            throw new RuntimeException("Couldn't connect to jira", e);
        }

    }

    public void refreshCaches() {
        long start = System.currentTimeMillis();
        try {
            getToken();
            RemoteProject[] projects = jira.getProjectsNoSchemes(token);

            for (RemoteProject project : projects) {
                projectMap.put(project.getKey(), project);
                //System.out.println("Project: " + project.getName() + ":" + project.getId() + " (" + project.getDescription() + ")");

                RemoteIssue[] issues = jira.getIssuesFromTextSearch(token, "rhq");

//                System.out.println("PROJECT: " + project.getName());
//                System.out.println("\tLead: " + project.getLead());
//                System.out.println("\tDescription: " + project.getDescription());
//                System.out.println("\tId: " + project.getId());
//                System.out.println("\tURL: " + project.getProjectUrl());
//                System.out.println("\tIssues: " + issues.length);

            }

//            System.out.println("------------------------");
            RemoteFilter[] filterArray = jira.getSavedFilters(token);
            for (RemoteFilter filter : filterArray) {
                String projectKey = null;
                for (RemoteProject p : projects) {
                    if (filter.getProject() != null && filter.getProject().equals(p.getId())) {
                        projectKey = p.getKey();
                    }
                }
                if (projectKey != null) {
                    List<RemoteFilter> projectFilters = filters.get(projectKey);
                    if (projectFilters == null) {
                        projectFilters = new ArrayList<RemoteFilter>();
                        filters.put(projectKey,projectFilters);
                    }
                    projectFilters.add(filter);
                }
                //System.out.println("Filter: " + filter.getName() + " (" + filter.getProject() + ")");
            }

        } catch (org.rhq.plugins.jira.soapclient.jira.RemotePermissionException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (org.rhq.plugins.jira.soapclient.jira.RemoteAuthenticationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (org.rhq.plugins.jira.soapclient.jira.RemoteException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (java.rmi.RemoteException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
//        System.out.println("JiraClient cache load time: " + (System.currentTimeMillis() - start));
    }

    public String getToken() {
        if (token == null) {
            try {
                JiraSoapServiceServiceLocator l = new JiraSoapServiceServiceLocator();
                l.setJirasoapserviceV2EndpointAddress(url + WS_ENDPOINT);
                jira = l.getJirasoapserviceV2();

                token = jira.login(user, password);


            } catch (org.rhq.plugins.jira.soapclient.jira.RemoteAuthenticationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (org.rhq.plugins.jira.soapclient.jira.RemoteException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (java.rmi.RemoteException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (javax.xml.rpc.ServiceException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        return token;
    }

    public Map<String, RemoteProject> getProjectMap() {
        return projectMap;
    }

    public Map<String, List<RemoteFilter>> getFilters() {
        return filters;
    }

    public static void main(String[] args) throws Exception {

        JiraClient jc = new JiraClient("user", "pass", "http://jira.rhq-project.org");
        jc.refreshCaches();

        /*


        URL service = new URL("http://jira.jboss.com/jira/rpc/soap/jirasoapservice-v2");
        JiraSoapServiceServiceLocator l = new JiraSoapServiceServiceLocator();
        l.setJirasoapserviceV2EndpointAddress("http://jira.jboss.com/jira/rpc/soap/jirasoapservice-v2");
        JiraSoapService jira = l.getJirasoapserviceV2();

        String token = jira.login("usre", "pass");

        RemoteProject[] projects = jira.getProjectsNoSchemes(token);

        Map<String, RemoteProject> projectMap = new HashMap<String, RemoteProject>();

        for (RemoteProject project : projects) {
            projectMap.put(project.getKey(), project);
            System.out.println("Project: " + project.getName() + ":" + project.getId() + " (" + project.getDescription() + ")");
        }

        //RemoteProject proj = jira.getProjectByKey(token, "JBNADM");

        System.out.println("------------------------");
        RemoteFilter[] filters = jira.getSavedFilters(token);
        for (RemoteFilter filter : filters) {
            System.out.println("Filter: " + filter.getName() + " (" + filter.getProject() + ")");
        }


        RemoteServerInfo serverInfo = jira.getServerInfo(token);
        System.out.println("Server Version: " + serverInfo.getVersion());


        System.out.println("---------------------------------------------");
        RemoteProject jbnadm = projectMap.get("JBNADM");
        System.out.println("PROJECT: " + jbnadm.getName());
        System.out.println("\tLead: " + jbnadm.getLead());
        System.out.println("\tDescription: " + jbnadm.getDescription());
        System.out.println("\tId: " + jbnadm.getId());
        System.out.println("\tURL: " + jbnadm.getProjectUrl());

        for (RemoteFilter filter : filters) {
            if (jbnadm.getId().equals(filter.getProject())) {

                long count = jira.getIssueCountForFilter(token, filter.getId());
                System.out.println("Filter: " + filter.getName() + " Count: " + count);
            }

        }

        RemoteVersion[] versions = jira.getVersions(token, jbnadm.getKey());
        for (RemoteVersion version : versions) {
            System.out.println("\tVersion: " + version.getName() + " (" + (version.getReleaseDate() != null ? version.getReleaseDate().getTime() : "") + ")");


        }*/

        /* Slow and uses way too much memory
        System.out.println("-----------------");
        RemoteIssue[] issues = jira.getIssuesFromTextSearch(token,"jbnadm open ff:2.0.1");
        System.out.println("Found: " + issues.length + " issues");
        Map<String, Integer> priorityCounts = new HashMap<String, Integer>();
        for (RemoteIssue issue :issues) {
            Integer c = priorityCounts.get(issue.getPriority());
            if (c == null) {
                c = 0;
            }
            priorityCounts.put(issue.getPriority(), c + 1);
        }

        for (String priority : priorityCounts.keySet()) {
            System.out.println("\t" + priority + ": " + priorityCounts.get(priority));
        }*/


    }

    public long getIssueCount(String filterId) throws RemoteException {
        return jira.getIssueCountForFilter(token, filterId);
    }
}
