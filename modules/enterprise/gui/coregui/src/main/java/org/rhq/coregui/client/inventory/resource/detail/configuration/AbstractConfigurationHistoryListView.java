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
package org.rhq.coregui.client.inventory.resource.detail.configuration;

import java.util.ArrayList;
import java.util.List;

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.DSCallback;
import com.smartgwt.client.data.DSRequest;
import com.smartgwt.client.data.DSResponse;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.AbstractResourceConfigurationUpdateCriteria;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.configuration.ConfigurationComparisonView;
import org.rhq.coregui.client.components.table.AbstractTableAction;
import org.rhq.coregui.client.components.table.TableActionEnablement;
import org.rhq.coregui.client.components.table.TableSection;
import org.rhq.coregui.client.components.table.Table.TableActionInfo.ButtonColor;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * The superclass to the main plugin/resource views that lists all configuration history items.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public abstract class AbstractConfigurationHistoryListView<T extends AbstractConfigurationHistoryDataSource<? extends AbstractResourceConfigurationUpdate, ? extends AbstractResourceConfigurationUpdateCriteria>>
    extends TableSection<T> {

    private Integer resourceId;
    private boolean hasWritePerm; // can delete history or rollback to a previous config

    /**
     * Use this constructor to view config histories for all viewable Resources.
     */
    public AbstractConfigurationHistoryListView(String title, boolean hasWritePerm) {
        super(title);
        this.hasWritePerm = hasWritePerm;
        this.resourceId = null;
    }

    /**
     * Use this constructor to view the config history for the Resource with the specified ID.
     *
     * @param resourceId a Resource ID
     */
    public AbstractConfigurationHistoryListView(String title, boolean hasWritePerm, int resourceId) {
        super(title, createCriteria(resourceId));
        this.hasWritePerm = hasWritePerm;
        this.resourceId = resourceId;
    }

    private static Criteria createCriteria(int resourceId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(AbstractConfigurationHistoryDataSource.CriteriaField.RESOURCE_ID, resourceId);
        return criteria;
    }

    public Integer getResourceId() {
        return resourceId;
    }

    public boolean hasWritePermission() {
        return hasWritePerm;
    }

    @Override
    protected void configureTable() {
        List<ListGridField> fields = getDataSource().getListGridFields(this.resourceId == null);
        setListGridFields(true, fields.toArray(new ListGridField[fields.size()])); // true = always show the ID field

        addTableAction(MSG.common_button_delete(), MSG.common_msg_areYouSure(), ButtonColor.RED,
            new AbstractTableAction(hasWritePerm ? TableActionEnablement.ANY : TableActionEnablement.NEVER) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                if (selection != null && selection.length > 0) {
                    int[] doomedIds = new int[selection.length];
                    int i = 0;
                    for (ListGridRecord selected : selection) {
                        doomedIds[i++] = selected.getAttributeAsInt(AbstractConfigurationHistoryDataSource.Field.ID);
                        if (selected.getAttribute(AbstractConfigurationHistoryDataSource.Field.GROUP_CONFIG_UPDATE_ID) != null) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_configurationHistoryList_cannotDeleteGroupItems(),
                                    Severity.Warning));
                            return; // abort
                        }
                        if (Boolean.parseBoolean(selected
                            .getAttribute(AbstractConfigurationHistoryDataSource.Field.CURRENT_CONFIG))) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_configurationHistoryList_cannotDeleteCurrent(), Severity.Warning));
                            return; // abort
                        }
                    }
                    delete(doomedIds);
                }
            }
        });

        addTableAction(MSG.common_button_compare(), null, ButtonColor.BLUE, new AbstractTableAction(
            TableActionEnablement.MULTIPLE) {
            public void executeAction(ListGridRecord[] selection, Object actionValue) {
                // The config updates do not have their Configurations fetched, so we need to reload the selected
                // config updates, specifying that their Configurations should be fetched, in order to compare the
                // Configurations.

                List<Integer> updateIds = new ArrayList<Integer>();
                for (ListGridRecord record : selection) {
                    int updateId = record.getAttributeAsInt(AbstractConfigurationHistoryDataSource.Field.ID);
                    updateIds.add(updateId);
                }

                Criteria criteria = new Criteria();
                criteria.addCriteria(AbstractConfigurationHistoryDataSource.CriteriaField.IDS,
                    updateIds.toArray(new Integer[updateIds.size()]));

                DSRequest requestProperties = new DSRequest();
                requestProperties.setAttribute(
                    AbstractConfigurationHistoryDataSource.RequestProperty.FETCH_CONFIGURATION, true);

                getDataSource().fetchData(criteria, new DSCallback() {
                    public void execute(DSResponse response, Object rawData, DSRequest request) {
                        ArrayList<AbstractResourceConfigurationUpdate> updatesWithConfigs = new ArrayList<AbstractResourceConfigurationUpdate>();
                        Record[] records = response.getData();
                        for (Record record : records) {
                            AbstractResourceConfigurationUpdate update = (AbstractResourceConfigurationUpdate) record
                                .getAttributeAsObject(AbstractConfigurationHistoryDataSource.Field.OBJECT);
                            updatesWithConfigs.add(update);
                        }
                        ConfigurationComparisonView.displayComparisonDialog(updatesWithConfigs);
                    }
                }, requestProperties);
            }
        });

        if (getResourceId() != null) {
            addTableAction(MSG.view_configurationHistoryList_rollback(), MSG.common_msg_areYouSure(), ButtonColor.RED,
                new AbstractTableAction(hasWritePerm ? TableActionEnablement.SINGLE : TableActionEnablement.NEVER) {
                    public void executeAction(ListGridRecord[] selection, Object actionValue) {
                        if (selection != null && selection.length == 1) {
                            ListGridRecord record = selection[0];
                            int configHistoryIdToRollbackTo = record
                                .getAttributeAsInt(AbstractConfigurationHistoryDataSource.Field.ID);
                            rollback(configHistoryIdToRollbackTo);
                        }
                    }
                });
        }

        super.configureTable();
    }

    @Override
    protected String getDetailsLinkColumnName() {
        return AbstractConfigurationHistoryDataSource.Field.ID;
    }

    @Override
    protected CellFormatter getDetailsLinkColumnCellFormatter() {
        return new CellFormatter() {
            public String format(Object value, ListGridRecord record, int i, int i1) {
                Integer recordId = getId(record);
                String detailsUrl = "#" + getBasePath() + "/" + recordId;
                String cellHtml = LinkManager.getHref(detailsUrl, value.toString());
                String isCurrentConfig = record
                    .getAttribute(AbstractConfigurationHistoryDataSource.Field.CURRENT_CONFIG);
                if (Boolean.parseBoolean(isCurrentConfig)) {
                    cellHtml = Canvas.imgHTML(ImageManager.getApproveIcon()) + cellHtml;
                }
                return cellHtml;
            }
        };
    }

    protected abstract void rollback(int configHistoryIdToRollbackTo);

    protected abstract void delete(int[] doomedIds);
}
