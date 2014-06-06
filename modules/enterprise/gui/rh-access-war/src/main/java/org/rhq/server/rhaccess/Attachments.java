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
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;

import com.redhat.gss.redhat_support_lib.api.API;

import org.apache.log4j.Logger;
import org.json.JSONObject;

public class Attachments extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String SERVER_REPORT = "JBoss ON Server JDR Report";

    private final static Logger log = Logger.getLogger(Attachments.class);

    /**
     *  we generate options (Available reports to attach) (each on new line, ?checked=true to enable auto-check for this option for user
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            StringBuilder options = new StringBuilder();
            options.append(SERVER_REPORT + "?checked=true\n");
            response.getWriter().write(options.toString());
        } catch (Throwable t) {
            log.error(t);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Error");
        }
    }

    /**
     * frond-end POSTs selected option (returned from GET) request that was checked, 
     * we're expected to obtain and upload particular attachment to RHA 
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
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
                String path = new JdrReportRunner().getReport();
                api.getAttachments().add(caseNum, true, path, attachment);
                log.info("File attached to URL " + api.getConfigHelper().getUrl());
            }
        } catch (Throwable t) {
            log.error(t);
            if (t.getLocalizedMessage().contains("401")) {
                log.error("Unauthorized");
                response.sendError(HttpServletResponse.SC_CONFLICT, "Unauthorized");
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server Error");
            }
        }
    }
}
