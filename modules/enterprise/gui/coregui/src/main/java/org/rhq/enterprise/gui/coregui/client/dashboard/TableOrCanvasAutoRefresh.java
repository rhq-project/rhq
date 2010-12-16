/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.dashboard;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;

/**An implementation of a shared portet refresh utility.
 * @author Simeon Pinder
 */
public class TableOrCanvasAutoRefresh {

    private Timer reloader;

    /** reloads the Table or Canvas page based on the pageRefreshInterval saved to the
     *  current user's settings.
     */
    public TableOrCanvasAutoRefresh(final Canvas toBeReloaded) {
        if (toBeReloaded != null) {
            //current setting
            final int retrievedRefreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();
            if (this.reloader != null) {
                this.reloader.cancel();
            }
            if (retrievedRefreshInterval >= MeasurementUtility.MINUTES) {
                this.reloader = new Timer() {
                    public void run() {
                        if (toBeReloaded instanceof Table) {
                            ((Table) toBeReloaded).refresh();
                        } else {
                            ((Canvas) toBeReloaded).redraw();
                        }

                        //launch again until shedule stopped or GC.
                        reloader.schedule(retrievedRefreshInterval);
                    }
                };
                this.reloader.schedule(retrievedRefreshInterval);
            }
        }
    }

    //stops the timer from previous run.
    public void stopTimer() {
        if (this.reloader != null) {
            this.reloader.cancel();
        }
    }
}
