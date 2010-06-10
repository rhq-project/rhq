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

import javax.faces.application.FacesMessage;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.EnterpriseFacesContextUIBean;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderInfo;
import org.rhq.enterprise.server.plugin.pc.alert.CustomAlertSenderBackingBean;

@Scope(ScopeType.EVENT)
@Name("customContentUIBean")
public class CustomContentUIBean extends EnterpriseFacesContextUIBean {

    @RequestParameter("nid")
    private Integer alertNotificationId;

    @In
    private AlertNotificationManagerLocal alertNotificationManager;

    private String contentUrl;
    private CustomAlertSenderBackingBean customBackingBean;

    public String getContentUrl() {
        return contentUrl;
    }

    @Create
    public void init() {
        if (alertNotificationId == null) {
            return;
        }

        AlertNotification activeNotification = alertNotificationManager.getAlertNotification(getSubject(),
            alertNotificationId);
        if (activeNotification == null) {
            return; // this was just removed on the previous request
        }
        String senderName = activeNotification.getSenderName();

        AlertSenderInfo info = alertNotificationManager.getAlertInfoForSender(senderName);

        if (info != null && info.getUiSnippetUrl() != null) {
            this.contentUrl = info.getUiSnippetUrl().toString();
        }

        String backingBeanName = alertNotificationManager.getBackingBeanNameForSender(senderName);
        customBackingBean = alertNotificationManager.getBackingBeanForSender(senderName, alertNotificationId);

        if (backingBeanName != null && customBackingBean != null) {
            customBackingBean.setWebUser(getSubject());
            customBackingBean.setContext(FacesContextUtility.getRequiredRequestParameter("context"));
            customBackingBean.setContextId(FacesContextUtility.getRequiredRequestParameter("contextId"));
            customBackingBean.setContextSubId(FacesContextUtility.getOptionalRequestParameter("contextSubId",
                (String) null));
            customBackingBean.setAlertNotificationId(alertNotificationId);
            customBackingBean.loadView();
            outjectBean(backingBeanName, customBackingBean);
        }
    }

    /**
     * We are just getting an Object from the plugin manager which acts as our backing bean.
     * This method is used instead of @Out or @Factory because we need to be able to
     * dynamically assign the component's name so that the plugin author can define the
     * name of bean, but this class is not an "official" seam component.
     */
    private void outjectBean(String name, CustomAlertSenderBackingBean bean) {
        Context context = Contexts.getPageContext();
        context.set(name, bean);
    }

    public String saveConfiguration() {
        try {
            customBackingBean.saveView();

            int notificationId = customBackingBean.getAlertNotificationId();
            AlertNotification notification = alertNotificationManager
                .getAlertNotification(getSubject(), notificationId);

            if (customBackingBean.getExtraParameters() != null) {
                notification.setExtraConfiguration(customBackingBean.getExtraParameters());
            }
            int alertDefinitionId = Integer.parseInt(customBackingBean.getContextId());
            alertNotificationManager.updateAlertNotification(getSubject(), alertDefinitionId, notification);

            AlertNotificationsUIBean notificationsUIBean = (AlertNotificationsUIBean) Contexts
                .lookupInStatefulContexts("alertNotificationsUIBean");
            notificationsUIBean.reselectActiveNotificationUsingDataComparison(notification);
        } catch (Throwable t) {
            FacesContextUtility.addMessage(FacesMessage.SEVERITY_ERROR, "Failed to save alert notification", t);
        }

        return OUTCOME_SUCCESS;
    }
}
