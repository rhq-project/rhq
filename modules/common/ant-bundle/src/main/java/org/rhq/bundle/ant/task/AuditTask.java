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

import java.util.Date;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

/**
 * The rhq:audit task is a way recipe authors can add their own audit messages to the stream
 * of audit messages that the server gets to see how the progress went with the provisioning of a bundle.
 *
 * &lt;rhq:audit status='SUCCESS|WARN|FAILURE' action="My Step" info="summary info" message="intermediate details">
 *    long details here
 * &lt;/rhq:audit>
 *
 * @author John Mazzitelli
 */
public class AuditTask extends AbstractBundleTask {
    private String status = "SUCCESS"; // must match one of SUCCESS, WARN, or FAILURE (see BundleResourceDeploymentHistory.Status)
    private String action = null;
    private String info = null;
    private String message = "";
    private String details = "";

    @Override
    public void maybeConfigure() throws BuildException {
        super.maybeConfigure(); // inits the attribute fields
        validateAttributes();
    }

    @Override
    public void execute() throws BuildException {
        // this will log a message with a very specific format that is understood
        // by the agent-side build listener's messageLogged method:
        // org.rhq.plugins.ant.DeploymentAuditorBuildListener.messageLogged(BuildEvent)
        // RHQ_AUDIT_MESSAGE___<status>___<action>___<info>___<message>___<details>
        StringBuilder str = new StringBuilder("RHQ_AUDIT_MESSAGE___");
        str.append(this.status);
        str.append("___");
        str.append((this.action != null) ? this.action : "Audit Message");
        str.append("___");
        str.append((this.info != null) ? this.info : "Timestamp: " + new Date().toString());
        str.append("___");
        str.append(this.message);
        str.append("___");
        str.append(this.details);
        getProject().log(str.toString(), Project.MSG_INFO);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    protected void validateAttributes() throws BuildException {
        if (this.status == null) {
            this.status = "SUCCESS";
        } else if (!this.status.equalsIgnoreCase("SUCCESS") && !this.status.equalsIgnoreCase("FAILURE")
            && !this.status.equalsIgnoreCase("WARN")) {
            throw new BuildException("The 'result' attribute must be either 'SUCCESS', 'WARN' or 'FAILURE'");
        }
        this.status = this.status.toUpperCase();
    }

}