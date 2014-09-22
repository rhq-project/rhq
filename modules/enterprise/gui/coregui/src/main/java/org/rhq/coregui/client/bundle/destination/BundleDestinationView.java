/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

package org.rhq.coregui.client.bundle.destination;

import static org.rhq.coregui.client.CoreGUI.getErrorHandler;
import static org.rhq.coregui.client.CoreGUI.getMessageCenter;
import static org.rhq.coregui.client.CoreGUI.goToView;
import static org.rhq.coregui.client.CoreGUI.isTagsEnabledForUI;

import java.util.Date;
import java.util.HashSet;

import com.google.gwt.core.client.Duration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDestination;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleDestinationCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.IconEnum;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.bundle.deploy.BundleDeployWizard;
import org.rhq.coregui.client.bundle.deployment.BundleDeploymentListView;
import org.rhq.coregui.client.bundle.revert.BundleRevertWizard;
import org.rhq.coregui.client.components.HeaderLabel;
import org.rhq.coregui.client.components.buttons.BackButton;
import org.rhq.coregui.client.components.table.Table;
import org.rhq.coregui.client.components.tagging.TagEditorView;
import org.rhq.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.StringUtility;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class BundleDestinationView extends EnhancedVLayout implements BookmarkableView {
    private BundleDestination destination;
    private Bundle bundle;

    private boolean canDelete;
    private boolean canDeploy;
    private boolean canTag;

    public BundleDestinationView(boolean canDelete, boolean canDeploy, boolean canTag) {
        super();
        this.canDelete = canDelete;
        this.canDeploy = canDeploy;
        this.canTag = canTag;
        setWidth100();
        setHeight100();
        //setMargin(10); // do not set margin, we already have our margin set outside of us
    }

    private void viewBundleDestination(BundleDestination bundleDestination) {
        // Whenever a new view request comes in, make sure to clean house to avoid ID conflicts for sub-widgets
        this.destroyMembers();

        this.destination = bundleDestination;
        this.bundle = bundleDestination.getBundle();

        BackButton backButton = new BackButton(MSG.view_bundle_dest_backToBundle() + ": "
            + StringUtility.escapeHtml(bundle.getName()), "Bundles/Bundle/" + bundle.getId());

        HeaderLabel header = new HeaderLabel(Canvas.getImgURL(IconEnum.BUNDLE_DESTINATION.getIcon24x24Path()),
            StringUtility.escapeHtml(destination.getName()));

        Canvas detail = new Canvas();
        detail.setHeight("50%");
        detail.hide();

        addMember(backButton);
        addMember(header);

        //conditionally add tags. Defaults to true, not available in JON builds.
        if (isTagsEnabledForUI()) {
            addMember(createTagEditor());
        }

        addMember(createSummaryForm());
        addMember(createDeploymentsTable());
        addMember(detail);
    }

    private DynamicForm createSummaryForm() {
        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setColWidths("20%", "40%", "40%");
        form.setNumCols(3);
        form.setAutoHeight();
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
        actionItem.setRowSpan(5);
        actionItem.setShowTitle(false);
        actionItem.setCanvas(getActionLayout());

        StaticTextItem created = new StaticTextItem("created", MSG.view_bundle_dest_created());
        created.setValue(new Date(destination.getCtime()));

        StaticTextItem destinationGroup = new StaticTextItem("group");
        destinationGroup.setTitle(MSG.view_bundle_dest_group());
        destinationGroup.setValue("<a href=\"" + LinkManager.getResourceGroupLink(destination.getGroup()) + "\">"
            + StringUtility.escapeHtml(destination.getGroup().getName()) + "</a>");

        StaticTextItem baseDirName = new StaticTextItem("baseDir", MSG.view_bundle_dest_baseDirName());
        baseDirName.setValue(destination.getDestinationBaseDirectoryName());

        StaticTextItem path = new StaticTextItem("path", MSG.view_bundle_dest_deployDir());
        path.setValue(destination.getDeployDir());

        StaticTextItem description = new StaticTextItem("description", MSG.common_title_description());
        description.setValue(StringUtility.escapeHtml(destination.getDescription()));

        form.setFields(bundleName, actionItem, created, destinationGroup, baseDirName, path, description);
        return form;
    }

    private TagEditorView createTagEditor() {
        boolean readOnly = !this.canTag;
        TagEditorView tagEditor = new TagEditorView(destination.getTags(), readOnly, new TagsChangedCallback() {
            @Override
            public void tagsChanged(HashSet<Tag> tags) {
                GWTServiceLookup.getTagService().updateBundleDestinationTags(destination.getId(), tags,
                    new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            getErrorHandler().handleError(MSG.view_bundle_dest_tagUpdateFailure(), caught);
                        }

                        @Override
                        public void onSuccess(Void result) {
                            getMessageCenter().notify(
                                new Message(MSG.view_bundle_dest_tagUpdateSuccessful(), Message.Severity.Info));
                        }
                    });
            }
        });
        tagEditor.setAutoHeight();
        tagEditor.setExtraSpace(10);
        return tagEditor;
    }

    private Canvas getActionLayout() {
        EnhancedVLayout actionLayout = new EnhancedVLayout(10);
        IButton deployButton = new EnhancedIButton(MSG.view_bundle_deploy(), ButtonColor.BLUE);
        //deployButton.setIcon(IconEnum.BUNDLE_DEPLOY.getIcon16x16Path());
        deployButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                new BundleDeployWizard(destination).startWizard();
            }
        });
        actionLayout.addMember(deployButton);

        IButton revertButton = new EnhancedIButton(MSG.view_bundle_revert(), ButtonColor.RED);
        //revertButton.setIcon(IconEnum.BUNDLE_REVERT.getIcon16x16Path());
        revertButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                SC.ask(MSG.view_bundle_dest_revertConfirm(), new BooleanCallback() {
                    @Override
                    public void execute(Boolean aBoolean) {
                        if (aBoolean) {
                            new BundleRevertWizard(destination).startWizard();
                        }
                    }
                });
            }
        });
        actionLayout.addMember(revertButton);

        IButton purgeButton = new EnhancedIButton(MSG.view_bundle_purge(), ButtonColor.RED);
        //purgeButton.setIcon(IconEnum.BUNDLE_DESTINATION_PURGE.getIcon16x16Path());
        purgeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                SC.ask(MSG.view_bundle_dest_purgeConfirm(), new BooleanCallback() {
                    @Override
                    public void execute(Boolean aBoolean) {
                        if (aBoolean) {
                            BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService(600000); // 10m should be enough right?
                            bundleService.purgeBundleDestination(destination.getId(), new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    getErrorHandler().handleError(
                                        MSG.view_bundle_dest_purgeFailure(destination.getName()), caught);
                                }

                                @Override
                                public void onSuccess(Void result) {
                                    getMessageCenter().notify(
                                        new Message(MSG.view_bundle_dest_purgeSuccessful(destination.getName()),
                                            Message.Severity.Info));
                                    // Bundle destination is purged, go back to bundle destination view
                                    goToView(LinkManager.getBundleDestinationLink(bundle.getId(), destination.getId()),
                                        true);
                                }
                            });
                        }
                    }
                });
            }
        });
        checkIfDisabled(purgeButton);
        actionLayout.addMember(purgeButton);

        IButton deleteButton = new EnhancedIButton(MSG.common_button_delete(), ButtonColor.RED);
        //deleteButton.setIcon(IconEnum.BUNDLE_DESTINATION_DELETE.getIcon16x16Path());
        deleteButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                SC.ask(MSG.view_bundle_dest_deleteConfirm(), new BooleanCallback() {
                    @Override
                    public void execute(Boolean confirmed) {
                        if (confirmed) {
                            doDeleteBundleDestination();
                        }
                    }
                });
            }
        });
        actionLayout.addMember(deleteButton);

        if (!canDelete) {
            deleteButton.setDisabled(true);
        }

        if (!canDeploy) {
            deployButton.setDisabled(true);
            revertButton.setDisabled(true);
            purgeButton.setDisabled(true);
        }

        return actionLayout;
    }

    private void doDeleteBundleDestination() {
        String deleteSubmittedMessage = MSG.view_bundle_dest_deleteSubmitted(destination.getName(), destination
            .getBundle().getName());
        getMessageCenter().notify(new Message(deleteSubmittedMessage, Message.Severity.Info));
        final Duration duration = new Duration();
        BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();
        bundleService.deleteBundleDestination(destination.getId(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(final Throwable caught) {
                Timer timer = new Timer() {
                    @Override
                    public void run() {
                        String message = MSG.view_bundle_dest_deleteFailure(destination.getName());
                        getErrorHandler().handleError(message, caught);
                    }
                };
                // Delay the showing of the result to give the user some time to see the deleteSubmitted notif
                timer.schedule(Math.max(0, 3 * 1000 - duration.elapsedMillis()));
            }

            @Override
            public void onSuccess(Void result) {
                Timer timer = new Timer() {
                    @Override
                    public void run() {
                        String message = MSG.view_bundle_dest_deleteSuccessful(destination.getName());
                        getMessageCenter().notify(new Message(message, Message.Severity.Info));
                        // Bundle destination is deleted, go back to bundle destinations root view
                        goToView(LinkManager.getBundleDestinationLink(bundle.getId(), 0), true);
                    }
                };
                // Delay the showing of the result to give the user some time to see the deleteSubmitted notif
                timer.schedule(Math.max(0, 3 * 1000 - duration.elapsedMillis()));
            }
        });
    }

    private void checkIfDisabled(final IButton purgeButton) {
        BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();
        BundleDeploymentCriteria criteria = new BundleDeploymentCriteria();
        criteria.addFilterDestinationId(destination.getId());
        bundleService.findBundleDeploymentsByCriteria(criteria, new AsyncCallback<PageList<BundleDeployment>>() {
            @Override
            public void onFailure(Throwable caught) {
                purgeButton.setDisabled(false);
            }

            @Override
            public void onSuccess(PageList<BundleDeployment> result) {
                for (BundleDeployment deployment : result) {
                    if (deployment.isLive()) {
                        purgeButton.setDisabled(false);
                        return;
                    }
                }
                purgeButton.setDisabled(true);
            }
        });
    }

    private Table createDeploymentsTable() {
        Criteria criteria = new Criteria();
        criteria.addCriteria("bundleDestinationId", destination.getId());
        BundleDeploymentListView deployments = new BundleDeploymentListView(criteria, canDeploy);
        deployments.setHeight100();
        deployments.setShowResizeBar(true);
        return deployments;
    }

    @Override
    public void renderView(final ViewPath viewPath) {
        int bundleDestinationId = Integer.parseInt(viewPath.getCurrent().getPath());

        BundleDestinationCriteria criteria = new BundleDestinationCriteria();
        criteria.addFilterId(bundleDestinationId);
        criteria.fetchBundle(true);
        criteria.fetchDeployments(true);
        criteria.fetchTags(true);

        BundleGWTServiceAsync bundleService = GWTServiceLookup.getBundleService();
        bundleService.findBundleDestinationsByCriteria(criteria, new AsyncCallback<PageList<BundleDestination>>() {
            @Override
            public void onFailure(Throwable caught) {
                getErrorHandler().handleError(MSG.view_bundle_dest_loadFailure(), caught);
            }

            @Override
            public void onSuccess(PageList<BundleDestination> result) {
                final BundleDestination destination = result.get(0);
                viewBundleDestination(destination);
            }
        });
    }

}
