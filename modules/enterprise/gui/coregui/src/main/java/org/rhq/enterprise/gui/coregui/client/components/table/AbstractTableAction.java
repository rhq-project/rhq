/*
 * RHQ Management Platform
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * @author Ian Springer
 */
public abstract class AbstractTableAction implements TableAction {
    private TableActionEnablement enablement;

    protected AbstractTableAction() {
        this(TableActionEnablement.ALWAYS);
    }

    protected AbstractTableAction(TableActionEnablement enablement) {
        this.enablement = enablement;
    }

    public boolean isEnabled(ListGridRecord[] selection) {
        int count = selection.length;
        boolean enabled;
        switch (this.enablement) {
        case NEVER:
            enabled = false;
            break;
        case SINGLE:
            enabled = (count == 1);
            break;
        case MULTIPLE:
            enabled = (count > 1);
            break;
        case ANY:
            enabled = (count >= 1);
            break;
        case ALWAYS:
            enabled = true;
            break;
        default:
            throw new IllegalStateException("Unsupported SelectionEnablement: " + enablement.name());
        }
        return enabled;
    }

    public abstract void executeAction(ListGridRecord[] selection, Object actionValue);
}
