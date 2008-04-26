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

package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.visibility;

import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.legacy.KeyConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

/**
 * Utility class to get / save / remove charts from the dashboard
 *
 * Charts are saved in the RHQ_CONFIG_PROPERTY table. Each is saved in its own entry in the DB table under the name
 * ".dashContent.charts.x" where x is a number starting at 0
 * @author Jessica Sant
 */
public class ChartUtility {
    private final Log log = LogFactory.getLog(ChartUtility.class.getName());

    WebUser webUser = null;

    public ChartUtility( WebUser webUser )
    {
        this.webUser = webUser;
    }


    /**
     * Retrieves all charts stored in the user preferences and returns them as a list
     * @return ArrayList of chart names and urls
     * @throws Exception
     */
    public ArrayList<String> getAllChartsAsList() throws Exception
    {
        ArrayList<String> chartsList = new ArrayList<String>(2);
        int counter = 0;
        try {
            String chart = null;
            do
            {
                chart = webUser.getPreference(KeyConstants.USER_DASHBOARD_CHARTS + "." + counter);
                if( chart != null && !chart.equals(""))
                {
                    chartsList.add( counter, webUser.getPreference(KeyConstants.USER_DASHBOARD_CHARTS + "." + counter) );
                }
                counter++;
            } while ( chart != null && !chart.equals("") );

        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled())
                log.debug("Preferences for user " + webUser.getName() + " not found: " + e.getMessage());
        }

        return chartsList;
    }


    /**
     * Saves the new chart to the user's preferences
     * @param newChart the new chart to be stored
     * @param chartsList list of existing charts
     * @throws Exception
     */
    public void saveNewChart(String newChart, ArrayList<String> chartsList) throws Exception {
        try {
            webUser.setPreference(KeyConstants.USER_DASHBOARD_CHARTS +"." + chartsList.size(), newChart );

        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled())
                log.debug("Preferences for user " + webUser.getName() + " not found: " + e.getMessage());
        }

    }


    /**
     * Removes the given chart from the user's preferences
     * @param removeChart the chart to be removed
     * @return true if the chart is removed successfully, false otherwise
     * @throws Exception
     */
    public boolean remove( String removeChart) throws Exception
    {
        boolean removed = false;
        try {
            ArrayList<String> chartsList = this.getAllChartsAsList();
            for( int i = 0; i < chartsList.size(); i++ )
            {
                //remove all the charts, then add them all back except the one to be removed
                webUser.unsetPreference(KeyConstants.USER_DASHBOARD_CHARTS + "." + i);
            }
            removed = chartsList.remove(removeChart);
            for( int i = 0; i < chartsList.size(); i++ )
            {
                webUser.setPreference(KeyConstants.USER_DASHBOARD_CHARTS + "." + i, chartsList.get(i) );
            }
        } catch (IllegalArgumentException e) {
            if (log.isDebugEnabled())
                log.debug("Preferences for user " + webUser.getName() + " not found: " + e.getMessage());
        }
        return removed;
    }
    
}
