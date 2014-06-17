/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.server.rhaccess;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import com.redhat.gss.redhat_support_lib.api.API;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.auth.SessionManager;
import org.rhq.enterprise.server.util.LookupUtil;

public class AttachmentsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String SERVER_REPORT = "JBoss ON Server JDR Report";
    private static final String RESOURCE_REPORT = "EAP JDR Report";

    private static final List<String> SUPPORTED_RESOURCE_TYPES = Arrays.asList("JBossAS7 Standalone Server",
        "Managed Server");
    private final static Logger log = Logger.getLogger(AttachmentsServlet.class);

    private String getResourceDetails(Resource r) {
        return r.getName() + "[" + r.getResourceType().getName() + "] on " + r.getAgent().getName() + " [availability="
            + r.getCurrentAvailability().getAvailabilityType() + "]";
    }

    private String getAvailableReports(int resourceId) {
        ResourceCriteria rc = new ResourceCriteria();
        rc.addFilterId(resourceId);
        rc.fetchAgent(true);
        rc.fetchResourceType(true);
        rc.fetchChildResources(true);

        PageList<Resource> resources = LookupUtil.getResourceManager().findResourcesByCriteria(
            LookupUtil.getSubjectManager().getOverlord(), rc);
        if (resources.size() > 0) {
            StringBuilder options = new StringBuilder();
            Resource r = resources.get(0);
            String checked = "true";
            if (!r.getCurrentAvailability().equals(AvailabilityType.UP)) {
                checked = "false";
            }
            if (SUPPORTED_RESOURCE_TYPES.contains(r.getResourceType().getName())) {
                options.append(RESOURCE_REPORT + "/" + resourceId + ": " + getResourceDetails(r) + "?checked="
                    + checked + "\n");
            }
            for (Resource child : r.getChildResources()) {
                if (SUPPORTED_RESOURCE_TYPES.contains(child.getResourceType().getName())) {
                    checked = "true";
                    if (!child.getCurrentAvailability().equals(AvailabilityType.UP)) {
                        checked = "false";
                    }
                    options.append(RESOURCE_REPORT + "/" + child.getId() + ": " + getResourceDetails(child)
                        + "?checked=" + checked + "\n");
                }
            }
            return options.toString();

        }

        return null;
    }

    private boolean isAuthorized(HttpServletRequest request) {
        String sessionId = request.getHeader("RHQ_SessionID");
        if (sessionId != null) {
            try {
                return SessionManager.getInstance().getSubject(Integer.parseInt(sessionId)) != null;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     *  we generate options (Available reports to attach) (each on new line, ?checked=true to enable auto-check for this option for user
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            StringBuilder options = new StringBuilder();
            String resourceId = request.getParameter("resourceId");
            if (resourceId != null) {
                if (!isAuthorized(request)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                    return;
                }
                int resId = Integer.parseInt(resourceId);
                options.append(getAvailableReports(resId));
            } else {
                options.append(SERVER_REPORT + "?checked=true\n");
            }
            response.getWriter().write(options.toString());
        } catch (Throwable t) {
            log.error("Server Error", t);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Error");
        }
    }

    /**
     * frond-end POSTs selected option (returned from GET) request that was checked, 
     * we're expected to obtain and upload particular attachment to RHA 
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            if (!isAuthorized(request)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                return;
            }
            StringBuffer sb = new StringBuffer();
            String line = null;
            try {
                BufferedReader reader = request.getReader();
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (Exception e) {
                log.error("Error Reading Request");
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error Reading Request");
            }
            JSONObject jsonObject = new JSONObject(sb.toString());
            String authToken = jsonObject.getString("authToken");
            String attachment = jsonObject.getString("attachment");
            String caseNum = jsonObject.getString("caseNum");
            String decodedCredentials = new String(DatatypeConverter.parseBase64Binary(authToken));
            String[] splitCredentials = decodedCredentials.split(":");
            String username = null;
            String password = null;
            if (splitCredentials != null) {
                if (splitCredentials[0] != null) {
                    username = splitCredentials[0];
                }
                if (splitCredentials[1] != null) {
                    password = splitCredentials[1];
                }
            }
            Config config = new Config();
            API api = new API(username, password, config.getURL(), config.getProxyUser(), config.getProxyPassword(),
                config.getProxyURL(), config.getProxyPort(), config.getUserAgent(), config.isDevel());
            // check if we are authorized
            api.getProblems().diagnoseStr("test");
            if (SERVER_REPORT.equalsIgnoreCase(attachment)) {
                String report = new JdrReportRunner().getReport();
                api.getAttachments().add(caseNum, true, report, attachment);
                log.info("File attached to URL " + api.getConfigHelper().getUrl());
                try {
                    new File(report).delete();
                    log.debug("Report " + report + " deleted");
                } catch (Exception e) {
                    log.error("Failed to delete JDR Report File", e);
                }
            }
            if (attachment.startsWith(RESOURCE_REPORT)) {
                String resourceId = attachment.replaceAll(".*/", "").replaceAll("\\:.*", "").trim();
                log.info("About to attach report for resourceId=" + resourceId);
                int resId = Integer.parseInt(resourceId);
                String report = new ResourceJdrReportRunner(resId).getReport();
                if (report != null) {
                    api.getAttachments().add(caseNum, true, report, attachment);
                    try {
                        new File(report).delete();
                        log.debug("Report " + report + " deleted");
                    } catch (Exception e) {
                        log.error("Failed to delete JDR Report File", e);
                    }
                } else {
                    throw new Exception("Failed to attach JDR Report for resourceId=" + resId + " no data returned");
                }
            }
        } catch (JdrReportFailedException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
            log.error("Failed to create attachment", e);
        } catch (Throwable t) {

            if (t.getLocalizedMessage().contains("401")) {
                log.error("Unauthorized");
                response.sendError(HttpServletResponse.SC_CONFLICT, "Unauthorized");
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Error");
                log.error("Failed to create attachment", t);
            }
        }
    }
}
