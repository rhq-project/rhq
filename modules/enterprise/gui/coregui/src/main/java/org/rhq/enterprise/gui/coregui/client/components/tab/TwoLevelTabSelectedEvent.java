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
package org.rhq.enterprise.gui.coregui.client.components.tab;

import com.google.gwt.event.shared.GwtEvent;
import com.smartgwt.client.widgets.Canvas;

/**
 * @author Greg Hinkle
 */
public class TwoLevelTabSelectedEvent extends GwtEvent<TwoLevelTabSelectedHandler> {

    public static final GwtEvent.Type<TwoLevelTabSelectedHandler> TYPE = new Type<TwoLevelTabSelectedHandler>();

    private String id;
    private String subTabId;

    private int tabNum;

    private Canvas subTabPane;

    public TwoLevelTabSelectedEvent(String id, String subTabId, int tabNum, Canvas subTabPane) {
        this.id = id;
        this.subTabId = subTabId;
        this.tabNum = tabNum;
        this.subTabPane = subTabPane;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubTabId() {
        return subTabId;
    }

    public void setSubTabId(String subTabId) {
        this.subTabId = subTabId;
    }

    public int getTabNum() {
        return tabNum;
    }

    public void setTabNum(int tabNum) {
        this.tabNum = tabNum;
    }

    public Canvas getSubTabPane() {
        return subTabPane;
    }

    public void setSubTabPane(Canvas subTabPane) {
        this.subTabPane = subTabPane;
    }


    @Override
    public Type<TwoLevelTabSelectedHandler> getAssociatedType() {
        return TYPE;
    }

    @Override
    protected void dispatch(TwoLevelTabSelectedHandler handler) {
        handler.onTabSelected(this);
    }


}
