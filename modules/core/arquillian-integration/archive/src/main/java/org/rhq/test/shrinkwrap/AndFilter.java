/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.test.shrinkwrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.shrinkwrap.api.Filter;

/**
 * 
 *
 * @author Lukas Krejci
 */
public class AndFilter<T> implements Filter<T> {

    private List<Filter<T>> filters;
    
    public AndFilter(Filter<T> filter) {
        this.filters = Collections.singletonList(filter);
    }
    
    public AndFilter(Filter<T> f1, Filter<T> f2) {
        filters = new ArrayList<Filter<T>>();
        filters.add(f1);
        filters.add(f2);
    }
    
    public AndFilter(Filter<T> f1, Filter<T> f2, Filter<T> f3) {
        filters = new ArrayList<Filter<T>>();
        filters.add(f1);
        filters.add(f2);
        filters.add(f3);
    }
    
    public AndFilter(Filter<T> f1, Filter<T> f2, Filter<T> f3, Filter<T> f4) {
        filters = new ArrayList<Filter<T>>();
        filters.add(f1);
        filters.add(f2);
        filters.add(f3);
        filters.add(f4);
    }
    
    public AndFilter(Collection<? extends Filter<T>> filters) {
        this.filters = new ArrayList<Filter<T>>(filters);
    }
    
    @Override
    public boolean include(T object) {
        for(Filter<T> f : filters) {
            if (!f.include(object)) {
                return false;
            }
        }
        return true;
    }

}
