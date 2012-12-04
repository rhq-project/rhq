/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin.topology;

import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentNodeDatasourceField.FIELD_SERVER;
import static org.rhq.enterprise.gui.coregui.client.admin.topology.AgentNodeDatasourceField.FIELD_SERVER_ID;

import java.util.List;

import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.IconEnum;
import org.rhq.enterprise.gui.coregui.client.admin.AdministrationView;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.HasViewName;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.util.StringUtility;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * @author Jiri Kremser
 */
public class AgentTableView extends TableSection<AgentNodeDatasource> implements HasViewName {

    public static final ViewName VIEW_ID = new ViewName("Agents(GWT)", MSG.view_adminTopology_agents() + "(GWT)",
        IconEnum.SERVERS);
    
    public static final String VIEW_PATH = AdministrationView.VIEW_ID + "/"
        + AdministrationView.SECTION_TOPOLOGY_VIEW_ID + "/" + VIEW_ID;

    public AgentTableView(String locatorId, String tableTitle, Integer serverId) {
        super(locatorId, tableTitle);
        setHeight100();
        setWidth100();
        setDataSource(new AgentNodeDatasource(serverId));
    }

    @Override
    protected void configureTable() {
        super.configureTable();
        List<ListGridField> fields = getDataSource().getListGridFields();
        ListGrid listGrid = getListGrid();
        listGrid.setFields(fields.toArray(new ListGridField[fields.size()]));
        listGrid.sort(FIELD_NAME, SortDirection.ASCENDING);
        
        for (ListGridField field : fields) {
            // adding the cell formatter for name field (clickable link)
            if (field.getName() == FIELD_NAME) {
                field.setCellFormatter(new CellFormatter() {
                    @Override
                    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                        if (value == null) {
                            return "";
                        }
                        String detailsUrl = "#" + VIEW_PATH + "/" + getId(record);
                        String formattedValue = StringUtility.escapeHtml(value.toString());
                        return SeleniumUtility.getLocatableHref(detailsUrl, formattedValue, null);

                    }
                });
            } else if (field.getName() == FIELD_SERVER.propertyName()) {
                field.setCellFormatter(new CellFormatter() {
                    @Override
                    public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
                        if (value == null || value.toString().isEmpty()) {
                            return "";
                        }
                        String detailsUrl = "#" + ServerTableView.VIEW_PATH + "/"
                            + record.getAttributeAsString(FIELD_SERVER_ID.propertyName());
                        String formattedValue = StringUtility.escapeHtml(value.toString());
                        return SeleniumUtility.getLocatableHref(detailsUrl, formattedValue, null);

                    }
                });
            }
            // TODO: adding the cell formatter for affinity group (clickable link)

        }

        
    }

    @Override
    public Canvas getDetailsView(Integer id) {
        return new AgentDetailView(extendLocatorId("detailsView"), id);
    }

    @Override
    public ViewName getViewName() {
        return VIEW_ID;
    }

}
