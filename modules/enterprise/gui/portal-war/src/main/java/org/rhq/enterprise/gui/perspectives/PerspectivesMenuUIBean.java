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
package org.rhq.enterprise.gui.perspectives;

import java.util.List;

import javax.faces.application.FacesMessage;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.cloud.Server;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.perspective.PerspectiveException;
import org.rhq.enterprise.server.perspective.PerspectiveManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 *  Bean dynamically provides menu information used by menu.xhtml.  This bean should be viewed 
 *  concurrently with menu.xhtml and it's designed to provide static or dynamic menu content 
 *  for menu.xhtml.
 * 
 * @author Simeon Pinder
 * @author Jay Shaughnessy  
 *
 */
public class PerspectivesMenuUIBean {

    PerspectiveManagerLocal perspectiveManager = LookupUtil.getPerspectiveManager();

    public List<org.rhq.enterprise.server.perspective.MenuItem> getCoreMenu() {
        Subject subject = EnterpriseFacesContextUtility.getSubject();
        List<org.rhq.enterprise.server.perspective.MenuItem> result = null;

        try {
            result = perspectiveManager.getMenu(subject);
        } catch (PerspectiveException e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to get core menu.", e);
        }

        return result;
    }

    /**
     * Using the required request parameter 'targetUrlKey', as set in the extension, resolve that key into
     * the targetUrl for the extension's content.
     * 
     * @return null if 'targetUrlKey' param is invalid. 
     */
    public String getUrlViaKey() {
        String targetUrlKey = FacesContextUtility.getRequiredRequestParameter("targetUrlKey");
        String result = null;

        try {
            result = perspectiveManager.getUrlViaKey(Integer.valueOf(targetUrlKey));
            result = addContextParams(result);
        } catch (NumberFormatException e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR,
                "Required request parameter 'targetUrlKey' was not numeric: " + targetUrlKey, e);
        } catch (Exception e) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Could not get url for targetUrlKey: "
                + targetUrlKey, e);
        }

        return result;
    }

    private String addContextParams(String url) {
        StringBuilder sb = new StringBuilder(url);
        Server server = LookupUtil.getServerManager().getServer();
        String host = server.getAddress();
        String port = String.valueOf(server.getPort());
        String sessionId = String.valueOf(EnterpriseFacesContextUtility.getSubject().getSessionId());
        String resourceId = FacesContextUtility.getOptionalRequestParameter("id", "-1");

        addContextParam(sb, "rhqServerHost", host, url.contains("?") ? "&" : "?");
        addContextParam(sb, "rhqServerPort", port, "&");
        addContextParam(sb, "rhqSessionId", sessionId, "&");
        if (!"-1".equals(resourceId)) {
            addContextParam(sb, "rhqResourceId", resourceId, "&");
        }

        return sb.toString();
    }

    private void addContextParam(StringBuilder sb, String name, String value, String separator) {
        sb.append(separator);
        sb.append(name);
        sb.append("=");
        sb.append(value);
    }

}
