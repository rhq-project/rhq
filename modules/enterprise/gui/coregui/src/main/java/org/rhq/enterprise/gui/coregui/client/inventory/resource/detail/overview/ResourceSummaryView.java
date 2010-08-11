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
package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.overview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.DynamicForm;
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
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.ResourceSelectListener;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.type.ResourceTypeRepository;

/**
 * @author Greg Hinkle
 */
public class ResourceSummaryView extends DynamicForm implements ResourceSelectListener {

    private Resource resource;

    @Override
    protected void onDraw() {
        super.onDraw();

        setNumCols(4);
        setWrapItemTitles(false);
        setLeft("10%");
        setWidth("80%");
    }


    public void onResourceSelected(Resource resource) {

        this.resource = resource;

        ResourceTypeRepository.Cache.getInstance().getResourceTypes(resource.getResourceType().getId(),
                EnumSet.of(ResourceTypeRepository.MetadataType.measurements),
                new ResourceTypeRepository.TypeLoadedCallback() {
                    public void onTypesLoaded(ResourceType type) {
                        try {
                            buildForm(type);
                            loadValues();
                        } catch (Exception e) {
                            SC.say("Form load failure");
                            e.printStackTrace();
                        }
                    }
                });


        markForRedraw();
    }

    private void loadValues() {
        GWTServiceLookup.getMeasurementDataService().findCurrentTraitsForResource(
                resource.getId(),
                DisplayType.SUMMARY,
                new AsyncCallback<List<MeasurementDataTrait>>() {
                    public void onFailure(Throwable caught) {
                        SC.say("Failed to load traits");
                        CoreGUI.getErrorHandler().handleError("Failed to load traits information for resource",caught);
                    }

                    public void onSuccess(List<MeasurementDataTrait> result) {
                        // TODO: Implement this method.
                        for (MeasurementDataTrait trait : result) {
                            String formId = trait.getName().replaceAll("\\.", "_").replaceAll(" ", "__");
                            FormItem item = getItem(formId);

                            if (item != null) {
                                setValue(formId, trait.getValue());
                            }
                        }
                        markForRedraw();
                    }
                }
        );

    }

    private void buildForm(ResourceType type) {
        ArrayList<MeasurementDefinition> traits = new ArrayList<MeasurementDefinition>();

        for (MeasurementDefinition measurement : type.getMetricDefinitions()) {
            if (measurement.getDataType() == DataType.TRAIT && measurement.getDisplayType() == DisplayType.SUMMARY) {
                traits.add(measurement);
            }
        }

        Collections.sort(traits, new Comparator<MeasurementDefinition>() {
            public int compare(MeasurementDefinition o1, MeasurementDefinition o2) {
                return new Integer(o1.getDisplayOrder()).compareTo(o2.getDisplayOrder());
            }
        });


        ArrayList<FormItem> formItems = new ArrayList<FormItem>();
        ArrayList<String> itemIds = new ArrayList<String>();

        HeaderItem headerItem = new HeaderItem("header", "Summary");
        headerItem.setValue("Summary");
        formItems.add(headerItem);

        StaticTextItem typeItem = new StaticTextItem("typeItem", "Type");
        typeItem.setTooltip("Plugin: " + type.getPlugin() + "\n<br>" + "Type: " + type.getName());
        typeItem.setValue(type.getName() + " (" + type.getPlugin() + ")");
        formItems.add(typeItem);
        itemIds.add(typeItem.getName());

        StaticTextItem descriptionItem = new StaticTextItem("descriptionItem", "Description");
        descriptionItem.setValue(resource.getDescription());
        formItems.add(descriptionItem);
        itemIds.add(descriptionItem.getName());

        StaticTextItem versionItem = new StaticTextItem("versionItem", "Version");
        formItems.add(versionItem);
        itemIds.add(versionItem.getName());


        StaticTextItem parentItem = new StaticTextItem("parentItem", "Parent");
        formItems.add(parentItem);
        itemIds.add(parentItem.getName());


        for (MeasurementDefinition trait : traits) {

            String id = trait.getDisplayName().replaceAll("\\.", "_").replaceAll(" ", "__");
            itemIds.add(id);

            StaticTextItem item = new StaticTextItem(id, trait.getDisplayName());
            item.setTooltip(trait.getDescription());
            formItems.add(item);
//            item.setValue("?");
        }

//        SectionItem section = new SectionItem("Summary", "Summary");
//        section.setTitle("Summary");
//        section.setDefaultValue("Summary");
//        section.setCanCollapse(true);
//        section.setCellStyle("HidablePlainSectionHeader");
//        section.setItemIds(itemIds.toArray(new String[itemIds.size()]));
//        formItems.add(0, section);

        formItems.add(new SpacerItem());
        setItems(formItems.toArray(new FormItem[formItems.size()]));


        setValue("typeItem", type.getName() + " (" + type.getPlugin() + ")");
        setValue("descriptionItem", resource.getDescription());
        setValue("versionItem", resource.getVersion());
        setValue("parentItem", resource.getParentResource() == null ? null :
                ("<a href=\"#Resource/" + resource.getParentResource().getId() + "\">" +
                        resource.getParentResource().getName() + "</a>"));


    }
}
