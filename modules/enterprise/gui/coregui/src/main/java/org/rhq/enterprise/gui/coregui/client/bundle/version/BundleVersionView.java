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
package org.rhq.enterprise.gui.coregui.client.bundle.version;

import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
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
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.LinkManager;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.deployment.BundleDeploymentListView;
import org.rhq.enterprise.gui.coregui.client.bundle.version.file.FileListView;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableIButton;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTab;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableTabSet;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableVLayout;

/**
 * @author Greg Hinkle
 */
public class BundleVersionView extends LocatableVLayout implements BookmarkableView {

    private BundleGWTServiceAsync bundleManager = GWTServiceLookup.getBundleService();
    private BundleVersion version;
    private boolean canManageBundles = false;

    public BundleVersionView(String locatorId, boolean canManageBundles) {
        super(locatorId);
        this.canManageBundles = canManageBundles;
        setWidth100();
        setHeight100();
        //setMargin(10); // do not set margin, we already have our margin set outside of us
    }

    private void viewBundleVersion(BundleVersion version, ViewId nextViewId) {
        // Whenever a new view request comes in, make sure to clean house to avoid ID conflicts for sub-widgets
        this.destroyMembers();

        this.version = version;

        addMember(new BackButton(extendLocatorId("BackButton"), MSG.view_bundle_version_backToBundle() + ": "
            + version.getBundle().getName(), LinkManager.getBundleLink(version.getBundle().getId())));

        addMember(new HeaderLabel(Canvas.getImgURL("subsystems/bundle/BundleVersion_24.png"), version.getName() + ": "
            + version.getVersion()));

        addMember(createTagEditor());

        addMember(createSummaryForm());

        TabSet tabs = new LocatableTabSet(extendLocatorId("Tabs"));
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

    private LocatableDynamicForm createSummaryForm() {

        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("Summary"));
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
        actionItem.setCanvas(getActionLayout(form.extendLocatorId("actions")));

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

    private Canvas getActionLayout(String locatorId) {
        LocatableVLayout actionLayout = new LocatableVLayout(locatorId, 10);
        IButton deleteButton = new LocatableIButton(extendLocatorId("Delete"), MSG.common_button_delete());
        deleteButton.setIcon("subsystems/bundle/BundleVersionAction_Delete_16.png");
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
                                    CoreGUI.goToView(LinkManager.getBundleVersionLink(version.getBundle().getId(), 0));
                                }
                            });
                        }
                    }
                });
            }
        });
        actionLayout.addMember(deleteButton);

        if (!canManageBundles) {
            deleteButton.setDisabled(true);
        }

        return actionLayout;
    }

    private TagEditorView createTagEditor() {
        boolean readOnly = !this.canManageBundles;
        TagEditorView tagEditor = new TagEditorView(extendLocatorId("Tags"), version.getTags(), readOnly,
            new TagsChangedCallback() {
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
        LocatableTab tab = new LocatableTab(extendLocatorId("Recipe"), MSG.view_bundle_recipe());
        LocatableDynamicForm form = new LocatableDynamicForm(extendLocatorId("RecipeForm"));

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
        LocatableTab tab = new LocatableTab(extendLocatorId("Deployments"), MSG.view_bundle_deployments());
        Criteria criteria = new Criteria();
        criteria.setAttribute("bundleVersionId", version.getId());
        tab.setPane(new BundleDeploymentListView(tab.getLocatorId(), criteria, this.canManageBundles));
        return tab;
    }

    private Tab createFilesTab() {
        LocatableTab tab = new LocatableTab(extendLocatorId("Files"), MSG.view_bundle_files());
        FileListView filesView = new FileListView(tab.getLocatorId(), version.getId());
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
