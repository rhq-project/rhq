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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;

public class MetricMetadataForm extends ActionForm {
    private Integer m; /* template id */
    private String ctype; /* autogroup's child resource type, if any */
    private String eid; /* AppdefEntityID */

    /**
     * @return String
     */
    public String getCtype() {
        return ctype;
    }

    /**
     * @return String
     */
    public String getEid() {
        return eid;
    }

    /**
     * @return Integer
     */
    public Integer getM() {
        return m;
    }

    /**
     * Sets the ctype.
     *
     * @param ctype The ctype to set
     */
    public void setCtype(String ctype) {
        this.ctype = ctype;
    }

    /**
     * Sets the eid.
     *
     * @param eid The eid to set
     */
    public void setEid(String eid) {
        this.eid = eid;
    }

    /**
     * Sets the m.
     *
     * @param m The m to set
     */
    public void setM(Integer m) {
        this.m = m;
    }

    public void reset(ActionMapping mapping, HttpServletRequest request) {
        super.reset(mapping, request);
        m = null;
        eid = null;
        ctype = null;
    }
}