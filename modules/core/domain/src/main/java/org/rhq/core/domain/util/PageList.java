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

import org.rhq.core.domain.criteria.Criteria;

/**
 * Tracks the result set for a paginated data lookup. Includes the data and the total rows that are available.
 *
 * @author Greg Hinkle
 */
@XmlRootElement
public class PageList<E> extends ArrayList<E> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int totalSize = 0;
    private boolean isUnbounded; // Is the total size of the list known?
    private PageControl pageControl;

    public PageList() {
    }

    public PageList(PageControl pageControl) {
        super();
        this.isUnbounded = true;
        this.pageControl = pageControl;
    }

    /**
     * Used to represent the cardinality of a result set without when the data is not needed 
     * 
     * @see Criteria.Restriction#COUNT_ONLY
     */
    public PageList(int totalSize, PageControl pageControl) {
        super();
        this.totalSize = totalSize;
        this.pageControl = pageControl;
    }

    /**
     * Used to represent a result set when the cardinality of the data is not needed 
     * 
     * @see Criteria.Restriction#COLLECTION_ONLY
     */
    public PageList(Collection<? extends E> collection, PageControl pageControl) {
        super(collection);
        isUnbounded = true;
        this.pageControl = pageControl;
    }

    public PageList(Collection<? extends E> collection, int totalSize, PageControl pageControl) {
        super(collection);
        this.totalSize = totalSize;
        if (collection.size() == 0 && totalSize > 0) {
            /* 
             * this can be seen attempting to navigate to a non-existent page of the result set (e.g. page 10 in a
             * collection of 3 items).  The mechanism controlling pagination at the user level should be notified of
             * this, so it can decide how to gracefully handle this condition.
             */
            throw new IllegalArgumentException("PageList was passed an empty collection but 'totalSize' was "
                + totalSize + ", " + pageControl);
        }
        this.isUnbounded = false;
        this.pageControl = pageControl;
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
     *
     * @return Value of property listSize.
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

    public boolean isUnbounded() {
        return this.isUnbounded;
    }

    public void setUnbounded(boolean isUnbounded) {
        this.isUnbounded = isUnbounded;
    }

    @Override
    public String toString() {
        return "PageList" + super.toString();
    }
}