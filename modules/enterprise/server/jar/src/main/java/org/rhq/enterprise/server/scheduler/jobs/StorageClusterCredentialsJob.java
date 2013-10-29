/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Stefan Negrea
 */
public class StorageClusterCredentialsJob extends AbstractStatefulJob {

    private Log log = LogFactory.getLog(StorageClusterCredentialsJob.class);

    @Override
    public void executeJobCode(JobExecutionContext context) throws JobExecutionException {
        log.debug("Run credential update job for the storage cluster.");
        boolean success = LookupUtil.getStorageClientManager().refreshCredentialsAndSession();
        if (!success) {
            log.error("Storage session credentials not succesfully refreshed");
        } else {
            log.debug("Storage session credentials succesfully refreshed");
        }
    }
}
