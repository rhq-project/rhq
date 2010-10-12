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

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.ConfigurationUpdateStatus;
import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationComparisonView;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;

/**
 * @author Greg Hinkle
 */
public class ConfigurationHistoryView extends TableSection {
    public static final String VIEW_ID = "RecentConfigurationChanges";
    private static final String TITLE = "Configuration History";

    private Integer resourceId;

    /**
     * Use this constructor to view config histories for all viewable Resources.
     */
    public ConfigurationHistoryView(String locatorId) {
        super(locatorId, TITLE);
        final ConfigurationHistoryDataSource datasource = new ConfigurationHistoryDataSource();
        setDataSource(datasource);
    }

    /**
     * Use this constructor to view the config history for the Resource with the specified ID.
     *
     * @param resourceId a Resource ID
     */
    public ConfigurationHistoryView(String locatorId, int resourceId) {
        super(locatorId, TITLE, createCriteria(resourceId));
        this.resourceId = resourceId;
        ConfigurationHistoryDataSource datasource = new ConfigurationHistoryDataSource();
        setDataSource(datasource);
    }

    private static Criteria createCriteria(int resourceId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria("resourceId", Integer.valueOf(resourceId));
        return criteria;
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        ListGrid grid = getListGrid();

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

        addTableAction(extendLocatorId("Remove"), "Remove", Table.SelectionEnablement.ANY,
            "Are you sure you want to delete # configurations?", new TableAction() {
                public void executeAction(ListGridRecord[] selection) {
                    // TODO: Implement this method.
                    CoreGUI.getErrorHandler().handleError("Not implemented");
                }
            });

        addTableAction(extendLocatorId("Compare"), "Compare", Table.SelectionEnablement.MULTIPLE, null,
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

        addTableAction(extendLocatorId("ShowDetail"), "Show Details", Table.SelectionEnablement.SINGLE, null,
            new TableAction() {
                public void executeAction(ListGridRecord[] selection) {

                    ListGridRecord record = selection[0];

                    showDetails(record);
                }
            });

    }

    @Override
    public Canvas getDetailsView(int id) {
        ConfigurationHistoryDetailView detailView = new ConfigurationHistoryDetailView(this.getLocatorId());

        return detailView;

    }

    // -------- Static Utility loaders ------------

    public static ConfigurationHistoryView getHistoryOf(String locatorId, int resourceId) {
        return new ConfigurationHistoryView(locatorId, resourceId);
    }

}
