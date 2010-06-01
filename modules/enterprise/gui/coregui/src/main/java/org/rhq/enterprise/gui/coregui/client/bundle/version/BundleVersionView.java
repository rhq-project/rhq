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
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleVersionCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.Breadcrumb;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.bundle.deployment.BundleDeploymentListView;
import org.rhq.enterprise.gui.coregui.client.bundle.version.file.FileListView;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class BundleVersionView extends VLayout implements BookmarkableView {

    private BundleVersion version;

    public BundleVersionView() {
        setWidth100();
        setHeight100();
    }

    public void viewBundleVersion(BundleVersion version, ViewId nextViewId) {
        this.version = version;


        TabSet tabs = new TabSet();
        tabs.addTab(createSummaryTab());

        tabs.addTab(createLiveDeploymentsTab());

        tabs.addTab(createFilesTab());

        tabs.addTab(createUpdateHistoryTab());


        addMember(new BackButton("Back to Bundle: " + version.getBundle().getName(),"Bundles/Bundle/" + version.getBundle().getId()));

        addMember(new HeaderLabel(Canvas.getImgURL("subsystems/bundle/BundleVersion_24.png"), version.getName() + ": " + version.getVersion()));

        addMember(tabs);

        if (nextViewId != null) {
            if (nextViewId.getPath().equals("deployments")) {
                tabs.selectTab(1);
            } else if (nextViewId.getPath().equals("files")) {
                tabs.selectTab(2);
            } else if (nextViewId.getPath().equals("history")) {
                tabs.selectTab(3);
            }
        }

        markForRedraw();
    }


    private Tab createSummaryTab() {
        Tab tab = new Tab("Summary");

        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setHeight100();
        form.setNumCols(4);

        StaticTextItem versionItem = new StaticTextItem("version","Version");
        versionItem.setValue(version.getVersion());

        CanvasItem tagItem = new CanvasItem("tag");
        tagItem.setShowTitle(false);
        TagEditorView tagEditor = new TagEditorView(version.getTags(), false, new TagsChangedCallback() {
            public void tagsChanged(HashSet<Tag> tags) {
                GWTServiceLookup.getTagService().updateBundleVersionTags(version.getId(), tags, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to update bundle version's tags", caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(new Message("Bundle Version tags updated", Message.Severity.Info));
                    }
                });
            }
        });
        tagEditor.setVertical(true);
        tagItem.setCanvas(tagEditor);
        tagItem.setRowSpan(4);

        StaticTextItem descriptionItem = new StaticTextItem("description","Description");
        descriptionItem.setValue(version.getDescription());

        StaticTextItem liveDeploymentsItem = new StaticTextItem("deployments","Deployments");
        liveDeploymentsItem.setValue(version.getBundleDeployments().size());

        StaticTextItem filesItems = new StaticTextItem("files","Files");
        filesItems.setValue(version.getBundleFiles().size());




        TextAreaItem recipeItem = new TextAreaItem("recipe","Recipe");
        recipeItem.setDisabled(true);
        recipeItem.setTitleOrientation(TitleOrientation.TOP);
        recipeItem.setColSpan(4);
        recipeItem.setWidth("*");
        recipeItem.setHeight("*");
        recipeItem.setValue(version.getRecipe());


        form.setFields(versionItem,tagItem, descriptionItem, liveDeploymentsItem, filesItems, recipeItem);

        tab.setPane(form);

        return tab;
    }

    private Tab createLiveDeploymentsTab() {
        Tab tab = new Tab("Deployments");

        Criteria criteria = new Criteria();
        criteria.setAttribute("bundleVersionId", version.getId());

        BundleDeploymentListView table = new BundleDeploymentListView(criteria);

        tab.setPane(table);

        return tab;
    }

    private Tab createFilesTab() {
        Tab tab = new Tab("Files");

        FileListView filesView = new FileListView(version.getId());

        tab.setPane(filesView);

        return tab;
    }

    private Tab createUpdateHistoryTab() {
        Tab tab = new Tab("Update History");

        return tab;
    }




    public void renderView(final ViewPath viewPath) {
        int bundleVersionId = Integer.parseInt(viewPath.getCurrent().getPath());

        final ViewId viewId = viewPath.getCurrent();

        BundleVersionCriteria criteria = new BundleVersionCriteria();
        criteria.addFilterId(bundleVersionId);
        criteria.fetchBundle(true);
        criteria.fetchBundleFiles(true);
        criteria.fetchBundleDeployments(true);
        criteria.fetchConfigurationDefinition(true);
        criteria.fetchTags(true);

        GWTServiceLookup.getBundleService().findBundleVersionsByCriteria(criteria,
                new AsyncCallback<PageList<BundleVersion>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load budle version", caught);
                    }

                    public void onSuccess(PageList<BundleVersion> result) {
                        BundleVersion version = result.get(0);
                        viewBundleVersion(version, viewPath.getCurrent());
                        viewId.getBreadcrumbs().set(0,new Breadcrumb(String.valueOf(version.getId()), version.getName()));
                    }
                });


    }
}
