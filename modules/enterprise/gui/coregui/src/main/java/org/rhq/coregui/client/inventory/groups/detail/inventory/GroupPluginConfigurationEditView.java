/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.coregui.client.inventory.groups.detail.inventory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.composite.ResourceConfigurationComposite;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.resource.ResourceAncestryFormat;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourcePermission;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.resource.group.composite.ResourceGroupComposite;
import org.rhq.core.domain.util.ResourceTypeUtility;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.coregui.client.components.configuration.GroupConfigurationEditor;
import org.rhq.coregui.client.components.configuration.GroupMemberConfiguration;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;
import org.rhq.coregui.client.util.message.MessageCenter;

/**
 * A view for editing a group's current plugin configuration.
 *
 * @author Ian Springer
 * @author John Mazzitelli
 */
public class GroupPluginConfigurationEditView extends EnhancedVLayout implements PropertyValueChangeListener,
    RefreshableView {
    private final ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();
    private final ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();

    private ResourceGroup group;
    private ResourcePermission resourcePermission;
    private ConfigurationDefinition configurationDefinition;
    private List<GroupMemberConfiguration> memberConfigurations;

    private ConfigurationEditor editor;
    private IButton saveButton;

    private boolean refreshing = false;

    public GroupPluginConfigurationEditView(ResourceGroupComposite groupComposite) {
        super();

        this.group = groupComposite.getResourceGroup();
        this.resourcePermission = groupComposite.getResourcePermission();
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        ToolStrip toolStrip = new ToolStrip();
        toolStrip.setWidth100();
        toolStrip.setExtraSpace(10);
        toolStrip.setMembersMargin(5);
        toolStrip.setLayoutMargin(5);

        this.saveButton = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
        this.saveButton.setTooltip(MSG.view_group_pluginConfig_edit_saveTooltip());
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });

        toolStrip.addMember(saveButton);

        addMember(toolStrip);
        refresh();

        if (!this.resourcePermission.isInventory()) {
            Message message = new Message(MSG.view_group_pluginConfig_edit_noperm(), Message.Severity.Info, EnumSet.of(
                Message.Option.Transient, Message.Option.Sticky));
            CoreGUI.getMessageCenter().notify(message);
        }
    }

    @Override
    public void refresh() {
        if (this.refreshing) {
            return; // we are already in the process of refreshing, don't do it again
        }

        this.refreshing = true;
        this.saveButton.disable();
        if (editor != null) {
            editor.destroy();
            removeMember(editor);
        }
        // TODO (ips): If editor != null, use editor.reload() instead.

        loadConfigurationDefinition();
        loadConfigurations();
    }

    private void initEditor() {
        if (this.configurationDefinition != null && this.memberConfigurations != null) {
            this.editor = new GroupConfigurationEditor(this.configurationDefinition, this.memberConfigurations);
            this.editor.setEditorTitle(MSG.view_group_pluginConfig_edit_currentGroupProperties());
            this.editor.setOverflow(Overflow.AUTO);
            this.editor.addPropertyValueChangeListener(this);
            this.editor.setReadOnly(!this.resourcePermission.isConfigureWrite());
            addMember(editor, 0);
            this.refreshing = false;
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
        this.configurationService.findPluginConfigurationsForGroup(group.getId(),
            new AsyncCallback<Map<Integer, Configuration>>() {

                public void onFailure(Throwable caught) {
                    handleLoadFailure(caught);
                }

                public void onSuccess(final Map<Integer, Configuration> configMap) {
                    final Integer[] resourceIds = configMap.keySet().toArray(new Integer[configMap.size()]);
                    resourceService.getResourcesAncestry(resourceIds, ResourceAncestryFormat.EXTENDED,
                        new AsyncCallback<Map<Integer, String>>() {

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
        refreshing = false;
        if (caught.getMessage().contains("ConfigurationUpdateStillInProgressException")) {
            CoreGUI.getMessageCenter().notify(
                new Message(MSG.view_group_pluginConfig_members_fetchFailureConnInProgress(), caught, Severity.Info));
        } else {
            CoreGUI.getErrorHandler().handleError(
                MSG.view_group_pluginConfig_members_fetchFailureConn(group.toString()), caught);
        }
    }

    private void save() {
        List<ResourceConfigurationComposite> resourceConfigurations = convertToCompositeList();
        GWTServiceLookup.getConfigurationService().updatePluginConfigurationsForGroup(this.group.getId(),
            resourceConfigurations, new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    String typeName = ResourceTypeUtility.displayName(group.getResourceType());
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_group_pluginConfig_edit_saveFailure(typeName, group.getName()), caught);
                }

                public void onSuccess(Void result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(MSG.view_group_pluginConfig_edit_saveInitiated_concise(), MSG
                            .view_group_pluginConfig_edit_saveInitiated_full(
                                ResourceTypeUtility.displayName(group.getResourceType()),
                                group.getName()), Message.Severity.Info));
                    refresh();
                }
            });
    }

    private List<ResourceConfigurationComposite> convertToCompositeList() {
        List<ResourceConfigurationComposite> resourceConfigurations = new ArrayList<ResourceConfigurationComposite>(
            this.memberConfigurations.size());
        for (GroupMemberConfiguration memberConfiguration : this.memberConfigurations) {
            resourceConfigurations.add(new ResourceConfigurationComposite(memberConfiguration.getId(),
                memberConfiguration.getConfiguration()));
        }
        return resourceConfigurations;
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        MessageCenter messageCenter = CoreGUI.getMessageCenter();
        Message message;
        if (event.isInvalidPropertySetChanged()) {
            Map<String, String> invalidPropertyNames = event.getInvalidPropertyNames();
            if (invalidPropertyNames.isEmpty()) {
                this.saveButton.enable();
                message = new Message(MSG.view_group_pluginConfig_edit_valid(), Message.Severity.Info, EnumSet.of(
                    Message.Option.Transient, Message.Option.Sticky));
            } else {
                this.saveButton.disable();
                message = new Message(
                    MSG.view_group_pluginConfig_edit_invalid(invalidPropertyNames.values().toString()),
                    Message.Severity.Error, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            }
            messageCenter.notify(message);
        } else if (event.getInvalidPropertyNames().isEmpty()) {
            this.saveButton.enable();
        } else {
            this.saveButton.disable();
        }
    }
}
