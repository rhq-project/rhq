/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.enterprise.gui.legacy.taglib.display;

import java.util.List;
import javax.servlet.jsp.PageContext;

/**
 * This class provides some basic functionality for all objects which serve as decorators for the objects in the List
 * being displayed.
 */

public abstract class Decorator implements Cloneable {
    private PageContext ctx = null;
    private List list = null;

    private Object obj = null;
    private int viewIndex = -1;
    private int listIndex = -1;

    public Decorator() {
    }

    public void init(PageContext ctx, List list) {
        this.ctx = ctx;
        this.list = list;
    }

    public String initRow(Object obj, int viewIndex, int listIndex) {
        this.obj = obj;
        this.viewIndex = viewIndex;
        this.listIndex = listIndex;
        return "";
    }

    public String finishRow() {
        return "";
    }

    public void finish() {
    }

    public void setPageContext(PageContext context) {
        this.ctx = context;
    }

    public PageContext getPageContext() {
        return this.ctx;
    }

    public List getList() {
        return this.list;
    }

    public Object getObject() {
        return this.obj;
    }

    public int getViewIndex() {
        return this.viewIndex;
    }

    public int getListIndex() {
        return this.listIndex;
    }

    public void release() {
        ctx = null;
        list = null;
        obj = null;
        viewIndex = -1;
        listIndex = -1;
    }
}