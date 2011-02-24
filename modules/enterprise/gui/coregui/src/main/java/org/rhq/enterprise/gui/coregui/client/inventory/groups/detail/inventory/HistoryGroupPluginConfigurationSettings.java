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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.inventory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.composite.ResourceConfigurationComposite;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.DisambiguationReport;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.configuration.GroupConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.GroupMemberConfiguration;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.resource.disambiguation.ReportDecorator;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * Read only view that shows group plugin configuration properties. These are properties
 * that are common across all members of the group.
 *
 * @author John Mazzitelli
 */
public class HistoryGroupPluginConfigurationSettings extends LocatableVLayout {
    private final ResourceGroup group;
    private final ResourcePermission groupPerms;
    private final int groupUpdateId;
    private ConfigurationDefinition configurationDefinition;
    private List<GroupMemberConfiguration> memberConfigurations;
    private GroupConfigurationEditor editor;

    public HistoryGroupPluginConfigurationSettings(String locatorId, ResourceGroupComposite groupComposite, int updateId) {
        super(locatorId);
        this.group = groupComposite.getResourceGroup();
        this.groupPerms = groupComposite.getResourcePermission();
        this.groupUpdateId = updateId;

        setMargin(5);
        setMembersMargin(5);
        String backPath = LinkManager.getGroupPluginConfigurationUpdateHistoryLink(this.group.getId());
        BackButton backButton = new BackButton(extendLocatorId("BackButton"), MSG.view_tableSection_backButton(),
            backPath);
        addMember(backButton);
    }

    @Override
    protected void onDraw() {
        super.onDraw();
        if (this.groupPerms.isInventory()) {
            loadConfigurationDefinition();
            loadConfigurations();
        } else {
            CoreGUI.getMessageCenter().notify(new Message(MSG.view_group_pluginConfig_view_noperm()));
        }
    }

    private void initEditor() {
        if (this.configurationDefinition != null && this.memberConfigurations != null) {
            this.editor = new GroupConfigurationEditor(this.extendLocatorId("Editor"), this.configurationDefinition,
                this.memberConfigurations);
            this.editor.setEditorTitle(MSG.view_group_pluginConfig_view_groupProperties() + " - " + this.groupUpdateId);
            this.editor.setOverflow(Overflow.AUTO);
            this.editor.setReadOnly(true);
            addMember(this.editor);
        }
    }

    private void loadConfigurationDefinition() {
        if (this.configurationDefinition == null) {
            final ResourceType type = this.group.getResourceType();
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

    private void loadConfigurations() {
        this.memberConfigurations = null;
        GWTServiceLookup.getConfigurationService().findPluginConfigurationsForGroupUpdate(groupUpdateId,
            new AsyncCallback<List<DisambiguationReport<ResourceConfigurationComposite>>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_group_pluginConfig_members_fetchFailureConn(group.toString()), caught);
                }

                public void onSuccess(List<DisambiguationReport<ResourceConfigurationComposite>> results) {
                    memberConfigurations = new ArrayList<GroupMemberConfiguration>(results.size());
                    for (DisambiguationReport<ResourceConfigurationComposite> result : results) {
                        int resourceId = result.getOriginal().getResourceId();
                        String label = ReportDecorator.decorateDisambiguationReport(result, resourceId, false);
                        Configuration configuration = result.getOriginal().getConfiguration();
                        GroupMemberConfiguration memberConfiguration = new GroupMemberConfiguration(resourceId, label,
                            configuration);
                        if (configuration == null || configuration.getProperties().isEmpty()) {
                            throw new RuntimeException(
                                "One or more null or empty member connection settings was returned by the Server.");
                        }
                        memberConfigurations.add(memberConfiguration);
                    }
                    initEditor();
                }
            });
    }
}
