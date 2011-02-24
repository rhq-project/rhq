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
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.SortDirection;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationComparisonView;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.ConfigurationHistoryDataSource.Field;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * The main view that lists all resource configuration history items.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class ConfigurationHistoryView extends TableSection<ConfigurationHistoryDataSource> {
    public static final ViewName VIEW_ID = new ViewName("RecentConfigurationChanges", MSG
        .view_configurationHistoryList_title());

    private Integer resourceId;
    private boolean hasWritePerm; // can delete history or rollback to a previous config

    /**
     * Use this constructor to view config histories for all viewable Resources.
     */
    public ConfigurationHistoryView(String locatorId, boolean hasWritePerm) {
        super(locatorId, VIEW_ID.getTitle());
        this.hasWritePerm = hasWritePerm;
        this.resourceId = null;
        ConfigurationHistoryDataSource datasource = new ConfigurationHistoryDataSource();
        setDataSource(datasource);
    }

    /**
     * Use this constructor to view the config history for the Resource with the specified ID.
     *
     * @param resourceId a Resource ID
     */
    public ConfigurationHistoryView(String locatorId, boolean hasWritePerm, int resourceId) {
        super(locatorId, VIEW_ID.getTitle(), createCriteria(resourceId));
        this.hasWritePerm = hasWritePerm;
        this.resourceId = resourceId;
        ConfigurationHistoryDataSource datasource = new ConfigurationHistoryDataSource();
        setDataSource(datasource);
    }

    private static Criteria createCriteria(int resourceId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(ConfigurationHistoryDataSource.CriteriaField.RESOURCE_ID, resourceId);
        return criteria;
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = getDataSource().getListGridFields(this.resourceId == null);
        setListGridFields(true, fields.toArray(new ListGridField[fields.size()])); // true = always show the ID field
        getListGrid().sort(Field.ID, SortDirection.DESCENDING);

        addTableAction(extendLocatorId("Delete"), MSG.common_button_delete(), MSG.common_msg_areYouSure(),
            new AbstractTableAction(hasWritePerm ? TableActionEnablement.ANY : TableActionEnablement.NEVER) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    if (selection != null && selection.length > 0) {
                        int[] doomedIds = new int[selection.length];
                        int i = 0;
                        for (ListGridRecord selected : selection) {
                            doomedIds[i] = selected.getAttributeAsInt(Field.ID);
                            if (selected.getAttribute(Field.GROUP_CONFIG_UPDATE_ID) != null) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_configurationHistoryList_cannotDeleteGroupItems(),
                                        Severity.Warning));
                                return; // abort
                            }
                        }
                        delete(doomedIds);
                    }
                }
            });

        addTableAction(extendLocatorId("Compare"), MSG.common_button_compare(), null, new AbstractTableAction(
            TableActionEnablement.MULTIPLE) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                ArrayList<ResourceConfigurationUpdate> configs = new ArrayList<ResourceConfigurationUpdate>();
                for (ListGridRecord record : selection) {
                    ResourceConfigurationUpdate update = (ResourceConfigurationUpdate) record
                        .getAttributeAsObject(ConfigurationHistoryDataSource.Field.OBJECT);
                    configs.add(update);
                }
                ConfigurationComparisonView.displayComparisonDialog(configs);
            }
        });

        addTableAction(extendLocatorId("Rollback"), MSG.view_configurationHistoryList_rollback(), MSG
            .common_msg_areYouSure(), new AbstractTableAction(hasWritePerm ? TableActionEnablement.SINGLE
            : TableActionEnablement.NEVER) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                if (selection != null && selection.length == 1) {
                    ListGridRecord record = selection[0];
                    rollback(record.getAttributeAsInt(Field.ID).intValue());
                }
            }
        });

        super.configureTable();
    }

    @Override
    protected String getDetailsLinkColumnName() {
        return Field.ID;
    }

    @Override
    protected CellFormatter getDetailsLinkColumnCellFormatter() {
        return new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                Integer recordId = getId(record);
                String detailsUrl = "#" + getBasePath() + "/" + recordId;
                String cellHtml = SeleniumUtility.getLocatableHref(detailsUrl, value.toString(), null);
                String isCurrentConfig = record.getAttribute(Field.CURRENT_CONFIG);
                if (Boolean.parseBoolean(isCurrentConfig)) {
                    cellHtml = Canvas.imgHTML(ImageManager.getApproveIcon()) + cellHtml;
                }
                return cellHtml;
            }
        };
    }

    @Override
    public Canvas getDetailsView(int id) {
        ConfigurationHistoryDetailView detailView = new ConfigurationHistoryDetailView(this.getLocatorId());
        return detailView;
    }

    private void rollback(int configHistoryIdToRollbackTo) {
        GWTServiceLookup.getConfigurationService().rollbackResourceConfiguration(this.resourceId.intValue(),
            configHistoryIdToRollbackTo, new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_configurationHistoryList_rollback_success(), Severity.Info));
                    refresh();
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_configurationHistoryList_rollback_failure(), caught);
                }
            });
    }

    private void delete(int[] doomedIds) {
        GWTServiceLookup.getConfigurationService().purgeResourceConfigurationUpdates(doomedIds, true,
            new AsyncCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_configurationHistoryList_delete_success(), Severity.Info));
                    refresh();
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_configurationHistoryList_delete_failure(), caught);
                }
            });
    }

    // -------- Static Utility loaders ------------

    public static ConfigurationHistoryView getHistoryOf(String locatorId, boolean hasWritePerm, int resourceId) {
        return new ConfigurationHistoryView(locatorId, hasWritePerm, resourceId);
    }

}
