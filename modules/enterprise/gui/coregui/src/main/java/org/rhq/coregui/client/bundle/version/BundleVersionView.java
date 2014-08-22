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
package org.rhq.coregui.client.bundle.version;

import java.util.HashSet;

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
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.BookmarkableView;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.LinkManager;
import org.rhq.coregui.client.ViewId;
import org.rhq.coregui.client.ViewPath;
import org.rhq.coregui.client.bundle.deployment.BundleDeploymentListView;
import org.rhq.coregui.client.bundle.version.file.FileListView;
import org.rhq.coregui.client.components.HeaderLabel;
import org.rhq.coregui.client.components.buttons.BackButton;
import org.rhq.coregui.client.components.tagging.TagEditorView;
import org.rhq.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.coregui.client.gwt.GWTServiceLookup;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton;
import org.rhq.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.coregui.client.util.enhanced.EnhancedIButton.ButtonColor;
import org.rhq.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class BundleVersionView extends EnhancedVLayout implements BookmarkableView {

    private BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();
    private BundleVersion version;
    private boolean canDelete;
    private boolean canDeploy;
    private boolean canTag;

    public BundleVersionView(boolean canDelete, boolean canDeploy, boolean canTag) {
        super();
        this.canDelete = canDelete;
        this.canDeploy = canDeploy;
        this.canTag = canTag;
        setWidth100();
        setHeight100();
        //setMargin(10); // do not set margin, we already have our margin set outside of us
    }

    private void viewBundleVersion(BundleVersion version, ViewId nextViewId) {
        // Whenever a new view request comes in, make sure to clean house to avoid ID conflicts for sub-widgets
        this.destroyMembers();

        this.version = version;

        addMember(new BackButton(MSG.view_bundle_version_backToBundle() + ": " + version.getBundle().getName(),
            LinkManager.getBundleLink(version.getBundle().getId())));

        addMember(new HeaderLabel(Canvas.getImgURL("subsystems/bundle/BundleVersion_24.png"), version.getName() + ": "
            + version.getVersion()));

        //conditionally add tags. Defaults to true, not available in JON builds.
        if (CoreGUI.isTagsEnabledForUI()) {
            addMember(createTagEditor());
        }

        addMember(createSummaryForm());

        TabSet tabs = new TabSet();
        tabs.addTab(createRecipeTab());
        tabs.addTab(createLiveDeploymentsTab());
        tabs.addTab(createFilesTab());
        addMember(tabs);

        if (nextViewId != null) {
            if (nextViewId.getPath().equals("recipe")) {
                tabs.selectTab(0);
            } else if (nextViewId.getPath().equals("deployments")) {
                tabs.selectTab(1);
            } else if (nextViewId.getPath().equals("files")) {
                tabs.selectTab(2);
            } else {
                // should we throw an exception? someone gave a bad URL; just bring them to first tab
                tabs.selectTab(0);
            }
        }

        markForRedraw();
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

        CanvasItem actionItem = new CanvasItem("actions");
        actionItem.setColSpan(1);
        actionItem.setRowSpan(4);
        actionItem.setShowTitle(false);
        actionItem.setCanvas(getActionLayout());

        StaticTextItem versionItem = new StaticTextItem("version", MSG.common_title_version());
        versionItem.setValue(version.getVersion());

        StaticTextItem liveDeploymentsItem = new StaticTextItem("deployments", MSG.view_bundle_deployments());
        liveDeploymentsItem.setValue(version.getBundleDeployments().size());

        StaticTextItem filesItems = new StaticTextItem("files", MSG.view_bundle_files());
        filesItems.setValue(version.getBundleFiles().size());

        StaticTextItem descriptionItem = new StaticTextItem("description", MSG.common_title_description());
        descriptionItem.setValue(version.getDescription());

        form.setFields(versionItem, actionItem, liveDeploymentsItem, filesItems, descriptionItem);
        return form;
    }

    private Canvas getActionLayout() {
        EnhancedVLayout actionLayout = new EnhancedVLayout(10);
        IButton deleteButton = new EnhancedIButton(MSG.common_button_delete(), ButtonColor.RED);
        //deleteButton.setIcon("subsystems/bundle/BundleVersionAction_Delete_16.png");
        deleteButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                SC.ask(MSG.view_bundle_version_deleteConfirm(), new BooleanCallback() {
                    public void execute(Boolean aBoolean) {
                        if (aBoolean) {
                            bundleManager.deleteBundleVersion(version.getId(), false, new AsyncCallback<Void>() {
                                public void onFailure(Throwable caught) {
                                    CoreGUI.getErrorHandler().handleError(
                                        MSG.view_bundle_version_deleteFailure(version.getVersion()), caught);
                                }

                                public void onSuccess(Void result) {
                                    CoreGUI.getMessageCenter().notify(
                                        new Message(MSG.view_bundle_version_deleteSuccessful(version.getVersion()),
                                            Message.Severity.Info));
                                    // Bundle version is deleted, go back to main bundle view
                                    CoreGUI.goToView(LinkManager.getBundleVersionLink(version.getBundle().getId(), 0),
                                        true);
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
                GWTServiceLookup.getTagService().updateBundleVersionTags(version.getId(), tags,
                    new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError(
                                MSG.view_bundle_version_bundleVersionTagUpdateFailure(), caught);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message(MSG.view_bundle_version_bundleVersionTagUpdateSuccessful(),
                                    Message.Severity.Info));
                        }
                    });
            }
        });
        tagEditor.setAutoHeight();
        tagEditor.setExtraSpace(10);
        return tagEditor;
    }

    private Tab createRecipeTab() {
        Tab tab = new Tab(MSG.view_bundle_recipe());
        DynamicForm form = new DynamicForm();

        TextAreaItem recipeCanvas = new TextAreaItem("recipe", MSG.view_bundle_recipe());
        recipeCanvas.setShowTitle(false);
        recipeCanvas.setColSpan(2);
        recipeCanvas.setWidth("100%");
        recipeCanvas.setHeight("100%");
        recipeCanvas.setValue(version.getRecipe());
        recipeCanvas.addChangeHandler(new ChangeHandler() {
            @Override
            public void onChange(ChangeEvent event) {
                // makes this read-only; however, since its not disabled, user can still select/copy the text
                event.cancel();
            }
        });

        form.setHeight100();
        form.setWidth100();
        form.setItems(recipeCanvas);
        tab.setPane(form);
        return tab;
    }

    private Tab createLiveDeploymentsTab() {
        Tab tab = new Tab(MSG.view_bundle_deployments());
        Criteria criteria = new Criteria();
        criteria.setAttribute("bundleVersionId", version.getId());
        tab.setPane(new BundleDeploymentListView(criteria, this.canDeploy));
        return tab;
    }

    private Tab createFilesTab() {
        Tab tab = new Tab(MSG.view_bundle_files());
        FileListView filesView = new FileListView(version.getId());
        tab.setPane(filesView);
        return tab;
    }

    public void renderView(final ViewPath viewPath) {
        int bundleVersionId = Integer.parseInt(viewPath.getCurrent().getPath());

        BundleVersionCriteria criteria = new BundleVersionCriteria();
        criteria.addFilterId(bundleVersionId);
        criteria.fetchBundle(true);
        criteria.fetchBundleFiles(true);
        criteria.fetchBundleDeployments(true);
        criteria.fetchConfigurationDefinition(true);
        criteria.fetchTags(true);

        bundleManager.findBundleVersionsByCriteria(criteria, new AsyncCallback<PageList<BundleVersion>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError(MSG.view_bundle_version_loadFailure(), caught);
            }

            public void onSuccess(PageList<BundleVersion> result) {
                BundleVersion version = result.get(0);
                ViewId nextPath = viewPath.next().getCurrent();
                viewBundleVersion(version, nextPath);
            }
        });
    }
}
