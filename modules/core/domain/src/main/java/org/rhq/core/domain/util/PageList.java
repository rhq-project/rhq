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