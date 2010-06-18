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

import com.google.gwt.core.client.GWT;
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

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.enterprise.gui.coregui.client.components.lookup.ResourceLookupComboBoxItem;
import org.rhq.enterprise.gui.coregui.client.dashboard.CustomSettingsPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.Portlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletViewFactory;
import org.rhq.core.domain.dashboard.DashboardPortlet;
import org.rhq.enterprise.gui.coregui.client.dashboard.PortletWindow;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.ResourceScheduledMetricDatasource;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.SmallGraphView;

/**
 * @author Greg Hinkle
 */
public class GraphPortlet extends SmallGraphView implements CustomSettingsPortlet {

    public static final String KEY = "Resource Graph";

    PortletWindow portletWindow;
    DashboardPortlet storedPortlet;

    public GraphPortlet() {
        setOverflow(Overflow.HIDDEN);
    }

    public void configure(PortletWindow portletWindow, DashboardPortlet storedPortlet) {
        this.portletWindow = portletWindow;
        this.storedPortlet = storedPortlet;
        if (storedPortlet.getConfiguration().getSimple("resourceId") != null) {
            setResourceId(storedPortlet.getConfiguration().getSimple("resourceId").getIntegerValue());
            setDefinitionId(storedPortlet.getConfiguration().getSimple("definitionId").getIntegerValue());
        }
    }

    public Canvas getHelpCanvas() {
        return new HTMLFlow("<h3>Graph Portlet</h3>This Portlet supports the graphing of a resource metric.");
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        ConfigurationDefinition def = new ConfigurationDefinition("Graph Config", "Configuration of the graph portlet");
        def.put(new PropertyDefinitionSimple("resourceId", "The resource to graph", true, PropertySimpleType.INTEGER));
        def.put(new PropertyDefinitionSimple("definitionId", "The metric definition id to graph", true, PropertySimpleType.INTEGER));

        return def;
    }


    
    @Override
    protected void onDraw() {
        removeMembers(getMembers());        
        if (storedPortlet.getConfiguration().getSimple("resourceId") != null) {
            super.onDraw();
        } else {
            addMember(new Label("This graph is unconfigured, click the settings button to configure."));
        }
    }

    public DynamicForm getCustomSettingsForm() {
        final DynamicForm form = new DynamicForm();


        final ResourceLookupComboBoxItem resourceLookupComboBoxItem = new ResourceLookupComboBoxItem("resourceId", "Resource");
        resourceLookupComboBoxItem.setWidth(300);

        final SelectItem metric = new SelectItem("definitionId", "Metric") {
            @Override
            protected Criteria getPickListFilterCriteria() {
                Criteria criteria = new Criteria();

                if (resourceLookupComboBoxItem.getValue() != null) {
                    int resourceId = (Integer) resourceLookupComboBoxItem.getValue();
                    criteria.addCriteria("resourceId", resourceId);
                }
                return criteria;
            }
        };

        metric.setWidth(300);
        metric.setValueField("id");
        metric.setDisplayField("displayName");
        metric.setOptionDataSource(new ResourceScheduledMetricDatasource());


        resourceLookupComboBoxItem.addChangedHandler(new com.smartgwt.client.widgets.form.fields.events.ChangedHandler() {
            public void onChanged(ChangedEvent
                    event) {

                if (form.getValue("resourceId") instanceof Integer) {
                    metric.fetchData();
                    form.clearValue("defininitionId");
                }
            }
        });

        if (storedPortlet.getConfiguration().getSimple("resourceId") != null) {
            form.setValue("resourceId", storedPortlet.getConfiguration().getSimple("resourceId").getIntegerValue());
            form.setValue("definitionId", storedPortlet.getConfiguration().getSimple("definitionId").getIntegerValue());
        }

        form.setFields(resourceLookupComboBoxItem, metric);

        form.addSubmitValuesHandler(new SubmitValuesHandler() {
            public void onSubmitValues(SubmitValuesEvent submitValuesEvent) {
                storedPortlet.getConfiguration().put(new PropertySimple("resourceId", form.getValue("resourceId")));
                storedPortlet.getConfiguration().put(new PropertySimple("definitionId", form.getValue("definitionId")));
                
            }
        });

        return form;
    }

    public static final class Factory implements PortletViewFactory {
        public static PortletViewFactory INSTANCE = new Factory();

        public final Portlet getInstance() {
            return GWT.create(GraphPortlet.class);
        }
    }
}
