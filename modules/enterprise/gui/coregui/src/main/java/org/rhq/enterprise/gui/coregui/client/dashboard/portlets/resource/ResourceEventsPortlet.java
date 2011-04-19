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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupEventsPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.util.GwtTuple;
import org.rhq.enterprise.gui.coregui.client.util.MeasurementUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

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

    public ResourceEventsPortlet(String locatorId) {
        super(locatorId);
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        this.resourceId = Integer.valueOf(elements[1]);
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new ResourceEventsPortlet(locatorId);
        }
    }

    /** Fetches recent events and updates the DynamicForm instance with the latest
     *  event information over last 24hrs.
     */
    @Override
    protected void getRecentEventUpdates() {
        final int resourceId = this.resourceId;
        long end = System.currentTimeMillis();
        long start = end - (24 * 60 * 60 * 1000);

        //result timeframe if enabled
        PropertySimple property = portletConfig.getSimple(Constant.METRIC_RANGE_ENABLE);
        if (Boolean.valueOf(property.getBooleanValue())) {//then proceed setting

            boolean isAdvanced = false;
            //detect type of widget[Simple|Advanced]
            property = portletConfig.getSimple(Constant.METRIC_RANGE_BEGIN_END_FLAG);
            if (property != null) {
                isAdvanced = property.getBooleanValue();
            }
            if (isAdvanced) {
                //Advanced time settings
                property = portletConfig.getSimple(Constant.METRIC_RANGE);
                if (property != null) {
                    String currentSetting = property.getStringValue();
                    String[] range = currentSetting.split(",");
                    start = Long.valueOf(range[0]);
                    end = Long.valueOf(range[1]);
                }
            } else {
                //Simple time settings
                property = portletConfig.getSimple(Constant.METRIC_RANGE_LASTN);
                if (property != null) {
                    int lastN = property.getIntegerValue();
                    property = portletConfig.getSimple(Constant.METRIC_RANGE_UNIT);
                    int lastUnits = property.getIntegerValue();
                    ArrayList<Long> beginEnd = MeasurementUtility.calculateTimeFrame(lastN, Integer.valueOf(lastUnits));
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
                    LocatableVLayout column = new LocatableVLayout(recentEventsContent.extendLocatorId("canvas"));
                    column.setHeight(10);

                    if (!results.isEmpty()) {
                        int rowNum = 0;
                        for (GwtTuple<EventSeverity, Integer> tuple : results) {
                            // event history records do not have a usable locatorId, we'll use rownum, which is unique and
                            // may be repeatable.
                            LocatableDynamicForm row = new LocatableDynamicForm(recentEventsContent
                                .extendLocatorId(String.valueOf(rowNum++)));
                            row.setNumCols(2);
                            row.setWidth(10);//pack.

                            //icon
                            StaticTextItem iconItem = AbstractActivityView.newTextItemIcon(ImageManager
                                .getEventSeverityIcon(tuple.getLefty()), tuple.getLefty().name());
                            //count
                            StaticTextItem count = AbstractActivityView.newTextItem(String.valueOf(tuple.righty));
                            row.setItems(iconItem, count);

                            column.addMember(row);
                        }
                        //insert see more link
                        LocatableDynamicForm row = new LocatableDynamicForm(recentEventsContent.extendLocatorId(String
                            .valueOf(rowNum++)));

                        String link = LinkManager.getResourceEventHistoryListLink(resourceId);
                        AbstractActivityView.addSeeMoreLink(row, link, column);
                    } else {
                        LocatableDynamicForm row = AbstractActivityView.createEmptyDisplayRow(recentEventsContent
                            .extendLocatorId("None"), AbstractActivityView.RECENT_CRITERIA_EVENTS_NONE);
                        column.addMember(row);
                    }
                    //cleanup
                    for (Canvas child : recentEventsContent.getChildren()) {
                        child.destroy();
                    }
                    recentEventsContent.addChild(column);
                    recentEventsContent.markForRedraw();
                }
            });
    }
}