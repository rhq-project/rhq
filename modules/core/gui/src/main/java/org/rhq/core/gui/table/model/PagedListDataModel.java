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
package org.rhq.core.gui.table.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.gui.table.bean.AbstractPagedDataUIBean;

import javax.faces.model.DataModel;

/**
 * @author Ian Springer
 * @author Joseph Marques 
 *
 * @param <T> the data object type
 */
public abstract class PagedListDataModel<T> extends DataModel {

    private final Log log = LogFactory.getLog(PagedListDataModel.class);

    private int currentRowIndex;
    private PageList<T> pageList;
    private AbstractPagedDataUIBean pagedDataBean;

    /**
     * Create a data model that pages through the data set, showing the specified number of rows on each page.
     */
    public PagedListDataModel(AbstractPagedDataUIBean pagedDataBean) {
        super();
        this.currentRowIndex = -1;
        this.pagedDataBean = pagedDataBean;
    }

    /**
     * Not used in this class; data is fetched via a callback to the {@link #getDataPage()} method rather
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
        return getCurrentPage().getTotalSize();
    }

    /**
     * Return a PageList object; if one is not currently available, then fetch one. Note that this doesn't ensure that
     * the PageList returned includes the current {@link #getRowIndex()} row; see {@link #getRowData()}.
     *
     * @return the current page
     */
    private PageList<T> getCurrentPage() {
        // ensure page exists - first time going to this view
        if (this.pageList == null) {
            this.pageList = getDataPage();
        } else {
            PageControl currentPageControl = this.pagedDataBean.getPageControl();
            PageControl lastUsedPageControl = this.pageList.getPageControl();
            if (!currentPageControl.equals(lastUsedPageControl)) {
                // One or more paging/sorting settings have been updated - we need to load a new page.
                this.pageList = getDataPage();
            }
        }
        return this.pageList;
    }

    /**
     * Return the object corresponding to the current {@link #getRowIndex()}. If the PageList object currently cached
     * doesn't include that index then {@link #getDataPage()} is called to retrieve the appropriate page.
     *
     * @return the row data that corresponds to {@link #getRowIndex()}
     *
     * @throws IllegalArgumentException if the {@link #getRowIndex()} is outside the range of the dataset size
     */
    @Override
    public Object getRowData() {
        getCurrentPage();
        if (!isRowAvailable()) {
            /*
             * March 11, 2009 - the only currently known way this can fail is if the countQuery returned 0 but the
             * actual data query returned nothing; generally, this is a programming error, but it's possible that
             * the facelet changed (new columns shown, other columns removed) or the query itself changed (and perhaps
             * the sortable columns are different); in either case, let's be paranoid and try it all over again with
             * a default PageControl object.
             */
            // Reset back to the default page control, which will also persist the default page control.
            this.pagedDataBean.resetPageControl();
            getCurrentPage();            
            // Tell the framework to start back at the first row in the page.
            this.currentRowIndex = 0;
        }
        int pageIndex = this.currentRowIndex - getPageControl().getStartRow();

        return this.pageList.get(pageIndex);
    }

    @Override
    public Object getWrappedData() {
        return pageList;
    }

    /**
     * Return <code>true</code> if the {@link #getRowIndex()} value is currently set to a value that matches some
     * element in the dataset. Note that it may match a row that is not in the currently cached PageList; if so then
     * when {@link #getRowData()} is called the required PageList will be fetched by calling
     * {@link #getDataPage()}.
     *
     * @return <code>true</code> if the row is available
     */
    @Override
    public boolean isRowAvailable() {
        getCurrentPage();
        return (this.pageList != null &&
                this.currentRowIndex >= 0 &&
                this.currentRowIndex < pageList.getTotalSize());
    }

    private PageList<T> getDataPage() {
        long start = System.currentTimeMillis();
        PageControl pageControl = this.pagedDataBean.getPageControl();
        PageList<T> results = fetchPageGuarded(pageControl);
        if (log.isDebugEnabled()) {
            long time = System.currentTimeMillis() - start;

            log.debug("Fetch time was [" + time + "]ms for " + pageControl);
            if (time > 2000L) {
                log.debug("Slow loading page " + pageControl);
            }
        }
        return results;
    }

    @NotNull
    private PageList<T> fetchPageGuarded(PageControl pageControl) {
        PageList<T> results = null;
        boolean tryQueryAgain = false;
        try {

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
            }

            // try the data fetch with the potentially changed (and persisted) PageControl object
            if (log.isTraceEnabled()) {
                log.trace("Fetching page using: " + pageControl);
            }
            results = fetchPage(pageControl);
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
                        //    + "] was less than PageControl startRow[" + pageControl.getStartRow() + "]");
                    } else {
                        //log.trace(pageControlView + ": Results were empty, but pageNumber was non-zero");
                    }
                }
                pageControl.reset();
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
            pageControl.reset();
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
                results = fetchPage(pageControl);
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

    @NotNull
    public PageControl getPageControl() {
        return this.pagedDataBean.getPageControl();
    }

    public void setPageControl(PageControl pageControl) {
        this.pagedDataBean.setPageControl(pageControl);        
    }

    /**
     * Method which must be implemented in cooperation with the managed bean class to fetch data on demand.
     *
     * @param  pageControl information such as the first row of data to be fetched, the number of rows of data to be fetched and
     *            sorting data
     *
     * @return the data page with the rows in memory
     */
    public abstract PageList<T> fetchPage(PageControl pageControl);

    /**
     * TODO
     */
    public void reset() {
        this.pageList = null;
    }
}