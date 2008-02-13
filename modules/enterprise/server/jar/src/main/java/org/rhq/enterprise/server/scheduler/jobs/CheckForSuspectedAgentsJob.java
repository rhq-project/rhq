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
package org.rhq.enterprise.server.scheduler.jobs;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * This is a Quartz scheduler job whose sole purpose in life is, when triggered, ask the agent manager to see if there
 * are agents that it suspects are down.
 *
 * <p>This implements {@link StatefulJob} (as opposed to {@link Job}) because we do not need nor want this job triggered
 * concurrently. That is, we don't need multiple instances of this job running at the same time.</p>
 *
 * @author John Mazzitelli
 */
public class CheckForSuspectedAgentsJob implements StatefulJob {
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LookupUtil.getAgentManager().checkForSuspectAgents();
    }
}