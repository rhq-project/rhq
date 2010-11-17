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

import java.util.EnumSet;

import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableLayout;

/**
 * @author Greg Hinkle
 */
public class CallTimeView extends LocatableLayout {

    private Resource resource;
    private int scheduleId;

    public CallTimeView(String locatorId, Resource resource) {
        super(locatorId);
        this.resource = resource;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {

                    for (final MeasurementDefinition def : type.getMetricDefinitions()) {
                        if (def.getDataType() == DataType.CALLTIME) {

                            ResourceCriteria criteria = new ResourceCriteria();
                            criteria.addFilterId(resource.getId());
                            criteria.fetchSchedules(true);

                            GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
                                new AsyncCallback<PageList<Resource>>() {
                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError("Failed to load resource for call time",
                                            caught);
                                    }

                                    public void onSuccess(PageList<Resource> result) {
                                        if (result.size() == 1) {

                                            Resource res = result.get(0);
                                            for (MeasurementSchedule s : res.getSchedules()) {

                                                if (s.getDefinition().getId() == def.getId()) {

                                                    scheduleId = s.getId();

                                                    setup();
                                                    return;
                                                }
                                            }
                                        }
                                        setupNone();
                                    }
                                });
                            break;

                        }

                    }

                }
            });

    }

    public void setupNone() {
        addMember(new Label("No calltime data available for this resource"));
    }

    public void setup() {

        Table table = new Table(extendLocatorId("Table"), "Call Time Data", new Criteria("scheduleId", String
            .valueOf(scheduleId)));
        table.getListGrid().setAlternateRecordStyles(false);
        table.setDataSource(new CallTimeDataSource());
        table.getListGrid().setUseAllDataSourceFields(true);

        final NumberFormat format = NumberFormat.getFormat("0");

        ListGridField callDestination = new ListGridField("callDestination", "Call Destination");
        ListGridField count = new ListGridField("count", 70);
        ListGridField minimum = new ListGridField("minimum", 70);
        ListGridField average = new ListGridField("average", 70);        
        average.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return format.format(((Number) o).doubleValue());
            }
        });
        ListGridField maximum = new ListGridField("maximum", 70);
        ListGridField total = new ListGridField("total", 70);

        table.getListGrid().setFields(callDestination, count, minimum, average, maximum, total);

        addMember(table);
        markForRedraw();
    }

}
