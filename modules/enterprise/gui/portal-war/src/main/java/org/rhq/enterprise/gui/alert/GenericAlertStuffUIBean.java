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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.web.RequestParameter;

import org.rhq.core.domain.alert.notification.AlertNotificationLog;
import org.rhq.enterprise.server.alert.AlertNotificationManagerLocal;
import org.rhq.enterprise.server.plugin.pc.alert.AlertBackingBean;
import org.rhq.enterprise.server.plugin.pc.alert.AlertSenderPluginManager;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * // TODO: Document this
 * @author Heiko W. Rupp
 */
@Name("GenericAlertStuffUIBean")
//@Scope(ScopeType.STATELESS)
public class GenericAlertStuffUIBean {

    private final Log log = LogFactory.getLog(GenericAlertStuffUIBean.class);


    @RequestParameter("sender")
    String sender;
    String value;

    public GenericAlertStuffUIBean() {
    }

    public Map<String,String> getTheMap() {

        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();
        AlertBackingBean bean = mgr.getBackingBeanForSender("Roles");
        Map<String, String> map ;
        if (bean!=null)
            map = bean.getMap();
        else
            map = new HashMap<String,String>();

        return map;
    }


    public String getTheString() {

        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();
        AlertBackingBean bean = mgr.getBackingBeanForSender("Roles");

        String res = "";
        if (bean !=null)
            res = bean.getString();

        return res;
    }

    public String submitForm() {

        AlertNotificationManagerLocal mgr = LookupUtil.getAlertNotificationManager();
        AlertBackingBean bean = mgr.getBackingBeanForSender("Roles");

        bean.submit();

        return "OK";
    }

    public String getTheValue() {
        return value;
    }

    public void setTheValue(String value) {
        this.value = value;
    }
}
