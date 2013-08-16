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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.groups;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent;
import org.rhq.enterprise.gui.coregui.client.dashboard.portlets.PortletConfigurationEditorComponent.Constant;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.detail.summary.AbstractActivityView.ChartViewWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.D3GroupGraphListView;
import org.rhq.enterprise.gui.coregui.client.util.GwtRelativeDurationConverter;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;

/**This portlet allows the end user to customize the OOB display
 *
 * @author Simeon Pinder
 */
public class GroupOobsPortlet extends EnhancedVLayout implements CustomSettingsPortlet, AutoRefreshPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "GroupOobs";
    // A default displayed, persisted name for the portlet
    public static final String NAME = MSG.view_portlet_defaultName_group_oobs();
    public static final String ID = "id";

    private EntityContext groupContext;

    private int groupId = -1;
    protected Canvas recentOobContent = new Canvas();
    protected boolean currentlyLoading = false;

    // set on initial configuration, the window for this portlet view.
    protected PortletWindow portletWindow;
    //instance ui widgets

    protected Timer refreshTimer;

    //defines the list of configuration elements to load/persist for this portlet
    protected static List<String> CONFIG_INCLUDE = new ArrayList<String>();
    static {
        CONFIG_INCLUDE.add(Constant.RESULT_COUNT);
    }

    public GroupOobsPortlet(int groupId) {
        super();
        this.groupId = groupId;
        groupContext = EntityContext.forGroup(groupId);
    }

    @Override
    protected void onInit() {
        initializeUi();
        loadData();
    }

    /**Defines layout for the portlet page.
     */
    protected void initializeUi() {
        setPadding(5);
        setMembersMargin(5);
        addMember(recentOobContent);
    }

    /** Responsible for initialization and lazy configuration of the portlet values
     */
    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        //populate portlet configuration details
        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        Configuration portletConfig = storedPortlet.getConfiguration();

        //lazy init any elements not yet configured.
        for (String key : PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION.keySet()) {
            if ((portletConfig.getSimple(key) == null) && CONFIG_INCLUDE.contains(key)) {
                portletConfig.put(new PropertySimple(key,
                    PortletConfigurationEditorComponent.CONFIG_PROPERTY_INITIALIZATION.get(key)));
            }
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_oobs());
    }

    public static final class Factory implements PortletViewFactory {
        public static final PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(EntityContext context) {

            if (EntityContext.Type.ResourceGroup != context.getType()) {
                throw new IllegalArgumentException("Context [" + context + "] not supported by portlet");
            }

            return new GroupOobsPortlet(context.getGroupId());
        }
    }

    protected void loadData() {
        currentlyLoading = true;
        getRecentOobs();
    }

    @Override
    public DynamicForm getCustomSettingsForm() {
        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();

        DynamicForm customSettings = new DynamicForm();
        EnhancedVLayout page = new EnhancedVLayout();
        //build editor form container
        final DynamicForm form = new DynamicForm();
        form.setMargin(5);
        //add result count selector
        final SelectItem resultCountSelector = PortletConfigurationEditorComponent.getResultCountEditor(portletConfig);
        form.setItems(resultCountSelector);

        //submit handler
        customSettings.addSubmitValuesHandler(new SubmitValuesHandler() {

            @Override
            public void onSubmitValues(SubmitValuesEvent event) {

                //results count
                Configuration updatedConfig = AbstractActivityView.saveResultCounterSettings(resultCountSelector,
                    portletConfig);

                //persist
                storedPortlet.setConfiguration(updatedConfig);
                configure(portletWindow, storedPortlet);
                refresh();
            }

        });
        page.addMember(form);
        customSettings.addChild(page);
        return customSettings;
    }

    /** Fetches OOB measurements and updates the DynamicForm instance with the latest N
     *  oob change details.
     */
    protected void getRecentOobs() {
        final DashboardPortlet storedPortlet = this.portletWindow.getStoredPortlet();
        final Configuration portletConfig = storedPortlet.getConfiguration();

        final int groupId = this.groupId;

        //result count
        PropertySimple property = portletConfig.getSimple(Constant.RESULT_COUNT);
        String currentSetting = property.getStringValue();
        final int resultCountFinal = (currentSetting.trim().isEmpty() || currentSetting.equalsIgnoreCase("5")) ? 5: Integer.valueOf(currentSetting);

        //locate resourceGroupRef
        ResourceGroupCriteria criteria = new ResourceGroupCriteria();
        criteria.addFilterId(groupId);
        criteria.fetchConfigurationUpdates(false);
        criteria.fetchExplicitResources(false);
        criteria.fetchGroupDefinition(false);
        criteria.fetchOperationHistories(false);

        // for autoclusters and autogroups we need to add more criteria
        final boolean isAutoCluster = isAutoCluster();
        final boolean isAutoGroup = isAutoGroup();
        if (isAutoCluster) {
            criteria.addFilterVisible(false);
        } else if (isAutoGroup) {
            criteria.addFilterVisible(false);
            criteria.addFilterPrivate(true);
        }

        //locate the resource group
        GWTServiceLookup.getResourceGroupService().findResourceGroupCompositesByCriteria(criteria,
                new AsyncCallback<PageList<ResourceGroupComposite>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Log.debug("Error retrieving resource group composite for group [" + groupId + "]:"
                                + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(PageList<ResourceGroupComposite> results) {
                        if (!results.isEmpty()) {
                            final ResourceGroupComposite groupComposite = results.get(0);


                            GWTServiceLookup.getMeasurementDataService().getHighestNOOBsForGroup(groupId, resultCountFinal,
                                    new AsyncCallback<PageList<MeasurementOOBComposite>>() {
                                        @Override
                                        public void onFailure(Throwable caught) {
                                            Log.debug("Error retrieving recent out of bound metrics for group [" + groupId + "]:"
                                                    + caught.getMessage());
                                            currentlyLoading = false;
                                        }

                                        @Override
                                        public void onSuccess(PageList<MeasurementOOBComposite> result) {
                                            VLayout column = new VLayout();
                                            column.setHeight(10);
                                            if (!result.isEmpty()) {
                                                for (final MeasurementOOBComposite oob : result) {
                                                    DynamicForm row = new DynamicForm();
                                                    row.setNumCols(2);

                                                    final String title = oob.getScheduleName();
                                                    LinkItem link = new LinkItem();
                                                    link.setLinkTitle(title);
                                                    link.setTitle(title);
                                                    link.setShowTitle(false);
                                                    link.addClickHandler(new ClickHandler() {
                                                        @Override
                                                        public void onClick(ClickEvent event) {
                                                            ChartViewWindow window = new ChartViewWindow(title);
                                                            D3GroupGraphListView graphView = new D3GroupGraphListView
                                                                    (groupComposite.getResourceGroup(), false);

                                                            window.addItem(graphView);
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
                                                DynamicForm row = AbstractActivityView
                                                        .createEmptyDisplayRow(AbstractActivityView.RECENT_OOB_NONE);
                                                column.addMember(row);
                                            }
                                            recentOobContent.setContents("");
                                            for (Canvas child : recentOobContent.getChildren()) {
                                                child.destroy();
                                            }
                                            recentOobContent.addChild(column);
                                            recentOobContent.markForRedraw();
                                            currentlyLoading = false;
                                            markForRedraw();
                                        }
                                    });

                        }
                    }
                });



    }

    private boolean isAutoGroup() {
        return groupContext.isAutoGroup();
    }

    private boolean isAutoCluster() {
        return groupContext.isAutoCluster();
    }

    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycle(this, this, refreshTimer);
        recentOobContent.markForRedraw();
        markForRedraw();
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);

        super.onDestroy();
    }

    public boolean isRefreshing() {
        return this.currentlyLoading;
    }

    @Override
    public void refresh() {
        if (!isRefreshing()) {
            loadData();
        }
    }
}