/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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

/**
 * A page control that shows all results. The page number and page size are 0 and {@link #SIZE_UNLIMITED} respectively,
 * and are immutable.
 *
 * @author Ian Springer
 */
public class UnlimitedPageControl extends PageControl {

    private static final long serialVersionUID = 1L;

    public UnlimitedPageControl() {
        this(new OrderingField[0]);
    }

    public UnlimitedPageControl(OrderingField... orderingFields) {
        super(0, SIZE_UNLIMITED, orderingFields);
    }

    @Override
    public void setPageNumber(int pageNumber) {
        throw new UnsupportedOperationException("page number cannot be changed from 0 for an UnlimitedPageControl.");
    }

    @Override
    public void setPageSize(int pageSize) {
        throw new UnsupportedOperationException("page size cannot be changed from " + SIZE_UNLIMITED
            + " for an UnlimitedPageControl.");
    }

    @Override
    public boolean isUnlimited() {
        return true;
    }

    @Override
    public void reset() {
        getOrderingFields().clear();
    }

}
