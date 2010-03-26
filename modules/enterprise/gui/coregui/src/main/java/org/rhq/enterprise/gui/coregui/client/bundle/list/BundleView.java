/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.bundle.list;

import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import com.smartgwt.client.widgets.tab.TabSet;

import org.rhq.core.domain.bundle.composite.BundleWithLatestVersionComposite;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.HeaderLabel;

public class BundleView extends VLayout {

    private int bundleBeingViewed = 0;
    private Label message = new Label("Select a bundle...");
    private VLayout canvas;
    private HeaderLabel headerLabel;
    private StaticTextItem descriptionItem;
    private StaticTextItem latestVersionItem;

    public BundleView() {
        super();
        setPadding(10);
        setOverflow(Overflow.AUTO);
    }

    @Override
    protected void onInit() {
        super.onInit();
        addMember(message);
        addMember(buildCanvas());
        canvas.hide();
    }

    private Canvas buildCanvas() {
        canvas = new VLayout();

        headerLabel = new HeaderLabel("<bundle name>");

        TabSet tabs = new TabSet();
        Tab summaryTab = createSummaryTab();
        tabs.addTab(summaryTab);

        Tab versionsTab = createVersionsTab();
        tabs.addTab(versionsTab);

        Tab deploymentsTab = createDeploymentsTab();
        tabs.addTab(deploymentsTab);

        canvas.addMember(headerLabel);
        canvas.addMember(tabs);
        return canvas;
    }

    private Tab createDeploymentsTab() {
        return new Tab("Deployments");
    }

    private Tab createVersionsTab() {
        return new Tab("Versions");
    }

    private Tab createSummaryTab() {
        Tab summaryTab = new Tab("Summary");

        DynamicForm form = new DynamicForm();
        form.setPadding(10);

        descriptionItem = new StaticTextItem("description", "Description");
        descriptionItem.setTitleAlign(Alignment.LEFT);
        descriptionItem.setAlign(Alignment.LEFT);
        descriptionItem.setWrap(false);
        descriptionItem.setValue("");

        latestVersionItem = new StaticTextItem("latestVersion", "Latest Version");
        latestVersionItem.setTitleAlign(Alignment.LEFT);
        latestVersionItem.setAlign(Alignment.LEFT);
        latestVersionItem.setWrap(false);
        latestVersionItem.setValue("");

        form.setFields(descriptionItem, latestVersionItem);
        summaryTab.setPane(form);

        return summaryTab;
    }

    public void viewRecord(Record record) {
        final BundleWithLatestVersionComposite object;
        object = (BundleWithLatestVersionComposite) record.getAttributeAsObject("object");

        if (bundleBeingViewed != object.getBundleId()) {
            headerLabel.setContents(object.getBundleName());
            latestVersionItem.setValue(object.getLatestVersion());
            descriptionItem.setValue(object.getBundleDescription());
        }

        try {
            message.hide();
            canvas.show();
            markForRedraw();
        } catch (Throwable t) {
            CoreGUI.getErrorHandler().handleError("Cannot view bundle record", t);
        }
    }

    public void viewNone() {
        message.show();
        canvas.hide();
        markForRedraw();
    }
}
