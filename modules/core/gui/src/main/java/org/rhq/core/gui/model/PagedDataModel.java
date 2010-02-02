/*
 * RHQ Management Platform
 * Copyright (C) 2009-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.core.gui.model;

import org.ajax4jsf.model.DataVisitor;
import org.ajax4jsf.model.ExtendedDataModel;
import org.ajax4jsf.model.Range;
import org.ajax4jsf.model.SequenceRange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.util.OrderingField;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.richfaces.model.FilterField;
import org.richfaces.model.Modifiable;
import org.richfaces.model.Ordering;
import org.richfaces.model.SortField2;

import javax.el.Expression;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author Ian Springer
 * @author Joseph Marques
 *
 * @param <T> the type of domain object (e.g. org.rhq.core.domain.Resource) that this data model represents
 */
public class PagedDataModel<T> extends ExtendedDataModel implements Modifiable {
    private final Log log = LogFactory.getLog(this.getClass());

    protected Object currentRowKey;

    protected PageList<T> currentPage;
    protected LinkedHashMap<Object, T> currentPageDataByKey;

    private PagedDataProvider<T> dataProvider;

    /**
     * the default pagination and sort settings
     */
    private PageControl defaultPageControl;

    private SequenceRange currentSequenceRange;
    private List<OrderingField> currentOrderingFields = Collections.emptyList();

    private boolean sortRequested;

    public PagedDataModel(PagedDataProvider<T> dataProvider) {
        this.dataProvider = dataProvider;
        // TODO: get the default page control from some managed bean
        this.defaultPageControl = new PageControl(0, 15);
    }

    /**
     * @see org.ajax4jsf.model.ExtendedDataModel#getRowKey()
     */
    @Override
    public Object getRowKey() {
        return this.currentRowKey;
    }

    /**
     * @see org.ajax4jsf.model.ExtendedDataModel#setRowKey(Object)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setRowKey(Object key) {
        this.currentRowKey = key;
    }

    /**
     * @see org.ajax4jsf.model.ExtendedDataModel#walk(javax.faces.context.FacesContext,
     *      org.ajax4jsf.model.DataVisitor, org.ajax4jsf.model.Range,
     *      Object)
     */
    @Override
    public void walk(FacesContext facesContext, DataVisitor dataVisitor, Range range, Object argument)
            throws IOException {
        SequenceRange sequenceRange = (SequenceRange) range;
        boolean newPageRequested =
                (this.currentSequenceRange == null ||
                 sequenceRange.getFirstRow() != this.currentSequenceRange.getFirstRow() ||
                 sequenceRange.getRows() != this.currentSequenceRange.getRows());
        if (newPageRequested) {
            log.info("*** New page requested.");
        }
        this.currentSequenceRange = sequenceRange;

        if (newPageRequested || this.sortRequested) {
            // If this is the very first request for this data set, or if the user has specified new paging or sorting
            // settings, then we request data from data provider.
            PageControl pageControl = createPageControl();
            loadDataPage(pageControl);
            this.sortRequested = false;
        }

        // Let the visitor pay a visit to each item in the current page. this.currentPageDataByKey is a LinkedHashMap,
        // so we know we'll visit the nodes in the correct order.
        for (Object key : this.currentPageDataByKey.keySet()) {
            dataVisitor.process(facesContext, key, argument);
        }
    }

    /**
     * @see org.richfaces.model.Modifiable#modify(java.util.List, java.util.List)
     */
    public void modify(List<FilterField> filterFields, List<SortField2> sortFields) {
        if (!sortRequested) {
            List<OrderingField> orderingFields = toOrderingFields(sortFields);
            fixOrder(orderingFields);
            this.sortRequested = (!orderingFields.equals(this.currentOrderingFields));
            if (this.sortRequested) {
                log.info("*** Sort requested - order: " + orderingFields + ", previous order: " + this.currentOrderingFields);
            }            
            this.currentOrderingFields = orderingFields;
        }
    }

    /**
     * @see javax.faces.model.DataModel#isRowAvailable()
     */
    @Override
    public boolean isRowAvailable() {
        if (this.currentRowKey == null) {
            return false;
        }        
        if (this.currentPageDataByKey.containsKey(this.currentRowKey)) {
            return true;
        }
        // TODO: Should we load the current page here and look again for the current key - I don't think so.
        return false;
    }

    /**
     * @see javax.faces.model.DataModel#getRowData()
     */
    @Override
    public Object getRowData() {
        if (this.currentRowKey == null) {
            // TODO: Is it right to return null here?
            return null;
        }
        T dataObject = this.currentPageDataByKey.get(this.currentRowKey);
        if (dataObject == null) {
            loadDataPage(this.defaultPageControl);
        }
        return dataObject;
    }

    /**
     * @see javax.faces.model.DataModel#getRowCount()
     */
    @Override
    public int getRowCount() {
        if (this.currentPage == null) {
            loadDataPage(this.defaultPageControl);
        }
        return this.currentPage.getTotalSize();
    }

    /**
     * @see javax.faces.model.DataModel#setRowIndex(int)
     */
    @Override
    public void setRowIndex(final int rowIndex) {
        throw new UnsupportedOperationException("This method is not called by the RichFaces rich:extendedDataTable component.");
    }

    /**
     * @see javax.faces.model.DataModel#setWrappedData(Object)
     */
    @Override
    public void setWrappedData(final Object data) {
        throw new UnsupportedOperationException("This method is not called by the RichFaces rich:extendedDataTable component.");
    }

    /**
     * @see javax.faces.model.DataModel#getRowIndex()
     */
    @Override
    public int getRowIndex() {
        throw new UnsupportedOperationException("This method is not called by the rich:extendedDataTable component.");
    }

    /**
     * @see javax.faces.model.DataModel#getWrappedData()
     */
    @Override
    public Object getWrappedData() {
        throw new UnsupportedOperationException("This method is not called by the rich:extendedDataTable component.");
    }

    private PageControl createPageControl() {
        int pageSize = this.currentSequenceRange.getRows();
        int pageNumber = this.currentSequenceRange.getFirstRow() / pageSize;
        PageControl pageControl = new PageControl(pageNumber, pageSize);
        pageControl.getOrderingFields().addAll(this.currentOrderingFields);
        return pageControl;
    }

    /**
     * Convert RichFaces SortField2s to RHQ OrderingFields.
     *
     * @param sortFields the SortField2s to be converted
     *
     * @return the equivalent RHQ OrderingFields
     */
    private List<OrderingField> toOrderingFields(List<SortField2> sortFields) {
        if (sortFields == null) {
            sortFields = Collections.emptyList();
        }
        List<OrderingField> orderingFields = new ArrayList<OrderingField>(sortFields.size());
        for (SortField2 sortField : sortFields) {
            Expression expression = sortField.getExpression();
            String expressionString = expression.getExpressionString();
            String field;
            if (expression.isLiteralText()) {
                field = expressionString;
            } else {
                field = expressionString.replaceAll("[#|$]\\{", "").replaceAll("\\}", "");
            }
            Ordering ordering = sortField.getOrdering();
            PageOrdering pageOrdering = (ordering == Ordering.ASCENDING) ? PageOrdering.ASC : PageOrdering.DESC;
            OrderingField orderingField = new OrderingField(field, pageOrdering);
            orderingFields.add(orderingField);
        }
        return orderingFields;
    }
    
    private void loadDataPage(PageControl pageControl) {
        this.currentPage = getDataPage(pageControl);
        this.currentPageDataByKey = new LinkedHashMap<Object, T>(this.currentPage.size());
        for (T dataObject : this.currentPage) {
            Object key = getPrimaryKey(dataObject);
            this.currentPageDataByKey.put(key, dataObject);
        }
    }

    /**
     * Return the domain object's primary key - typically an Integer. Pretty much all RHQ domain objects use 'id' as the
     * field name for the primary key, so provide a default implementation that grabs the 'id' field to make things as
     * easy as possible for subclasses.
     *
     * @param object a domain object
     * @return U the domain object's primary key - typically an Integer
     */
    protected Object getPrimaryKey(T object) {
        Method method;
        try {
            method = object.getClass().getMethod("getId");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(object.getClass() + " does not define a public getId() method.");
        }
        try {
            return method.invoke(object);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke getId() on " + object + ".", e);
        }
    }

    private PageList<T> getDataPage(PageControl pageControl) {
        PageList<T> results = null;
        boolean tryQueryAgain = false;
        try {
            if (log.isTraceEnabled()) {
                //log.trace(pageControlView + ": " + pageControl);
            }
            if (pageControl.getPageSize() == PageControl.SIZE_UNLIMITED && pageControl.getPageNumber() != 0) {
                /*
                 * user is trying to get all of the results (SIZE_UNLIMITED), but not starting
                 * on the first page.  while this is technically allowable, it generally doesn't
                 * make all that much sense and was most likely due to a mistake upstream in the
                 * usage of the pagination / sorting framework.
                 */
                if (log.isTraceEnabled()) {
                    //log.trace(pageControlView + ": Forcing UNLIMITED PageControl's pageNumber to 0");
                }
                pageControl.setPageNumber(0);
                //setPageControl(pageControl);
            }

            // try the data fetch with the potentially changed (and persisted) PageControl object
            results = this.dataProvider.getDataPage(pageControl);
            if (log.isTraceEnabled()) {
                //log.trace(pageControlView + ": Successfully fetched page (first time)");
            }

            /*
             * do the results make sense?  there are certain times when no exception will be thrown but the
             * user interface won't be properly updated because of the multi-user environment.  if one user
             * is looking at some data set while another user deletes that entire data set, the current user
             * upon next sort or pagination action should realize that no results exist for his current page
             * and the view should be rendered to reflect that.  however, due to some defensive coding in the
             * RF components, the DataTable component does not see this change.  so, we have to explicitly
             * update the page control to get the view consistent with the backend once again.
             */
            if (results.getTotalSize() <= pageControl.getStartRow() || (results.isEmpty() && pageControl.getPageNumber() != 0)) {
                if (log.isTraceEnabled()) {
                    if (results.getTotalSize() <= pageControl.getStartRow()) {
                        //log.trace(pageControlView + ": Results size[" + results.getTotalSize()
                        //   + "] was less than PageControl startRow[" + pageControl.getStartRow() + "]");
                    } else {
                        //log.trace(pageControlView + ": Results were empty, but pageNumber was non-zero");
                    }
                }
                pageControl.reset(); // this will reset to page 1, but will not reset any ordering.
                if (log.isTraceEnabled()) {
                    //log.trace(pageControlView + ": resetting to " + pageControl);
                }
                tryQueryAgain = true;
            }
        } catch (Throwable t) {
            /*
             * known issues during pagination:
             *
             * 1) IndexOutOfBoundsException - trying to access a non-existent page
             * 2) QuerySyntaxException - when the token passed by the SortableColumnHeaderListener does not
             *                           match some alias on the underlying query that fetches the results
             *
             * but let's be extra careful and catch Throwable so as to handle any other exceptional case
             * we've yet to uncover.  however, we still want to return value data to the user, so let's
             * try the query once again; this time, we want the first page and will not specify any explicit
             * ordering (though the underlying SLSB may add a default ordering downstream).
             */
            pageControl.reset(); // this will reset to page 1, but will not reset any ordering.
            if (log.isTraceEnabled()) {
                //log.trace(pageControlView + ": Received error[" + t.getMessage() + "], resetting to " + pageControl);
            }
            tryQueryAgain = true;
        }

        // round 2 should be guaranteed because of use of defaultPageControl
        if (tryQueryAgain) {
            if (log.isTraceEnabled()) {
                //log.trace(pageControlView + ": Trying query again");
            }
            try {
                results = this.dataProvider.getDataPage(pageControl);
                if (log.isTraceEnabled()) {
                    //log.trace(pageControlView + ": Successfully fetched page (second time)");
                }
            } catch (Throwable t) {
                //log.error("Could not retrieve collection for " + pageControlView, t);
            }
        }

        if (results == null) {
            results = new PageList<T>();
        }

        return results;
    }

    private void fixOrder(List<OrderingField> orderingFields) {
        Collections.reverse(orderingFields);
        if (this.currentOrderingFields != null && orderingFields.size() > 1 &&
                orderingFields.size() == this.currentOrderingFields.size()) {
            for (int i = 1, sortFieldsSize = orderingFields.size(); i < sortFieldsSize; i++) {
                OrderingField orderingField = orderingFields.get(i);
                OrderingField currentOrderingField = this.currentOrderingFields.get(i);
                if (orderingField.getField().equals(currentOrderingField.getField()) &&
                    orderingField.getOrdering() != currentOrderingField.getOrdering()) {
                    orderingFields.remove(i);
                    orderingFields.add(0, orderingField);
                }
            }
        }
    }    
}