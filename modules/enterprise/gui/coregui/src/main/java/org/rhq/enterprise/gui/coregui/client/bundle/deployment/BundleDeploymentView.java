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
package org.rhq.enterprise.gui.coregui.client.bundle.deployment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.types.AutoFitWidthApproach;
import com.smartgwt.client.types.DateDisplayFormat;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.resource.Resource;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ErrorMessageWindow;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class BundleDeploymentView extends LocatableVLayout implements BookmarkableView {
    private BundleGWTServiceAsync bundleService;

    private BundleDeployment deployment;
    private BundleVersion version;
    private Bundle bundle;

    private VLayout detail;
    private boolean canManageBundles;

    private final HashMap<String, String> statusIcons;

    public BundleDeploymentView(String locatorId, boolean canManageBundles) {
        super(locatorId);
        this.canManageBundles = canManageBundles;
        setWidth100();
        setHeight100();
        //setMargin(10); // do not set margin, we already have our margin set outside of us

        statusIcons = new HashMap<String, String>();
        statusIcons.put(BundleDeploymentStatus.IN_PROGRESS.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.FAILURE.name(), "subsystems/bundle/Error_11.png");
        statusIcons.put(BundleDeploymentStatus.MIXED.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.WARN.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.SUCCESS.name(), "subsystems/bundle/Ok_11.png");
    }

    private void viewBundleDeployment(BundleDeployment bundleDeployment, ViewId current) {
        // Whenever a new view request comes in, make sure to clean house to avoid ID conflicts for sub-widgets
        this.destroyMembers();

        this.deployment = bundleDeployment;
        this.version = bundleDeployment.getBundleVersion();
        this.bundle = bundleDeployment.getBundleVersion().getBundle();

        addMember(new BackButton(extendLocatorId("BackButton"), MSG.view_bundle_deploy_backButton() + ": "
            + deployment.getDestination().getName(), LinkManager.getBundleDestinationLink(version.getBundle().getId(),
            deployment.getDestination().getId())));
        addMember(new HeaderLabel(Canvas.getImgURL("subsystems/bundle/BundleDeployment_24.png"), deployment.getName()));
        addMember(createTagEditor());
        addMember(createSummaryForm());
        addMemberDeploymentsTable();

        detail = new VLayout();
        detail.setAutoHeight();
        detail.hide();
        addMember(detail);
    }

    private LocatableDynamicForm createSummaryForm() {
        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("Summary"));
        form.setWidth100();
        form.setAutoHeight();
        form.setNumCols(4);
        form.setWrapItemTitles(false);
        form.setExtraSpace(10);
        form.setIsGroup(true);
        form.setGroupTitle(MSG.common_title_summary());
        form.setPadding(5);

        LinkItem bundleName = new LinkItem("bundle");
        bundleName.setTitle(MSG.view_bundle_bundle());
        bundleName.setValue(LinkManager.getBundleLink(bundle.getId()));
        bundleName.setLinkTitle(bundle.getName());
        bundleName.setTarget("_self");

        LinkItem bundleVersionName = new LinkItem("bundleVersion");
        bundleVersionName.setTitle(MSG.view_bundle_bundleVersion());
        bundleVersionName.setValue(LinkManager.getBundleVersionLink(bundle.getId(), deployment.getBundleVersion()
            .getId()));
        bundleVersionName.setLinkTitle(deployment.getBundleVersion().getVersion());
        bundleVersionName.setTarget("_self");

        StaticTextItem deployed = new StaticTextItem("deployed", MSG.view_bundle_deployed());
        deployed.setDateFormatter(DateDisplayFormat.TOLOCALESTRING);
        deployed.setValue(new Date(deployment.getCtime()));

        StaticTextItem deployedBy = new StaticTextItem("deployedBy", MSG.view_bundle_deploy_deployedBy());
        deployedBy.setValue(deployment.getSubjectName());

        StaticTextItem description = new StaticTextItem("description", MSG.common_title_description());
        description.setValue(deployment.getDescription());

        StaticTextItem status = new StaticTextItem("status", MSG.common_title_status());
        status.setValue(deployment.getStatus().name());
        status.setValueIcons(statusIcons);
        status.setValueIconHeight(11);
        status.setValueIconWidth(11);
        status.setShowValueIconOnly(true);
        if (deployment.getErrorMessage() != null) {
            status.setTooltip(MSG.view_bundle_deploy_clickForError());
            status.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    ErrorMessageWindow win = new ErrorMessageWindow(extendLocatorId("errWin"),
                        MSG.common_title_error(), "<pre>" + deployment.getErrorMessage() + "</pre>");
                    win.show();
                }
            });
        }

        LinkItem destinationGroup = new LinkItem("group");
        destinationGroup.setTitle(MSG.common_title_resource_group());
        destinationGroup.setValue(LinkManager.getResourceGroupLink(deployment.getDestination().getGroup().getId()));
        destinationGroup.setLinkTitle(deployment.getDestination().getGroup().getName());
        destinationGroup.setTarget("_self");

        StaticTextItem path = new StaticTextItem("path", MSG.view_bundle_deployDir());
        path.setValue(deployment.getDestination().getDeployDir());

        form.setFields(bundleName, deployed, bundleVersionName, deployedBy, //
            description, status, destinationGroup, path);
        return form;
    }

    private TagEditorView createTagEditor() {
        boolean readOnly = !this.canManageBundles;
        TagEditorView tagEditor = new TagEditorView(extendLocatorId("tagEditor"), version.getTags(), readOnly,
            new TagsChangedCallback() {
                public void tagsChanged(HashSet<Tag> tags) {
                    GWTServiceLookup.getTagService().updateBundleDeploymentTags(deployment.getId(), tags,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler()
                                    .handleError(MSG.view_bundle_deploy_tagUpdateFailure(), caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(
                                    new Message(MSG.view_bundle_deploy_tagUpdateSuccessful(), Message.Severity.Info));
                            }
                        });
                }
            });
        tagEditor.setAutoHeight();
        tagEditor.setExtraSpace(10);
        return tagEditor;
    }

    private Table addMemberDeploymentsTable() {
        Table table = new Table(extendLocatorId("Deployments"), MSG.view_bundle_deploy_deploymentPlatforms());

        table.setTitleComponent(new HTMLFlow(MSG.view_bundle_deploy_selectARow()));

        // resource icon field
        ListGridField resourceIcon = new ListGridField("resourceAvailability");
        HashMap<String, String> icons = new HashMap<String, String>();
        icons.put(AvailabilityType.UP.name(), ImageManager.getResourceIcon(ResourceCategory.PLATFORM, Boolean.TRUE));
        icons.put(AvailabilityType.DOWN.name(), ImageManager.getResourceIcon(ResourceCategory.PLATFORM, Boolean.FALSE));
        resourceIcon.setValueIcons(icons);
        resourceIcon.setValueIconSize(16);
        resourceIcon.setType(ListGridFieldType.ICON);
        resourceIcon.setWidth(40);

        // resource field
        ListGridField resource = new ListGridField("resource", MSG.common_title_platform());
        resource.setAutoFitWidth(true);
        resource.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);
        resource.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"" + LinkManager.getResourceLink(listGridRecord.getAttributeAsInt("resourceId"))
                    + "\">" + o + "</a>";

            }
        });

        // resource version field
        ListGridField resourceVersion = new ListGridField("resourceVersion", MSG.view_bundle_deploy_operatingSystem());
        resourceVersion.setAutoFitWidth(true);
        resourceVersion.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);

        // status icon field
        ListGridField status = new ListGridField("status", MSG.common_title_status());
        status.setValueIcons(statusIcons);
        status.setValueIconHeight(11);
        status.setValueIconWidth(11);
        status.setWidth("*");

        ArrayList<ListGridRecord> records = new ArrayList<ListGridRecord>();
        for (BundleResourceDeployment rd : deployment.getResourceDeployments()) {
            ListGridRecord record = new ListGridRecord();
            Resource rr = rd.getResource();
            record.setAttribute("resource", rr.getName());
            record.setAttribute("resourceAvailability", rr.getCurrentAvailability().getAvailabilityType().name());
            record.setAttribute("resourceId", rr.getId());
            record.setAttribute("resourceVersion", rr.getVersion());
            record.setAttribute("status", rd.getStatus().name());
            record.setAttribute("id", rd.getId());
            record.setAttribute("object", rd);
            records.add(record);
        }

        // To get the ListGrid the Table must be initialized (via onInit()) by adding to the Canvas
        table.setHeight("30%");
        table.setWidth100();
        table.setShowResizeBar(true);
        table.setResizeBarTarget("next");
        addMember(table);

        ListGrid listGrid = table.getListGrid();
        listGrid.setFields(resourceIcon, resource, resourceVersion, status);
        listGrid.setData(records.toArray(new ListGridRecord[records.size()]));
        listGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {

                    BundleResourceDeployment bundleResourceDeployment = (BundleResourceDeployment) selectionEvent
                        .getRecord().getAttributeAsObject("object");
                    BundleResourceDeploymentHistoryListView detailView = new BundleResourceDeploymentHistoryListView(
                        "Detail", bundleResourceDeployment);

                    detail.removeMembers(detail.getMembers());
                    detail.addMember(detailView);
                    detail.setHeight("50%");
                    detail.animateShow(AnimationEffect.SLIDE);
                } else {
                    detail.animateHide(AnimationEffect.SLIDE);
                }
            }
        });

        return table;
    }

    public void renderView(final ViewPath viewPath) {
        int bundleDeploymentId = Integer.parseInt(viewPath.getCurrent().getPath());

        BundleDeploymentCriteria criteria = new BundleDeploymentCriteria();
        criteria.addFilterId(bundleDeploymentId);
        criteria.fetchBundleVersion(true);
        criteria.fetchConfiguration(true);
        criteria.fetchResourceDeployments(true);
        criteria.fetchDestination(true);
        criteria.fetchTags(true);

        bundleService = GWTServiceLookup.getBundleService();
        bundleService.findBundleDeploymentsByCriteria(criteria, new AsyncCallback<PageList<BundleDeployment>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deploy_loadFailure(), caught);
            }

            public void onSuccess(PageList<BundleDeployment> result) {
                final BundleDeployment deployment = result.get(0);
                BundleCriteria bundleCriteria = new BundleCriteria();
                bundleCriteria.addFilterId(deployment.getBundleVersion().getBundle().getId());
                bundleService.findBundlesByCriteria(bundleCriteria, new AsyncCallback<PageList<Bundle>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deploy_loadBundleFailure(), caught);
                    }

                    public void onSuccess(PageList<Bundle> result) {
                        final Bundle bundle = result.get(0);
                        deployment.getBundleVersion().setBundle(bundle);
                        BundleResourceDeploymentCriteria criteria = new BundleResourceDeploymentCriteria();
                        criteria.addFilterBundleDeploymentId(deployment.getId());
                        criteria.fetchHistories(true);
                        criteria.fetchResource(true);
                        criteria.fetchBundleDeployment(true);
                        bundleService.findBundleResourceDeploymentsByCriteria(criteria,
                            new AsyncCallback<PageList<BundleResourceDeployment>>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deploy_loadFailure(), caught);
                                }

                                public void onSuccess(PageList<BundleResourceDeployment> result) {
                                    deployment.setResourceDeployments(result);
                                    viewBundleDeployment(deployment, viewPath.getCurrent());
                                }
                            });
                    }
                });
            }
        });
    }

}
