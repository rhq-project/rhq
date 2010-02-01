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
package org.rhq.core.util.progresswatch;

/**
 * Simple class who's goal is to:
 * 
 * 1) Total up the amount of work to be done
 * 2) Track the amount of work that is actually done
 * 3) Report the % of the work completed
 * 
 * The amount of work is arbitrary and stored in simple integer format.  Percent complete is calculated 
 * based on the finished work divided by the work remaining.
 * 
 * Possible additions to the future could add ability to track timing (current, remaining estimate).
 *  
 * @author mmccune
 *
 */
public class ProgressWatcher {

    private boolean started = false;
    private int totalWork = -1;
    private int finishedWork = -1;

    /**
     * Start watching the progress of a given amount of work.
     */
    public void start() {
        totalWork = 0;
        finishedWork = 0;
        started = true;
    }

    /**
     * Get the percentage complete of the total work specified.
     * @return float 0-100% of the amount of work copleted.  integer so no decimal points.
     * @throws IllegalStateException if this ProgressWatcher has not been started yet.
     */
    public int getPercentComplete() throws IllegalStateException {
        if (!started) {
            throw new IllegalStateException(this.getClass().getSimpleName()
                + " not started yet. call start() to set progress to 0 and start watching.");
        }
        if (totalWork == 0) {
            return 0;
        } else {
            float percentComp = (((float) finishedWork / (float) totalWork) * 100);
            return (int) percentComp;
        }

    }

    /**
     * Set the total amount of work to be completed.
     * 
     * @param totalWorkIn to set.
     */
    public void setTotalWork(int totalWorkIn) {
        this.totalWork = totalWorkIn;
    }

    /**
     * Add a unit of work to be completed.
     * 
     * @param workToAdd 
     */
    public void addWork(int workToAdd) {
        totalWork = totalWork + workToAdd;
    }

    /**
     * Indicate that a # of work units has been completed.
     * 
     * @param workToRemove
     */
    public void finishWork(int workToRemove) {
        if (!started) {
            throw new IllegalStateException(this.getClass().getSimpleName()
                + " not started yet. call start() to set progress to 0 and start watching.");
        }
        finishedWork += workToRemove;
    }

    /**
     * Indicate this ProgressWatcher is finished watching.
     */
    public void stop() {
        this.started = false;
    }

    /**
     * Reset the ProgressWatcher to zero.
     */
    public void resetToZero() {
        stop();
        start();
    }
}
