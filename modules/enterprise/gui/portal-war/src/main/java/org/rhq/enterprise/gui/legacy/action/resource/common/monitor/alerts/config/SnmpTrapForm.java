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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionMapping;

/**
 * The Struts form for SnmpActionProps.jsp.
 *
 * @author Ian Springer
 */
public class SnmpTrapForm extends AddNotificationsForm {
    private static final int DEFAULT_PORT = 161;

    private String host;
    private Integer port = DEFAULT_PORT;
    private String oid;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        host = null;
        port = DEFAULT_PORT;
        oid = null;
        super.reset(mapping, request);
    }
}