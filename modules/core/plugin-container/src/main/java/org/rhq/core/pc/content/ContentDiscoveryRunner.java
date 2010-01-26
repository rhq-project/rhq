 /*
  * RHQ Management Platform
  * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.pc.content;

import java.util.concurrent.Callable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.server.content.ContentDiscoveryReport;

 /**
 * Thread run by the {@link ContentManager} to perform content discoveries. Much of the logic in these discoveries takes
 * place in that manager; this is largely just a <code>Runnable</code> implementation in order to thread the
 * discoveries.
 *
 * @author Jason Dobies
 */
public class ContentDiscoveryRunner implements Runnable, Callable<ContentDiscoveryReport> {
    // Attributes  --------------------------------------------

    private final Log log = LogFactory.getLog(ContentDiscoveryRunner.class);

    /**
     * If specified, this constrains this instance to running the described discovery. If this is not present, this
     * class will ask the manager for the next discovery to run.
     */
    private ScheduledContentDiscoveryInfo discoveryInfo;

    /**
     * Indicates the execution of this runner is intended for a one time execution. In other words, any scheduling logic
     * should not be performed.
     */
    private boolean oneTimeDiscovery;

    /**
     * Hook into the content manager to perform the content handling logic.
     */
    private ContentManager contentManager;

    // Constructors  --------------------------------------------

    /**
     * Creates a new runner that will contact the manager to determine the next piece of scheduled content work that
     * needs to take place.
     *
     * @param contentManager hook to the manager
     */
    public ContentDiscoveryRunner(ContentManager contentManager) {
        this.contentManager = contentManager;
        this.discoveryInfo = null;
        this.oneTimeDiscovery = false;
    }

    /**
     * Creates a new runner that will be scoped to performing the specified discovery.
     *
     * @param contentManager hook to the manager
     * @param discoveryInfo  specific discovery to perform
     */
    public ContentDiscoveryRunner(ContentManager contentManager, ScheduledContentDiscoveryInfo discoveryInfo) {
        this.contentManager = contentManager;
        this.discoveryInfo = discoveryInfo;
        this.oneTimeDiscovery = true;
    }

    // Runnable Implementation  --------------------------------------------

    public void run() {
        try {
            call();
        } catch (Exception e) {
            log.error("Content discovery runner failed", e);
        }
    }

    // Callable Implementation  --------------------------------------------

    public ContentDiscoveryReport call() throws Exception {
        ScheduledContentDiscoveryInfo info = this.discoveryInfo;

        // If this instance is not scoped to a particular discovery, get the next discovery chunk of work from the manager
        if (info == null) {
            info = this.contentManager.getNextScheduledDiscovery();

            // If there is still no discovery waiting to happen, do nothing
            if (info == null) {
                return null;
            }

            // Check to see if the discovery is executing too far past its next scheduled discovery
            // If it is, skip this discovery and increment its next discovery.
            if ((System.currentTimeMillis() - 120000L) > info.getNextDiscovery()) {
                log.debug("Content discovery is falling behind. Missed discovery for " + info + " by: "
                    + (System.currentTimeMillis() - info.getNextDiscovery()) + "ms");
            }
        }

        log.debug("Performing discovery: " + info);

        // Catch any error that may occur on the plugin
        ContentDiscoveryReport result = null;
        try {
            result = contentManager.performContentDiscovery(info.getResourceId(), info.getPackageType());
        } catch (Throwable throwable) {
            log.warn("Exception received from component while attempting content retrieval", throwable);
        } finally {
            // Reschedule after this report has been sent (or failed). Putting this here will delay further discoveries
            // if the limited concurrency puts off sending this message for a while.
            if (!oneTimeDiscovery) {                
                contentManager.rescheduleDiscovery(info);
            }
        }

        return result;
    }
}