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
package org.rhq.coregui.client.inventory.groups.detail.inventory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceAncestryFormat;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.components.buttons.BackButton;
import org.rhq.coregui.client.components.configuration.GroupConfigurationEditor;
import org.rhq.coregui.client.components.configuration.GroupMemberConfiguration;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * Read only view that shows group plugin configuration properties. These are properties
 * that are common across all members of the group.
 *
 * @author John Mazzitelli
 */
public class HistoryGroupPluginConfigurationSettings extends EnhancedVLayout {
    private final ResourceGroup group;
    private final ResourcePermission groupPerms;
    private final int groupUpdateId;
    private ConfigurationDefinition configurationDefinition;
    private List<GroupMemberConfiguration> memberConfigurations;
    private GroupConfigurationEditor editor;

    public HistoryGroupPluginConfigurationSettings(ResourceGroupComposite groupComposite, int updateId) {
        super();
        this.group = groupComposite.getResourceGroup();
        this.groupPerms = groupComposite.getResourcePermission();
        this.groupUpdateId = updateId;

        setMargin(5);
        setMembersMargin(5);
        String backPath = LinkManager.getGroupPluginConfigurationUpdateHistoryLink(EntityContext.forGroup(this.group),
            null);
        BackButton backButton = new BackButton(MSG.view_tableSection_backButton(), backPath);
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
            this.editor = new GroupConfigurationEditor(this.configurationDefinition, this.memberConfigurations);
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
            new AsyncCallback<Map<Integer, Configuration>>() {

                public void onFailure(Throwable caught) {
                    handleLoadFailure(caught);
                }

                public void onSuccess(final Map<Integer, Configuration> configMap) {
                    final Integer[] resourceIds = configMap.keySet().toArray(new Integer[configMap.size()]);
                    GWTServiceLookup.getResourceService().getResourcesAncestry(resourceIds,
                        ResourceAncestryFormat.EXTENDED, new AsyncCallback<Map<Integer, String>>() {

                            public void onFailure(Throwable caught) {
                                handleLoadFailure(caught);
                            }

                            public void onSuccess(Map<Integer, String> labelMap) {
                                memberConfigurations = new ArrayList<GroupMemberConfiguration>(configMap.size());
                                for (Integer resourceId : resourceIds) {
                                    String label = labelMap.get(resourceId);
                                    Configuration configuration = configMap.get(resourceId);
                                    GroupMemberConfiguration memberConfiguration = new GroupMemberConfiguration(
                                        resourceId, label, configuration);
                                    if (configuration == null || configuration.getProperties().isEmpty()) {
                                        throw new RuntimeException(
                                            "The server did not return the connection settings for one or more member resources.");
                                    }
                                    memberConfigurations.add(memberConfiguration);
                                }
                                initEditor();
                            }
                        });
                }
            });
    }

    private void handleLoadFailure(Throwable caught) {
        if (caught.getMessage().contains("ConfigurationUpdateStillInProgressException")) {
            CoreGUI.getMessageCenter().notify(
                new Message(MSG.view_group_pluginConfig_members_fetchFailureConnInProgress(), caught, Severity.Info));
        } else {
            CoreGUI.getErrorHandler().handleError(
                MSG.view_group_pluginConfig_members_fetchFailureConn(group.toString()), caught);
        }
    }

}