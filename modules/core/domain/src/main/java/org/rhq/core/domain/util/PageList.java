/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Tracks the result set for a paginated data lookup. Includes the data and the total rows that are available.
 *
 * @author Greg Hinkle
 */
@XmlRootElement
public class PageList<E> extends ArrayList<E> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalSize;
    private boolean isUnbounded; // Is the total size of the list known?
    private PageControl pageControl;

    public PageList() {
    }

    /**
     * Using this constructor one obtains a {@code PageList} instance that contains no data but it has an associated
     * page control.
     * <p/>
     * Such constructor can be used if you don't know or care about the total number of results in the list and
     * want to add the members of the list at some later point in time.
     * <p/>
     * <b>Note:</b> This constructor sets up the {@code PageList} to be {@link #isUnbounded() unbounded}.
     *
     * @param pageControl the page control to associate with the {@code PageList}
     */
    public PageList(PageControl pageControl) {
        super();
        this.isUnbounded = true;
        this.pageControl = pageControl;
    }

    /**
     * Used to represent the cardinality of a result set when the data is not needed.
     * <p/>
     * In another words you can use this constructor to setup the {@code PageList} such that it correctly declares
     * the {@link #getTotalSize() total size}, is NOT {@link #isUnbounded() unbounded} and has an associated page
     * control, but actually contains no data (the data can be added later on of course).
     *
     * @param totalSize the total number of records of which this instance contains a subset of
     * @param pageControl defines what subset of data is contained in this instance
     *
     * @see org.rhq.core.domain.criteria.Criteria.Restriction#COUNT_ONLY
     */
    public PageList(int totalSize, PageControl pageControl) {
        super();
        this.totalSize = totalSize;
        this.pageControl = pageControl;
    }

    /**
     * Used to represent a result set when the cardinality of the data is not needed.
     * <p/>
     * In another words you can use this constructor to setup a {@code PageList} which IS {@link #isUnbounded()
     * unbounded} and has an associated page control.
     *
     * @param collection the data contained in this instance - a shallow copy of the collection will be inserted into
     *                   this instance
     * @param pageControl defines what subset of the total number of items is present in this instance
     *
     * @see org.rhq.core.domain.criteria.Criteria.Restriction#COLLECTION_ONLY
     */
    public PageList(Collection<? extends E> collection, PageControl pageControl) {
        super(collection);
        this.isUnbounded = true;
        this.pageControl = pageControl;
    }

    /**
     * This constructor creates a fully configured {@code PageList} that contains the page control, the subset of the
     * records that conforms to that page control, as well as the information about the total number of records.
     * <p/>
     * Note that it is perfectly legal for the collection to be empty, even though the {@code totalSize} is &gt; 0.
     * This can be caused by a couple of things:
     * <ul>
     *     <li>the page control used to obtain this page list points "past" the total size,</li>
     *     <li>there has been a drastic change in the database between obtaining the previous "page" and the next
     *     page using the same page control resulting essentially in the previous case,</li>
     *     <li>there has been a concurrent DB activity while obtaining the data and totalSize of the list, resulting
     *     in the two being based on different DB state. There is an attempt to mitigate this condition in the while
     *     performing the criteria queries but due to the nature of the READ_COMMITTED transaction isolation level,
     *     this cannot be completely prevented.</li>
     * </ul>
     * @param collection the subset of the records as described by the {@code pageControl}
     * @param totalSize the total number of records, the {@code collection} is subset of
     * @param pageControl the page control object describing the subset
     */
    public PageList(Collection<? extends E> collection, int totalSize, PageControl pageControl) {
        super(collection);
        this.totalSize = totalSize;
        this.isUnbounded = false;
        this.pageControl = pageControl;
    }

    /**
     * Pages the given collection, using the supplied page list data from an
     * existing wrapped collection.
     */
    public PageList(Collection<? extends E> collection, PageList<?> results) {
        this(collection, results.getTotalSize(), results.getPageControl());
        this.isUnbounded = results.isUnbounded;
    }

    public PageControl getPageControl() {
        return pageControl;
    }

    public void setPageControl(PageControl pageControl) {
        this.pageControl = pageControl;
    }

    public ArrayList<E> getValues() {
        return this;
    }

    public void setValues(ArrayList<E> values) {
        this.clear();
        this.addAll(values);
    }

    /**
     * Returns the total size of the "master list" that this page is a subset of.
     * <p/>
     * <b>Note:</b> This method merely returns the size of this list if it is {@link #isUnbounded() unbounded}.
     * @return the total size
     */
    public int getTotalSize() {
        return Math.max(this.size(), this.totalSize);
    }

    /**
     * Sets the total size of the "master list" that this page is a subset of.
     *
     * @param totalSize New value of property totalSize.
     */
    public void setTotalSize(int totalSize) {
        this.isUnbounded = false;
        this.totalSize = totalSize;
    }

    /**
     * @return whether the total size of the list is known or not
     */
    public boolean isUnbounded() {
        return this.isUnbounded;
    }

    public void setUnbounded(boolean isUnbounded) {
        this.isUnbounded = isUnbounded;
        if (isUnbounded) {
            //reset this to 0, so that #getTotalSize() behaves consistently
            totalSize = 0;
        }
    }

    /**
     * @see PageControl#isConsistentWith(PageList)
     *
     * @return true if this page list is consistent with its page control
     */
    public boolean isConsistent() {
        return pageControl == null || pageControl.isConsistentWith(this);
    }

    @Override
    public String toString() {
        return "PageList" + super.toString();
    }
}
