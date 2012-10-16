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
package org.rhq.enterprise.gui.coregui.client.util;

import java.util.LinkedList;

/**
 * A BoundedLinkedList with a max bounds on the number of elements
 * it can contain. Once the limit is reached the oldest element is
 * removed from the start of the list and a new value added to the back of
 * the list. This is useful for sliding windows of data (i.e., Live Graphs)
 * There are other standard JDK collections that can't be used here because
 * GWT only supports a subset of these collections. So this is the smallest
 * working solution to address the sliding window problem.
 *
 * @author Mike Thompson
 */
public class BoundedLinkedList<E> extends LinkedList<E>{
   private int maxElements = 0;

    public BoundedLinkedList(int maxElements)
    {
        this.maxElements = maxElements;
    }

    @Override
    /**
     * Protect the linkedList from going over a certain bound by
     * deleting the oldest element and then adding to the end of the list.
     */
    public void addLast(E e){
        if(size() >= maxElements){
            Log.debug(" *** Removing first element");
            removeFirst();
        }
        super.addLast(e);
    }

}
