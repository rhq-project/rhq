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
package org.rhq.enterprise.gui.alert.common;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;

@Scope(ScopeType.EVENT)
@Name("AlertNotificationContextUIBean")
public class AlertNotificationContextUIBean {
    private enum Context {
        AlertNotificationTemplate, //
        ResourceAlertDefinition, //
        GroupAlertDefinition, //
        AlertTemplate, //
        NotificationDetails;
    }

    @RequestParameter("context")
    private String context;
    @RequestParameter("contextId")
    private Integer contextId;
    @RequestParameter("contextSubId")
    private Integer contextSubId;

    private String name;
    private String redirect;
    private String refresh;

    @Create
    public void init() {
        System.out.println("AlertNotificationContextUIBean: context = " + context);
        System.out.println("AlertNotificationContextUIBean: contextId = " + contextId);
        System.out.println("AlertNotificationContextUIBean: contextSubId = " + contextSubId);

        if (context.equals("template")) {
            name = "Alert Notification Template";
            redirect = create(Context.AlertNotificationTemplate, "mode", "view", "templateId", contextId);

        } else if (context.equals("resource")) {
            name = "Resource Alert Definition";
            redirect = create(Context.ResourceAlertDefinition, "mode", "viewRoles", "ad", contextId, "id", contextSubId);

        } else if (context.equals("group")) {
            name = "Group Alert Definition";
            redirect = create(Context.GroupAlertDefinition, "mode", "viewRoles", "ad", contextId, "groupId",
                contextSubId);

        } else if (context.equals("type")) {
            name = "Alert Template";
            redirect = create(Context.AlertTemplate, "mode", "viewRoles", "ad", contextId, "type", contextSubId);

        }

        refresh = create(Context.NotificationDetails, "context", context, "contextId", contextId, "contextSubId",
            contextSubId);

        System.out.println("AlertNotificationContextUIBean: name = " + name);
        System.out.println("AlertNotificationContextUIBean: redirect = " + redirect);
        System.out.println("AlertNotificationContextUIBean: refresh = " + refresh);
    }

    public String create(Context context, Object... nameValuePairs) {
        StringBuilder results = new StringBuilder();

        if (context == Context.AlertNotificationTemplate) {
            results.append("/rhq/admin/alert/template/notification/details.xhtml");
        } else if (context == Context.NotificationDetails) {
            results.append("rhq/common/alert/notification/details.xhtml");
        } else {
            results.append("/alerts/Config.do");
        }
        results.append("?");
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            if (i != 0) {
                results.append("&");
            }
            if (nameValuePairs[i + 1] != null) {
                results.append(nameValuePairs[i]);
                results.append("=");
                results.append(nameValuePairs[i + 1]);
            }
        }

        return results.toString();
    }

    public String getName() {
        return name;
    }

    public String getRedirect() {
        return redirect;
    }

    public String getRefresh() {
        return refresh;
    }

    public String getContext() {
        return context;
    }

    public Integer getContextId() {
        return contextId;
    }

    public Integer getContextSubId() {
        return contextSubId;
    }
}
