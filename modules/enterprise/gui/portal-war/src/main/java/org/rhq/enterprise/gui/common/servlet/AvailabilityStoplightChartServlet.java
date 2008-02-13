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
package org.rhq.enterprise.gui.common.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.enterprise.gui.image.chart.AvailabilityReportChart;
import org.rhq.enterprise.gui.image.chart.Chart;

public class AvailabilityStoplightChartServlet extends ChartServlet {
    Log log = LogFactory.getLog(AvailabilityStoplightChartServlet.class.getName());

    /**
     */
    public AvailabilityStoplightChartServlet() {
        super();
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.common.servlet.ChartServlet#createChart()
     */
    @Override
    protected Chart createChart() {
        return new AvailabilityReportChart();
    }

    /* (non-Javadoc)
     * @see org.rhq.enterprise.gui.common.servlet.ChartServlet#plotData(javax.servlet.http.HttpServletRequest)
     */
    @Override
    protected void plotData(HttpServletRequest request, Chart chart) throws ServletException {
        //        Subject subject = RequestUtils.getSubject(request);
        //
        //        Integer resourceId = RequestUtils.getResourceId(request);
        //
        //        // the child resource type
        //        AppdefEntityTypeID ctype = RequestUtils.getHqChildResourceTypeId(request);
        //
        //        MeasurementBoss boss = ContextUtils.getMeasurementBoss( getServletContext() );
        //        try {
        //            MeasurementSummary summary = boss.getSummarizedResourceAvailability(0,
        //                null, ctype.getType(), ctype.getId());
        //            AvailabilityReportChart availChart = (AvailabilityReportChart) chart;
        //            DataPointCollection data = availChart.getDataPoints();
        //            data.clear();
        //           for (Integer integer : summary.asList())
        //           {
        //              Integer avail = (Integer)integer;
        //              data.add(new AvailabilityDataPoint(avail));
        //           }
        //
        //        } catch (AppdefEntityNotFoundException e) {
        //            log.error("failed: ", e);
        //        } catch (SessionTimeoutException e) {
        //            log.error("failed: ", e);
        //        } catch (SessionNotFoundException e) {
        //            log.error("failed: ", e);
        //        } catch (PermissionException e) {
        //            log.error("failed: ", e);
        //        } catch (IllegalArgumentException e) {
        //            log.error("failed: ", e);
        //        } catch (RemoteException e) {
        //            log.error("failed: ", e);
        //        }
        throw new IllegalStateException("deprecated code");
    }
}