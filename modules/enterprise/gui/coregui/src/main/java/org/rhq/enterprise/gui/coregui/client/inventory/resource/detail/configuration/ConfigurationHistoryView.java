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
import com.smartgwt.client.types.SelectionAppearance;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationComparisonView;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

/**
 * @author Greg Hinkle
 */
public class ConfigurationHistoryView extends VLayout {

    private int resourceId;

    ConfigurationHistoryDetailView detailView;


    /**
     * Resource list filtered by a given criteria
     */
    public ConfigurationHistoryView(final int resourceId) {
        this.resourceId = resourceId;

        setWidth100();
        setHeight100();
        setAnimateMembers(true);
    }


    @Override
    protected void onDraw() {
        super.onDraw();

        Criteria criteria = new Criteria();
        criteria.addCriteria("resourceId", resourceId);


        final ConfigurationHistoryDataSource datasource = new ConfigurationHistoryDataSource();


        Table table = new Table("Configuration History", criteria);
        table.setDataSource(datasource);
        table.getListGrid().setUseAllDataSourceFields(true);

        ListGridField idField = new ListGridField("id", "ID", 60);
        ListGridField createdField = new ListGridField("createdTime", "Timestamp", 200);
        ListGridField statusField = new ListGridField("status", "Status", 100);
        ListGridField userField = new ListGridField("subject", "User", 150);
        table.getListGrid().setFields(idField, createdField, statusField, userField);

        table.getListGrid().setSelectionType(SelectionStyle.SIMPLE);
        table.getListGrid().setSelectionAppearance(SelectionAppearance.CHECKBOX);
        table.getListGrid().setResizeFieldsInRealTime(true);


        table.addTableAction("Remove", Table.SelectionEnablement.ANY,
                "Are you sure you want to delete # configurations?",
                new TableAction() {
                    public void executeAction(ListGridRecord[] selection) {
                        // TODO: Implement this method.
                    }
                });

        table.addTableAction("Compare", Table.SelectionEnablement.MULTIPLE, null,
                new TableAction() {
                    public void executeAction(ListGridRecord[] selection) {
                        ArrayList<ResourceConfigurationUpdate> configs = new ArrayList<ResourceConfigurationUpdate>();
                        for (ListGridRecord record : selection) {
                            ResourceConfigurationUpdate update = (ResourceConfigurationUpdate) record.getAttributeAsObject("entity");
                            configs.add(update);
                        }
                        ConfigurationComparisonView.displayComparisonDialog(configs);
                    }
                });


        table.setShowResizeBar(true);
        table.setResizeBarTarget("next");
        addMember(table);


        detailView = new ConfigurationHistoryDetailView();
        detailView.setHeight("70%");
        detailView.hide();

        addMember(detailView);


        table.getListGrid().addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {
                    ListGridRecord record = (ListGridRecord) selectionEvent.getRecord();
                    final ResourceConfigurationUpdate update = (ResourceConfigurationUpdate) record.getAttributeAsObject("entity");

                    ResourceTypeRepository.Cache.getInstance().getResourceTypes(
                            update.getResource().getResourceType().getId(),
                            EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                            new ResourceTypeRepository.TypeLoadedCallback() {

                                public void onTypesLoaded(ResourceType type) {

                                    ConfigurationDefinition definition = type.getResourceConfigurationDefinition();

                                    detailView.setConfiguration(definition, update.getConfiguration());
                                    detailView.setHeight("75%");
                                    showMember(detailView);
                                }
                            });
                } else {
                    hideMember(detailView);
                }
            }
        });
    }


    // -------- Static Utility loaders ------------


    public static ConfigurationHistoryView getHistoryOf(int resourceId) {


        return new ConfigurationHistoryView(resourceId);
    }

}
