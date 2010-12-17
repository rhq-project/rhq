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
package org.rhq.enterprise.gui.coregui.client.bundle.destination;

import java.util.Date;
import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.deploy.BundleDeployWizard;
import org.rhq.enterprise.gui.coregui.client.bundle.deployment.BundleDeploymentListView;
import org.rhq.enterprise.gui.coregui.client.bundle.revert.BundleRevertWizard;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class BundleDestinationView extends LocatableVLayout implements BookmarkableView {
    private BundleGWTServiceAsync bundleService;

    private BundleDestination destination;
    private Bundle bundle;

    private Canvas detail;

    private boolean canManageBundles;

    public BundleDestinationView(String locatorId, boolean canManageBundles) {
        super(locatorId);
        this.canManageBundles = canManageBundles;
        setWidth100();
        setHeight100();
        setMargin(10);
    }

    private void viewBundleDestination(BundleDestination bundleDestination, ViewId current) {
        // Whenever a new view request comes in, make sure to clean house to avoid ID conflicts for sub-widgets
        this.destroyMembers();

        this.destination = bundleDestination;
        this.bundle = bundleDestination.getBundle();

        addMember(new BackButton(extendLocatorId("BackButton"), MSG.view_bundle_dest_backToBundle() + ": "
            + bundle.getName(), "Bundles/Bundle/" + bundle.getId()));

        addMember(new HeaderLabel(Canvas.getImgURL("subsystems/bundle/BundleDestination_24.png"), destination.getName()));

        LocatableDynamicForm form = new LocatableDynamicForm(getLocatorId());
        form.setWidth100();
        form.setNumCols(4);
        form.setColWidths("20%", "30%", "25%", "25%");

        LinkItem bundleName = new LinkItem("bundle");
        bundleName.setTitle(MSG.view_bundle_bundle());
        bundleName.setValue("#Bundles/Bundle/" + bundle.getId());
        bundleName.setLinkTitle(bundle.getName());
        bundleName.setTarget("_self");

        CanvasItem tagItem = new CanvasItem("tag");
        tagItem.setShowTitle(false);
        TagEditorView tagEditor = new TagEditorView(form.extendLocatorId("Tags"), destination.getTags(),
            !canManageBundles, new TagsChangedCallback() {
                public void tagsChanged(HashSet<Tag> tags) {
                    GWTServiceLookup.getTagService().updateBundleDestinationTags(destination.getId(), tags,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_dest_tagUpdateFailure(), caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_bundle_dest_tagUpdateSuccessful(), Message.Severity.Info));
                            }
                        });
                }
            });
        tagEditor.setVertical(true);
        tagItem.setCanvas(tagEditor);
        tagItem.setRowSpan(4);

        CanvasItem actionItem = new CanvasItem("actions");
        actionItem.setShowTitle(false);
        actionItem.setCanvas(getActionLayout(form.extendLocatorId("actions")));
        actionItem.setRowSpan(4);

        StaticTextItem created = new StaticTextItem("created", MSG.view_bundle_dest_created());
        created.setValue(new Date(destination.getCtime()));

        LinkItem destinationGroup = new LinkItem("group");
        destinationGroup.setTitle(MSG.view_bundle_dest_group());
        destinationGroup.setValue("#ResourceGroup/" + destination.getGroup().getId());
        destinationGroup.setLinkTitle(destination.getGroup().getName());
        destinationGroup.setTarget("_self");

        StaticTextItem path = new StaticTextItem("path", MSG.view_bundle_dest_deployDir());
        path.setValue(destination.getDeployDir());

        form.setFields(bundleName, tagItem, actionItem, created, destinationGroup, path);

        addMember(form);

        Table deployments = createDeploymentsTable();
        deployments.setHeight100();
        deployments.setShowResizeBar(true);
        addMember(createDeploymentsTable());

        detail = new Canvas();
        detail.setHeight("50%");
        detail.hide();
        addMember(detail);
    }

    private Canvas getActionLayout(String locatorId) {
        LocatableVLayout actionLayout = new LocatableVLayout(locatorId);
        actionLayout.setMembersMargin(10);
        IButton deployButton = new LocatableIButton(actionLayout.extendLocatorId("Deploy"), MSG.view_bundle_deploy());
        deployButton.setIcon("subsystems/bundle/BundleAction_Deploy_16.png");
        deployButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                new BundleDeployWizard(destination).startWizard();
            }
        });
        if (!canManageBundles) {
            deployButton.setDisabled(true);
        }
        actionLayout.addMember(deployButton);

        IButton revertButton = new LocatableIButton(actionLayout.extendLocatorId("Revert"), MSG.view_bundle_revert());
        revertButton.setIcon("subsystems/bundle/BundleAction_Revert_16.png");
        revertButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                new BundleRevertWizard(destination).startWizard();
            }
        });
        if (!canManageBundles) {
            revertButton.setDisabled(true);
        }
        actionLayout.addMember(revertButton);
        return actionLayout;
    }

    private Table createDeploymentsTable() {
        Criteria criteria = new Criteria();
        criteria.addCriteria("bundleDestinationId", destination.getId());
        return new BundleDeploymentListView(extendLocatorId("Deployments"), criteria);
    }

    public void renderView(final ViewPath viewPath) {
        int bundleDestinationId = Integer.parseInt(viewPath.getCurrent().getPath());

        BundleDestinationCriteria criteria = new BundleDestinationCriteria();
        criteria.addFilterId(bundleDestinationId);
        criteria.fetchBundle(true);
        criteria.fetchDeployments(true);
        criteria.fetchTags(true);

        bundleService = GWTServiceLookup.getBundleService();
        bundleService.findBundleDestinationsByCriteria(criteria, new AsyncCallback<PageList<BundleDestination>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_dest_loadFailure(), caught);
            }

            public void onSuccess(PageList<BundleDestination> result) {
                final BundleDestination destination = result.get(0);
                viewBundleDestination(destination, viewPath.getCurrent());
            }
        });
    }

}