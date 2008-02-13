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
package org.rhq.enterprise.gui.legacy.action;

import org.apache.struts.action.ActionMapping;

/**
 * An <code>ActionMapping</code> subclass that adds fields for participating in a workflow struts-config.xml To use:
 * <CODE><set-property property="workflow" value="server/EditGeneralProperties"/></CODE> The value is used as a key into
 * a HashMap of Queues of URLs to participate in a workflow.
 */
public class BaseActionMapping extends ActionMapping {
    /**
     * This field allows an ActionMapping to participate in a workflow, which is a Stack of returnUrls that is stored in
     * the session and retrieved for building forwards for that workflow.
     */
    private String workflow = null;

    /**
     * Flag indicating whether or not this is the first action in a workflow. If true, this resets the workflow.
     */
    private Boolean start = Boolean.TRUE;

    private String title;

    public BaseActionMapping() {
        super();
    }

    public String getWorkflow() {
        return this.workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    public Boolean getIsFirst() {
        return this.start;
    }

    public void setIsFirst(Boolean start) {
        this.start = start;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(super.toString());
        buf.append("];BaseActionMapping[");
        buf.append("workflow=").append(workflow).append(",");
        buf.append("start=").append(start).append(",");
        buf.append("title=").append(title).append(",");
        buf.append("]");
        return buf.toString();
    }
}