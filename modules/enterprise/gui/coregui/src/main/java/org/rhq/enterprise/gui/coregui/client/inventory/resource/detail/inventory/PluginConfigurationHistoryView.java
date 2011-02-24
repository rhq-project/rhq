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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.inventory;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.view.ViewName;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.configuration.AbstractConfigurationHistoryView;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * The main view that lists all plugin configuration history items.
 * 
 * @author Greg Hinkle
 * @author John Mazzitelli
 */
public class PluginConfigurationHistoryView extends
    AbstractConfigurationHistoryView<PluginConfigurationHistoryDataSource> {
    public static final ViewName VIEW_ID = new ViewName("PluginConfigurationHistoryView", MSG
        .view_tabs_common_connectionSettingsHistory());

    /**
     * Use this constructor to view plugin config histories for all viewable Resources.
     */
    public PluginConfigurationHistoryView(String locatorId, boolean hasWritePerm) {
        super(locatorId, VIEW_ID.getTitle(), hasWritePerm);
        PluginConfigurationHistoryDataSource datasource = new PluginConfigurationHistoryDataSource();
        setDataSource(datasource);
    }

    /**
     * Use this constructor to view the plugin config history for the Resource with the specified ID.
     *
     * @param resourceId a Resource ID
     */
    public PluginConfigurationHistoryView(String locatorId, boolean hasWritePerm, int resourceId) {
        super(locatorId, VIEW_ID.getTitle(), hasWritePerm, resourceId);
        PluginConfigurationHistoryDataSource datasource = new PluginConfigurationHistoryDataSource();
        setDataSource(datasource);
    }

    @Override
    public Canvas getDetailsView(int id) {
        PluginConfigurationHistoryDetailView detailView = new PluginConfigurationHistoryDetailView(this.getLocatorId());
        return detailView;
    }

    @Override
    protected void rollback(int configHistoryIdToRollbackTo) {
        GWTServiceLookup.getConfigurationService().rollbackPluginConfiguration(getResourceId().intValue(),
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

    @Override
    protected void delete(int[] doomedIds) {
        GWTServiceLookup.getConfigurationService().purgePluginConfigurationUpdates(doomedIds, true,
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
}
