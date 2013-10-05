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
package org.rhq.coregui.client.dashboard.portlets.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupEventsPortlet;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.coregui.client.util.GwtTuple;
import org.rhq.coregui.client.util.Log;
import org.rhq.coregui.client.util.MeasurementUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;

/**This portlet allows the end user to customize the Events display
 *
 * @author Simeon Pinder
 */
public class ResourceEventsPortlet extends GroupEventsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceEvents";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resource_events();

    private int resourceId = -1;

    public ResourceEventsPortlet(int resourceId) {
        super(null);
        this.resourceId = resourceId;
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            if (EntityContext.Type.Resource != context.getType()) {
                throw new IllegalArgumentException("Context [" + context + "] not supported by portlet");
            }

            return new ResourceEventsPortlet(context.getResourceId());
        }
    }

    /** Fetches recent events and updates the DynamicForm instance with the latest
     *  event information over last 24hrs.
     */
    @Override
    protected void getRecentEventUpdates() {
        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();
        final int resourceId = this.resourceId;
        long end = System.currentTimeMillis();
        long start = end - (24 * 60 * 60 * 1000);

        //result timeframe if enabled
        PropertySimple property = portletConfig.getSimple(Constant.METRIC_RANGE_ENABLE);
        if (null != property && Boolean.valueOf(property.getBooleanValue())) {//then proceed setting

            boolean isAdvanced = Boolean.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_BEGIN_END_FLAG,
                Constant.METRIC_RANGE_BEGIN_END_FLAG_DEFAULT));
            if (isAdvanced) {
                //Advanced time settings
                String currentSetting = portletConfig.getSimpleValue(Constant.METRIC_RANGE,
                    Constant.METRIC_RANGE_DEFAULT);
                String[] range = currentSetting.split(",");
                if (range.length == 2) {
                    start = Long.valueOf(range[0]);
                    end = Long.valueOf(range[1]);
                }
            } else {
                //Simple time settings
                property = portletConfig.getSimple(Constant.METRIC_RANGE_LASTN);
                if (property != null) {
                    int lastN = Integer.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_LASTN,
                        Constant.METRIC_RANGE_LASTN_DEFAULT));
                    int units = Integer.valueOf(portletConfig.getSimpleValue(Constant.METRIC_RANGE_UNIT,
                        Constant.METRIC_RANGE_UNIT_DEFAULT));
                    ArrayList<Long> beginEnd = MeasurementUtility.calculateTimeFrame(lastN, units);
                    start = Long.valueOf(beginEnd.get(0));
                    end = Long.valueOf(beginEnd.get(1));
                }
            }
        }

        GWTServiceLookup.getEventService().getEventCountsBySeverity(resourceId, start, end,
            new AsyncCallback<Map<EventSeverity, Integer>>() {

                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving event counts for resource [" + resourceId + "]:" + caught.getMessage());
                    setCurrentlyRefreshing(false);
                }

                @Override
                public void onSuccess(Map<EventSeverity, Integer> eventCounts) {
                    //Now populated Tuples
                    List<GwtTuple<EventSeverity, Integer>> results = new ArrayList<GwtTuple<EventSeverity, Integer>>();
                    for (EventSeverity severity : eventCounts.keySet()) {
                        int count = eventCounts.get(severity);
                        if (count > 0) {
                            results.add(new GwtTuple<EventSeverity, Integer>(severity, count));
                        }
                    }
                    //build display
                    EnhancedVLayout column = new EnhancedVLayout();
                    column.setHeight(10);

                    if (!results.isEmpty()) {
                        int rowNum = 0;
                        for (GwtTuple<EventSeverity, Integer> tuple : results) {
                            DynamicForm row = new DynamicForm();
                            row.setNumCols(2);
                            row.setWidth(10);//pack.

                            //icon
                            StaticTextItem iconItem = AbstractActivityView.newTextItemIcon(
                                ImageManager.getEventSeverityIcon(tuple.getLefty()), tuple.getLefty().name());
                            //count
                            StaticTextItem count = AbstractActivityView.newTextItem(String.valueOf(tuple.righty));
                            row.setItems(iconItem, count);

                            column.addMember(row);
                        }
                        //insert see more link
                        DynamicForm row = new DynamicForm();

                        String link = LinkManager.getResourceEventHistoryListLink(resourceId);
                        AbstractActivityView.addSeeMoreLink(row, link, column);
                    } else {
                        DynamicForm row = AbstractActivityView
                            .createEmptyDisplayRow(AbstractActivityView.RECENT_CRITERIA_EVENTS_NONE);
                        column.addMember(row);
                    }
                    //cleanup
                    for (Canvas child : recentEventsContent.getChildren()) {
                        child.destroy();
                    }
                    recentEventsContent.addChild(column);
                    setCurrentlyRefreshing(false);
                    recentEventsContent.markForRedraw();
                }
            });
    }
}