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
package org.rhq.enterprise.gui.coregui.client.inventory.groups.detail.configuration;

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
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.GroupConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.GroupMemberConfiguration;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.gwt.ConfigurationGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceDetailView;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A view for editing a group's configuration.
 *
 * @author Ian Springer
 */
public class GroupResourceConfigurationEditView extends LocatableVLayout implements PropertyValueChangeListener,
    RefreshableView {
    private final ConfigurationGWTServiceAsync configurationService = GWTServiceLookup.getConfigurationService();

    private ResourceGroup group;
    private ResourcePermission resourcePermission;
    private ConfigurationDefinition configurationDefinition;
    private List<GroupMemberConfiguration> memberConfigurations;

    private ConfigurationEditor editor;
    private IButton saveButton;

    private boolean refreshing = false;

    public GroupResourceConfigurationEditView(String locatorId, ResourceGroupComposite groupComposite) {
        super(locatorId);

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

        this.saveButton = new LocatableIButton(this.extendLocatorId("Save"), MSG.common_button_save());
        this.saveButton.setTooltip(MSG.view_group_resConfig_edit_saveTooltip());
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        toolStrip.addMember(saveButton);

        addMember(toolStrip);
        refresh();

        if (!this.resourcePermission.isConfigureWrite()) {
            Message message = new Message(MSG.view_group_resConfig_edit_noperm(), Message.Severity.Info, EnumSet.of(
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
            this.editor = new GroupConfigurationEditor(this.extendLocatorId("Editor"), this.configurationDefinition,
                this.memberConfigurations);
            this.editor.setOverflow(Overflow.AUTO);
            this.editor.addPropertyValueChangeListener(this);
            this.editor.setReadOnly(!this.resourcePermission.isConfigureWrite());
            addMember(this.editor);
            this.refreshing = false; // when we get here, we know we are done the refresh
        }
    }

    private void loadConfigurationDefinition() {
        if (this.configurationDefinition == null) {
            final ResourceType type = this.group.getResourceType();
            ResourceTypeRepository.Cache.getInstance().getResourceTypes(new Integer[] { type.getId() },
                EnumSet.of(ResourceTypeRepository.MetadataType.resourceConfigurationDefinition),
                new ResourceTypeRepository.TypesLoadedCallback() {
                    public void onTypesLoaded(Map<Integer, ResourceType> types) {
                        configurationDefinition = types.get(type.getId()).getResourceConfigurationDefinition();
                        if (configurationDefinition == null) {
                            throw new IllegalStateException("Configuration is not supported by this group.");
                        }
                        initEditor();
                    }
                });
        }
    }

    private void loadConfigurations() {
        this.memberConfigurations = null;
        this.configurationService.findResourceConfigurationsForGroup(group.getId(),
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
                                            "One or more null or empty member connection settings was returned by the Server.");
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
        CoreGUI.getErrorHandler().handleError(MSG.view_group_resConfig_edit_loadFail(group.toString()), caught);
    }

    private void save() {
        List<ResourceConfigurationComposite> resourceConfigurations = convertToCompositeList();
        GWTServiceLookup.getConfigurationService().updateResourceConfigurationsForGroup(this.group.getId(),
            resourceConfigurations, new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_group_resConfig_edit_saveFailure(group.getResourceType().getName(), group.getName()),
                        caught);
                }

                public void onSuccess(Void result) {
                    String configHistoryUrl = LinkManager.getResourceGroupTabLink(group.getId(),
                            ResourceDetailView.Tab.CONFIGURATION, ResourceDetailView.ConfigurationSubTab.HISTORY);
                    String configHistoryView = configHistoryUrl.substring(1); // chop off the leading '#'
                    Message message = new Message(MSG.view_group_resConfig_edit_saveInitiated_concise(), MSG
                            .view_group_resConfig_edit_saveInitiated_full(group.getResourceType().getName(), group
                                .getName()), Message.Severity.Info);
                    CoreGUI.goToView(configHistoryView, message);
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
                message = new Message(MSG.view_group_resConfig_edit_valid(), Message.Severity.Info, EnumSet.of(
                    Message.Option.Transient, Message.Option.Sticky));
            } else {
                this.saveButton.disable();
                message = new Message(MSG.view_group_resConfig_edit_invalid(invalidPropertyNames.values().toString()),
                    Message.Severity.Error, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            }
            messageCenter.notify(message);
        } else {
            this.saveButton.enable();
        }
    }
}
