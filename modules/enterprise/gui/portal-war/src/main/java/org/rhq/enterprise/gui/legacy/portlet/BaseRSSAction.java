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
package org.rhq.enterprise.gui.legacy.portlet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.action.BaseAction;
import org.rhq.enterprise.gui.legacy.util.SessionUtils;
import org.rhq.enterprise.gui.util.WebUtility;

/**
 * Base RSSAction class to extend. Provides utility methods.
 */
public abstract class BaseRSSAction extends BaseAction {
    private static final Log log = LogFactory.getLog(BaseRSSAction.class.getName());

    protected String getUsername(HttpServletRequest request) {
        return WebUtility.getRequiredRequestParameter(request, "user");
    }

    protected WebUser getWebUser(HttpServletRequest request) {
        try {
            return SessionUtils.getWebUser(request.getSession());
        } catch (Exception e) {
            log.error("No RSS feeds allowed for user without configuration: " + e);
            return null;
        }
    }

    protected void setManagingEditor(HttpServletRequest request) {
        // Get "from" sender for managingEditor field
        request.setAttribute("managingEditor", System.getProperty("rhq.server.email.from-address"));
    }

    private String getBaseURL(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
    }

    protected RSSFeed getNewRSSFeed(HttpServletRequest request) {
        return new RSSFeed(getBaseURL(request));
    }
}