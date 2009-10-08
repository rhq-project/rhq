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
package org.rhq.enterprise.gui.common.table;

import javax.faces.component.html.HtmlCommandButton;
import javax.faces.context.FacesContext;

public class SelectCommandButton extends HtmlCommandButton {
    private String target;
    private String low;
    private String high;

    public static final String COMPONENT_TYPE = "org.jboss.on.SelectCommandButton";
    public static final String COMPONENT_FAMILY = "org.jboss.on.SelectCommandButton";

    public SelectCommandButton() {
        super();
        // BUG: see JBNADM-1386
        //setDisabled(true);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    /*
     * The button must be conditional on something, so this is required and points to the name attribute of the Select
     * component that this button should be registered against
     */
    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    /*
     * This is a conditional button, so if 'low' is not specified the renderer will assume 1 by default
     */
    public String getLow() {
        return low;
    }

    public void setLow(String low) {
        this.low = low;
    }

    /*
     * It's perfectly valid to have a conditional button operate on an arbitrary number of selections, so 'high' is not
     * required
     */
    public String getHigh() {
        return high;
    }

    public void setHigh(String high) {
        this.high = high;
    }

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[4];
        values[0] = super.saveState(context);
        values[1] = target;
        values[2] = low;
        values[3] = high;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        target = (String) values[1];
        low = (String) values[2];
        high = (String) values[3];
    }
}