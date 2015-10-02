/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
package org.rhq.coregui.client.bundle.create;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.HeaderItem;
import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.bundle.composite.BundleGroupAssignmentComposite;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.bundle.group.BundleGroupSelector;
import org.rhq.coregui.client.bundle.group.BundleGroupsDataSource;
import org.rhq.coregui.client.components.wizard.AbstractWizardStep;
import org.rhq.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.FormUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Jay Shaughnessy
 */
public class BundleGroupsStep extends AbstractWizardStep {

    // BZ 1252142: We don't want the usual 30s timeout for GWT service calls when creating the initial bundle
    // version. We can't advance the wizard until the bundle version creation is complete and for large
    // distribution files it can take some time to move the uploaded [tmp] file from disk into the DB. So, use
    // a liberal timeout here...
    private static final int BUNDLE_CREATE_INITIAL_VERSION_TIMEOUT = 10 * 60 * 1000;

    private EnhancedVLayout canvas;
    private DynamicForm radioForm;
    private AbstractBundleCreateWizard wizard = null;
    private boolean isInitialVersion;
    private BundleGroupSelector selector;

    public BundleGroupsStep(AbstractBundleCreateWizard wizard) {
        this.wizard = wizard;
    }

    public Canvas getCanvas() {
        canvas = new EnhancedVLayout();
        canvas.setWidth100();

        // go get the assignable/assigned bundle groups for this new bundle version, initial or not
        this.isInitialVersion = this.wizard.getBundleVersion() == null
            || this.wizard.getBundleVersion().getVersionOrder() == 0;

        if (isInitialVersion) {
            prepareInitialVersionCanvas(canvas);
        } else {
            prepareNonInitialVersionCanvas(canvas);
        }

        return canvas;
    }

    private void prepareInitialVersionCanvas(final EnhancedVLayout canvas) {
        BundleGroupAssignmentComposite composite = wizard.getBundleGroupAssignmentComposite();
        final Map<BundleGroup, Boolean> map = composite.getBundleGroupMap();

        if (composite.isCanBeUnassigned()) {
            radioForm = new DynamicForm();
            radioForm.setNumCols(1);
            radioForm.setColWidths(350);

            final RadioGroupItem radioGroupItem = new RadioGroupItem("RadioOptions");
            radioGroupItem.setTitleOrientation(TitleOrientation.TOP);
            radioGroupItem.setTitle(MSG.view_bundle_createWizard_groupsStep_radioTitle());
            radioGroupItem.setRequired(true);
            radioGroupItem.setAlign(Alignment.LEFT);

            LinkedHashMap<String, String> valueMap = new LinkedHashMap<String, String>();
            valueMap.put("unassigned", MSG.view_bundle_createWizard_groupsStep_leaveUnassigned());
            valueMap.put("assign", MSG.view_bundle_createWizard_groupsStep_assign());
            radioGroupItem.setValueMap(valueMap);
            radioGroupItem.setValue(map.isEmpty() ? "unassigned" : "assign");

            radioGroupItem.addChangedHandler(new ChangedHandler() {
                public void onChanged(ChangedEvent event) {
                    if (map.isEmpty()) {
                        radioGroupItem.setValue("unassigned");

                    } else {
                        selector.setDisabled("unassigned".equals(event.getValue()));
                    }

                    canvas.markForRedraw();
                }
            });
            FormUtility.addContextualHelp(radioGroupItem, MSG.view_bundle_createWizard_groupsStep_help());

            radioForm.setItems(radioGroupItem);
            canvas.addMember(radioForm);

            selector = getSelector(map, false);
            selector.setDisabled(map.isEmpty());
            canvas.addMember(selector);

        } else if (!map.isEmpty()) {
            DynamicForm form = new DynamicForm();
            form.setWidth100();
            HeaderItem selectorTitleItem = new HeaderItem();
            selectorTitleItem.setWidth(375);
            selectorTitleItem.setValue(MSG.view_bundle_createWizard_groupsStep_assign());
            FormUtility.addContextualHelp(selectorTitleItem, MSG.view_bundle_createWizard_groupsStep_help());
            form.setItems(selectorTitleItem);
            canvas.addMember(form);

            selector = getSelector(map, false);
            canvas.addMember(selector);

        } else {
            // can't call wizard show message, the canvas is not yet created, so make the canvas the message
            DynamicForm form = new DynamicForm();
            form.setWidth100();
            HeaderItem errorHeaderItem = new HeaderItem();
            errorHeaderItem.setAttribute("wrap", true); // forum tip, only way I could get the text to wrap
            errorHeaderItem.setWidth(650);
            errorHeaderItem.setValue(MSG.view_bundle_createWizard_groupsStep_noAssignable());
            FormUtility.addContextualHelp(errorHeaderItem, MSG.view_bundle_createWizard_groupsStep_help());
            form.setItems(errorHeaderItem);

            canvas.addMember(form);

            CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_groupsStep_noAssignable());
        }
    }

    private void prepareNonInitialVersionCanvas(EnhancedVLayout canvas) {
        BundleGroupAssignmentComposite composite = wizard.getBundleGroupAssignmentComposite();
        final Map<BundleGroup, Boolean> map = composite.getBundleGroupMap();
        selector = getSelector(map, true);

        if (!selector.hasInitialSelection()) {
            selector.destroy();

            DynamicForm form = new DynamicForm();
            form.setWidth100();
            HeaderItem unassignedHeaderItem = new HeaderItem();
            unassignedHeaderItem.setAttribute("wrap", true); // forum tip, only way I could get the text to wrap
            unassignedHeaderItem.setWidth(650);
            unassignedHeaderItem.setValue(MSG.view_bundle_createWizard_groupsStep_unassigned());
            FormUtility.addContextualHelp(unassignedHeaderItem, MSG.view_bundle_createWizard_groupsStep_help());
            form.setItems(unassignedHeaderItem);

            canvas.addMember(form);

        } else {
            DynamicForm form = new DynamicForm();
            form.setWidth100();
            HeaderItem selectorTitleItem = new HeaderItem();
            selectorTitleItem.setWidth(650);
            selectorTitleItem.setValue(MSG.view_bundle_createWizard_groupsStep_assigned());
            FormUtility.addContextualHelp(selectorTitleItem, MSG.view_bundle_createWizard_groupsStep_help());
            form.setItems(selectorTitleItem);

            canvas.addMember(form);
            canvas.addMember(selector);
        }
    }

    private BundleGroupSelector getSelector(final Map<BundleGroup, Boolean> map, boolean readOnly) {
        BundleGroupSelector result;

        if (map.isEmpty()) {
            result = new BundleGroupSelector(readOnly);

        } else {
            Set<BundleGroup> bundleGroups = map.keySet();
            Integer[] idsFilter = new Integer[bundleGroups.size()];
            int i = 0;
            for (BundleGroup bundleGroup : bundleGroups) {
                idsFilter[i++] = bundleGroup.getId();
            }

            List<ListGridRecord> initiallyAssigned = new ArrayList<ListGridRecord>(bundleGroups.size());
            BundleGroupsDataSource ds = new BundleGroupsDataSource();
            for (BundleGroup bundleGroup : bundleGroups) {
                if (map.get(bundleGroup) == Boolean.TRUE) {
                    initiallyAssigned.add(ds.copyValues(bundleGroup));
                }
            }
            result = new BundleGroupSelector(idsFilter, initiallyAssigned.toArray(new ListGridRecord[initiallyAssigned
                .size()]), readOnly);
        }

        return result;
    }

    public Set<BundleGroup> getSelectedBundleGroups() {
        return selector.getSelectedItems();
    }

    public boolean nextPage() {
        wizard.getView().hideMessage();

        if (isInitialVersion) {

            if (selector.isDisabled()) {
                // if the user chooses to leave unassigned and the bundle version has already been created, we're done
                if (null != wizard.getBundleVersion()) {
                    return true;
                }
            } else {
                // make sure at least one group is selected if the selector is active
                if (selector.getSelectedItems().isEmpty()) {
                    wizard.getView().showMessage(MSG.view_bundle_createWizard_groupsStep_noneAssigned());
                    return false;
                }
            }
        }

        if (null == wizard.getBundleVersion()) {
            if (null != wizard.getCreateInitialBundleVersionRecipe()) {
                processRecipe();
            } else if (null != wizard.getCreateInitialBundleVersionToken()) {
                processToken();
            } else {
                Exception e = new IllegalStateException(
                    "Unexpected error: can't create initial version, no recipe or token");
                wizard.getView().showMessage(e.getMessage());
                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_createFailure(), e);
                wizard.setBundleVersion(null);
                wizard.setCreateInitialBundleVersionRecipe("");
            }
        } else {
            processAssignment();
        }

        return false;
    }

    private int[] getInitialBundleIds() {
        if (selector.isDisabled()) {
            return null;
        }

        Set<BundleGroup> bundleGroups = selector.getSelectedItems();

        wizard.setInitialBundleGroups(bundleGroups);

        int[] result = new int[bundleGroups.size()];
        int i = 0;
        for (BundleGroup bundleGroup : bundleGroups) {
            result[i++] = bundleGroup.getId();
        }

        return result;
    }

    private void processRecipe() {

        BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService(BUNDLE_CREATE_INITIAL_VERSION_TIMEOUT);
        bundleServer.createInitialBundleVersionViaRecipe(getInitialBundleIds(),
            this.wizard.getCreateInitialBundleVersionRecipe(), new AsyncCallback<BundleVersion>() {
                @Override
                public void onSuccess(BundleVersion result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(
                            MSG.view_bundle_createWizard_createSuccessful(result.getName(), result.getVersion()),
                            Message.Severity.Info));
                    wizard.setBundleVersion(result);
                    wizard.getView().incrementStep(); // go to the next step
                }

                @Override
                public void onFailure(Throwable caught) {
                    wizard.getView().showMessage(caught.getMessage());
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_createFailure(), caught);
                    wizard.setBundleVersion(null);
                    wizard.setCreateInitialBundleVersionRecipe("");
                }
            });
    }

    private void processToken() {

        BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService(BUNDLE_CREATE_INITIAL_VERSION_TIMEOUT);
        bundleServer.createInitialBundleVersionViaToken(getInitialBundleIds(),
            this.wizard.getCreateInitialBundleVersionToken(), new AsyncCallback<BundleVersion>() {
                @Override
                public void onSuccess(BundleVersion result) {
                    CoreGUI.getMessageCenter().notify(
                        new Message(
                            MSG.view_bundle_createWizard_createSuccessful(result.getName(), result.getVersion()),
                            Message.Severity.Info));
                    wizard.setBundleVersion(result);
                    wizard.getView().incrementStep(); // go to the next step
                }

                @Override
                public void onFailure(Throwable caught) {
                    wizard.getView().showMessage(caught.getMessage());
                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_createWizard_createFailure(), caught);
                    wizard.setBundleVersion(null);
                    wizard.setCreateInitialBundleVersionToken(null);
                }
            });
    }

    private void processAssignment() {

        BundleGWTServiceAsync bundleServer = GWTServiceLookup.getBundleService();
        bundleServer.assignBundlesToBundleGroups(getInitialBundleIds(), new int[] { wizard.getBundleVersion()
            .getBundle().getId() }, new AsyncCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                CoreGUI.getMessageCenter().notify(
                    new Message(MSG.view_bundle_createWizard_groupsStep_successAssign(wizard.getBundleVersion()
                        .getBundle().getName(), wizard.getBundleVersion().getName()), Message.Severity.Info));
                wizard.getView().incrementStep(); // go to the next step
            }

            @Override
            public void onFailure(Throwable caught) {
                String msg = MSG.view_bundle_createWizard_groupsStep_failedAssign(wizard.getBundleVersion().getBundle()
                    .getName(), wizard.getBundleVersion().getName());
                wizard.getView().showMessage(msg);
                CoreGUI.getErrorHandler().handleError(msg, caught);
            }
        });
    }

    @Override
    public boolean previousPage() {
        wizard.getView().hideMessage();
        return true;
    }

    public String getName() {
        return MSG.common_title_bundleGroups();
    }
}
