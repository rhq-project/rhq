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

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.rhq.bundle.ant.BundleAntProject;

/**
 * This is the base class for all custom bundle tasks. It provides all bundle tasks
 * access to the encompassing bundle ant project object so this task can report back to the 
 * project object things that the task is doing.
 *
 * As new tasks are created by extending this task object, developers must make sure
 * they add the new tasks to the bundle-ant-tasks.properties file.
 * 
 * Also provides a common method for any task to invoke to send an audit message.
 *
 * @author John Mazzitelli
 */
public abstract class AbstractBundleTask extends Task {
    // these statuses should match those of see BundleResourceDeploymentHistory.Status
    enum AuditStatus {
        SUCCESS, FAILURE, WARN
    };

    /**
     * Returns the specific {@link BundleAntProject} object that is invoking this task.
     * This task can call methods on the returned project object to inform the project
     * of things this task is doing.
     * 
     * @return the bundle Ant project object
     */
    @Override
    public BundleAntProject getProject() {
        return (BundleAntProject) super.getProject();
    }

    /**
     * Logs a message in a format that our audit task/agent-side audit log listener knows about.
     * When running in the agent, this audit log will be sent to the server.
     * It is always logged at part of the normal Ant logger mechanism.
     * 
     * @param status SUCCESS, FAILURE or WARN
     * @param action audit action, a short summary easily displayed (e.g "File Download")
     * @param info information about the action target, easily displayed (e.g. "myfile.zip")
     * @param message Optional, brief (one or two lines) information message
     * @param details Optional, verbose data, such as full file text or long error messages/stack traces  
     */
    protected void auditLog(AuditStatus status, String action, String info, String message, String details) {
        if (status == null) {
            status = AuditStatus.SUCCESS;
        }

        // this will log a message with a very specific format that is understood
        // by the agent-side build listener's messageLogged method:
        // org.rhq.plugins.ant.DeploymentAuditorBuildListener.messageLogged(BuildEvent)
        // RHQ_AUDIT_MESSAGE___<status>___<action>___<info>___<message>___<details>
        StringBuilder str = new StringBuilder("RHQ_AUDIT_MESSAGE___");
        str.append(status.name());
        str.append("___");
        str.append((action != null) ? action : "Audit Message");
        str.append("___");
        str.append((info != null) ? info : "Timestamp: " + new Date().toString());
        str.append("___");
        str.append((message != null) ? message : "");
        str.append("___");
        str.append((details != null) ? details : "");
        getProject().log(str.toString(), Project.MSG_INFO);
    }
}