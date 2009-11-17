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

package org.custom;

import org.rhq.enterprise.server.plugin.pc.ScheduledJobInvocationContext;

/**
 * Example of a stateless class that can process scheduled job invocations.
 * 
 * @author John Mazzitelli
 */
public class MyScheduledJob {
    public void executeWithContext(ScheduledJobInvocationContext jobContext) throws Exception {
        System.out.println("Sample scheduled job has been triggered! [" + this.getClass() + "] jobId="
            + jobContext.getJobDefinition().getJobId() + ", methodName="
            + jobContext.getJobDefinition().getMethodName() + ", callbackData="
            + jobContext.getJobDefinition().getCallbackData());
    }

    public void executeNoArg() throws Exception {
        System.out.println("Sample scheduled job has been triggered! [" + this.getClass() + "] NO CONTEXT!");
    }
}
