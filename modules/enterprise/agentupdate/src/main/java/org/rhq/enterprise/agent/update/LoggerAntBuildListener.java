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
package org.rhq.enterprise.agent.update;

import java.io.PrintWriter;
import java.util.Date;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.Task;

/**
 * Listens for ANT build events and logs them to a log stream.
 *
 * @author John Mazzitelli
 */
public class LoggerAntBuildListener implements BuildListener {
    private final String mainTarget;
    private final PrintWriter output;
    private final int priorityThreshold;

    public LoggerAntBuildListener(String target, PrintWriter logFile, int priorityThreshold) {
        this.mainTarget = (target != null) ? target : "(default)";
        this.output = logFile;
        this.priorityThreshold = priorityThreshold;

        // start with the date this was started
        this.output.println("======================================");
        this.output.println("ANT target [" + this.mainTarget + "]");
        this.output.println(new Date().toString());
        this.output.println("======================================");
    }

    public void buildFinished(BuildEvent event) {
        logEvent(event, "FINISHED! [" + this.mainTarget + ']');
    }

    public void buildStarted(BuildEvent event) {
        logEvent(event, "STARTED! [" + this.mainTarget + ']');
    }

    public void messageLogged(BuildEvent event) {
        logEvent(event, null);
    }

    public void targetFinished(BuildEvent event) {
        logEvent(event, null);
    }

    public void targetStarted(BuildEvent event) {
        logEvent(event, null);
    }

    public void taskFinished(BuildEvent event) {
        logEvent(event, null);
    }

    public void taskStarted(BuildEvent event) {
        logEvent(event, null);
    }

    private void logEvent(BuildEvent event, String additionalMessage) {
        if (event.getPriority() > this.priorityThreshold) {
            return;
        }

        String message = event.getMessage();
        Throwable exception = event.getException();
        Target target = event.getTarget();
        Task task = event.getTask();

        if (additionalMessage != null) {
            output.println(additionalMessage);
        }

        if (target != null) {
            output.print("[" + target.getName() + "] ");
        }

        if (task != null) {
            output.print("[" + task.getTaskName() + "] ");
        }

        if (message != null) {
            output.print(message);
        }

        if (exception != null) {
            output.println();
            exception.printStackTrace(output);
        }

        output.println();
        output.flush();

        return;
    }
}