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

import java.io.Serializable;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Unwrap;
import org.jboss.seam.annotations.web.RequestParameter;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.auth.Subject;
import org.rhq.enterprise.server.alert.AlertDefinitionManagerLocal;

@AutoCreate
@Scope(ScopeType.PAGE)
@Name("alertDefinition")
public class AlertDefinitionComponent implements Serializable {

    @In("#{webUser.subject}")
    private Subject subject;
    @In
    private AlertDefinitionManagerLocal alertDefinitionManager;
    @RequestParameter("ad")
    private Integer alertDefinitionId;
    private AlertDefinition alertDefinition;

    @Unwrap
    public AlertDefinition lookupAlertDefinition() {
        if (this.alertDefinition == null) {
            if (this.alertDefinitionId != null) {
                this.alertDefinition = this.alertDefinitionManager.getAlertDefinitionById(this.subject , this.alertDefinitionId);
            } else {
                this.alertDefinition = new AlertDefinition();
                this.alertDefinition.setAlertDampening(new AlertDampening(AlertDampening.Category.NONE));
            }
        }

        return this.alertDefinition;
    }
}