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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets.inventory.resource.graph;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;

import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.components.lookup.ResourceLookupComboBoxItem;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.ResourceScheduledMetricDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.SmallGraphView;

/**
 * @author Greg Hinkle
 */
public class GraphPortlet extends SmallGraphView implements CustomSettingsPortlet {

    // A non-displayed, persisted identifier for the portlet
    public static final String KEY = "Graph";
    // A default displayed, persisted name for the portlet    
    public static final String NAME = MSG.view_portlet_defaultName_graph();

    // set on initial configuration, the window for this portlet view. 
    private PortletWindow portletWindow;

    public static final String CFG_RESOURCE_ID = "resourceId";
    public static final String CFG_DEFINITION_ID = "definitionId";
    public static final String CFG_RESOURCE_GROUP_ID = "resourceGroupId";

    public GraphPortlet(String locatorId) {
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

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID) != null) {
            setResourceId(storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID).getIntegerValue());
            setDefinitionId(storedPortlet.getConfiguration().getSimple(CFG_DEFINITION_ID).getIntegerValue());
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow(MSG.view_portlet_help_graph());
    }

    @Override
    protected void onDraw() {
        DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID) != null) {
            super.onDraw();
        } else {
            removeMembers(getMembers());
            addMember(new Label("<i>" + MSG.view_portlet_configure_needed() + "</i>"));
        }
    }

    public DynamicForm getCustomSettingsForm() {
        final DynamicForm form = new DynamicForm();

        final ResourceLookupComboBoxItem resourceLookupComboBoxItem = new ResourceLookupComboBoxItem(CFG_RESOURCE_ID,
            MSG.common_title_resource());
        resourceLookupComboBoxItem.setWidth(300);

        final SelectItem metric = new SelectItem(CFG_DEFINITION_ID, MSG.common_title_metric()) {
            @Override
            protected Criteria getPickListFilterCriteria() {
                Criteria criteria = new Criteria();

                if (resourceLookupComboBoxItem.getValue() != null) {
                    int resourceId = (Integer) resourceLookupComboBoxItem.getValue();
                    criteria.addCriteria(CFG_RESOURCE_ID, resourceId);
                }
                return criteria;
            }
        };

        final DashboardPortlet storedPortlet = portletWindow.getStoredPortlet();

        metric.setWidth(300);
        metric.setValueField("id");
        metric.setDisplayField("displayName");
        metric.setOptionDataSource(new ResourceScheduledMetricDatasource());

        resourceLookupComboBoxItem
            .addChangedHandler(new com.smartgwt.client.widgets.form.fields.events.ChangedHandler() {
                public void onChanged(ChangedEvent event) {

                    if (form.getValue(CFG_RESOURCE_ID) instanceof Integer) {
                        metric.fetchData();
                        form.clearValue("defininitionId");
                    }
                }
            });

        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID) != null) {
            form.setValue(CFG_RESOURCE_ID, storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID)
                .getIntegerValue());
            form.setValue(CFG_DEFINITION_ID, storedPortlet.getConfiguration().getSimple(CFG_DEFINITION_ID)
                .getIntegerValue());
        }

        form.setFields(resourceLookupComboBoxItem, metric);

        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            public void onSubmitValues(SubmitValuesEvent submitValuesEvent) {
                storedPortlet.getConfiguration().put(
                    new PropertySimple(CFG_RESOURCE_ID, form.getValue(CFG_RESOURCE_ID)));
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
        if (storedPortlet.getConfiguration().getSimple(CFG_RESOURCE_ID) != null) {
            renderGraph();
        } else {
            addMember(new Label("<i>" + MSG.view_portlet_configure_needed() + "</i>"));
        }
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance(String locatorId) {

            return new GraphPortlet(locatorId);
        }
    }
}
