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
package org.rhq.enterprise.gui.coregui.client.test.i18n;

import java.util.List;

import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.util.rpc.RemoteServiceStatistics;
import org.rhq.enterprise.gui.coregui.client.util.rpc.RemoteServiceStatistics.Stat;

/**
 * A view that gives a display of statistics for all remote services executed since the application was loaded.
 *  
 * @author Joseph Marques
 */
public class TestRemoteServiceStatisticsView extends Table {

    private static final SortSpecifier[] defaultSorts = new SortSpecifier[] { new SortSpecifier("average",
        SortDirection.DESCENDING) };

    public TestRemoteServiceStatisticsView(String locatorId) {
        super(locatorId, "Remote Service Statistics", null, defaultSorts, null, false);
    }

    @Override
    protected void configureTable() {
        ListGridField serviceName = new ListGridField("serviceName", "Service Name");
        ListGridField methodName = new ListGridField("methodName", "Method Name");
        ListGridField count = new ListGridField("count", "Count");
        count.setAlign(Alignment.CENTER);
        ListGridField total = new ListGridField("total", "Total (ms)");
        total.setAlign(Alignment.RIGHT);
        ListGridField average = new ListGridField("average", "Average (ms)");
        average.setAlign(Alignment.RIGHT);
        ListGridField latest = new ListGridField("latest", "Latest (ms)");
        latest.setAlign(Alignment.RIGHT);
        ListGridField slowest = new ListGridField("slowest", "Slowest (ms)");
        slowest.setAlign(Alignment.RIGHT);
        ListGridField fastest = new ListGridField("fastest", "Fastest (ms)");
        fastest.setAlign(Alignment.RIGHT);

        getListGrid().setFields(serviceName, methodName, count, total, average, latest, slowest, fastest);
        getListGrid().setRecords(transform(RemoteServiceStatistics.getAll()));
    }

    private ListGridRecord[] transform(List<Stat> stats) {
        ListGridRecord[] results = new ListGridRecord[stats.size()];
        for (int i = 0; i < stats.size(); i++) {
            results[i] = transform(stats.get(i));
        }
        return results;
    }

    private ListGridRecord transform(Stat stat) {
        ListGridRecord record = new ListGridRecord();
        record.setAttribute("serviceName", stat.getServiceName());
        record.setAttribute("methodName", stat.getMethodName());
        record.setAttribute("count", stat.getCount());
        record.setAttribute("total", stat.getTotal());
        record.setAttribute("average", stat.getTotal() / stat.getCount());
        record.setAttribute("latest", stat.getLatest());
        record.setAttribute("slowest", stat.getSlowest());
        record.setAttribute("fastest", stat.getFastest());
        return record;
    }

}
