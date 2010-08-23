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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration;

import java.util.ArrayList;
import java.util.EnumSet;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickHandler;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationComparisonView;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class ConfigurationHistoryView extends LocatableVLayout {

    private Integer resourceId;

    public ConfigurationHistoryView(String locatorId) {
        super(locatorId);
        setWidth100();
        setHeight100();
        setAnimateMembers(true);

    }

    public ConfigurationHistoryView(String locatorId, final int resourceId) {
        this(locatorId);
        this.resourceId = resourceId;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        Criteria criteria = new Criteria();
        if (resourceId != null) {
            criteria.addCriteria("resourceId", (int) resourceId);
        }

        final ConfigurationHistoryDataSource datasource = new ConfigurationHistoryDataSource();

        Table table = new Table(getLocatorId(), "Configuration History", criteria);
        table.setDataSource(datasource);
        table.getListGrid().setUseAllDataSourceFields(true);

        ListGrid grid = table.getListGrid();

        grid.getField("id").setWidth(60);
        grid.getField("createdTime").setWidth(200);

        if (resourceId != null) {
            grid.hideField("resource");
        } else {
            grid.getField("resource").setCellFormatter(new CellFormatter() {
                public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                    Resource res = (Resource) o;
                    return "<a href=\"#Resource/" + res.getId() + "\">" + res.getName() + "</a>";
                }
            });
        }

        grid.getField("status").setWidth(100);
        grid.getField("status").setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                ConfigurationUpdateStatus status = ConfigurationUpdateStatus.valueOf((String) o);
                String icon = "";
                switch (status) {
                case INPROGRESS:
                    break;
                case SUCCESS:
                    icon = "_ok";
                    break;
                case FAILURE:
                    icon = "_failed";
                    break;
                case NOCHANGE:
                    break;
                }

                return Canvas.imgHTML("subsystems/configure/Configure" + icon + "_16.png", 16, 16) + o;
            }
        });

        grid.getField("subject").setWidth(150);

        table.addTableAction(extendLocatorId("Remove"), "Remove", Table.SelectionEnablement.ANY,
            "Are you sure you want to delete # configurations?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    // TODO: Implement this method.
                    CoreGUI.getErrorHandler().handleError("Not implemented");
                }
            });

        table.addTableAction(extendLocatorId("Compare"), "Compare", Table.SelectionEnablement.MULTIPLE, null,
            new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    ArrayList<ResourceConfigurationUpdate> configs = new ArrayList<ResourceConfigurationUpdate>();
                    for (ListGridRecord record : selection) {
                        ResourceConfigurationUpdate update = (ResourceConfigurationUpdate) record
                            .getAttributeAsObject("entity");
                        configs.add(update);
                    }
                    ConfigurationComparisonView.displayComparisonDialog(configs);
                }
            });

        table.getListGrid().addCellDoubleClickHandler(new CellDoubleClickHandler() {
            public void onCellDoubleClick(CellDoubleClickEvent cellDoubleClickEvent) {
                ListGridRecord record = cellDoubleClickEvent.getRecord();
                showDetails(record);
            }
        });

        table.addTableAction(extendLocatorId("ShowDetail"), "Show Details", Table.SelectionEnablement.SINGLE, null,
            new TableAction() {
                public void executeAction(ListGridRecord[] selection) {

                    ListGridRecord record = selection[0];

                    showDetails(record);
                }
            });

        addMember(table);
    }

    public static void showDetails(ListGridRecord record) {
        final ResourceConfigurationUpdate update = (ResourceConfigurationUpdate) record.getAttributeAsObject("entity");

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(update.getResource().getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {

                public void onTypesLoaded(ResourceType type) {

                    ConfigurationDefinition definition = type.getResourceConfigurationDefinition();

                    ConfigurationHistoryDetailView detailView = new ConfigurationHistoryDetailView();

                    detailView.setConfiguration(definition, update.getConfiguration());

                    detailView.displayInDialog();

                }
            });

    }

    // -------- Static Utility loaders ------------

    public static ConfigurationHistoryView getHistoryOf(String locatorId, int resourceId) {

        return new ConfigurationHistoryView(locatorId, resourceId);
    }

}
