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
package org.rhq.coregui.client.bundle.deployment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.types.AutoFitWidthApproach;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
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
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.ErrorMessageWindow;
import org.rhq.coregui.client.ImageManager;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.ViewId;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.bundle.revert.BundleRevertWizard;
import org.rhq.coregui.client.components.HeaderLabel;
import org.rhq.coregui.client.components.TitleBar;
import org.rhq.coregui.client.components.buttons.BackButton;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.table.TimestampCellFormatter;
import org.rhq.coregui.client.components.tagging.TagEditorView;
import org.rhq.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class BundleDeploymentView extends EnhancedVLayout implements BookmarkableView {
    private BundleDeployment deployment;
    private BundleVersion version;
    private Bundle bundle;

    private VLayout detail;
    private boolean canDelete;
    private boolean canDeploy;
    private boolean canTag;

    private final HashMap<String, String> statusIcons;

    public BundleDeploymentView(boolean canDelete, boolean canDeploy, boolean canTag) {
        super();
        this.canDelete = canDelete;
        this.canDeploy = canDeploy;
        this.canTag = canTag;
        setWidth100();
        setHeight100();
        //setMargin(10); // do not set margin, we already have our margin set outside of us

        statusIcons = new HashMap<String, String>();
        statusIcons.put(BundleDeploymentStatus.PENDING.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.IN_PROGRESS.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.FAILURE.name(), "subsystems/bundle/Error_11.png");
        statusIcons.put(BundleDeploymentStatus.MIXED.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.SUCCESS.name(), "subsystems/bundle/Ok_11.png");
    }

    private void viewBundleDeployment(BundleDeployment bundleDeployment, ViewId current) {
        // Whenever a new view request comes in, make sure to clean house to avoid ID conflicts for sub-widgets
        this.destroyMembers();

        this.deployment = bundleDeployment;
        this.version = bundleDeployment.getBundleVersion();
        this.bundle = bundleDeployment.getBundleVersion().getBundle();

        addMember(new BackButton(MSG.view_bundle_deploy_backButton() + ": " + deployment.getDestination().getName(),
            LinkManager.getBundleDestinationLink(version.getBundle().getId(), deployment.getDestination().getId())));
        addMember(new HeaderLabel(Canvas.getImgURL("subsystems/bundle/BundleDeployment_24.png"), deployment.getName()));

        //conditionally add tags. Defaults to true, not available in JON builds.
        if (CoreGUI.isTagsEnabledForUI()) {
            addMember(createTagEditor());
        }
        addMember(createSummaryForm());
        addMemberDeploymentsTable();

        detail = new VLayout();
        detail.setAutoHeight();
        detail.hide();
        addMember(detail);
    }

    private DynamicForm createSummaryForm() {
        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setAutoHeight();
        form.setNumCols(5);
        form.setWrapItemTitles(false);
        form.setExtraSpace(10);
        form.setIsGroup(true);
        form.setGroupTitle(MSG.common_title_summary());
        form.setPadding(5);

        StaticTextItem bundleName = new StaticTextItem("bundle");
        bundleName.setTitle(MSG.common_title_bundle());
        bundleName.setValue("<a href=\"" + LinkManager.getBundleLink(bundle.getId()) + "\">"
            + StringUtility.escapeHtml(bundle.getName()) + "</a>");

        CanvasItem actionItem = new CanvasItem("actions");
        actionItem.setColSpan(1);
        actionItem.setRowSpan(4);
        actionItem.setShowTitle(false);
        actionItem.setCanvas(getActionLayout());

        StaticTextItem bundleVersionName = new StaticTextItem("bundleVersion");
        bundleVersionName.setTitle(MSG.view_bundle_bundleVersion());
        bundleVersionName.setValue("<a href=\""
            + LinkManager.getBundleVersionLink(bundle.getId(), deployment.getBundleVersion().getId()) + "\">"
            + StringUtility.escapeHtml(deployment.getBundleVersion().getVersion()) + "</a>");

        StaticTextItem deployed = new StaticTextItem("deployed", MSG.view_bundle_deployed());
        deployed.setValue(TimestampCellFormatter.format(deployment.getCtime(),
            TimestampCellFormatter.DATE_TIME_FORMAT_FULL));

        StaticTextItem deployedBy = new StaticTextItem("deployedBy", MSG.view_bundle_deploy_deployedBy());
        deployedBy.setValue(deployment.getSubjectName());

        StaticTextItem destinationGroup = new StaticTextItem("group");
        destinationGroup.setTitle(MSG.common_title_resource_group());
        destinationGroup.setValue("<a href=\""
            + LinkManager.getResourceGroupLink(deployment.getDestination().getGroup()) + "\">"
            + StringUtility.escapeHtml(deployment.getDestination().getGroup().getName()) + "</a>");

        StaticTextItem destBaseDir = new StaticTextItem("destBaseDir", MSG.view_bundle_dest_baseDirName());
        destBaseDir.setValue(deployment.getDestination().getDestinationBaseDirectoryName());

        StaticTextItem path = new StaticTextItem("path", MSG.view_bundle_deployDir());
        path.setValue(deployment.getDestination().getDeployDir());

        StaticTextItem description = new StaticTextItem("description", MSG.common_title_description());
        description.setValue(StringUtility.escapeHtml(deployment.getDescription()));

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
                    ErrorMessageWindow win = new ErrorMessageWindow(MSG.common_severity_error(), "<pre>"
                        + deployment.getErrorMessage() + "</pre>");
                    win.show();
                }
            });
        }

        form.setFields(bundleName, bundleVersionName, actionItem, deployed, deployedBy, destinationGroup, destBaseDir,
            description, path, status);

        return form;
    }

    private Canvas getActionLayout() {
        EnhancedVLayout actionLayout = new EnhancedVLayout(10);

        // we can only revert the live deployments, only show revert button when appropriate
        // in addition, we provide a purge button if you are viewing the live deployment, so
        // they can be shown an option to purge the platform content (since only the "live"
        // deployment represents content on the remote machines, showing purge only for live
        // deployments makes sense).
        if (deployment.isLive()) {
            IButton revertButton = new EnhancedIButton(MSG.view_bundle_revert(), ButtonColor.RED);
            revertButton.setIcon("subsystems/bundle/BundleAction_Revert_16.png");
            revertButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
                    new BundleRevertWizard(deployment.getDestination()).startWizard();
                }
            });
            actionLayout.addMember(revertButton);

            IButton purgeButton = new EnhancedIButton(MSG.view_bundle_purge(), ButtonColor.RED);
            purgeButton.setIcon("subsystems/bundle/BundleDestinationAction_Purge_16.png");
            purgeButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
                public void onClick(com.smartgwt.client.widgets.events.ClickEvent clickEvent) {
                    SC.ask(MSG.view_bundle_dest_purgeConfirm(), new BooleanCallback() {
                        public void execute(Boolean aBoolean) {
                            if (aBoolean) {
                                final int destinationId = deployment.getDestination().getId();
                                final String destinationName = deployment.getDestination().getName();
                                BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService(600000); // 10m should be enough right?
                                bundleService.purgeBundleDestination(destinationId, new AsyncCallback<Void>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError(
                                            MSG.view_bundle_dest_purgeFailure(destinationName), caught);
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        CoreGUI.getMessageCenter().notify(
                                            new Message(MSG.view_bundle_dest_purgeSuccessful(destinationName),
                                                Message.Severity.Info));
                                        // Bundle destination is purged, go back to bundle deployment view - it is not live anymore
                                        CoreGUI.goToView(
                                            LinkManager.getBundleDeploymentLink(bundle.getId(), deployment.getId()),
                                            true);
                                    }
                                });
                            }
                        }
                    });
                }
            });
            actionLayout.addMember(purgeButton);

            if (!canDeploy) {
                revertButton.setDisabled(true);
                purgeButton.setDisabled(true);
            }
        }

        IButton deleteButton = new EnhancedIButton(MSG.common_button_delete(), ButtonColor.RED);
        deleteButton.setIcon("subsystems/bundle/BundleDeploymentAction_Delete_16.png");
        deleteButton.addClickHandler(new com.smartgwt.client.widgets.events.ClickHandler() {
            @Override
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
                SC.ask(MSG.view_bundle_deploy_deleteConfirm(), new BooleanCallback() {
                    public void execute(Boolean aBoolean) {
                        if (aBoolean) {
                            BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();
                            bundleService.deleteBundleDeployment(deployment.getId(), new AsyncCallback<Void>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError(
                                        MSG.view_bundle_deploy_deleteFailure(deployment.getName()), caught);
                                }

                                public void onSuccess(Void result) {
                                    CoreGUI.getMessageCenter().notify(
                                        new Message(MSG.view_bundle_deploy_deleteSuccessful(deployment.getName()),
                                            Message.Severity.Info));
                                    // Bundle deployment is deleted, go back to main bundle destinations view
                                    CoreGUI.goToView(LinkManager.getBundleDestinationLink(bundle.getId(), deployment
                                        .getDestination().getId()), true);
                                }
                            });
                        }
                    }
                });
            }
        });
        actionLayout.addMember(deleteButton);

        if (!canDelete) {
            deleteButton.setDisabled(true);
        }

        return actionLayout;
    }

    private TagEditorView createTagEditor() {
        boolean readOnly = !this.canTag;
        TagEditorView tagEditor = new TagEditorView(version.getTags(), readOnly, new TagsChangedCallback() {
            public void tagsChanged(HashSet<Tag> tags) {
                GWTServiceLookup.getTagService().updateBundleDeploymentTags(deployment.getId(), tags,
                    new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(MSG.view_bundle_deploy_tagUpdateFailure(), caught);
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
        Table table = new Table(MSG.view_bundle_deploy_deploymentPlatforms());
        table.setShowFooterRefresh(false);

        TitleBar titleBar = new TitleBar(MSG.view_bundle_deploy_selectARow());
        table.setTitleBar(titleBar);

        // resource icon field
        ResourceCategory resourceCategory = ResourceCategory.PLATFORM;
        try {
            resourceCategory = deployment.getDestination().getGroup().getResourceType().getCategory();
        } catch (Exception skip) { // BZ 1027732 in case a group somehow got empty or switched to a mixed group, avoid NPE
        }
        ListGridField resourceIcon = new ListGridField("resourceAvailability");
        HashMap<String, String> icons = new HashMap<String, String>();
        icons.put(AvailabilityType.UP.name(), ImageManager.getResourceIcon(resourceCategory, AvailabilityType.UP));
        icons.put(AvailabilityType.DOWN.name(), ImageManager.getResourceIcon(resourceCategory, AvailabilityType.DOWN));
        icons.put(AvailabilityType.DISABLED.name(),
            ImageManager.getResourceIcon(resourceCategory, AvailabilityType.DISABLED));
        icons.put(AvailabilityType.UNKNOWN.name(),
            ImageManager.getResourceIcon(resourceCategory, AvailabilityType.UNKNOWN));
        resourceIcon.setValueIcons(icons);
        resourceIcon.setValueIconSize(16);
        resourceIcon.setType(ListGridFieldType.ICON);
        resourceIcon.setWidth(40);

        // resource field
        ListGridField resource = new ListGridField("resource", MSG.common_title_resource());
        resource.setWidth("*");
        resource.setCellFormatter(new CellFormatter() {
            public String format(Object value, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"" + LinkManager.getResourceLink(listGridRecord.getAttributeAsInt("resourceId"))
                    + "\">" + StringUtility.escapeHtml(String.valueOf(value)) + "</a>";

            }
        });

        // resource version field
        ListGridField resourceVersion = new ListGridField("resourceVersion", MSG.common_title_version());
        resourceVersion.setAutoFitWidth(true);
        resourceVersion.setAutoFitWidthApproach(AutoFitWidthApproach.BOTH);

        // status icon field
        ListGridField status = new ListGridField("status", MSG.common_title_status());
        status.setValueIcons(statusIcons);
        status.setValueIconHeight(11);
        status.setValueIconWidth(11);
        status.setShowValueIconOnly(true);
        status.setWidth(60);

        List<ListGridRecord> records = new ArrayList<ListGridRecord>();
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
                        bundleResourceDeployment);

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

        final BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();
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
