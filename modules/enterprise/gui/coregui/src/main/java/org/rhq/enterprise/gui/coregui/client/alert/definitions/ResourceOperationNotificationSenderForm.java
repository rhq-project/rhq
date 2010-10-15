/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.form.validator.CustomValidator;
import com.smartgwt.client.widgets.layout.HLayout;

import org.rhq.core.domain.alert.notification.AlertNotification;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.criteria.ResourceCriteria;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.alert.definitions.ResourceOperationNotificationInfo.ResourceSelectionMode;
import org.rhq.enterprise.gui.coregui.client.components.configuration.ConfigurationEditor;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.SingleResourcePicker;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.selection.ResourcePicker.OkHandler;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableHLayout;

/**
 * This notification form will be used for the Resource Operation sender. This form lets
 * you pick a resource operation that the sender will invoke when an alert is triggered.
 *
 * @author John Mazzitelli
 */
public class ResourceOperationNotificationSenderForm extends AbstractNotificationSenderForm {

    private static final String RESOURCE_ID_ATTRIBUTE = "singleResourceId";
    private static final String OPERATION_DEFS_ATTRIBUTE = "operationDefinitions";

    private final ResourceType resourceType; // the type representing the current resource or the current type being edited
    private final Resource theResource; // if we are editing a resource instance, this is it - otherwise, will be null (for group/template alert defs)

    private LocatableDynamicForm dynamicForm;
    private SelectItem modeSelectItem;
    private StaticTextItem singleResourceTextItem;
    private SelectItem ancestorTypeSelectItem;
    private SelectItem descendantTypeSelectItem;
    private TextItem descendantNameTextItem;
    private HLayout operationArgumentsCanvasItem;
    private SelectItem operationSelectItem;

    public ResourceOperationNotificationSenderForm(String locatorId, AlertNotification notif, String sender,
        ResourceType resourceType, Resource res) {

        super(locatorId, notif, sender);
        this.resourceType = resourceType;
        this.theResource = res;

        buildUI();
    }

    private void buildUI() {

        dynamicForm = new LocatableDynamicForm(extendLocatorId("resOpForm"));
        dynamicForm.setNumCols(3);

        operationArgumentsCanvasItem = new LocatableHLayout(extendLocatorId("opArgLayout"));
        operationArgumentsCanvasItem.setOverflow(Overflow.VISIBLE);
        operationArgumentsCanvasItem.setHeight(400);
        operationArgumentsCanvasItem.setWidth(500);

        operationSelectItem = new SelectItem("operationSelectItem", "Operation");
        operationSelectItem.setStartRow(true);
        operationSelectItem.setEndRow(true);
        operationSelectItem.setWrapTitle(false);
        operationSelectItem.setRedrawOnChange(true);
        operationSelectItem.setVisible(false);
        operationSelectItem.setDefaultToFirstOption(true);
        operationSelectItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                int operationId = Integer.valueOf(event.getValue().toString());
                selectOperation(operationId);
            }
        });

        // for SPECIFIC mode

        singleResourceTextItem = new StaticTextItem("singleResourceTextItem", "Resource");
        singleResourceTextItem.setStartRow(true);
        singleResourceTextItem.setEndRow(false);
        singleResourceTextItem.setValue("Pick a resource...");
        singleResourceTextItem.setShowIfCondition(new ShowIfModeFunction(ResourceSelectionMode.SPECIFIC));
        singleResourceTextItem.setAttribute(RESOURCE_ID_ATTRIBUTE, 0); // we hide the resource ID in this attribute
        singleResourceTextItem.setValidators(new ResourceIdValidator(singleResourceTextItem));

        ButtonItem singleResourceButtonItem = new ButtonItem("singleResourceButtonItem", "Pick");
        singleResourceButtonItem.setStartRow(false);
        singleResourceButtonItem.setEndRow(true);
        singleResourceButtonItem.setShowTitle(false);
        singleResourceButtonItem.setShowIfCondition(new ShowIfModeFunction(ResourceSelectionMode.SPECIFIC));
        singleResourceButtonItem.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                SingleResourcePicker singleResourcePicker;
                singleResourcePicker = new SingleResourcePicker(extendLocatorId("singleResourcePicker"),
                    new OkHandler() {
                        @Override
                        public boolean ok(HashSet<Integer> resourceIdSelection) {
                            final int resourceId = resourceIdSelection.iterator().next().intValue();
                            setSpecificResource(resourceId, null, null);
                            return true;
                        }
                    }, null);
                singleResourcePicker.show();
            }
        });

        // for RELATIVE mode

        ancestorTypeSelectItem = new SelectItem("ancestorTypeSelectItem", "Start Search From");
        ancestorTypeSelectItem.setStartRow(true);
        ancestorTypeSelectItem.setEndRow(true);
        ancestorTypeSelectItem.setWrapTitle(false);
        ancestorTypeSelectItem.setRedrawOnChange(true);
        ancestorTypeSelectItem.setDefaultToFirstOption(true);
        ancestorTypeSelectItem.setHoverWidth(200);
        ancestorTypeSelectItem
            .setTooltip("Select the top of the type hierarchy from which to search its descedant tree for the Filter By type");
        ancestorTypeSelectItem.setShowIfCondition(new ShowIfModeFunction(ResourceSelectionMode.RELATIVE));
        ancestorTypeSelectItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                populateRelativeDescendantsDropDownMenu(null, null, null);
            }
        });

        descendantTypeSelectItem = new SelectItem("descendantTypeSelectItem", "Then Filter By");
        descendantTypeSelectItem.setStartRow(true);
        descendantTypeSelectItem.setEndRow(false);
        descendantTypeSelectItem.setWrapTitle(false);
        descendantTypeSelectItem.setRedrawOnChange(true);
        descendantTypeSelectItem.setDefaultToFirstOption(true);
        descendantTypeSelectItem.setHoverWidth(200);
        descendantTypeSelectItem
            .setTooltip("The resource type to search for under the root type defined in the Start Search From selection.");
        descendantTypeSelectItem.setShowIfCondition(new ShowIfModeFunction(ResourceSelectionMode.RELATIVE));
        descendantTypeSelectItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                setOperationDropDownMenuValues(Integer.valueOf(event.getItem().getValue().toString()), null, null);
            }
        });

        descendantNameTextItem = new TextItem("descendantNameTextItem");
        descendantNameTextItem.setStartRow(false);
        descendantNameTextItem.setEndRow(true);
        descendantNameTextItem.setShowTitle(false);
        descendantNameTextItem.setRequired(false);
        descendantNameTextItem
            .setTooltip("A specific name to uniquely identify a resource when more than one resource of the selected type might exist. This is optional if there will only ever be one resource of the resource type in the selected type hierarchy.");
        descendantNameTextItem.setHoverWidth(200);
        descendantNameTextItem.setShowIfCondition(new ShowIfModeFunction(ResourceSelectionMode.RELATIVE));

        // the mode selector menu

        modeSelectItem = new SelectItem("modeSelectItem", "Resource Selection Mode");
        modeSelectItem.setStartRow(true);
        modeSelectItem.setEndRow(true);
        modeSelectItem.setWrapTitle(false);
        modeSelectItem.setRedrawOnChange(true);
        LinkedHashMap<String, String> modes = new LinkedHashMap<String, String>(3);
        modes.put(ResourceSelectionMode.SELF.name(), ResourceSelectionMode.SELF.getDisplayString());
        modes.put(ResourceSelectionMode.SPECIFIC.name(), ResourceSelectionMode.SPECIFIC.getDisplayString());
        modes.put(ResourceSelectionMode.RELATIVE.name(), ResourceSelectionMode.RELATIVE.getDisplayString());
        modeSelectItem.setValueMap(modes);
        modeSelectItem.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                ResourceSelectionMode mode = ResourceSelectionMode.valueOf(event.getValue().toString());
                switch (mode) {
                case SELF: {
                    setOperationDropDownMenuValues(resourceType.getId(), null, null);
                    break;
                }
                case SPECIFIC: {
                    singleResourceTextItem.setValue("Pick a resource...");
                    singleResourceTextItem.setAttribute(RESOURCE_ID_ATTRIBUTE, 0);
                    hideOperationDropDownMenu();
                    break;
                }
                case RELATIVE: {
                    ancestorTypeSelectItem.clearValue();
                    descendantTypeSelectItem.clearValue();
                    descendantNameTextItem.clearValue();
                    hideOperationDropDownMenu();
                    populateRelativeDropDownMenus(null, null, null, null);
                    break;
                }
                }
            }
        });

        dynamicForm.setFields(modeSelectItem, singleResourceTextItem, singleResourceButtonItem, ancestorTypeSelectItem,
            descendantTypeSelectItem, descendantNameTextItem, operationSelectItem);

        addMember(dynamicForm);
        addMember(operationArgumentsCanvasItem);

        // prepopulate the form
        ResourceOperationNotificationInfo notifInfo;
        notifInfo = ResourceOperationNotificationInfo.load(getConfiguration(), getExtraConfiguration());
        ResourceSelectionMode mode = notifInfo.getMode();
        if (mode != null) {
            modeSelectItem.setValue(mode.name());
            switch (mode) {
            case SELF: {
                setOperationDropDownMenuValues(resourceType.getId(), notifInfo.getOperationId(), notifInfo
                    .getOperationArguments());
                break;
            }
            case SPECIFIC: {
                setSpecificResource(notifInfo.getResourceId(), notifInfo.getOperationId(), notifInfo
                    .getOperationArguments());
                break;
            }
            case RELATIVE: {
                populateRelativeDropDownMenus(notifInfo.getAncestorTypeId(), notifInfo.getDescendantTypeId(), notifInfo
                    .getOperationId(), notifInfo.getOperationArguments());
                if (notifInfo.getDescendantName() != null) {
                    descendantNameTextItem.setValue(notifInfo.getDescendantName());
                }
                break;
            }
            }
        } else {
            modeSelectItem.setValue(ResourceSelectionMode.SELF.name());
            setOperationDropDownMenuValues(resourceType.getId(), null, null);
        }
    }

    private void populateRelativeDropDownMenus(final Integer selectedResourceTypeId,
        final Integer descendantResourceTypeId, final Integer selectedOpId, final Configuration opArgs) {

        if (ancestorTypeSelectItem.getValue() == null) {
            AsyncCallback<ArrayList<ResourceType>> callback = new AsyncCallback<ArrayList<ResourceType>>() {
                @Override
                public void onSuccess(ArrayList<ResourceType> results) {
                    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(results.size());
                    for (ResourceType rt : results) {
                        map.put(String.valueOf(rt.getId()), rt.getName());
                    }
                    ancestorTypeSelectItem.setValueMap(map);
                    if (selectedResourceTypeId != null) {
                        ancestorTypeSelectItem.setValue(selectedResourceTypeId.toString());
                    } else {
                        ancestorTypeSelectItem.setValue(String.valueOf(results.get(0).getId()));
                    }
                    populateRelativeDescendantsDropDownMenu(descendantResourceTypeId, selectedOpId, opArgs);
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Cannot get type ancestry", caught);
                }
            };

            if (this.theResource != null) {
                GWTServiceLookup.getResourceTypeGWTService().getResourceTypesForResourceAncestors(
                    this.theResource.getId(), callback);
            } else {
                GWTServiceLookup.getResourceTypeGWTService().getAllResourceTypeAncestors(this.resourceType.getId(),
                    callback);
            }
        } else {
            populateRelativeDescendantsDropDownMenu(descendantResourceTypeId, selectedOpId, opArgs);
        }
    }

    private void populateRelativeDescendantsDropDownMenu(final Integer selectedDescendantResourceTypeId,
        final Integer selectedOpId, final Configuration opArgs) {
        Object rootResourceTypeIdObj = ancestorTypeSelectItem.getValue();
        final int rootResourceTypeId;

        if (rootResourceTypeIdObj == null) {
            rootResourceTypeId = this.resourceType.getId();
        } else {
            rootResourceTypeId = Integer.parseInt(rootResourceTypeIdObj.toString());
        }
        GWTServiceLookup.getResourceTypeGWTService().getResourceTypeDescendantsWithOperations(rootResourceTypeId,
            new AsyncCallback<HashMap<Integer, String>>() {

                @Override
                public void onSuccess(HashMap<Integer, String> results) {
                    LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(results.size() + 1);
                    map.put(String.valueOf(rootResourceTypeId), "Root Ancestor Type");
                    for (Map.Entry<Integer, String> entry : results.entrySet()) {
                        map.put(entry.getKey().toString(), entry.getValue());
                    }
                    descendantTypeSelectItem.setValueMap(map);
                    if (selectedDescendantResourceTypeId != null) {
                        descendantTypeSelectItem.setValue(selectedDescendantResourceTypeId.toString());
                        setOperationDropDownMenuValues(selectedDescendantResourceTypeId.intValue(), selectedOpId,
                            opArgs);
                    } else {
                        descendantTypeSelectItem.setValue(String.valueOf(rootResourceTypeId));
                        setOperationDropDownMenuValues(rootResourceTypeId, selectedOpId, opArgs);
                    }

                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Cannot get type descendants", caught);
                }
            });
    }

    /**
     * This assumes the user or our code selected a specific operation from the drop down list.
     * This will show the operation arguments as appropriate for the selected operation.
     * 
     * @param operationId the selected operation; if 0 or less, selects the value of the drop down component
     */
    @SuppressWarnings("unchecked")
    private void selectOperation(int operationId) {
        // someone changed the operation selection.
        // there is a attribute on the select item that is a map<int,def> of all ops in the drop down list.
        // if there is a non-empty map (i.e. we know what ops are available) we need to show the arguments.
        // if there are no parameters needed for this operation, null out the extra config
        LinkedHashMap<Integer, OperationDefinition> ops = (LinkedHashMap<Integer, OperationDefinition>) operationSelectItem
            .getAttributeAsObject(OPERATION_DEFS_ATTRIBUTE);
        if (ops == null || ops.isEmpty()) {
            // why did this happen? there are no op defs so we should not be able to see any drop down menu anyway
            hideOperationDropDownMenu();
            return;
        }

        if (operationId <= 0) {
            operationId = Integer.valueOf(operationSelectItem.getValue().toString());
        }

        OperationDefinition def = ops.get(operationId);
        if (def != null) {
            ConfigurationDefinition paramDef = def.getParametersConfigurationDefinition();
            if (paramDef != null) {
                Configuration extraConfig = getExtraConfiguration();
                if (extraConfig == null) {
                    extraConfig = new Configuration();
                    setExtraConfiguration(extraConfig);
                } else {
                    cleanExtraConfiguration();
                }
                showOperationArguments(paramDef, extraConfig);
            } else {
                cleanExtraConfiguration();
                showOperationArguments(null, null);
            }
        } else {
            hideOperationDropDownMenu();
        }
    }

    /**
     * Sets up the operation drop down menu by looking up the resource type and gets its operation definitions.
     * If opId is non-null, the drop down menu will default to that operation.
     * If args is non-null (and if opId is non-null), this will pre-populate the argument config editor.
     * 
     * @param resourceTypeId the type whose operation definitions are to be shown in the operation drop down menu
     * @param selectedOpId if not-null, the selected operation
     * @param args if not-null (and opId is not null), this will prepopulate the argument config 
     */
    private void setOperationDropDownMenuValues(int resourceTypeId, final Integer selectedOpId, final Configuration args) {
        ResourceTypeCriteria criteria = new ResourceTypeCriteria();
        criteria.addFilterId(resourceTypeId);
        criteria.fetchOperationDefinitions(true);
        GWTServiceLookup.getResourceTypeGWTService().findResourceTypesByCriteria(criteria,
            new AsyncCallback<PageList<ResourceType>>() {

                @Override
                public void onSuccess(PageList<ResourceType> result) {
                    if (result == null || result.size() != 1) {
                        CoreGUI.getErrorHandler().handleError("Error getting operations: " + result);
                        hideOperationDropDownMenu();
                        return;
                    }
                    Set<OperationDefinition> set = result.get(0).getOperationDefinitions();
                    if (set != null && set.size() > 0) {
                        LinkedHashMap<Integer, OperationDefinition> opDefs;
                        LinkedHashMap<String, String> valueMap;
                        opDefs = new LinkedHashMap<Integer, OperationDefinition>(set.size());
                        valueMap = new LinkedHashMap<String, String>(set.size());
                        for (OperationDefinition def : set) {
                            opDefs.put(def.getId(), def);
                            valueMap.put(String.valueOf(def.getId()), def.getDisplayName());
                        }
                        operationSelectItem.setAttribute(OPERATION_DEFS_ATTRIBUTE, (Object) opDefs);
                        operationSelectItem.setValueMap(valueMap);
                        if (selectedOpId != null && selectedOpId > 0 && opDefs.containsKey(selectedOpId)) {
                            operationSelectItem.setValue(String.valueOf(selectedOpId));
                            showOperationArguments(opDefs.get(selectedOpId).getParametersConfigurationDefinition(),
                                args);
                        } else {
                            operationSelectItem.clearValue(); // sets it to the default
                            hideOperationArguments();
                            selectOperation(Integer.valueOf(operationSelectItem.getValue().toString()));
                        }
                        singleResourceTextItem.validate(); // makes sure a resource with ops is selected otherwise a validation error shows
                        operationSelectItem.show();
                        markForRedraw();
                    } else {
                        hideOperationDropDownMenu();
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Failed to load the list of available operations", caught);
                    hideOperationDropDownMenu();
                }
            });
    }

    /**
     * For the SPECIFIC mode, sets the selected resource and optionally the operation.
     * 
     * @param resourceId the resource selected
     * @param opId if not-null, the selected operation
     * @param args if not-null (and opId is not null), this will prepopulate the argument config 
     */
    private void setSpecificResource(final int resourceId, final Integer opId, final Configuration args) {
        singleResourceTextItem.setAttribute(RESOURCE_ID_ATTRIBUTE, resourceId);
        ResourceCriteria criteria = new ResourceCriteria();
        criteria.addFilterId(resourceId);
        criteria.fetchResourceType(true);
        GWTServiceLookup.getResourceService().findResourcesByCriteria(criteria,
            new AsyncCallback<PageList<Resource>>() {
                @Override
                public void onSuccess(PageList<Resource> result) {
                    if (result.size() > 0) {
                        Resource resource = result.get(0);
                        singleResourceTextItem.setValue(resource.getName());
                        setOperationDropDownMenuValues(resource.getResourceType().getId(), opId, args);
                    } else {
                        onFailure(new Exception("query returns no results"));
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("Cannot determine resource name", caught);
                    singleResourceTextItem.setValue(resourceId);
                    hideOperationDropDownMenu();
                }
            });
    }

    private void hideOperationDropDownMenu() {
        LinkedHashMap<Integer, OperationDefinition> ops = new LinkedHashMap<Integer, OperationDefinition>(0);
        LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>(0);
        operationSelectItem.setAttribute(OPERATION_DEFS_ATTRIBUTE, (Object) ops);
        operationSelectItem.setValueMap(valueMap);
        operationSelectItem.hide();
        hideOperationArguments();

        singleResourceTextItem.validate(); // makes sure a resource with ops is selected otherwise a validation error shows
    }

    private void hideOperationArguments() {
        Canvas[] canvii = operationArgumentsCanvasItem.getMembers();
        if (canvii != null) {
            for (Canvas canvas : canvii) {
                canvas.destroy();
            }
        }
        markForRedraw();
    }

    private void showOperationArguments(ConfigurationDefinition def, Configuration config) {
        Canvas[] canvii = operationArgumentsCanvasItem.getMembers();
        if (canvii != null) {
            for (Canvas canvas : canvii) {
                canvas.destroy();
            }
        }

        if (def != null) {
            ConfigurationEditor configEditor = new ConfigurationEditor(extendLocatorId("opArgs"), def, config);
            operationArgumentsCanvasItem.addMember(configEditor);
        } else {
            Label l = new Label("This operation does not take any parameters");
            l.setWrap(false);
            operationArgumentsCanvasItem.addMember(l);
        }

        markForRedraw();
    }

    private ConfigurationEditor getConfigurationEditor() {
        Canvas[] canvii = operationArgumentsCanvasItem.getMembers();
        if (canvii != null) {
            for (Canvas canvas : canvii) {
                if (canvas instanceof ConfigurationEditor) {
                    return (ConfigurationEditor) canvas;
                }
            }
        }
        return null;
    }

    @Override
    public boolean validate() {
        try {
            if (dynamicForm.validate(false)) {
                // let's make sure the args can be validated successfully.
                // If there is no config editor, there are no parameters for this operation
                ConfigurationEditor configEditor = getConfigurationEditor();
                if (configEditor != null) {
                    if (!configEditor.validate()) {
                        return false;
                    }
                    // nothing else to store - our config editor directly edited our extraConfig already
                } else {
                    setExtraConfiguration(null);
                }

                // now fill in the configuration object with the information based on what was selected
                String selectedModeString = modeSelectItem.getValue().toString();
                ResourceSelectionMode selectedMode = ResourceSelectionMode.valueOf(selectedModeString);
                cleanConfiguration(); // erase anything previously in here
                Configuration config = getConfiguration();
                config.put(new PropertySimple(ResourceOperationNotificationInfo.Constants.SELECTION_MODE
                    .getPropertyName(), selectedMode));
                switch (selectedMode) {
                case SELF: {
                    // nothing extra needs to be done
                    break;
                }
                case SPECIFIC: {
                    int resourceId = singleResourceTextItem.getAttributeAsInt(RESOURCE_ID_ATTRIBUTE);
                    config.put(new PropertySimple(ResourceOperationNotificationInfo.Constants.SPECIFIC_RESOURCE_ID
                        .getPropertyName(), resourceId));
                    break;
                }
                case RELATIVE: {
                    config.put(new PropertySimple(ResourceOperationNotificationInfo.Constants.RELATIVE_ANCESTOR_TYPE_ID
                        .getPropertyName(), ancestorTypeSelectItem.getValue()));
                    config.put(new PropertySimple(
                        ResourceOperationNotificationInfo.Constants.RELATIVE_DESCENDANT_TYPE_ID.getPropertyName(),
                        descendantTypeSelectItem.getValue()));
                    // descendant name is optional - only populate a non-null property
                    Object descandentNameString = descendantNameTextItem.getValue();
                    if (descandentNameString != null && descandentNameString.toString().trim().length() > 0) {
                        config.put(new PropertySimple(
                            ResourceOperationNotificationInfo.Constants.RELATIVE_DESCENDANT_NAME.getPropertyName(),
                            descandentNameString));
                    }
                    break;
                }
                }

                // indicate which operation is to be invoked by storing the op ID in the config
                String operationId = operationSelectItem.getValue().toString();
                config.put(new PropertySimple(ResourceOperationNotificationInfo.Constants.OPERATION_ID
                    .getPropertyName(), operationId));

                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            CoreGUI.getErrorHandler().handleError("Cannot save the notification configuration", e);
            return false;
        }
    }

    private class ShowIfModeFunction implements FormItemIfFunction {
        private final ResourceSelectionMode mode;

        public ShowIfModeFunction(ResourceSelectionMode mode) {
            this.mode = mode;
        }

        public boolean execute(FormItem item, Object value, DynamicForm form) {
            String modeTypeString = form.getValue("modeSelectItem").toString();
            return mode.name().equals(modeTypeString);
        }
    }

    private class ResourceIdValidator extends CustomValidator {
        private final FormItem idItem;

        public ResourceIdValidator(FormItem idItem) {
            this.idItem = idItem;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected boolean condition(Object value) {
            Integer id = Integer.valueOf(idItem.getAttributeAsInt(RESOURCE_ID_ATTRIBUTE));
            boolean valid = (id != null && id.intValue() != 0);
            if (!valid) {
                setErrorMessage("Please pick a resource");
            } else {
                LinkedHashMap<Integer, OperationDefinition> ops = (LinkedHashMap<Integer, OperationDefinition>) operationSelectItem
                    .getAttributeAsObject(OPERATION_DEFS_ATTRIBUTE);
                if (ops == null || ops.isEmpty()) {
                    valid = false;
                    setErrorMessage("Please pick a resource that has one or more operations");
                }
            }
            return valid;
        }
    }
}
