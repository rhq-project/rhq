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
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PluginConfigurationUpdate;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.PluginConfigurationUpdateCriteria;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Read only view that shows resource plugin configuration properties.
 *
 * @author John Mazzitelli
 */
public class HistoryPluginConfigurationSettings extends LocatableVLayout {
    private final Resource resource;
    private final ResourcePermission resourcePerms;
    private final int resourceUpdateId;
    private ConfigurationDefinition configurationDefinition;
    private Configuration resourceConfiguration;
    private ConfigurationEditor editor;

    public HistoryPluginConfigurationSettings(String locatorId, ResourceComposite composite, int updateId) {
        super(locatorId);
        this.resource = composite.getResource();
        this.resourcePerms = composite.getResourcePermission();
        this.resourceUpdateId = updateId;

        setMargin(5);
        setMembersMargin(5);
        String backPath = LinkManager.getResourcePluginConfigurationUpdateHistoryLink(this.resource.getId());
        BackButton backButton = new BackButton(extendLocatorId("BackButton"), MSG.view_tableSection_backButton(),
            backPath);
        addMember(backButton);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        if (this.resourcePerms.isInventory()) {
            loadConfigurationDefinition();
            loadConfiguration();
        } else {
            CoreGUI.getMessageCenter().notify(new Message(MSG.view_connectionSettingsHistory_view_noperm()));
        }
    }

    private void initEditor() {
        if (this.configurationDefinition != null && this.resourceConfiguration != null) {
            this.editor = new ConfigurationEditor(this.extendLocatorId("Editor"), this.configurationDefinition,
                this.resourceConfiguration);
            this.editor.setEditorTitle(MSG.view_connectionSettingsHistory_view_connProperties() + " - "
                + this.resourceUpdateId);
            this.editor.setOverflow(Overflow.AUTO);
            this.editor.setReadOnly(true);
            addMember(this.editor);
        }
    }

    private void loadConfigurationDefinition() {
        if (this.configurationDefinition == null) {
            final ResourceType type = this.resource.getResourceType();
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(new Integer[] { type.getId() },
                EnumSet.of(ResourceTypeRepository.MetadataType.pluginConfigurationDefinition),
                new ResourceTypeRepository.TypesLoadedCallback() {
                    public void onTypesLoaded(Map<Integer, ResourceType> types) {
                        configurationDefinition = types.get(type.getId()).getPluginConfigurationDefinition();
                        if (configurationDefinition == null) {
                            throw new IllegalStateException("Connection settings are not supported by this group.");
                        }
                        initEditor();
                    }
                });
        }
    }

    private void loadConfiguration() {
        this.resourceConfiguration = null;

        PluginConfigurationUpdateCriteria criteria = new PluginConfigurationUpdateCriteria();
        criteria.addFilterId(resourceUpdateId);
        criteria.fetchConfiguration(true);

        GWTServiceLookup.getConfigurationService().findPluginConfigurationUpdatesByCriteria(criteria,
            new AsyncCallback<PageList<PluginConfigurationUpdate>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_connectionSettingsHistory_settings_fetchFailure(resource.getName()), caught);
                }

                public void onSuccess(PageList<PluginConfigurationUpdate> results) {
                    if (results != null && results.size() > 0) {
                        resourceConfiguration = results.get(0).getConfiguration();
                        initEditor();
                    } else {
                        // should never really happen
                        CoreGUI.getErrorHandler().handleError(
                            MSG.view_connectionSettingsHistory_settings_fetchFailure(resource.getName()),
                            new Throwable("bad results"));
                    }
                }
            });
    }
}
