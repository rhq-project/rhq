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
package org.rhq.enterprise.gui.inventory.resource;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import org.rhq.core.gui.util.FacesComponentUtility;

public class ResourceLineageComponent extends UIComponentBase {
    public static final String COMPONENT_TYPE = "org.jboss.on.ResourceLineage";
    public static final String COMPONENT_FAMILY = "org.jboss.on.ResourceLineage";

    private static final String RESOURCE_ID_ATTRIBUTE = "resourceId";

    int resourceId;

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public int getResourceId() {
        if (this.resourceId == 0) {
            this.resourceId = FacesComponentUtility.getExpressionAttribute(this, RESOURCE_ID_ATTRIBUTE, Integer.class);
        }

        return this.resourceId;
    }

    public void setResourceId(int resourceId) {
        this.resourceId = resourceId;
    }

    private Object[] stateValues;

    public Object saveState(FacesContext facesContext) {
        if (this.stateValues == null) {
            this.stateValues = new Object[2];
        }

        this.stateValues[0] = super.saveState(facesContext);
        this.stateValues[1] = this.resourceId;
        return this.stateValues;
    }

    public void restoreState(FacesContext facesContext, Object stateValues) {
        this.stateValues = (Object[]) stateValues;
        super.restoreState(facesContext, this.stateValues[0]);
        this.resourceId = (Integer) this.stateValues[1];
    }
}