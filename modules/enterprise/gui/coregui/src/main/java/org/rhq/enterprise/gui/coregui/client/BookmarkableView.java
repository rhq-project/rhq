/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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

/**
 * A view that is responsible for rendering the remaining view IDs in the view path (aka history item, aka bookmark)
 * being rendered. For example, for the view path "Administration/Security/Users/10001", the UsersView class represents
 * the "Administration/Security/Users" portion of the view. UsersView implements BookmarkableView, since it is
 * responsible for rendering the remaining ID(s) in the view path (in this example, just "10001"). The BookmarkableView
 * whose renderView() method rendered UsersView (i.e. UsersView's parent view) is responsible for advancing the
 * ViewPath's current view ID pointer to point at the ID following "Users" (the ID corresponding to UsersView) -
 * "10001". For example, UsersView's parent view's renderView() implementation would look something like this:
 *
 * <code>
 * public void renderView(ViewPath viewPath) {
 *      // passed-in view path is "Administration/Security/Users/10001"
 *      // and current view ID is here --------------------^
 *
 *      // the current view ID is the view we're being asked to render.
 *      // create the view and render it.
 *      String viewId = viewPath.getCurrent().getPath();
 *      Canvas view;
 *      if (viewId.equals("Users")) {
 *          view = new UsersView();
 *      } else {
 *          // handle other recognized view IDs
 *      }
 *      ourView.addMember(view);
 *
 *      // check if UsersView implements BookmarkableView, and
 *      // if so, advance the view ID pointer to "10001" and then call
 *      // its renderView() method so it can render that view ID.
 *      if (view instanceof BookmarkableView) {
 *           viewPath.next();
 *           //     "Administration/Security/Users/10001"
 *           // current view ID now points here ---^
 *           ((BookmarkableView)view).renderView(viewPath);
 *      }
 * }
 * </code>
 *
 * <b>NOTE:</b> If the view implementing BookmarkableView also implements {@link RefreshableView}, the
 * {@link #renderView} implementation is responsible for invoking {@link RefreshableView#refresh()} to ensure the
 * view's data is fresh.
 *
 * @author Ian Springer
 */
public interface BookmarkableView {

    /**
     * Render the current view ID of the specified view path.
     *
     * @param viewPath the view path whose current view ID is to be rendered
     */
    void renderView(ViewPath viewPath);

}