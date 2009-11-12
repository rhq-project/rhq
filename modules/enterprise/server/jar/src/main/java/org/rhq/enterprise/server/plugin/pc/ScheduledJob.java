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

package org.rhq.enterprise.server.plugin.pc;

import java.util.Properties;

/**
 * An interface to be implemented by any server plugin component that needs to be scheduled.
 *   
 * @author John Mazzitelli
 */
public interface ScheduledJob {
    /**
     * Called when the job triggers.
     * 
     * @param jobId the job identifier that was used when creating the job
     * @param lifecycleListener if the plugin has a lifecycle listener, this will be it.
     *                          Use this object to obtain the plugin context and any other
     *                          stateful plugin data specific to the plugin being invoked.
     *                          This may be <code>null</code> if the plugin did not define
     *                          a global lifecycle listener.
     * @param callbackData a map of data that was used when creating the job whose values are
     *                     passed back to this method as they were when the job was created.
     *
     * @throws Exception on any error that should cause the schedule to not fire. If an exception
     *                   is thrown, the schedule will not trigger again until the plugin is restarted
     */
    void execute(String jobId, ServerPluginLifecycleListener lifecycleListener, Properties callbackData)
        throws Exception;
}
