/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
package org.rhq.enterprise.server.storage.maintenance;

/**
 * Specifies what to do when a step fails. We may need to turn this into a callback interface of sorts if it turns out
 * that there is special cases that cannot be easily handled by StorageClusterMaintenanceManagerBean where it would be
 * simpler for the step runner to carry out whatever work is necessary for failure situations.
 *
 * @author John Sanda
 */
public enum StepFailureStrategy {

    /**
     * Abort the job and reschedule it for later execution.
     */
    ABORT,

    /**
     * Continue execution of the job. A new job will be created for the failed step and added to the queue.
     *
     */
    CONTINUE;

    public static StepFailureStrategy fromString(String strategy) {
        if (strategy.equals(ABORT.toString())) {
            return ABORT;
        } else if (strategy.equals(CONTINUE.toString())) {
            return CONTINUE;
        } else {
            throw new IllegalArgumentException(strategy + " is not a " + StepFailureStrategy.class.getSimpleName());
        }
    }

}
