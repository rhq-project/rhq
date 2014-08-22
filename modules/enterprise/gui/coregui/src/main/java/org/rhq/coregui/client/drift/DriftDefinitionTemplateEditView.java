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
package org.rhq.coregui.client.drift;

import java.util.EnumSet;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.criteria.DriftDefinitionTemplateCriteria;
import org.rhq.core.domain.drift.DriftConfigurationDefinition;
import org.rhq.core.domain.drift.DriftDefinition;
import org.rhq.core.domain.drift.DriftDefinitionTemplate;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.RefreshableView;
import org.rhq.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeEvent;
import org.rhq.coregui.client.components.configuration.PropertyValueChangeListener;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.MessageCenter;

/**
 * A view for editing a Resource's configuration.
 *
 * @author Jay Shaughnessy
 */
public class DriftDefinitionTemplateEditView extends EnhancedVLayout implements PropertyValueChangeListener,
    RefreshableView {

    private int driftTemplateId;
    private boolean hasWriteAccess;
    private ConfigurationEditor editor;
    private ToolStrip buttonbar;
    private IButton saveButton;

    private DriftDefinitionTemplate driftTemplate;

    private boolean refreshing = false;

    public DriftDefinitionTemplateEditView(int driftTemplateId, boolean hasWriteAccess) {
        super();

        this.driftTemplateId = driftTemplateId;
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

        this.saveButton = new EnhancedIButton(MSG.common_button_save(), ButtonColor.BLUE);
        this.saveButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                save();
            }
        });
        buttonbar.addMember(saveButton);
        // The button bar will remain hidden until the definition template has been successfully loaded.
        buttonbar.setVisible(false);
        addMember(buttonbar);

        refresh();

        if (!this.hasWriteAccess) {
            Message message = new Message(MSG.view_configurationDetails_noPermission(), Message.Severity.Info,
                EnumSet.of(Message.Option.Transient, Message.Option.Sticky));
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

        DriftDefinitionTemplateCriteria criteria = new DriftDefinitionTemplateCriteria();
        criteria.addFilterId(driftTemplateId);

        GWTServiceLookup.getDriftService().findDriftDefinitionTemplatesByCriteria(criteria,
            new AsyncCallback<PageList<DriftDefinitionTemplate>>() {

                public void onSuccess(final PageList<DriftDefinitionTemplate> result) {

                    driftTemplate = result.get(0);

                    editor = new ConfigurationEditor(DriftConfigurationDefinition.getExistingTemplateInstance(),
                        driftTemplate.getConfiguration());
                    editor.setOverflow(Overflow.AUTO);
                    editor.addPropertyValueChangeListener(DriftDefinitionTemplateEditView.this);
                    editor.setReadOnly(!hasWriteAccess || !driftTemplate.isUserDefined());
                    addMember(editor);

                    saveButton.disable();
                    buttonbar.setVisible(true);
                    markForRedraw();
                    refreshing = false;
                }

                public void onFailure(Throwable caught) {
                    refreshing = false;
                    CoreGUI.getErrorHandler().handleError("Failed to load definition.", caught);
                }
            });
    }

    private void save() {

        Configuration updatedConfiguration = editor.getConfiguration();
        DriftDefinition updatedDefinition = new DriftDefinition(updatedConfiguration);
        driftTemplate.setTemplateDefinition(updatedDefinition);

        GWTServiceLookup.getDriftService().updateTemplate(driftTemplate, new AsyncCallback<Void>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_configurationDetails_error_updateFailure(), caught);
            }

            public void onSuccess(Void result) {
                Message message = new Message(MSG.view_drift_success_templateUpdated(), Message.Severity.Info);
                CoreGUI.getMessageCenter().notify(message);
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
        } else if (event.getInvalidPropertyNames().isEmpty()) {
            this.saveButton.enable();
        } else {
            this.saveButton.disable();
        }
    }

}
