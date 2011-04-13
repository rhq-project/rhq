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

import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.configuration.AbstractResourceConfigurationUpdate;
import org.rhq.core.domain.criteria.AbstractResourceConfigurationUpdateCriteria;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationComparisonView;
import org.rhq.enterprise.gui.coregui.client.components.table.AbstractTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;
import org.rhq.enterprise.gui.coregui.client.components.table.TableSection;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;
import org.rhq.enterprise.gui.coregui.client.util.selenium.SeleniumUtility;

/**
 * The superclass to the main plugin/resource views that lists all configuration history items.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public abstract class AbstractConfigurationHistoryView<T extends AbstractConfigurationHistoryDataSource<? extends AbstractResourceConfigurationUpdate, ? extends AbstractResourceConfigurationUpdateCriteria>>
    extends TableSection<T> {

    private Integer resourceId;
    private boolean hasWritePerm; // can delete history or rollback to a previous config

    /**
     * Use this constructor to view config histories for all viewable Resources.
     */
    public AbstractConfigurationHistoryView(String locatorId, String title, boolean hasWritePerm) {
        super(locatorId, title);
        this.hasWritePerm = hasWritePerm;
        this.resourceId = null;
    }

    /**
     * Use this constructor to view the config history for the Resource with the specified ID.
     *
     * @param resourceId a Resource ID
     */
    public AbstractConfigurationHistoryView(String locatorId, String title, boolean hasWritePerm, int resourceId) {
        super(locatorId, title, createCriteria(resourceId));
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

        addTableAction(extendLocatorId("Delete"), MSG.common_button_delete(), MSG.common_msg_areYouSure(),
            new AbstractTableAction(hasWritePerm ? TableActionEnablement.ANY : TableActionEnablement.NEVER) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    if (selection != null && selection.length > 0) {
                        int[] doomedIds = new int[selection.length];
                        int i = 0;
                        for (ListGridRecord selected : selection) {
                            doomedIds[i] = selected.getAttributeAsInt(AbstractConfigurationHistoryDataSource.Field.ID);
                            if (selected
                                .getAttribute(AbstractConfigurationHistoryDataSource.Field.GROUP_CONFIG_UPDATE_ID) != null) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_configurationHistoryList_cannotDeleteGroupItems(),
                                        Severity.Warning));
                                return; // abort
                            }
                            if (Boolean.parseBoolean(selected
                                .getAttribute(AbstractConfigurationHistoryDataSource.Field.CURRENT_CONFIG))) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_configurationHistoryList_cannotDeleteCurrent(),
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
                ArrayList<AbstractResourceConfigurationUpdate> configs = new ArrayList<AbstractResourceConfigurationUpdate>();
                for (ListGridRecord record : selection) {
                    AbstractResourceConfigurationUpdate update = (AbstractResourceConfigurationUpdate) record
                        .getAttributeAsObject(AbstractConfigurationHistoryDataSource.Field.OBJECT);
                    configs.add(update);
                }
                ConfigurationComparisonView.displayComparisonDialog(configs);
            }
        });

        if (getResourceId() != null) {
            addTableAction(extendLocatorId("Rollback"), MSG.view_configurationHistoryList_rollback(), MSG
                .common_msg_areYouSure(), new AbstractTableAction(hasWritePerm ? TableActionEnablement.SINGLE
                : TableActionEnablement.NEVER) {
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    if (selection != null && selection.length == 1) {
                        ListGridRecord record = selection[0];
                        rollback(record.getAttributeAsInt(AbstractConfigurationHistoryDataSource.Field.ID).intValue());
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
                String cellHtml = SeleniumUtility.getLocatableHref(detailsUrl, value.toString(), null);
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
