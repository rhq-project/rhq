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
package org.rhq.enterprise.gui.common.paging;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.DataModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.util.FacesContextUtility;
import org.rhq.enterprise.gui.common.framework.PagedDataTableUIBean;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.util.HibernatePerformanceMonitor;

/**
 * <p>A special type of JSF DataModel to allow a datatable and datascroller to page through a large set of data without
 * having to hold the entire set of data in memory at once.</p>
 *
 * <p>Any time a managed bean wants to avoid holding an entire dataset, the managed bean should declare an inner class
 * which extends this class and implements the fetchData method. This method is called as needed when the table requires
 * data that isn't available in the current data page held by this object.</p>
 *
 * <p>This does require the managed bean (and in general the business method that the managed bean uses) to provide the
 * data wrapped in a PageList object that provides info on the full size of the dataset.</p>
 *
 * <p>Adapted from - http://wiki.apache.org/myfaces/WorkingWithLargeTables</p>
 *
 * @param <T> the data type to be stored in the dataset
 * @author Joseph Marques
 */
public abstract class PagedListDataModel<T> extends DataModel {

    private final Log log = LogFactory.getLog(PagedListDataModel.class);

    private int currentRowIndex;
    private PageControlView pageControlView;
    private PageList<T> pageList;
    private String lastViewId;
    private String beanName;

    /**
     * Create a datamodel that pages through the data showing the specified number of rows on each page.
     *
     * @param pageSize the total number of pages in the full dataset
     */
    public PagedListDataModel(PageControlView view, String beanName) {
        super();
        this.currentRowIndex = -1;
        this.pageControlView = view;
        this.pageList = null;
        this.beanName = beanName;
    }

    /**
     * Not used in this class; data is fetched via a callback to the {@link #getDataPage(PageControl)} method rather 
     * than by explicitly assigning a list.
     *
     * @param  o unused
     *
     * @throws UnsupportedOperationException thrown when this method is called
     */
    @Override
    public void setWrappedData(Object o) {
        throw new UnsupportedOperationException("setWrappedData");
    }

    @Override
    public int getRowIndex() {
        return this.currentRowIndex;
    }

    /**
     * Specify what the "current row" within the dataset is. Note that the UIData component will repeatedly call this
     * method followed by getRowData to obtain the objects to render in the table.
     *
     * @param index current row index, indexes start at 0
     */
    @Override
    public void setRowIndex(int index) {
        this.currentRowIndex = index;
    }

    /**
     * Return the total number of rows of data available (not just the number of rows in the current page!).
     *
     * @return number of rows in the full dataset
     */
    @Override
    public int getRowCount() {
        return getPage().getTotalSize();
    }

    /**
     * Return a PageList object; if one is not currently available then fetch one. Note that this doesn't ensure that
     * the PageList returned includes the current {@link #getRowIndex()} row; see {@link #getRowData()}.
     *
     * @return the current page
     */
    private PageList<T> getPage() {
        // ensure page exists - first time going to this view
        if (pageList == null) {
            PageControl pageControl = getPageControl();
            pageList = getDataPage(pageControl);
        }

        // the user has previously been to this view, and is returning
        else if (!FacesContext.getCurrentInstance().getViewRoot().getViewId().equals(lastViewId)) {
            PageControl pageControl = getPageControl();
            lastViewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
            pageList = getDataPage(pageControl);
        }

        // no longer needed because changes to the PageControl as a result of getting results are repersisted automatically
        //setPageControl(pageList.getPageControl());

        return pageList;
    }

    /**
     * Return the object corresponding to the current {@link #getRowIndex()}. If the PageList object currently cached
     * doesn't include that index then {@link #getDataPage(PageControl)} is called to retrieve the appropriate page.
     *
     * @return the row data that corresponds to {@link #getRowIndex()}
     *
     * @throws IllegalArgumentException if the {@link #getRowIndex()} is outside the range of the dataset size
     */
    @Override
    public Object getRowData() {
        if (this.currentRowIndex < 0) {
            throw new IllegalArgumentException("Invalid rowIndex for PagedListDataModel; not within page");
        }

        PageControl pageControl = getPageControl();

        int startRow = pageControl.getStartRow();
        int endRow;

        if (pageControl.getPageSize() == PageControl.SIZE_UNLIMITED) {
            endRow = Integer.MAX_VALUE;
        } else {
            int nRows = pageControl.getPageSize();
            endRow = startRow + nRows - 1;
        }

        // paging backwards - will we ever get in this if-statement if pageControl.getPageSize == SIZE_UNLIMITED?
        if (currentRowIndex < startRow) {
            int rowsBack = startRow - currentRowIndex;
            int pagesBack = (int) Math.ceil(rowsBack / (double) pageControl.getPageSize());
            int newPage = pageControl.getPageNumber() - pagesBack;

            if (newPage < 0) {
                newPage = 0;
            }

            pageControl.setPageNumber(newPage);
            pageList = getDataPage(pageControl);
            startRow = pageControl.getStartRow();
        }

        // paging forwards
        else if (currentRowIndex > endRow) {
            int rowsForward = currentRowIndex - endRow;
            int pagesForward = (int) Math.ceil(rowsForward / (double) pageControl.getPageSize());
            int newPage = pageControl.getPageNumber() + pagesForward;

            pageControl.setPageNumber(newPage);
            pageList = getDataPage(pageControl);
            startRow = pageControl.getStartRow();
        }

        /* 
         * March 11, 2009 - the only currently known way this can fail is if the countQuery returned 0 but the
         * actual data query returned nothing; generally, this is a programming error, but it's possible that
         * the facelet changed (new columns shown, other columns removed) or the query itself changed (and perhaps
         * the sortable columns are different); in either case, let's be paranoid and try it all over again with
         * a default PageControl object for this PageControlView
         */
        int getIndex = currentRowIndex - startRow;
        if (getIndex < 0 || getIndex >= pageList.size()) {
            // getting the default will repersist the new PageControl too
            pageControl = getDefaultPageControl();
            pageList = getDataPage(pageControl);

            // pageControl startRow should now be zero
            this.currentRowIndex = 0; // and tell the framework to start back at 0
            getIndex = 0; // now the getIndex should be 0
        }

        return pageList.get(getIndex);
    }

    @Override
    public Object getWrappedData() {
        return pageList;
    }

    /**
     * Return <code>true</code> if the {@link #getRowIndex()} value is currently set to a value that matches some
     * element in the dataset. Note that it may match a row that is not in the currently cached PageList; if so then
     * when {@link #getRowData()} is called the required PageList will be fetched by calling
     * {@link #getDataPage(PageControl)}.
     *
     * @return <code>true</code> if the row is available
     */
    @Override
    public boolean isRowAvailable() {
        PageList<T> page = getPage();
        if (page == null) {
            return false;
        }

        int rowIndex = getRowIndex();
        if (rowIndex < 0) {
            return false;
        } else if (rowIndex >= page.getTotalSize()) {
            return false;
        } else {
            return true;
        }
    }

    private PagedDataTableUIBean getPagedDataTableUIBean() {
        FacesContext facesContext = FacesContextUtility.getFacesContext();
        ExternalContext externalContext = facesContext.getExternalContext();
        PagedDataTableUIBean result = (PagedDataTableUIBean) externalContext.getRequestMap().get(beanName);
        if (result == null) {
            result = (PagedDataTableUIBean) externalContext.getSessionMap().get(beanName);
        }
        return result;
    }

    public PageControl getPageControl() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        return getPagedDataTableUIBean().getPageControl(user, pageControlView);
    }

    public PageControl getDefaultPageControl() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        return getPagedDataTableUIBean().getDefaultPageControl(user, pageControlView);
    }

    public void setPageControl(PageControl pageControl) {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        getPagedDataTableUIBean().setPageControl(user, pageControlView, pageControl);
    }

    public PageList<T> getDataPage(PageControl pc) {

        long start = System.currentTimeMillis();
        long monitorId = HibernatePerformanceMonitor.get().start();

        PageList<T> results = fetchPageGuarded(pc);

        HibernatePerformanceMonitor.get().stop(monitorId, pageControlView.toString());
        if (log.isDebugEnabled()) {
            long time = System.currentTimeMillis() - start;

            log.debug("Fetch time was [" + time + "]ms for " + pageControlView);
            if (time > 2000L) {
                log.debug("Slow loading page " + pageControlView);
            }
        }

        return results;
    }

    private PageList<T> fetchPageGuarded(PageControl pc) {
        PageList<T> results = null;
        boolean tryQueryAgain = false;
        try {
            if (log.isTraceEnabled()) {
                log.trace(pageControlView + ": " + pc);
            }
            if (pc.getPageSize() == PageControl.SIZE_UNLIMITED && pc.getPageNumber() != 0) {
                /* 
                 * user is trying to get all of the results (SIZE_UNLIMITED), but not starting
                 * on the first page.  while this is technically allowable, it generally doesn't
                 * make all that much sense and was most likely due to a mistake upstream in the
                 * usage of the pagination / sorting framework.
                 */
                if (log.isTraceEnabled()) {
                    log.trace(pageControlView + ": Forcing UNLIMITED PageControl's pageNumber to 0");
                }
                pc.setPageNumber(0);
                setPageControl(pc);
            }

            // try the data fetch with the potentially changed (and persisted) PageControl object
            results = fetchPage(pc);
            if (log.isTraceEnabled()) {
                log.trace(pageControlView + ": Successfully fetched page (first time)");
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
            if (results.getTotalSize() <= pc.getStartRow() || (results.isEmpty() && pc.getPageNumber() != 0)) {
                if (log.isTraceEnabled()) {
                    if (results.getTotalSize() <= pc.getStartRow()) {
                        log.trace(pageControlView + ": Results size[" + results.getTotalSize()
                            + "] was less than PageControl startRow[" + pc.getStartRow() + "]");
                    } else {
                        log.trace(pageControlView + ": Results were empty, but pageNumber was non-zero");
                    }
                }
                resetToDefaults(pc);
                if (log.isTraceEnabled()) {
                    log.trace(pageControlView + ": resetting to " + pc);
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
            resetToDefaults(pc);
            if (log.isTraceEnabled()) {
                log.trace(pageControlView + ": Received error[" + t.getMessage() + "], resetting to " + pc);
            }
            tryQueryAgain = true;
        }

        // round 2 should be guaranteed because of use of defaultPageControl
        if (tryQueryAgain) {
            if (log.isTraceEnabled()) {
                log.trace(pageControlView + ": Trying query again");
            }
            try {
                results = fetchPage(pc);
                if (log.isTraceEnabled()) {
                    log.trace(pageControlView + ": Successfully fetched page (second time)");
                }
            } catch (Throwable t) {
                log.error("Could not retrieve collection for " + pageControlView, t);
            }
        }

        if (results == null) {
            results = new PageList<T>();
        }

        return results;
    }

    private void resetToDefaults(PageControl pc) {
        pc.reset();
        setPageControl(pc);
    }

    /**
     * Method which must be implemented in cooperation with the managed bean class to fetch data on demand.
     *
     * @param  pc information such as the first row of data to be fetched, the number of rows of data to be fetched and
     *            sorting data
     *
     * @return the data page with the rows in memory
     */
    public abstract PageList<T> fetchPage(PageControl pc);

    public PageControlView getPageControlView() {
        return pageControlView;
    }
}