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
package org.rhq.enterprise.gui.common.sorting;

import javax.el.ValueExpression;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionListener;
import org.rhq.core.gui.util.FacesExpressionUtility;

public class SortableColumnHeader extends HtmlCommandLink {
    private String sort;

    public SortableColumnHeader() {
        super();
        addActionListener(new SortableColumnHeaderListener());

        setActionExpression(FacesExpressionUtility.createMethodExpression("#{TableSorter.obtainFromOutcome}",
            String.class, new Class[] {}));
    }

    public String getSort() {
        ValueExpression valueExp = getValueExpression("sort");
        if (valueExp != null) {
            sort = (java.lang.String) valueExp.getValue(getFacesContext().getELContext());
        }

        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    /* to be a valid component */

    public static final String COMPONENT_TYPE = "org.jboss.on.SortableColumnHeader";
    public static final String COMPONENT_FAMILY = "org.jboss.on.SortableColumnHeader";

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    private Object[] values;

    public Object saveState(FacesContext context) {
        for (ActionListener listener : getActionListeners()) {
            removeActionListener(listener);
        }

        if (values == null) {
            values = new Object[2];
        }

        values[0] = super.saveState(context);
        values[1] = sort;

        return values;
    }

    public void restoreState(FacesContext context, Object state) {
        values = (Object[]) state;

        super.restoreState(context, values[0]);
        this.sort = (String) values[1];
    }
}