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
package org.rhq.coregui.client.inventory.resource.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDataTrait;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.resource.composite.ResourceComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.form.EditableFormItem;
import org.rhq.coregui.client.components.form.EnhancedDynamicForm;
import org.rhq.coregui.client.components.form.SimpleEditableFormItem.ValueEditedHandler;
import org.rhq.coregui.client.components.form.StringLengthValidator;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGWTServiceAsync;
import org.rhq.coregui.client.inventory.resource.AncestryUtil;
import org.rhq.coregui.client.inventory.resource.ResourceDataSourceField;
import org.rhq.coregui.client.inventory.resource.type.ResourceTypeRepository;
import org.rhq.coregui.client.util.message.Message;

/**
 * The Resource Summary>Overview tab - Resource general properties + summary traits.
 *
 * @author Greg Hinkle
 * @author Ian Springer
 */
public class OverviewForm extends EnhancedDynamicForm {

    private ResourceGWTServiceAsync resourceService = GWTServiceLookup.getResourceService();
    private ResourceComposite resourceComposite;
    private boolean headerEnabled = true;
    private boolean displayCondensed = false;
    private final ResourceTitleBar titleBar;

    public OverviewForm(ResourceComposite resourceComposite, ResourceTitleBar titleBar) {
        super();
        this.resourceComposite = resourceComposite;
        this.titleBar = titleBar;
    }

    @Override
    protected void onDraw() {
        super.onDraw();

        setLeft("10%");
        setWidth("80%");
        if (isDisplayCondensed()) {
            //setPadding(5);
            setColWidths(150, 220, 150, 220);
            setWidth("45%");
            this.setAlign(Alignment.LEFT);
        }

        if (this.resourceComposite != null) {
            setResource(this.resourceComposite);
        }
    }

    public void setResource(ResourceComposite resourceComposite) {

        this.resourceComposite = resourceComposite;
        Resource resource = resourceComposite.getResource();

        // Load metric defs.
        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
            EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
            new ResourceTypeRepository.TypeLoadedCallback() {
                public void onTypesLoaded(ResourceType type) {
                    buildForm(type);
                    loadTraitValues();
                }
            });
    }

    private void loadTraitValues() {
        final Resource resource = resourceComposite.getResource();
        GWTServiceLookup.getMeasurementDataService().findCurrentTraitsForResource(resource.getId(),
            DisplayType.SUMMARY, new AsyncCallback<List<MeasurementDataTrait>>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError(
                        MSG.view_summaryOverviewForm_error_traitsLoadFailure(resource.toString()), caught);
                }

                public void onSuccess(List<MeasurementDataTrait> result) {
                    for (MeasurementDataTrait trait : result) {
                        String formItemId = buildFormItemIdFromTraitDisplayName(trait.getName());
                        FormItem item = getItem(formItemId);
                        if (item != null) {
                            setValue(formItemId, trait.getValue());
                        }
                    }
                    markForRedraw();
                }
            });
    }

    private static String buildFormItemIdFromTraitDisplayName(String traitName) {
        return traitName.replaceAll("\\.", "_").replaceAll(" ", "__");
    }

    private void buildForm(ResourceType type) {
        StringLengthValidator notEmptyOrNullValidator = new StringLengthValidator(1, null, false);
        //StringLengthValidator notNullValidator = new StringLengthValidator(null, null, false);

        List<FormItem> formItems = new ArrayList<FormItem>();

        if (isHeaderEnabled()) {//conditionally display header
            HeaderItem headerItem = new HeaderItem("header", MSG.view_summaryOverviewForm_header_summary());
            headerItem.setValue(MSG.view_summaryOverviewForm_header_summary());
            formItems.add(headerItem);
        }

        // Type
        StaticTextItem typeItem = new StaticTextItem("type", MSG.view_summaryOverviewForm_field_type());
        typeItem.setTooltip(MSG.common_title_plugin() + ": " + type.getPlugin() + "\n<br>" + MSG.common_title_type()
            + ": " + type.getName());
        typeItem.setValue(AncestryUtil.getFormattedType(type));
        formItems.add(typeItem);

        final Resource resource = this.resourceComposite.getResource();

        // Key
        StaticTextItem keyItem = new StaticTextItem(ResourceDataSourceField.KEY.propertyName(),
            ResourceDataSourceField.KEY.title());
        keyItem.setAttribute(OUTPUT_AS_HTML_ATTRIBUTE, true);
        keyItem.setValue(resource.getResourceKey());
        formItems.add(keyItem);

        boolean modifiable = this.resourceComposite.getResourcePermission().isInventory();

        // Name
        final FormItem nameItem = (modifiable) ? new EditableFormItem() : new StaticTextItem();
        nameItem.setName("name");

        nameItem.setTitle(MSG.view_summaryOverviewForm_field_name());
        nameItem.setValue(resource.getName());
        nameItem.setAttribute(OUTPUT_AS_HTML_ATTRIBUTE, true);
        if (nameItem instanceof EditableFormItem) {
            EditableFormItem togglableNameItem = (EditableFormItem) nameItem;
            togglableNameItem.setValidators(notEmptyOrNullValidator);
            togglableNameItem.setValueEditedHandler(new ValueEditedHandler() {
                public void editedValue(Object newValue) {
                    final String newName = newValue.toString();
                    final String oldName = resource.getName();
                    if (newName.equals(oldName)) {
                        return;
                    }
                    resource.setName(newName);
                    OverviewForm.this.resourceService.updateResource(resource, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_summaryOverviewForm_error_nameChangeFailure(String.valueOf(resource.getId()),
                                    oldName, newName), caught);
                            // We failed to update it on the Server, so change back the Resource and the form item to
                            // the original value.
                            resource.setName(oldName);
                            nameItem.setValue(oldName);
                        }

                        public void onSuccess(Void result) {
                            titleBar.displayResourceName(newName);
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_summaryOverviewForm_message_nameChangeSuccess(
                                    String.valueOf(resource.getId()), oldName, newName), Message.Severity.Info));
                        }
                    });
                }
            });
        }
        formItems.add(nameItem);

        // Description
        final FormItem descriptionItem = (modifiable) ? new EditableFormItem() : new StaticTextItem();
        descriptionItem.setName("description");
        descriptionItem.setTitle(MSG.common_title_description());
        descriptionItem.setValue(resource.getDescription());
        descriptionItem.setAttribute(OUTPUT_AS_HTML_ATTRIBUTE, true);
        if (descriptionItem instanceof EditableFormItem) {
            EditableFormItem togglableDescriptionItem = (EditableFormItem) descriptionItem;
            togglableDescriptionItem.setValueEditedHandler(new ValueEditedHandler() {
                public void editedValue(Object newValue) {
                    final String newDescription = newValue != null ? newValue.toString() : "";
                    final String oldDescription = resource.getDescription();
                    if (newDescription.equals(oldDescription)) {
                        return;
                    }
                    resource.setDescription(newDescription);
                    OverviewForm.this.resourceService.updateResource(resource, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_summaryOverviewForm_error_descriptionChangeFailure(
                                    String.valueOf(resource.getId()), oldDescription, newDescription), caught);
                            // We failed to update it on the Server, so change back the Resource and the form item to
                            // the original value.
                            resource.setDescription(oldDescription);
                            descriptionItem.setValue(oldDescription);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_summaryOverviewForm_message_nameChangeSuccess(
                                    String.valueOf(resource.getId()), oldDescription, newDescription),
                                    Message.Severity.Info));
                        }
                    });
                }
            });
        }
        formItems.add(descriptionItem);

        // Location
        final FormItem locationItem = (modifiable) ? new EditableFormItem() : new StaticTextItem();
        locationItem.setName("location");
        locationItem.setTitle(MSG.view_summaryOverviewForm_field_location());
        locationItem.setValue(resource.getLocation());
        locationItem.setAttribute(OUTPUT_AS_HTML_ATTRIBUTE, true);
        if (locationItem instanceof EditableFormItem) {
            EditableFormItem togglableLocationItem = (EditableFormItem) locationItem;
            togglableLocationItem.setValueEditedHandler(new ValueEditedHandler() {
                public void editedValue(Object newValue) {
                    final String newLocation = newValue != null ? newValue.toString() : "";
                    final String oldLocation = resource.getLocation();
                    if (newLocation.equals(oldLocation)) {
                        return;
                    }
                    resource.setLocation(newLocation);
                    OverviewForm.this.resourceService.updateResource(resource, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_summaryOverviewForm_error_locationChangeFailure(
                                    String.valueOf(resource.getId()), oldLocation, newLocation), caught);
                            // We failed to update it on the Server, so change back the Resource and the form item to
                            // the original value.
                            resource.setLocation(oldLocation);
                            locationItem.setValue(oldLocation);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter()
                                .notify(
                                    new Message(MSG.view_summaryOverviewForm_message_nameChangeSuccess(
                                        String.valueOf(resource.getId()), oldLocation, newLocation),
                                        Message.Severity.Info));
                        }
                    });
                }
            });
        }
        formItems.add(locationItem);

        // Version
        StaticTextItem versionItem = new StaticTextItem("version", MSG.view_summaryOverviewForm_field_version());
        String version = (resource.getVersion() != null) ? resource.getVersion() : "<i>" + MSG.common_label_none()
            + "</i>";
        versionItem.setValue(version);
        formItems.add(versionItem);

        // Timestamps
        StaticTextItem ctimeItem = new StaticTextItem(ResourceDataSourceField.CTIME.propertyName(),
            ResourceDataSourceField.CTIME.title());
        String ctime = TimestampCellFormatter.format(resource.getCtime());
        ctimeItem.setValue(ctime);
        formItems.add(ctimeItem);

        StaticTextItem itimeItem = new StaticTextItem(ResourceDataSourceField.ITIME.propertyName(),
            ResourceDataSourceField.ITIME.title());
        String itime = TimestampCellFormatter.format(resource.getItime());
        itimeItem.setValue(itime);
        formItems.add(itimeItem);

        StaticTextItem mtimeItem = new StaticTextItem(ResourceDataSourceField.MTIME.propertyName(),
            ResourceDataSourceField.MTIME.title());
        String mtime = TimestampCellFormatter.format(resource.getMtime());
        mtimeItem.setValue(mtime);
        formItems.add(mtimeItem);

        StaticTextItem modifierItem = new StaticTextItem(ResourceDataSourceField.MODIFIER.propertyName(),
            ResourceDataSourceField.MODIFIER.title());
        modifierItem.setValue(resource.getModifiedBy());
        formItems.add(modifierItem);

        // Traits
        List<MeasurementDefinition> summaryTraitDefs = new ArrayList<MeasurementDefinition>();
        for (MeasurementDefinition measurementDef : type.getMetricDefinitions()) {
            if (measurementDef.getDataType() == DataType.TRAIT
                && measurementDef.getDisplayType() == DisplayType.SUMMARY) {
                summaryTraitDefs.add(measurementDef);
            }
        }

        Collections.sort(summaryTraitDefs, new Comparator<MeasurementDefinition>() {
            public int compare(MeasurementDefinition md1, MeasurementDefinition md2) {
                return Integer.valueOf(md1.getDisplayOrder()).compareTo(md2.getDisplayOrder());
            }
        });

        for (MeasurementDefinition trait : summaryTraitDefs) {
            String formItemId = buildFormItemIdFromTraitDisplayName(trait.getDisplayName());
            StaticTextItem formItem = new StaticTextItem(formItemId, trait.getDisplayName());
            formItem.setTooltip(trait.getDescription());
            formItems.add(formItem);
            //            item.setValue("?");
        }

        //        SectionItem section = new SectionItem("Summary", "Summary");
        //        section.setTitle("Summary");
        //        section.setDefaultValue("Summary");
        //        section.setCanCollapse(true);
        //        section.setCellStyle("HidablePlainSectionHeader");
        //        section.setItemIds(itemIds.toArray(new String[itemIds.size()]));
        //        formItems.add(0, section);
        if (!isDisplayCondensed()) {
            formItems.add(new SpacerItem());
        }

        setItems(formItems.toArray(new FormItem[formItems.size()]));
        this.setTitleAlign(Alignment.LEFT);
        this.setTitleOrientation(TitleOrientation.LEFT);
        this.setWrapItemTitles(false);
    }

    public void loadData() {
        // TODO: Reload the ResourceComposite too.
        loadTraitValues();
        markForRedraw();
    }

    public boolean isHeaderEnabled() {
        return headerEnabled;
    }

    public void setHeaderEnabled(boolean headerEnabled) {
        this.headerEnabled = headerEnabled;
    }

    public boolean isDisplayCondensed() {
        return displayCondensed;
    }

    public void setDisplayCondensed(boolean displayCondensed) {
        this.displayCondensed = displayCondensed;
    }

}
