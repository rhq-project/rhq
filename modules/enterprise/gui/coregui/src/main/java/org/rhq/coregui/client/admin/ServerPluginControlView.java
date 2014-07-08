/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.coregui.client.admin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.FormItemIcon;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemClickHandler;
import com.smartgwt.client.widgets.form.fields.events.FormItemIconClickEvent;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.plugin.PluginKey;
import org.rhq.core.domain.plugin.ServerPlugin;
import org.rhq.core.domain.plugin.ServerPluginControlDefinition;
import org.rhq.core.domain.plugin.ServerPluginControlResults;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ErrorMessageWindow;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.coregui.client.components.form.SortedSelectItem;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.PluginGWTServiceAsync;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedVStack;
import org.rhq.coregui.client.util.message.Message;
import org.rhq.coregui.client.util.message.Message.Severity;

/**
 * A component used to invoke controls on a server plugin.
 *
 * @author John Mazzitelli
 */
public class ServerPluginControlView extends EnhancedVStack {

    private final ServerPlugin plugin;
    private final ArrayList<ServerPluginControlDefinition> controlDefinitions;
    private EnhancedVLayout paramsLayout;
    private EnhancedVLayout resultsLayout;
    private String selectedControlName;
    private ConfigurationDefinition selectedParamsDef;
    private ConfigurationEditor selectedParamsEditor;
    private ConfigurationDefinition selectedResultsDef;

    public ServerPluginControlView(ServerPlugin plugin, ArrayList<ServerPluginControlDefinition> controlDefs) {
        super();
        this.plugin = plugin;
        this.controlDefinitions = controlDefs;
        setAutoHeight();
        setMargin(5);
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        DynamicForm form = new DynamicForm();
        addMember(form);

        paramsLayout = new EnhancedVLayout();
        paramsLayout.setVisible(false);
        addMember(paramsLayout);

        resultsLayout = new EnhancedVLayout();
        resultsLayout.setVisible(false);
        addMember(resultsLayout);

        final FormItemIcon executeButton = new FormItemIcon();
        executeButton.setName("execute");
        executeButton.setSrc(ImageManager.getOperationIcon());
        executeButton.setPrompt(MSG.common_button_execute());

        final SelectItem controlNamesItem = new SortedSelectItem("controlMenu",
            MSG.view_admin_plugins_serverControls_name());
        LinkedHashMap<String, String> controlNames = new LinkedHashMap<String, String>();
        for (ServerPluginControlDefinition def : controlDefinitions) {
            controlNames.put(def.getName(), def.getDisplayName());
        }
        controlNamesItem.setValueMap(controlNames);
        controlNamesItem.setWidth(300);
        controlNamesItem.addChangedHandler(new ChangedHandler() {
            public void onChanged(ChangedEvent event) {
                // determine the control that was selected and remember information about it
                selectedControlName = (String) event.getValue();
                ServerPluginControlDefinition def = getControlDefinition(selectedControlName);
                selectedParamsDef = def.getParameters();
                selectedResultsDef = def.getResults();

                // we changed the control selected, hide any previously shown results
                resultsLayout.destroyMembers();
                resultsLayout.setVisible(false);

                // show the parameters, if there are any
                showParameters();

                // make sure we show our execute button now that a control has been selected
                if (controlNamesItem.getIcon(executeButton.getName()) == null) {
                    controlNamesItem.setIcons(executeButton);
                }
            }
        });
        form.setItems(controlNamesItem);

        // when this is clicked, the user wants to invoke the control
        executeButton.addFormItemClickHandler(new FormItemClickHandler() {
            public void onFormItemClick(FormItemIconClickEvent event) {
                Configuration params;
                try {
                    params = getParameters();
                } catch (IllegalStateException paramsInvalid) {
                    Message msg = new Message(MSG.view_admin_plugins_serverControls_badParams(), Severity.Warning,
                        EnumSet.of(Message.Option.Transient));
                    CoreGUI.getMessageCenter().notify(msg);
                    return;
                }
                PluginGWTServiceAsync pm = GWTServiceLookup.getPluginService();
                PluginKey pluginKey = PluginKey.createServerPluginKey(plugin.getType(), plugin.getName());
                pm.invokeServerPluginControl(pluginKey, selectedControlName, params,
                    new AsyncCallback<ServerPluginControlResults>() {
                        public void onSuccess(ServerPluginControlResults results) {
                            showResults(results);
                        }

                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_admin_plugins_serverControls_invokeFailure(), caught);
                        }
                    });
            }
        });

    }

    private void showParameters() {
        paramsLayout.destroyMembers();

        if (selectedParamsDef != null) {
            paramsLayout.setVisible(true);
            Configuration config = new Configuration();
            selectedParamsEditor = new ConfigurationEditor(selectedParamsDef, config);
            selectedParamsEditor.setAutoHeight();

            DynamicForm paramsForm = new DynamicForm();
            paramsForm.setWidth100();
            paramsForm.setIsGroup(true);
            paramsForm.setGroupTitle(MSG.view_admin_plugins_serverControls_parameters());
            CanvasItem paramsCanvas = new CanvasItem("paramsCanvas");
            paramsCanvas.setShowTitle(false);
            paramsCanvas.setColSpan(2);
            paramsCanvas.setCanvas(selectedParamsEditor);
            paramsForm.setItems(paramsCanvas);

            paramsLayout.addMember(paramsForm);
        } else {
            paramsLayout.setVisible(false);
        }
        return;
    }

    private Configuration getParameters() throws IllegalStateException {
        if (selectedParamsDef != null) {
            if (!selectedParamsEditor.validate()) {
                throw new IllegalStateException();
            }
            return selectedParamsEditor.getConfiguration();
        }
        return null;
    }

    private void showResults(final ServerPluginControlResults results) {
        resultsLayout.destroyMembers();

        DynamicForm resultsForm = new DynamicForm();
        resultsForm.setWidth100();
        resultsForm.setIsGroup(true);
        resultsForm.setGroupTitle(MSG.view_admin_plugins_serverControls_results());

        ArrayList<FormItem> formItems = new ArrayList<FormItem>(3);

        StaticTextItem statusItem = new StaticTextItem("status", MSG.common_title_status());
        statusItem.setValue(Img.imgHTML(ImageManager.getAvailabilityIcon(results.isSuccess())));
        formItems.add(statusItem);

        if (results.getError() != null) {
            statusItem.setPrompt(MSG.view_admin_plugins_serverControls_clickForError());
            statusItem.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    ErrorMessageWindow win = new ErrorMessageWindow(MSG.common_label_error(), results.getError());
                    win.show();
                }
            });
        }

        if (selectedResultsDef != null && results.getComplexResults() != null) {
            ConfigurationEditor editor = new ConfigurationEditor(selectedResultsDef, results.getComplexResults());
            editor.setReadOnly(true);
            editor.setAutoHeight();

            CanvasItem paramsCanvas = new CanvasItem("resultsCanvas");
            paramsCanvas.setShowTitle(false);
            paramsCanvas.setColSpan(2);
            paramsCanvas.setCanvas(editor);
            formItems.add(paramsCanvas);
        }

        resultsForm.setItems(formItems.toArray(new FormItem[0]));

        resultsLayout.addMember(resultsForm);
        resultsLayout.setVisible(true);
        return;
    }

    private ServerPluginControlDefinition getControlDefinition(String controlName) {
        for (ServerPluginControlDefinition def : controlDefinitions) {
            if (def.getName().equals(controlName)) {
                return def;
            }
        }
        return null;
    }
}
