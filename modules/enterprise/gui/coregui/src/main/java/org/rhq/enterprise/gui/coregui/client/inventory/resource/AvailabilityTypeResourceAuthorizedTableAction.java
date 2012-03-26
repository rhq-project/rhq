/*
 * RHQ Management Platform
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.rhq.enterprise.gui.coregui.client.inventory.resource;

import java.util.Collection;
import java.util.EnumSet;

import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.enterprise.gui.coregui.client.components.table.RecordExtractor;
import org.rhq.enterprise.gui.coregui.client.components.table.ResourceAuthorizedTableAction;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableActionEnablement;

/**
 * A {@link ResourceAuthorizedTableAction} that does further button enablement based on the {@link AvailabilityType}
 * of the selected resources. The button enables only if the selected records all have avail types in the specified set.
 *   
 * @author Jay Shaughnessy
 */
public abstract class AvailabilityTypeResourceAuthorizedTableAction extends ResourceAuthorizedTableAction {

    private EnumSet<AvailabilityType> availabilityTypes;
    private RecordExtractor<AvailabilityType> availExtractor;

    public AvailabilityTypeResourceAuthorizedTableAction(Table<?> table, TableActionEnablement enablement,
        EnumSet<AvailabilityType> availabilityTypes, Permission requiredPermission,
        RecordExtractor<AvailabilityType> availExtractor, RecordExtractor<Integer> idExtractor) {

        super(table, enablement, requiredPermission, idExtractor);

        this.availabilityTypes = availabilityTypes;
        this.availExtractor = availExtractor;
    }

    @Override
    public boolean isEnabled(ListGridRecord[] selection) {
        // first make sure row selection and auth enablement passes
        if (!super.isEnabled(selection)) {
            return false;
        }

        Collection<AvailabilityType> selectedAvailTypes = availExtractor.extract(selection);
        if (!this.availabilityTypes.containsAll(selectedAvailTypes)) {
            return false;
        }

        return true;
    }
}
