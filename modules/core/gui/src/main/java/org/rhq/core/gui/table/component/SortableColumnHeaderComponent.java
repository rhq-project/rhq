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

import org.ajax4jsf.event.AjaxEvent;
import org.ajax4jsf.event.AjaxListener;
import org.ajax4jsf.event.AjaxSource;
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
import java.util.ArrayList;
import java.util.List;

/**
 * @author Ian Springer
 * @author Joseph Marques 
 */
public class SortableColumnHeaderComponent extends HtmlCommandLink implements AjaxSource {
    public static final String COMPONENT_TYPE = "org.rhq.SortableColumnHeader";
    public static final String COMPONENT_FAMILY = "org.rhq.SortableColumnHeader";

    private String sortBy;
    private List<AjaxListener> ajaxListeners;

    public SortableColumnHeaderComponent() {
        super();
        SortableColumnHeaderListener listener = new SortableColumnHeaderListener();
        addActionListener(listener);
        this.ajaxListeners = new ArrayList<AjaxListener>(1);
        addAjaxListener(listener);
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


    public void addAjaxListener(AjaxListener listener) {
        this.ajaxListeners.add(listener);
    }

    public AjaxListener[] getAjaxListeners() {
        return this.ajaxListeners.toArray(new AjaxListener[this.ajaxListeners.size()]);
    }

    public void removeAjaxListener(AjaxListener listener) {
        this.ajaxListeners.remove(listener);
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


    public class SortableColumnHeaderListener implements ActionListener, AjaxListener {
        public void processAction(ActionEvent event) throws AbortProcessingException {            
            SortableColumnHeaderComponent sortableColumnHeader = (SortableColumnHeaderComponent)event.getComponent();
            sort(sortableColumnHeader);
        }

        public void processAjax(AjaxEvent event) {
            SortableColumnHeaderComponent sortableColumnHeader = (SortableColumnHeaderComponent)event.getComponent();
            sort(sortableColumnHeader);
        }

        private void sort(SortableColumnHeaderComponent sortableColumnHeader) {
            String sortBy = sortableColumnHeader.getSortBy();

            UIData data = FacesComponentUtility.getAncestorOfType(sortableColumnHeader, UIData.class);
            PagedListDataModel<?> model = (PagedListDataModel<?>) data.getValue();

            PageControl pageControl = model.getPageControl();
            pageControl.sortBy(sortBy);
            // Even though its the same PageControl instance, call setPageControl() so the updated version gets
            // persisted.
            model.setPageControl(pageControl);
        }
    }
}