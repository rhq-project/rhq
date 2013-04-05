/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.components.table;

import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.authz.Permission.Target;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;

/**
 * This class allows for TableAction (ie. Button) enablement based on row selection and resource perm authorization
 * on the selected rows.  If possible it is recommended that the table be built using composites that include the
 * resource permission information already, as performance will be better.  This approach will require an async
 * authz call to the server on each refresh of the TableActions (i.e. each time selection changes in the Table).
 * 
 * For Global Perm authorization see {@link AuthorizedTableAction}.
 * 
 * @author Jay Shaughnessy
 */
public abstract class ResourceAuthorizedTableAction extends AbstractTableAction {

    private Table<?> table;
    private Permission requiredPermission;
    private RecordExtractor<Integer> extractor;
    private Collection<Integer> authorizedResourceIds;

    private Boolean isAuthorized;

    /**
     * @param table
     * @param enablement
     * @param requiredPermission if null no check is performed and authorization always passes
     * @param extractor
     */
    protected ResourceAuthorizedTableAction(Table<?> table, Permission requiredPermission,
        RecordExtractor<Integer> extractor) {
        this(table, TableActionEnablement.ALWAYS, requiredPermission, extractor);
    }

    /**
     * @param table
     * @param enablement
     * @param requiredPermission if null no check is performed and authorization always passes
     * @param extractor
     */
    protected ResourceAuthorizedTableAction(Table<?> table, TableActionEnablement enablement,
        Permission requiredPermission, RecordExtractor<Integer> extractor) {
        super(enablement);

        this.table = table;
        this.requiredPermission = requiredPermission;
        this.extractor = extractor;

        if (null != this.requiredPermission && Target.RESOURCE != this.requiredPermission.getTarget()) {
            throw new IllegalArgumentException("Does not support Global permission");
        }
    }

    @Override
    public boolean isEnabled(ListGridRecord[] selection) {
        // first make sure row selection enablement passes
        if (!super.isEnabled(selection)) {
            return false;
        }

        // if there is no required permission then no check is performed
        if (null == requiredPermission) {
            return true;
        }

        final Collection<Integer> selectedResourceIds = extractor.extract(selection);
        boolean isNewSelection = !selectedResourceIds.equals(this.authorizedResourceIds);

        if (isNewSelection) {
            isAuthorized = false;
            authorizedResourceIds = selectedResourceIds;
        } else {
            return isAuthorized;
        }

        // kick off the async auth check. return false initially and update when the async call returns

        GWTServiceLookup.getAuthorizationService().hasResourcePermission(requiredPermission, selectedResourceIds,
            new AsyncCallback<Boolean>() {
                public void onFailure(Throwable caught) {
                    CoreGUI.getErrorHandler().handleError("", caught);
                }

                public void onSuccess(Boolean result) {
                    boolean isStale = !selectedResourceIds.equals(authorizedResourceIds);
                    if (isStale) {
                        return;
                    }

                    isAuthorized = result;
                    table.refreshTableInfo();
                }

            });

        return isAuthorized;
    }
}
