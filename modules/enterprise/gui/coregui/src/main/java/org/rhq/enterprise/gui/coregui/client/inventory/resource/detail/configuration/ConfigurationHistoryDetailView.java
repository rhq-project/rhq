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

import java.util.EnumSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.configuration.ResourceConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.ResourceConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

/**
 * @author Greg Hinkle
 */
public class ConfigurationHistoryDetailView extends VLayout implements BookmarkableView {

    private int configurationUpdateId;

    public ConfigurationHistoryDetailView() {
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onDraw() {
        super.onDraw();


    }


    private void displayHistory(final ResourceConfigurationUpdate update) {

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(update.getResource().getResourceType().getId(),
                EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                new ResourceTypeRepository.TypeLoadedCallback() {

                    public void onTypesLoaded(ResourceType type) {

                        ConfigurationDefinition definition = type.getResourceConfigurationDefinition();

                        ConfigurationEditor editor = new ConfigurationEditor(definition, update.getConfiguration());
                        editor.setReadOnly(true);
                        addMember(editor);
                        markForRedraw();
                    }
                });
    }

    public void displayInDialog() {

        Window window = new Window();
        window.setTitle("Configuration Details");
        window.setWidth(800);
        window.setHeight(800);
        window.setIsModal(true);
        window.setShowModalMask(true);
        window.setCanDragResize(true);
        window.centerInPage();
        window.addItem(this);
        window.show();
    }

    @Override
    public void renderView(ViewPath viewPath) {

        int updateId = viewPath.getCurrentAsInt();

        ResourceConfigurationUpdateCriteria criteria = new ResourceConfigurationUpdateCriteria();
        criteria.fetchConfiguration(true);
        criteria.fetchResource(true);
        criteria.addFilterId(updateId);


        GWTServiceLookup.getConfigurationService().findResourceConfigurationUpdatesByCriteria(criteria,
                new AsyncCallback<PageList<ResourceConfigurationUpdate>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Unable to load configuration history", caught);
                    }

                    public void onSuccess(PageList<ResourceConfigurationUpdate> result) {

                        ResourceConfigurationUpdate update = result.get(0);
                        displayHistory(update);
                    }
                });


    }
}
