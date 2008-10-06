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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tracks the result set for a paginated data lookup. Includes the data and the total rows that are available.
 *
 * @author Greg Hinkle
 */
@XmlRootElement
public class PageList<E> extends ArrayList<E> implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log LOG = LogFactory.getLog(PageList.class);

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

    public PageList(Collection<? extends E> collection, PageControl pageControl) {
        super(collection);
        isUnbounded = true;
        this.pageControl = pageControl;
    }

    public PageList(Collection<? extends E> collection, int totalSize, PageControl pageControl) {
        super(collection);
        this.totalSize = totalSize;
        if (collection.size() == 0 && totalSize > 0) {
            throw new IllegalArgumentException(
                "PageList was passed an empty collection but the 'totalSize' attribute was " + totalSize);
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