/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.drift;

import java.util.EnumSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.drift.DriftConfiguration;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.RefreshableView;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.enterprise.gui.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.ResourceDetailView;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.MessageCenter;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * A view for editing a Resource's configuration.
 *
 * @author Jay Shaughnessy
 */
public class DriftConfigurationEditView extends LocatableVLayout implements PropertyValueChangeListener,
    RefreshableView {

    private EntityContext context;
    private int driftConfigId;
    private boolean hasWriteAccess;
    private ConfigurationEditor editor;
    private ToolStrip buttonbar;
    private IButton saveButton;

    private boolean refreshing = false;

    public DriftConfigurationEditView(String locatorId, EntityContext context, int driftConfigId, boolean hasWriteAccess) {
        super(locatorId);

        this.context = context;
        this.driftConfigId = driftConfigId;
        this.hasWriteAccess = hasWriteAccess;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        this.buttonbar = new ToolStrip();
        buttonbar.setWidth100();
        buttonbar.setExtraSpace(10);
        buttonbar.setMembersMargin(5);
        buttonbar.setLayoutMargin(5);

        this.saveButton = new LocatableIButton(this.extendLocatorId("Save"), MSG.common_button_save());
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        buttonbar.addMember(saveButton);
        // The button bar will remain hidden until the configuration has been successfully loaded.
        buttonbar.setVisible(false);
        addMember(buttonbar);

        refresh();

        if (!this.hasWriteAccess) {
            Message message = new Message(MSG.view_configurationDetails_noPermission(), Message.Severity.Info, EnumSet
                .of(Message.Option.Transient, Message.Option.Sticky));
            CoreGUI.getMessageCenter().notify(message);
        }
    }

    @Override
    public void refresh() {
        if (this.refreshing) {
            return; // we are already in the process of refreshing, don't do it again
        }

        this.refreshing = true;
        this.buttonbar.setVisible(false);

        if (editor != null) {
            editor.destroy();
            removeMember(editor);
        }

        GWTServiceLookup.getDriftService().getDriftConfiguration(context, driftConfigId,
            new AsyncCallback<DriftConfiguration>() {
                @Override
                public void onSuccess(final DriftConfiguration result) {

                    editor = new ConfigurationEditor(extendLocatorId("Editor"), DriftConfigurationDefinition
                        .getInstance(), result.getConfiguration());
                    editor.setOverflow(Overflow.AUTO);
                    editor.addPropertyValueChangeListener(DriftConfigurationEditView.this);
                    editor.setReadOnly(!hasWriteAccess);
                    addMember(editor);

                    saveButton.disable();
                    buttonbar.setVisible(true);
                    markForRedraw();
                    refreshing = false;
                }

                @Override
                public void onFailure(Throwable caught) {
                    refreshing = false;
                    CoreGUI.getErrorHandler().handleError("Failed to load configuration.", caught);
                }
            });
    }

    private void save() {
        Configuration updatedConfiguration = editor.getConfiguration();

        GWTServiceLookup.getDriftService().updateDriftConfiguration(context,
            new DriftConfiguration(updatedConfiguration), new AsyncCallback<Void>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(MSG.view_configurationDetails_error_updateFailure(), caught);
                }

                public void onSuccess(Void result) {
                    Message message = new Message(MSG.view_drift_success_configurationUpdated(), Message.Severity.Info);

                    switch (context.getType()) {
                    case Resource:
                        int resourceId = context.getResourceId();

                        String driftHistoryUrl = LinkManager.getResourceTabLink(resourceId,
                            ResourceDetailView.Tab.DRIFT, ResourceDetailView.ConfigurationSubTab.HISTORY);
                        driftHistoryUrl = driftHistoryUrl.substring(1); // chop off the leading '#'
                        CoreGUI.goToView(driftHistoryUrl, message);

                        break;

                    default:
                        throw new IllegalArgumentException("Entity Context Type not supported [" + context + "]");
                    }
                }
            });
    }

    @Override
    public void propertyValueChanged(PropertyValueChangeEvent event) {
        MessageCenter messageCenter = CoreGUI.getMessageCenter();
        Message message;
        if (event.isInvalidPropertySetChanged()) {
            Map<String, String> invalidPropertyNames = event.getInvalidPropertyNames();
            if (invalidPropertyNames.isEmpty()) {
                this.saveButton.enable();
                message = new Message(MSG.view_configurationDetails_allPropertiesValid(), Message.Severity.Info,
                    EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            } else {
                this.saveButton.disable();
                message = new Message(MSG.view_configurationDetails_somePropertiesInvalid(invalidPropertyNames.values()
                    .toString()), Message.Severity.Error, EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
            }
            messageCenter.notify(message);
        } else {
            this.saveButton.enable();
        }
    }

}
