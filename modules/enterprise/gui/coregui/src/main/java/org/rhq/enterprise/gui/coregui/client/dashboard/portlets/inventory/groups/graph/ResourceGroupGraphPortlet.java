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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.groups.graph;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.core.domain.resource.group.GroupCategory;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedEvent;
import org.rhq.enterprise.gui.coregui.client.components.selector.AssignedItemsChangedHandler;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.monitoring.ResourceGroupMetricGraphView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.ResourceScheduledMetricDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.SingleResourceGroupSelector;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author Greg Hinkle
 * @author Jay Shaughnessy
 */
public class ResourceGroupGraphPortlet extends ResourceGroupMetricGraphView implements CustomSettingsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "ResourceGroupMetric";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = MSG.view_portlet_defaultName_groupMetric();

    // set on initial configuration, the window for this portlet view. 
    private PortletWindow portletWindow;

    public static final String CFG_RESOURCE_GROUP_ID = "resourceGroupId";
    public static final String CFG_DEFINITION_ID = "definitionId";

    public ResourceGroupGraphPortlet(String locatorId) {
        super(locatorId);
        setOverflow(Overflow.HIDDEN);
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {

        if (null == this.portletWindow && null != portletWindow) {
            this.portletWindow = portletWindow;
        }

        if ((null == storedPortlet) || (null == storedPortlet.getConfiguration())) {
            return;
        }

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID) != null) {
            setEntityId(storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID).getIntegerValue());
            setDefinitionId(storedPortlet.getConfiguration().getSimple(CFG_DEFINITION_ID).getIntegerValue());
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_graph());
    }

    @Override
    protected void onDraw() {
        DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID) != null) {
            super.onDraw();
        } else {
            removeMembers(getMembers());
            addMember(new Label("<i>" + MSG.view_portlet_configure_needed() + "</i>"));
        }
    }

    public DynamicForm getCustomSettingsForm() {
        final LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("Config"));
        form.setWidth(750);
        form.setNumCols(1);

        final CanvasItem selectorItem = new CanvasItem();
        selectorItem.setTitleOrientation(TitleOrientation.TOP);
        selectorItem.setShowTitle(false);

        final SingleResourceGroupSelector resourceGroupSelector = new SingleResourceGroupSelector(form
            .extendLocatorId("Selector"), GroupCategory.COMPATIBLE, false);
        resourceGroupSelector.setWidth(700);
        resourceGroupSelector.setHeight(300);
        //TODO, would probaby be nice to find a way to seed assigned with the current group 
        //ListGridRecord rec = new ListGridRecord();
        //rec.setAttribute("id", getEntityId());
        //rec.setAttribute("name", "current");
        //resourceGroupSelector.setAssigned(new ListGridRecord[] { rec });

        final SelectItem metric = new SelectItem(CFG_DEFINITION_ID, MSG.common_title_metric()) {
            @Override
            protected Criteria getPickListFilterCriteria() {
                Criteria criteria = new Criteria();

                if (resourceGroupSelector.getSelectedItems().size() == 1) {
                    int groupId = resourceGroupSelector.getSelectedItems().iterator().next().getId();
                    criteria.addCriteria(CFG_RESOURCE_GROUP_ID, groupId);
                    form.setValue(CFG_RESOURCE_GROUP_ID, groupId);
                }
                return criteria;
            }
        };
        metric.setWidth(300);
        metric.setTitleOrientation(TitleOrientation.TOP);
        metric.setValueField("id");
        metric.setDisplayField("displayName");
        metric.setOptionDataSource(new ResourceScheduledMetricDatasource());

        resourceGroupSelector.addAssignedItemsChangedHandler(new AssignedItemsChangedHandler() {

            public void onSelectionChanged(AssignedItemsChangedEvent event) {

                if (resourceGroupSelector.getSelectedItems().size() == 1) {
                    metric.fetchData();
                    form.clearValue(CFG_DEFINITION_ID);
                }
            }
        });

        final DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID) != null) {
            form.setValue(CFG_RESOURCE_GROUP_ID, storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID)
                .getIntegerValue());
            form.setValue(CFG_DEFINITION_ID, storedPortlet.getConfiguration().getSimple(CFG_DEFINITION_ID)
                .getIntegerValue());
        }

        selectorItem.setCanvas(resourceGroupSelector);
        form.setFields(selectorItem, metric, new SpacerItem());

        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            public void onSubmitValues(SubmitValuesEvent submitValuesEvent) {
                storedPortlet.getConfiguration().put(
                    new PropertySimple(CFG_RESOURCE_GROUP_ID, form.getValue(CFG_RESOURCE_GROUP_ID)));
                storedPortlet.getConfiguration().put(
                    new PropertySimple(CFG_DEFINITION_ID, form.getValue(CFG_DEFINITION_ID)));

                configure(portletWindow, storedPortlet);

                redraw();
            }
        });

        return form;
    }

    @Override
    public void redraw() {
        super.redraw();

        removeMembers(getMembers());

        DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();
        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_GROUP_ID) != null) {
            renderGraph();
        } else {
            addMember(new Label("<i>" + MSG.view_portlet_configure_needed() + "</i>"));
        }
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {

            return new ResourceGroupGraphPortlet(locatorId);
        }
    }
}
