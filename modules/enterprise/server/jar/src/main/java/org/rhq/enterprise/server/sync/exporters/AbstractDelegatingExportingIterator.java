/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.sync.exporters;

import java.util.Iterator;

/**
 * An abstract base class that delegates the {@link Iterator} functionality to a provided
 * "ordinary" iterator while leaving the exporting functionality to the subclasses.
 *
 * @author Lukas Krejci
 */
public abstract class AbstractDelegatingExportingIterator<E> implements ExportingIterator<E> {

    private Iterator<E> inner;
    private E current;
    
    public AbstractDelegatingExportingIterator(Iterator<E> wrapped) {
        inner = wrapped;
    }
    
    public boolean hasNext() {
        return inner.hasNext();
    }

    public E next() {  
        current = inner.next();
        return current;
    }

    public void remove() {
        inner.remove();
    }

    protected E getCurrent() {
        return current;
    }
}
