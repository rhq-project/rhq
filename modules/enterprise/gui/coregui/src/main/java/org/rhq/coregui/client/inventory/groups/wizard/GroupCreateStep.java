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
package org.rhq.coregui.client.inventory.groups.wizard;

import java.util.EnumSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.ValuesManager;
import com.smartgwt.client.widgets.form.fields.AutoFitTextAreaItem;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;

import org.rhq.core.domain.criteria.ResourceGroupCriteria;
import org.rhq.core.domain.resource.group.ResourceGroup;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class GroupCreateStep extends AbstractWizardStep {

    private DynamicForm form;
    private ResourceGroupGWTServiceAsync groupService;
    boolean canContinue = false;

    public GroupCreateStep() {

        groupService = GWTServiceLookup.getResourceGroupService();
    }

    public Canvas getCanvas() {

        if (form == null) {
            form = new DynamicForm();
            form.setValuesManager(new ValuesManager());
            form.setWidth100();
            form.setNumCols(2);

            TextItem name = new TextItem("name", MSG.common_title_name());
            name.setRequired(true);
            name.setWidth(300);
            name.setLength(100);
            name.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent changedEvent) {

                    final String newGroupName = form.getValueAsString("name");

                    ResourceGroupCriteria criteria = new ResourceGroupCriteria();
                    criteria.addFilterName(newGroupName);
                    criteria.addFilterVisible(true);
                    criteria.setRestriction(ResourceGroupCriteria.Restriction.COLLECTION_ONLY);

                    groupService.findResourceGroupsByCriteria(criteria, new AsyncCallback<PageList<ResourceGroup>>() {
                        @Override
                        public void onFailure(Throwable throwable) {
                            // failure is no issue - we will catch possible upd later
                        }

                        @Override
                        public void onSuccess(PageList<ResourceGroup> resourceGroups) {

                            // criteria query may return more than we want, so search for an exact match
                            boolean found = false;
                            for (ResourceGroup group : resourceGroups) {
                                if (group.getName().equals(newGroupName))
                                    found = true;
                            }

                            if (found) {
                                canContinue = false;
                                Message msg = new Message(MSG
                                    .view_groupCreateWizard_createStep_group_exists(newGroupName),
                                    Message.Severity.Warning, EnumSet.of(Message.Option.Transient));
                                CoreGUI.getMessageCenter().notify(msg);

                            } else {
                                canContinue = true;
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_selector_available(MSG.common_title_name()),
                                        Message.Severity.Blank, EnumSet.of(Message.Option.Transient)));
                            }
                        }
                    });
                }
            });

            TextAreaItem description = new AutoFitTextAreaItem("description", MSG.common_title_description());
            description.setWidth(300);
            description.setLength(100);

            CheckboxItem recursive = new CheckboxItem("recursive", MSG.view_groupCreateWizard_createStep_recursive());

            form.setFields(name, description, recursive);
        }

        return form;
    }

    public boolean nextPage() {
        return form.validate() && canContinue;
    }

    public String getName() {
        return MSG.view_groupCreateWizard_createStepName();
    }

    public ResourceGroup getGroup() {
        ResourceGroup group = new ResourceGroup(form.getValueAsString("name"));
        group.setDescription(form.getValueAsString("description"));
        group.setRecursive(form.getValue("recursive") != null ? true : false);

        return group;
    }
}
