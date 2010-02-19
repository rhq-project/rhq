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
package org.rhq.enterprise.gui.alert;

import java.util.ArrayList;
import java.util.List;
import javax.faces.model.SelectItem;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.rhq.core.domain.alert.AlertPriority;

@Scope(ScopeType.APPLICATION)
@Name("alertPrioritiesUIBean")
public class AlertPrioritiesUIBean {

    private List<SelectItem> priorities;

    public List<SelectItem> getPriorities() {
        return priorities;
    }

    @Create
    public void lookupPriorities() {
        this.priorities = new ArrayList<SelectItem>();

        for (AlertPriority priority : AlertPriority.values()) {
            this.priorities.add(new SelectItem(priority.name(), priority.getDisplayName()));
        }
    }
}