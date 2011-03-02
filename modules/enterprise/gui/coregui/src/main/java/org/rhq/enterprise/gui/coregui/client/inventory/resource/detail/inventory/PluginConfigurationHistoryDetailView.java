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

import java.util.EnumSet;

import com.google.gwt.user.client.rpc.AsyncCallback;

import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.PluginConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author John Mazzitelli
 */
public class PluginConfigurationHistoryDetailView extends LocatableVLayout implements BookmarkableView {

    public PluginConfigurationHistoryDetailView(String locatorId) {
        super(locatorId);

        setWidth100();
        setHeight100();
    }

    private void displayHistory(final PluginConfigurationUpdate update) {

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(update.getResource().getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.pluginConfigurationDefinition),
            new ResourceTypeRepository.TypeLoadedCallback() {

                public void onTypesLoaded(ResourceType type) {
                    ConfigurationDefinition definition = type.getPluginConfigurationDefinition();
                    ConfigurationEditor editor = new ConfigurationEditor("PluginConfigHist-"
                        + update.getResource().getName(), definition, update.getConfiguration());
                    editor.setReadOnly(true);
                    editor.setEditorTitle(MSG.common_title_version() + " - " + update.getId());
                    addMember(editor);
                    markForRedraw();
                }
            });
    }

    @Override
    public void renderView(ViewPath viewPath) {

        int updateId = viewPath.getCurrentAsInt();

        PluginConfigurationUpdateCriteria criteria = new PluginConfigurationUpdateCriteria();
        criteria.fetchConfiguration(true);
        criteria.fetchResource(true);
        criteria.addFilterId(updateId);

        GWTServiceLookup.getConfigurationService().findPluginConfigurationUpdatesByCriteria(criteria,
            new AsyncCallback<PageList<PluginConfigurationUpdate>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_configurationHistoryDetails_error_loadFailure(),
                        caught);
                }

                public void onSuccess(PageList<PluginConfigurationUpdate> result) {
                    PluginConfigurationUpdate update = result.get(0);
                    displayHistory(update);
                }
            });
    }
}
