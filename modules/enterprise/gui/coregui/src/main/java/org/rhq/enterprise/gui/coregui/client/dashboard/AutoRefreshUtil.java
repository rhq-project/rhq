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

package org.rhq.enterprise.gui.coregui.client.dashboard;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.inventory.AutoRefresh;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;

/**
 * @author Jay Shaughnessy
 */
public class AutoRefreshUtil {

    public static Timer startRefreshCycle(final AutoRefresh autoRefresh,
        final Canvas autoRefreshCanvas, Timer refreshTimer) {

        final int refreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();

        //cancel any existing timer
        if (null != refreshTimer) {
            refreshTimer.cancel();
            refreshTimer = null;
        }

        if (refreshInterval >= MeasurementUtility.MINUTES) {

            refreshTimer = new Timer() {
                public void run() {

                    // if the autoRefresh component is already refreshing or is not currently on screen then
                    // don't bother doing the work. this protects against unnecessary or unwanted db queries
                    // being performed in the background.
                    if (!autoRefresh.isRefreshing() && autoRefreshCanvas.isDrawn()
                        && autoRefreshCanvas.isVisible()) {

                        autoRefresh.refresh();
                    }
                }
            };

            refreshTimer.scheduleRepeating(refreshInterval);
        }

        return refreshTimer;
    }

    public static void onDestroy(Timer refreshTimer) {

        if (refreshTimer != null) {

            refreshTimer.cancel();
        }

    }

}
