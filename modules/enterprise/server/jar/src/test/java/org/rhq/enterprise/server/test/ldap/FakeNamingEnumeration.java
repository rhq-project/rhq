/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.test.ldap;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

/**
 * @author loleary
 * @param <T>
 *
 */
public class FakeNamingEnumeration<T> implements NamingEnumeration<T> {
    private List<SearchResult> srList = null;
    private int idx = 0;

    FakeNamingEnumeration(List<SearchResult> srList) {
        if (srList != null)
            this.srList = new ArrayList<SearchResult>(srList);
        else
            this.srList = new ArrayList<SearchResult>(0);
    }

    @Override
    public boolean hasMoreElements() {
        return srList != null && idx < srList.size() || false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T nextElement() {
        if (srList == null || idx >= srList.size()) {
            throw new NoSuchElementException();
        }

        return (T) srList.get(idx++);
    }

    @Override
    public void close() throws NamingException {
        srList = null;
    }

    @Override
    public boolean hasMore() throws NamingException {
        return hasMoreElements();
    }

    @Override
    public T next() throws NamingException {
        return nextElement();
    }

}
