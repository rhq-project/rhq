/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.bundle.ant.task;

import org.apache.tools.ant.BuildException;

import org.rhq.bundle.ant.BundleAntProject.AuditStatus;

/**
 * The rhq:audit task is a way recipe authors can add their own audit messages to the stream
 * of audit messages that the server gets to see how the progress went with the provisioning of a bundle.
 *
 * &lt;rhq:audit status='SUCCESS|INFO|WARN|FAILURE' action="My Step" info="summary info" message="intermediate details">
 *    long details here
 * &lt;/rhq:audit>
 *
 * @author John Mazzitelli
 */
public class AuditTask extends AbstractBundleTask {
    private AuditStatus status = AuditStatus.SUCCESS; // see BundleResourceDeploymentHistory.Status
    private String action = null;
    private String info = null;
    private String message = "";
    private String details = "";

    @Override
    public void maybeConfigure() throws BuildException {
        super.maybeConfigure(); // inits the attribute fields
    }

    @Override
    public void execute() throws BuildException {
        getProject().auditLog(status, action, info, message, details);
    }

    public AuditStatus getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (this.status == null) {
            this.status = AuditStatus.SUCCESS;
        } else {
            try {
                this.status = AuditStatus.valueOf(status.toUpperCase());
            } catch (Exception e) {
                throw new BuildException("The 'status' attribute must be either 'SUCCESS', 'INFO', 'WARN' or 'FAILURE'");
            }
        }
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = (message != null) ? message : "";
    }

    /**
     * Set a multiline message.
     * @param msg the CDATA text to append to the message text
     */
    public void addText(String msg) {
        if (msg != null) {
            this.details += getProject().replaceProperties(msg);
        }
    }
}