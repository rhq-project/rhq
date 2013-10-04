/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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

package org.rhq.coregui.client.components.table;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.criteria.RoleCriteria;
import org.rhq.core.domain.util.PageList;
import org.rhq.coregui.client.CoreGUI;
import org.rhq.coregui.client.UserSessionManager;
import org.rhq.coregui.client.gwt.GWTServiceLookup;

/**
 * This is used only to determine enablement of certain bundle buttons (new, deploy). It activates the button if the user
 * has any role with the specified permissions.  It requires only one async call and then uses the cached value.
 * 
 * For strict Global Perm authorization see {@link AuthorizedTableAction}.
 * 
 * @author Jay Shaughnessy
 */
public abstract class RoleAuthorizedTableAction extends AbstractTableAction {

    private Table<?> table;
    List<Permission> permissions;
    private Boolean isAuthorized;

    protected RoleAuthorizedTableAction(Table<?> table, Permission... permissions) {
        this.table = table;
        this.permissions = Arrays.asList(permissions);
    }

    @Override
    public boolean isEnabled(ListGridRecord[] selection) {
        // first make sure row selection enablement passes
        if (!super.isEnabled(selection)) {
            return false;
        }

        if (null != isAuthorized) {
            return isAuthorized.booleanValue();
        }

        // kick off the async auth check. return false initially and update when the async call returns
        RoleCriteria criteria = new RoleCriteria();
        Subject subject = UserSessionManager.getSessionSubject();
        criteria.addFilterSubjectId(subject.getId());
        criteria.addFilterPermissions(permissions);
        GWTServiceLookup.getRoleService().findRolesByCriteria(criteria, new AsyncCallback<PageList<Role>>() {
            public void onFailure(Throwable caught) {
                CoreGUI.getErrorHandler().handleError("", caught);
            }

            public void onSuccess(PageList<Role> result) {
                isAuthorized = !result.isEmpty();
                table.refreshTableInfo();
            }
        });

        return false;
    }
}
