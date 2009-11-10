/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.configuration.resource;

import java.util.HashMap;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("rawConfigDelegateMap")
@Scope(ScopeType.SESSION)
public class RawConfigDelegateMap extends HashMap<Integer, RawConfigDelegate> {

    /**
     * 
     */
    private static final long serialVersionUID = 941352750819758987L;

    public RawConfigDelegateMap() {
        super();
        // TODO Auto-generated constructor stub
    }

    public RawConfigDelegateMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        // TODO Auto-generated constructor stub
    }

    public RawConfigDelegateMap(int initialCapacity) {
        super(initialCapacity);
        // TODO Auto-generated constructor stub
    }

    public RawConfigDelegateMap(Map<? extends Integer, ? extends RawConfigDelegate> m) {
        super(m);
        // TODO Auto-generated constructor stub
    }

}