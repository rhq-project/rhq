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
package org.rhq.enterprise.gui.coregui.client;

import java.util.ArrayList;

/**
 * @author Greg Hinkle
 */
public class ViewPath {

    private ArrayList<ViewId> viewPath = new ArrayList<ViewId>();

    private int index = 0;

    public ViewPath() {
    }

    public ViewPath(String pathString) {
        for (String pe : pathString.split("/")) {
            viewPath.add(new ViewId(pe));
        }
    }

    public ArrayList<ViewId> getViewPath() {
        return viewPath;
    }


    public ViewPath next() {
        index++;
        return this;
    }

    public ViewId getViewForIndex(int index) {
        return viewPath.get(index);
    }

    public ViewId getCurrent() {
        if (index >= viewPath.size()) {
            return null;
        } else {
            return viewPath.get(index);
        }
    }

    public ViewId getNext() {
        return viewPath.get(index + 1);
    }

    public boolean isEnd() {
        return viewPath.size() <= index;
    }

    public boolean isCurrent(ViewId providedViewId) {
        return !(isEnd() || providedViewId == null || !getCurrent().equals(providedViewId));
    }


    public boolean isNext(ViewId providedViewId) {
        return ((index + 1) < viewPath.size() && providedViewId != null && getNext().equals(providedViewId));
    }

    public boolean isNextEnd() {
        return viewPath.size() <= index+1;
    }

    public int viewsLeft() {
        return viewPath.size() - index - 1;
    }
}
