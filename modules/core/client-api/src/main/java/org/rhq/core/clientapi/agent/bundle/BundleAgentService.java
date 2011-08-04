/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.clientapi.agent.bundle;

/**
 * The interface to agent's bundle subsystem which allows the server to request
 * the agent to perform tasks on a bundle (which is essentially a bundle of content
 * that needs to be installed).
 * 
 * @author John Mazzitelli
 */
public interface BundleAgentService {
    /**
     * Schedules the deployment of a bundle to occur immediately.
     * @param request
     * @return the results of the immediate scheduling
     */
    BundleScheduleResponse schedule(BundleScheduleRequest request);

    /**
     * Purges the live deployment off the local filesystem.
     * @param request
     * @return the results of the purge
     */
    BundlePurgeResponse purge(BundlePurgeRequest request);
}