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

package org.rhq.coregui.client.dashboard;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.coregui.client.LoginView;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.inventory.AutoRefresh;
import org.rhq.coregui.client.util.MeasurementUtility;

/**
 * @author Jay Shaughnessy
 */
public class AutoRefreshUtil {

    public static Timer startRefreshCycleWithPageRefreshInterval(final AutoRefresh autoRefresh,
        final Canvas autoRefreshCanvas, Timer refreshTimer) {

        final int refreshInterval = UserSessionManager.getUserPreferences().getPageRefreshInterval();

        return startRefreshCycle(autoRefresh, autoRefreshCanvas, refreshTimer, refreshInterval, (int) MeasurementUtility.MINUTES);
    }

    public static Timer startRefreshCycle(final AutoRefresh autoRefresh, final Canvas autoRefreshCanvas, Timer refreshTimer, int intervalMillis) {
        return startRefreshCycle(autoRefresh, autoRefreshCanvas, refreshTimer, intervalMillis, -1);
    }

    private static Timer startRefreshCycle(final AutoRefresh autoRefresh, final Canvas autoRefreshCanvas, Timer refreshTimer, int intervalMillis, int minIntervalMillis) {
        //cancel any existing timer
        if (null != refreshTimer) {
            refreshTimer.cancel();
            refreshTimer = null;
        }

        if (minIntervalMillis <= 0 || intervalMillis >= minIntervalMillis) {

            refreshTimer = new Timer() {
                public void run() {

                    // if the autoRefresh component is already refreshing or is not currently on screen then
                    // don't bother doing the work. this protects against unnecessary or unwanted db queries
                    // being performed in the background. Also, avoid refresh if the session has expired and we're
                    // waiting for the user to login and refresh his session.
                    if (!autoRefresh.isRefreshing() && autoRefreshCanvas.isDrawn() && autoRefreshCanvas.isVisible()
                        && !autoRefreshCanvas.isDisabled() && !LoginView.isLoginShowing()) {

                        autoRefresh.refresh();
                        UserSessionManager.refresh();
                    }
                }
            };

            refreshTimer.scheduleRepeating(intervalMillis);
        }

        return refreshTimer;
    }

    public static void onDestroy(Timer refreshTimer) {

        if (refreshTimer != null) {

            refreshTimer.cancel();
        }

    }

}
