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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.content.InstalledPackageHistory;
import org.rhq.core.domain.criteria.InstalledPackageHistoryCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.coregui.client.dashboard.Portlet;
import org.rhq.coregui.client.dashboard.PortletViewFactory;
import org.rhq.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.coregui.client.dashboard.portlets.groups.GroupPkgHistoryPortlet;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.coregui.client.util.GwtRelativeDurationConverter;
import org.rhq.coregui.client.util.Log;

/**This portlet allows the end user to customize the Package History display
 *
 * @author Simeon Pinder
 */
public class ResourcePkgHistoryPortlet extends GroupPkgHistoryPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourcePackageHistory";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_resource_pkg_hisory();

    private int resourceId = -1;

    public ResourcePkgHistoryPortlet(int resourceId) {
        super(-1);
        this.resourceId = resourceId;
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            if (EntityContext.Type.Resource != context.getType()) {
                throw new IllegalArgumentException("Context [" + context + "] not supported by portlet");
            }

            return new ResourcePkgHistoryPortlet(context.getResourceId());
        }
    }

    /** Fetches recent package history information and updates the DynamicForm instance with details.
     */
    @Override
    protected void getRecentPkgHistory() {
        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();
        final int resourceId = this.resourceId;
        InstalledPackageHistoryCriteria criteria = new InstalledPackageHistoryCriteria();

        PageControl pc = new PageControl();

        //result count
        String currentSetting = portletConfig.getSimpleValue(Constant.RESULT_COUNT, Constant.RESULT_COUNT_DEFAULT);
        if (currentSetting.trim().isEmpty()) {
            pc.setPageSize(Integer.valueOf(Constant.RESULT_COUNT_DEFAULT));
        } else {
            pc.setPageSize(Integer.valueOf(currentSetting));
        }

        criteria.setPageControl(pc);
        criteria.addFilterResourceId(resourceId);

        criteria.addSortStatus(PageOrdering.DESC);

        GWTServiceLookup.getContentService().findInstalledPackageHistoryByCriteria(criteria,

        new AsyncCallback<PageList<InstalledPackageHistory>>() {
            @Override
            public void onFailure(Throwable caught) {
                Log.debug("Error retrieving installed package history for group [" + resourceId + "]:"
                    + caught.getMessage());
                currentlyLoading = false;
            }

            @Override
            public void onSuccess(PageList<InstalledPackageHistory> result) {
                VLayout column = new VLayout();
                column.setHeight(10);
                if (!result.isEmpty()) {
                    for (InstalledPackageHistory history : result) {
                        DynamicForm row = new DynamicForm();
                        row.setNumCols(3);

                        StaticTextItem iconItem = AbstractActivityView.newTextItemIcon(
                            "subsystems/content/Package_16.png", null);
                        String title = history.getPackageVersion().getFileName() + ":";
                        String destination = "/portal/rhq/resource/content/audit-trail-item.xhtml?id=" + resourceId
                            + "&selectedHistoryId=" + history.getId();
                        //spinder 4/27/11: disabling links as they point into portal.war content pages
                        //                        LinkItem link = AbstractActivityView.newLinkItem(title, destination);
                        StaticTextItem link = AbstractActivityView.newTextItem(title);
                        StaticTextItem time = AbstractActivityView.newTextItem(GwtRelativeDurationConverter
                            .format(history.getTimestamp()));

                        row.setItems(iconItem, link, time);
                        column.addMember(row);
                    }
                    //                    //insert see more link
                    //                    DynamicForm row = new DynamicForm();
                    //                    String destination = "/portal/rhq/resource/content/audit-trail-item.xhtml?id=" + groupId;
                    //                    addSeeMoreLink(row, destination, column);
                } else {
                    DynamicForm row = AbstractActivityView.createEmptyDisplayRow(MSG.view_portlet_results_empty());
                    column.addMember(row);
                }
                //cleanup
                for (Canvas child : recentPkgHistoryContent.getChildren()) {
                    child.destroy();
                }
                recentPkgHistoryContent.addChild(column);
                recentPkgHistoryContent.markForRedraw();
                currentlyLoading = false;
                markForRedraw();
            }
        });
    }
}