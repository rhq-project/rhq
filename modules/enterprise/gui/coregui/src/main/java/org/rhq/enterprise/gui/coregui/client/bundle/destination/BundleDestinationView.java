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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleDeploymentStatus;
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

/**
 * @author Greg Hinkle
 */
public class BundleDestinationView extends VLayout implements BookmarkableView {
    private BundleGWTServiceAsync bundleService;

    private BundleDestination destination;
    private Bundle bundle;

    private Canvas detail;


    public BundleDestinationView() {
        setWidth100();
        setHeight100();
        setMargin(10);
    }

    private void viewBundleDestination(BundleDestination bundleDestination, ViewId current) {

        this.destination = bundleDestination;
        this.bundle = bundleDestination.getBundle();

        addMember(new BackButton("Back to Bundle: " + bundle.getName(),"Bundles/Bundle/" + bundle.getId()));

        addMember(new HeaderLabel(Canvas.getImgURL("subsystems/bundle/BundleDestination_24.png"), destination.getName()));

        DynamicForm form = new DynamicForm();
        form.setWidth100();
        form.setNumCols(4);
        form.setColWidths("20%","30%","25%","25%");

        LinkItem bundleName = new LinkItem("bundle");
        bundleName.setTitle("Bundle");
        bundleName.setValue("#Bundles/Bundle/" + bundle.getId());
        bundleName.setLinkTitle(bundle.getName());
        bundleName.setTarget("_self");

        CanvasItem tagItem = new CanvasItem("tag");
        tagItem.setShowTitle(false);
        TagEditorView tagEditor = new TagEditorView(destination.getTags(), false, new TagsChangedCallback() {
            public void tagsChanged(HashSet<Tag> tags) {
                GWTServiceLookup.getTagService().updateBundleDestinationTags(destination.getId(), tags,
                        new AsyncCallback<Void>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to update bundle destination's tags", caught);
                            }

                            public void onSuccess(Void result) {
                                CoreGUI.getMessageCenter().notify(
                                        new Message("Bundle Destination Tags updated", Message.Severity.Info));
                            }
                        });
            }
        });
        tagEditor.setVertical(true);
        tagItem.setCanvas(tagEditor);
        tagItem.setRowSpan(4);

        CanvasItem actionItem = new CanvasItem("actions");
        actionItem.setShowTitle(false);
        actionItem.setCanvas(getActionLayout());
        actionItem.setRowSpan(4);

        StaticTextItem created = new StaticTextItem("created", "Created");
        created.setValue(new Date(destination.getCtime()));

        LinkItem destinationGroup = new LinkItem("group");
        destinationGroup.setTitle("Group");
        destinationGroup.setValue("#ResourceGroup/" + destination.getGroup().getId());
        destinationGroup.setLinkTitle(destination.getGroup().getName());
        destinationGroup.setTarget("_self");

        StaticTextItem path = new StaticTextItem("path", "Path");
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

    private Canvas getActionLayout() {
        VLayout actionLayout = new VLayout();
        actionLayout.setMembersMargin(10);
        IButton deployButton = new IButton("Deploy");
        deployButton.setIcon("subsystems/bundle/BundleAction_Deploy_16.png");
        deployButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                new BundleDeployWizard(destination).startBundleWizard();
            }
        });
        actionLayout.addMember(deployButton);

        IButton revertButton = new IButton("Revert");
        revertButton.setIcon("subsystems/bundle/BundleAction_Revert_16.png");
        revertButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent clickEvent) {
                new BundleRevertWizard(destination).startBundleWizard();
            }
        });
        actionLayout.addMember(revertButton);
        return actionLayout;
    }

    private Table createDeploymentsTable() {

        Criteria criteria = new Criteria();
        criteria.addCriteria("bundleDestinationId", destination.getId());

        return new BundleDeploymentListView(criteria);


       /* Table table = new Table("Deployment History");

        ListGridField name = new ListGridField("name", "Name");
        name.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#Bundles/Bundle/" + bundle.getId() + "/deployments/"
                        + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
            }
        });

        ListGridField version = new ListGridField("version", "Version");
        ListGridField description = new ListGridField("description", "Description");
        ListGridField installDate = new ListGridField("installDate", "Install Date");
        ListGridField status = new ListGridField("status", "Status");
        HashMap<String, String> statusIcons = new HashMap<String, String>();
        statusIcons.put(BundleDeploymentStatus.IN_PROGRESS.name(), "subsystems/bundle/install-loader.gif");
        statusIcons.put(BundleDeploymentStatus.FAILURE.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.MIXED.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.WARN.name(), "subsystems/bundle/Warning_11.png");
        statusIcons.put(BundleDeploymentStatus.SUCCESS.name(), "subsystems/bundle/Ok_11.png");
        status.setValueIcons(statusIcons);
        status.setValueIconHeight(11);
        status.setWidth(80);


        table.getListGrid().setFields(name, version, description, installDate, status);

        ArrayList<ListGridRecord> records = new ArrayList<ListGridRecord>();
        for (BundleDeployment rd : destination.getDeployments()) {
            ListGridRecord record = new ListGridRecord();

            record.setAttribute("name", rd.getName());
            record.setAttribute("description", rd.getDescription());
            record.setAttribute("bundleId", bundle.getId());
            record.setAttribute("version", rd.getBundleVersion().getName());
            record.setAttribute("status", rd.getStatus().name());
            record.setAttribute("id", rd.getId());
            record.setAttribute("entity", rd);
            record.setAttribute("installDate", new Date(rd.getCtime()));
            records.add(record);
        }

        table.getListGrid().setData(records.toArray(new ListGridRecord[records.size()]));

        return table;*/
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
                CoreGUI.getErrorHandler().handleError("Failed to load bundle destiation", caught);
            }

            public void onSuccess(PageList<BundleDestination> result) {

                final BundleDestination destination = result.get(0);

                viewBundleDestination(destination, viewPath.getCurrent());

            }
        });

    }

}