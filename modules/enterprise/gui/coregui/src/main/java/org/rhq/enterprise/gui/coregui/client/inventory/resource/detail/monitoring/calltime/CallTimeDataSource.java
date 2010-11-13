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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.calltime;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.fields.DataSourceIntegerField;
import com.smartgwt.client.data.fields.DataSourceTextField;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.RPCDataSource;

/**
 * @author Greg Hinkle
 */
public class CallTimeDataSource extends RPCDataSource<CallTimeDataComposite> {

    private double maxMaximum;

    public CallTimeDataSource() {
        List<DataSourceField> fields = addDataSourceFields();
        addFields(fields);
    }

    @Override
    protected List<DataSourceField> addDataSourceFields() {
        List<DataSourceField> fields = super.addDataSourceFields();

        DataSourceTextField callDestination = new DataSourceTextField("callDestination", "Call Destination");
        fields.add(callDestination);
        DataSourceIntegerField count = new DataSourceIntegerField("count");
        fields.add(count);
        DataSourceIntegerField minimum = new DataSourceIntegerField("minimum");
        fields.add(minimum);
        DataSourceIntegerField average = new DataSourceIntegerField("average");
        fields.add(average);
        DataSourceIntegerField maximum = new DataSourceIntegerField("maximum");
        fields.add(maximum);
        DataSourceIntegerField total = new DataSourceIntegerField("total");
        fields.add(total);

        return fields;
    }

    @Override
    protected void executeFetch(final DSRequest request, final DSResponse response) {
        int scheduleId = Integer.parseInt((String) request.getCriteria().getValues().get("scheduleId"));
        long now = System.currentTimeMillis();
        long eightHoursAgo = now - (1000L * 60 * 60 * 8);

        PageControl pc = getPageControl(request);

        GWTServiceLookup.getMeasurementDataService().findCallTimeDataForResource(scheduleId, eightHoursAgo, now, pc,
            new AsyncCallback<PageList<CallTimeDataComposite>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Could not load call time data", caught);
                }

                public void onSuccess(PageList<CallTimeDataComposite> result) {
                    Record[] data = buildRecords(result);
                    setGraphs(data);
                    response.setData(data);

                    processResponse(request.getRequestId(), response);
                }
            });
    }

    @Override
    public CallTimeDataComposite copyValues(Record from) {
        throw new UnsupportedOperationException("Calltime not editable");
    }

    public void setGraphs(Record[] records) {
        for (Record record : records) {
            if (record.getAttributeAsInt("maximum") > maxMaximum)
                maxMaximum = record.getAttributeAsInt("maximum");
        }

        for (Record record : records) {

            int minWidth = (int) ((record.getAttributeAsInt("minimum") / maxMaximum) * 100d);
            int avgWidth = (int) ((record.getAttributeAsInt("average") / maxMaximum) * 100d);
            int maxWidth = (int) ((record.getAttributeAsInt("maximum") / maxMaximum) * 100d);

            if (record instanceof ListGridRecord) {
                ListGridRecord listGridRecord = (ListGridRecord)record;
                listGridRecord.setBackgroundComponent(new HTMLFlow("<div style=\"width: " + minWidth
                    + "%; height: 33%; background-color: #A5B391;\">&nbsp;</div>" + "<div style=\"width: " + avgWidth
                    + "%; height: 33%; background-color: #A5B391;\">&nbsp;</div>" + "<div style=\"width: " + maxWidth
                    + "%; height: 33%; background-color: #A5B391;\">&nbsp;</div>"));
            }
        }
    }

    @Override
    public ListGridRecord copyValues(CallTimeDataComposite from) {
        ListGridRecord record = new ListGridRecord();

        record.setAttribute("callDestination", from.getCallDestination());

        record.setAttribute("count", from.getCount());
        record.setAttribute("minimum", from.getMinimum());
        record.setAttribute("average", from.getAverage());
        record.setAttribute("maximum", from.getMaximum());
        record.setAttribute("total", from.getTotal());

        return record;
    }
}
