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
package org.rhq.enterprise.gui.coregui.client.components.table;

import com.google.gwt.user.client.History;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.data.SortSpecifier;
import com.smartgwt.client.types.AnimationEffect;
import com.smartgwt.client.widgets.AnimationCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickEvent;
import com.smartgwt.client.widgets.grid.events.CellDoubleClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.enterprise.gui.coregui.client.BookmarkableView;
import org.rhq.enterprise.gui.coregui.client.ViewPath;
import org.rhq.enterprise.gui.coregui.client.components.buttons.BackButton;

/**
 * @author Greg Hinkle
 */
public abstract class TableSection extends Table implements BookmarkableView {

    private VLayout detailsHolder;

    private Canvas detailsView;

    private String basePath;

    protected TableSection(String locatorId, String tableTitle) {
        super(locatorId, tableTitle);
    }

    protected TableSection(String locatorId, String tableTitle, Criteria criteria) {
        super(locatorId, tableTitle, criteria);
    }

    protected TableSection(String locatorId, String tableTitle, SortSpecifier[] sortSpecifiers) {
        super(locatorId, tableTitle, sortSpecifiers);
    }

    protected TableSection(String locatorId, String tableTitle, boolean autoFetchData) {
        super(locatorId, tableTitle, autoFetchData);
    }

    protected TableSection(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames) {
        super(locatorId, tableTitle, criteria, sortSpecifiers, excludedFieldNames);
    }

    protected TableSection(String locatorId, String tableTitle, Criteria criteria, SortSpecifier[] sortSpecifiers,
        String[] excludedFieldNames, boolean autoFetchData) {
        super(locatorId, tableTitle, criteria, sortSpecifiers, excludedFieldNames, autoFetchData);
    }

    @Override
    protected void onInit() {
        super.onInit();

        detailsHolder = new VLayout();
        detailsHolder.setWidth100();
        detailsHolder.setHeight100();
        detailsHolder.hide();

        addMember(detailsHolder);

    }

    @Override
    protected void onDraw() {
        super.onDraw();

        getListGrid().addCellDoubleClickHandler(new CellDoubleClickHandler() {
            @Override
            public void onCellDoubleClick(CellDoubleClickEvent event) {

                int id = event.getRecord().getAttributeAsInt("id");

                showDetails(id);
            }
        });

    }

    public void showDetails(int id) {
        History.newItem(basePath + "/" + id);
    }

    public abstract Canvas getDetailsView(int id);

    @Override
    public void renderView(ViewPath viewPath) {

        basePath = viewPath.getPathToCurrent();

        if (!viewPath.isEnd()) {

            int id = Integer.parseInt(viewPath.getCurrent().getPath());

            detailsView = getDetailsView(id);

            if (detailsView instanceof BookmarkableView) {

                ((BookmarkableView) detailsView).renderView(viewPath);
            }

            contents.animateHide(AnimationEffect.FADE, new AnimationCallback() {
                @Override
                public void execute(boolean b) {
                    detailsView.setWidth100();
                    detailsView.setHeight100();

                    detailsHolder.addMember(new BackButton(extendLocatorId("BackButton"), "Back to List", basePath));
                    detailsHolder.addMember(detailsView);
                    detailsHolder.animateShow(AnimationEffect.FADE);
                }
            });

        } else {
            if (contents != null) {
                contents.animateShow(AnimationEffect.FADE, new AnimationCallback() {
                    @Override
                    public void execute(boolean b) {
                        if (detailsHolder != null && detailsHolder.isVisible()) {
                            detailsHolder.animateHide(AnimationEffect.FADE);

                            for (Canvas child : detailsHolder.getMembers()) {
                                detailsHolder.removeMember(child);
                            }
                        }
                    }
                });
            }
        }
    }
}
