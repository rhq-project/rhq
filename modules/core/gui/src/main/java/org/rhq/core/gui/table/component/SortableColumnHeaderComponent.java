/*
 * RHQ Management Platform
 * Copyright (C) 2009-2010 Red Hat, Inc.
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
package org.rhq.core.gui.table.component;

import org.rhq.core.domain.util.PageControl;
import org.rhq.core.gui.table.model.PagedListDataModel;
import org.rhq.core.gui.util.FacesComponentUtility;

import javax.el.ValueExpression;
import javax.faces.component.UIData;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

/**
 * @author Ian Springer
 * @author Joseph Marques 
 */
public class SortableColumnHeaderComponent extends HtmlCommandLink {
    public static final String COMPONENT_TYPE = "org.rhq.SortableColumnHeader";
    public static final String COMPONENT_FAMILY = "org.rhq.SortableColumnHeader";

    private String sortBy;

    public SortableColumnHeaderComponent() {
        super();
        addActionListener(new SortableColumnHeaderActionListener());

        // TODO - Do we need this? I don't think so.
        //setActionExpression(FacesExpressionUtility.createMethodExpression("#{TableSorter.obtainFromOutcome}", String.class, new Class[] {}));
    }

    public String getSortBy() {
        ValueExpression valueExp = getValueExpression("sortBy");
        if (valueExp != null) {
            this.sortBy = (java.lang.String) valueExp.getValue(getFacesContext().getELContext());
        }
        return this.sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

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
        values[1] = sortBy;

        return values;
    }

    public void restoreState(FacesContext context, Object state) {
        values = (Object[]) state;

        super.restoreState(context, values[0]);
        this.sortBy = (String) values[1];
    }

    private class SortableColumnHeaderActionListener implements ActionListener {
        public void processAction(ActionEvent event) throws AbortProcessingException {            
            SortableColumnHeaderComponent sortableColumnHeader = (SortableColumnHeaderComponent)event.getComponent();
            String sortBy = sortableColumnHeader.getSortBy();

            UIData table = FacesComponentUtility.getAncestorOfType(sortableColumnHeader, UIData.class);
            PagedListDataModel<?> model = (PagedListDataModel<?>) table.getValue();

            // work-around to bypass stale data model caused by a4j:keepAlive for the PagedDataTableUIBean
            //PageControlView pageControlView = model.getPageControlView();
            //PagedDataTableUIBean pagedDataTableUIBean = pageControlView.getPagedDataTableUIBean();
            //pagedDataTableUIBean.setDataModel(null);

            PageControl pageControl = model.getPageControl();
            pageControl.sortBy(sortBy);
            model.setPageControl(pageControl);

        }
    }
}