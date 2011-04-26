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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.components.FullHTMLPane;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups.GroupOobsPortlet;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.enterprise.gui.coregui.client.util.GwtRelativeDurationConverter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**This portlet allows the end user to customize the OOB display
 *
 * @author Simeon Pinder
 */
public class ResourceOobsPortlet extends GroupOobsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceOobs";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resource_oobs();

    private int resourceId = -1;

    public ResourceOobsPortlet(String locatorId) {
        super(locatorId);
        //figure out which page we're loading
        String currentPage = History.getToken();
        String[] elements = currentPage.split("/");
        this.resourceId = Integer.valueOf(elements[1]);
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {
            return new ResourceOobsPortlet(locatorId);
        }
    }

    /** Fetches OOB measurements and updates the DynamicForm instance with the latest N
     *  oob change details.
     */
    @Override
    protected void getRecentOobs() {
        final int resourceId = this.resourceId;
        int resultCount = 5;//default to

        //result count
        PropertySimple property = portletConfig.getSimple(Constant.RESULT_COUNT);
        if (property != null) {
            String currentSetting = property.getStringValue();
            if (currentSetting.trim().isEmpty() || currentSetting.equalsIgnoreCase("5")) {
                resultCount = 5;
            } else {
                resultCount = Integer.valueOf(currentSetting);
            }
        }

        GWTServiceLookup.getMeasurementDataService().getHighestNOOBsForResource(resourceId, resultCount,
            new AsyncCallback<PageList<MeasurementOOBComposite>>() {
                @Override
                public void onFailure(Throwable caught) {
                    Log.debug("Error retrieving out of bound metrics for resource [" + resourceId + "]:"
                        + caught.getMessage());
                }

                @Override
                public void onSuccess(PageList<MeasurementOOBComposite> result) {
                    VLayout column = new VLayout();
                    column.setHeight(10);
                    if (!result.isEmpty()) {
                        for (MeasurementOOBComposite oob : result) {
                            LocatableDynamicForm row = new LocatableDynamicForm(recentOobContent.extendLocatorId(oob
                                .getScheduleName()));
                            row.setNumCols(2);

                            final String title = oob.getScheduleName();
                            final String destination = "/resource/common/monitor/Visibility.do?m="
                                + oob.getDefinitionId() + "&id=" + resourceId + "&mode=chartSingleMetricSingleResource";
                            LinkItem link = AbstractActivityView.newLinkItem(title, destination);
                            link.addClickHandler(new ClickHandler() {
                                @Override
                                public void onClick(ClickEvent event) {
                                    ChartViewWindow window = new ChartViewWindow(recentOobContent
                                        .extendLocatorId("ChartWindow"), title);
                                    //generate and include iframed content
                                    FullHTMLPane iframe = new FullHTMLPane(recentOobContent.extendLocatorId("View"),
                                        destination);
                                    window.addItem(iframe);
                                    window.show();
                                }
                            });

                            StaticTextItem time = AbstractActivityView.newTextItem(GwtRelativeDurationConverter
                                .format(oob.getTimestamp()));

                            row.setItems(link, time);
                            column.addMember(row);
                        }
                        //insert see more link spinder(2/24/11): no page that displays all oobs... See More not possible.
                    } else {
                        LocatableDynamicForm row = AbstractActivityView.createEmptyDisplayRow(recentOobContent
                            .extendLocatorId("None"), AbstractActivityView.RECENT_OOB_NONE);
                        column.addMember(row);
                    }
                    recentOobContent.setContents("");
                    for (Canvas child : recentOobContent.getChildren()) {
                        child.destroy();
                    }
                    recentOobContent.addChild(column);
                    recentOobContent.markForRedraw();
                }
            });
    }
}