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
package org.rhq.enterprise.gui.alert;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderInfo;
import org.rhq.enterprise.server.util.LookupUtil;

@Scope(ScopeType.PAGE)
@Name("customContentUIBean")
public class CustomContentUIBean {

    @RequestParameter
    private String senderName;
    private String contentUrl;

    public String getContentUrl() {
        return contentUrl;
    }

    @Create
    public void lookupContentUrl()  {
        AlertNotificationManagerLocal alertNotificationManager = LookupUtil.getAlertNotificationManager();
        AlertSenderInfo info = alertNotificationManager.getAlertInfoForSender(this.senderName);

        if (info != null && info.getUiSnippetUrl() != null) {
            this.contentUrl = info.getUiSnippetUrl().toString();
        } else {
            this.contentUrl = "/rhq/empty.xhtml";
        }
    }
}