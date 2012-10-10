/*
 * RHQ Management Platform
 * Copyright 2010-2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring;

import java.util.ArrayList;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.grid.ListGridField;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.components.measurement.UserPreferencesMeasurementRangeEditor;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.HasViewName;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;

/**
 * A view that displays a paginated table of calltime (aka response time) data.
 *
 * @author John Mazzitelli
 */
public class CalltimeView extends TableSection<CalltimeDataSource> implements HasViewName {

    public static final ViewName SUBSYSTEM_VIEW_ID = new ViewName("CalltimeData",
        MSG.view_resource_monitor_calltime_title(), IconEnum.CALLTIME);

    private TextItem destinationFilter;

    // for subsystem views
    public CalltimeView(String locatorId) {
        this(locatorId, EntityContext.forSubsystemView());
    }

    public CalltimeView(String locatorId, EntityContext context) {
        super(locatorId, SUBSYSTEM_VIEW_ID.getTitle());
        setDataSource(new CalltimeDataSource(context));
        destinationFilter = new TextItem(CalltimeDataSource.FILTER_DESTINATION,
            MSG.view_resource_monitor_calltime_destinationFilter());
    }

    @Override
    protected void configureTableFilters() {
        setFilterFormItems(this.destinationFilter);
    }

    @Override
    protected boolean isDetailsEnabled() {
        return false; // we don't have more details other than what the main table shows
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return null; // we do not support detail views
    }

    @Override
    protected void configureTable() {
        ArrayList<ListGridField> dataSourceFields = getDataSource().getListGridFields();
        getListGrid().setFields(dataSourceFields.toArray(new ListGridField[dataSourceFields.size()]));
        addExtraWidget(new UserPreferencesMeasurementRangeEditor(extendLocatorId("range")), true);

        super.configureTable();
    }

    @Override
    public ViewName getViewName() {
        return SUBSYSTEM_VIEW_ID;
    }
}
