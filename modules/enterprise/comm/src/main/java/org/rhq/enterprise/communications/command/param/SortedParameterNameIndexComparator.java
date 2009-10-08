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
package org.rhq.enterprise.communications.command.param;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A comparator that is used to sort {@link ParameterNameIndex} objects. This comparator sorts based on its
 * {@link ParameterNameIndex#getName() name}.
 *
 * @author John Mazzitelli
 */
public class SortedParameterNameIndexComparator implements Comparator<ParameterNameIndex>, Serializable {
    /**
     * the UID to identify the serializable version of this class. This is me being paranoid; I don't think making this
     * class Serializable is necessary, but since we may be persisting serialized parameters its possible we may persist
     * an instance of this comparator with it. So, just to be careful, I've defined this SUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Given two {@link ParameterNameIndex} objects, this will compare their
     * {@link ParameterNameIndex#getIndex() indices}. This allows you to sort parameters based on their sort indices.
     *
     * <p>Both <code>o1</code> and <code>o2</code> must be of type {@link ParameterNameIndex} - a <code>
     * ClassCastException</code> is thrown otherwise, as per the <code>Comparator.compare</code> interface contract.
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(ParameterNameIndex o1, ParameterNameIndex o2) {
        int index1 = o1.getIndex();
        int index2 = o2.getIndex();

        return ((index1 < index2) ? -1 : ((index1 == index2) ? 0 : 1));
    }
}