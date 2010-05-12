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
import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.CanvasItem;
import com.smartgwt.client.widgets.form.fields.LinkItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.bundle.Bundle;
import org.rhq.core.domain.bundle.BundleDeployment;
import org.rhq.core.domain.bundle.BundleResourceDeployment;
import org.rhq.core.domain.bundle.BundleVersion;
import org.rhq.core.domain.criteria.BundleCriteria;
import org.rhq.core.domain.criteria.BundleDeploymentCriteria;
import org.rhq.core.domain.criteria.BundleResourceDeploymentCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ViewId;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagEditorView;
import org.rhq.enterprise.gui.coregui.client.components.tagging.TagsChangedCallback;
import org.rhq.enterprise.gui.coregui.client.gwt.BundleGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;

/**
 * @author Greg Hinkle
 */
public class BundleDeploymentView extends VLayout implements BookmarkableView {
    private BundleGWTServiceAsync bundleService;

    private BundleDeployment deployment;
    private BundleVersion version;
    private Bundle bundle;

    private Canvas detail;

    private void viewBundleDeployment(BundleDeployment bundleDeployment, ViewId current) {

        this.deployment = bundleDeployment;
        this.version = bundleDeployment.getBundleVersion();
        this.bundle = bundleDeployment.getBundleVersion().getBundle();



        addMember(new HeaderLabel("<img src=\"" + Canvas.getImgURL("subsystems/bundle/BundleDeployment_24.png") + "\"/> " + deployment.getName()));



        DynamicForm form = new DynamicForm();
        form.setNumCols(4);

        LinkItem bundleName = new LinkItem("bundle");
        bundleName.setTitle("Bundle");
        bundleName.setTarget("#Bundles/Bundle/" + bundle.getId());
        bundleName.setValue(bundle.getName());



        CanvasItem tagItem = new CanvasItem("tag");
        tagItem.setShowTitle(false);
        TagEditorView tagEditor = new TagEditorView(version.getTags(), false, new TagsChangedCallback() {
            public void tagsChanged(HashSet<Tag> tags) {
                GWTServiceLookup.getTagService().updateBundleDeploymentTags(deployment.getId(), tags, new AsyncCallback<Void>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to update bundle deployment's tags", caught);
                    }

                    public void onSuccess(Void result) {
                        CoreGUI.getMessageCenter().notify(new Message("Bundle Deployment Tags updated", Message.Severity.Info));
                    }
                });
            }
        });
        tagItem.setCanvas(tagEditor);
        tagItem.setRowSpan(4);


        StaticTextItem deployed = new StaticTextItem("deployed","Deployed");
        deployed.setValue(new Date(deployment.getCtime()));


        LinkItem destinationGroup = new LinkItem("group");
        destinationGroup.setTarget("#ResourceGroup/" + deployment.getGroupDeployments().get(0).getGroup().getId());
        destinationGroup.setValue("Group");


        StaticTextItem path = new StaticTextItem("path","Path");
        path.setValue(deployment.getInstallDir());


        form.setFields(bundleName, tagItem, deployed, destinationGroup, path);

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

    private Table createDeploymentsTable() {
        Table table = new Table("Deployment Machines");

        ListGridField resource = new ListGridField("resource", "Resource");
        ListGridField status = new ListGridField("status", "Status");

        table.getListGrid().setFields(resource, status);

        ArrayList<ListGridRecord> records = new ArrayList<ListGridRecord>();
        for (BundleResourceDeployment rd : deployment.getResourceDeployments()) {
            ListGridRecord record = new ListGridRecord();
            record.setAttribute("resource", rd.getResource().getName());
            record.setAttribute("status",rd.getStatus().name());
            record.setAttribute("id",rd.getId());
            record.setAttribute("entity",rd);
            records.add(record);
        }

        table.getListGrid().setData(records.toArray(new ListGridRecord[records.size()]));


        table.getListGrid().addSelectionChangedHandler(new SelectionChangedHandler() {
            public void onSelectionChanged(SelectionEvent selectionEvent) {
                if (selectionEvent.getState()) {


                    BundleResourceDeployment bundleResourceDeployment =
                            (BundleResourceDeployment) selectionEvent.getRecord().getAttributeAsObject("entity");
                    BundleResourceDeploymentHistoryListView detailView =
                            new BundleResourceDeploymentHistoryListView(bundleResourceDeployment);

                    detail.addChild(detailView);
                    detail.animateShow(AnimationEffect.SLIDE);


/*
                    BundleResourceDeploymentCriteria criteria = new BundleResourceDeploymentCriteria();
                    criteria.addFilterId(selectionEvent.getRecord().getAttributeAsInt("id"));
                    criteria.fetchHistories(true);
                    criteria.fetchResource(true);
                    criteria.fetchBundleDeployment(true);
                    bundleService.findBundleResourceDeploymentsByCriteria(criteria, new AsyncCallback<PageList<BundleResourceDeployment>>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to load resource deployment history details",caught);
                        }

                        public void onSuccess(PageList<BundleResourceDeployment> result) {

                        }
                    });
*/


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
        criteria.fetchGroupDeployments(true);
        criteria.fetchTags(true);

        bundleService = GWTServiceLookup.getBundleService();
        bundleService.findBundleDeploymentsByCriteria(criteria,
                new AsyncCallback<PageList<BundleDeployment>>() {
                    public void onFailure(Throwable caught) {
                        CoreGUI.getErrorHandler().handleError("Failed to load budle version", caught);
                    }

                    public void onSuccess(PageList<BundleDeployment> result) {

                        final BundleDeployment deployment = result.get(0);

                        BundleCriteria bundleCriteria = new BundleCriteria();
                        bundleCriteria.addFilterId(deployment.getBundleVersion().getBundle().getId());
                        bundleService.findBundlesByCriteria(bundleCriteria, new AsyncCallback<PageList<Bundle>>() {
                            public void onFailure(Throwable caught) {
                                CoreGUI.getErrorHandler().handleError("Failed to find bundle",caught);
                            }

                            public void onSuccess(PageList<Bundle> result) {

                                deployment.getBundleVersion().setBundle(result.get(0));


                                BundleResourceDeploymentCriteria criteria = new BundleResourceDeploymentCriteria();
                                criteria.addFilterBundleDeploymentId(deployment.getId());
                                criteria.fetchHistories(true);
                                criteria.fetchResource(true);
                                criteria.fetchBundleDeployment(true);
                                bundleService.findBundleResourceDeploymentsByCriteria(criteria, new AsyncCallback<PageList<BundleResourceDeployment>>() {

                                    public void onFailure(Throwable caught) {
                                        CoreGUI.getErrorHandler().handleError("Failed to load deployment detail",caught);
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
