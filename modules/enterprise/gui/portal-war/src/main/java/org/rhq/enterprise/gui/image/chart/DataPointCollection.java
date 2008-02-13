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
package org.rhq.enterprise.gui.image.chart;

import java.util.ArrayList;
import org.rhq.enterprise.gui.image.data.IDataPoint;

/**
 * DataPointCollection holds a collection of objects that implement the org.rhq.enterprise.gui.image.data.IDataPoint
 * interface. The collection of objects form are charted when give to a subclass of net.hyperic.chart.Chart.
 */
public class DataPointCollection extends ArrayList {
    public DataPointCollection() {
    }

    public DataPointCollection(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     * Adds an element to the collection.
     *
     * @param  element The object to add to the collection.
     *
     * @return true of object was successfully added to the collection.
     *
     * @throws ClassCastException If the element does not implement the IDataPoint interface.
     */
    public boolean add(IDataPoint element) {
        return super.add(element);
    }

    /**
     * Adds the elements of the specified collection to this collection.
     *
     * @param  c The collection to add to this collection.
     *
     * @return true if the objects was successfully added to the collection.
     *
     * @throws ClassCastException If the specified collection is not a net.hyperic.chart.DataCollection or subclass.
     */
    public boolean addAll(DataPointCollection c) {
        return super.addAll(c);
    }

    /**
     * Determines whether the collection contains the specified element.
     *
     * @param  element The object to test for in the collection.
     *
     * @return true if the collection contains the specified element.
     *
     * @throws ClassCastException If the element does not implement the net.hyperic.chart.IDataPoint interface.
     */
    public boolean contains(IDataPoint element) {
        return super.contains(element);
    }

    /**
     * Determines whether the collection contains all of the elements in the specified collection.
     *
     * @param  c The collection containing the element to check for.
     *
     * @return true if the collection contains the specified objects.
     *
     * @throws ClassCastException If the specified collection is not a net.hyperic.chart.DataCollection or subclass.
     */
    public boolean containsAll(DataPointCollection c) {
        return super.containsAll(c);
    }

    /**
     * Removes the specified element from the collection.
     *
     * @param  o The object to remove from the collection.
     *
     * @return true if object was successfully removed from the collection.
     *
     * @throws ClassCastException If the element does not implement the IDataPoint interface.
     */
    public boolean remove(IDataPoint element) {
        return super.remove(element);
    }

    /**
     * Removes all of the elements in the specified collection from this collection.
     *
     * @param  c The collection of objects to remove.
     *
     * @return true if the elements were successfully removed from the collection.
     *
     * @throws ClassCastException If the specified collection is not a net.hyperic.chart.DataCollection or subclass.
     */
    public boolean removeAll(DataPointCollection c) {
        return super.removeAll(c);
    }

    /**
     * Removes all of the elements in the collection except those in the specified collection.
     *
     * @param  c The collection of objects to retain.
     *
     * @return true if the elements were succesfully retained.
     *
     * @throws ClassCastException If the specified collection is not a net.hyperic.chart.DataCollection or subclass.
     */
    public boolean retainAll(DataPointCollection c) {
        return super.retainAll(c);
    }
}