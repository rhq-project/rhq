/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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

package org.rhq.helpers.perftest.support.util;

import java.util.SortedSet;

import org.dbunit.dataset.filter.ITableFilter;
import org.dbunit.util.search.IEdge;
import org.dbunit.util.search.ISearchCallback;
import org.dbunit.util.search.SearchException;
import org.rhq.helpers.perftest.support.EntityDependencyGraph;

/**
 *
 * @author Lukas Krejci
 */
public class EntityDependencySearchCallbackFilteredByIds implements ISearchCallback {

    private static class Edge implements IEdge {

        private EntityDependencyGraph.Node from;
        private EntityDependencyGraph.Node to;
        
        public Edge(EntityDependencyGraph.Node from, EntityDependencyGraph.Node to) {
            this.from = from;
            this.to = to;
        }
        
        public int compareTo(Object o) {
            return from.getTable().compareTo(to.getTable());
        }

        public Object getFrom() {
            return from;
        }

        public Object getTo() {
            return to;
        }
    };
    
    ITableFilter getFilter() {
        
    }
    
    public SortedSet<Object> getEdges(Object fromNode) throws SearchException {
        // TODO Auto-generated method stub
        return null;
    }

    public void nodeAdded(Object fromNode) throws SearchException {
        // TODO Auto-generated method stub
        
    }

    public boolean searchNode(Object node) throws SearchException {
        // TODO Auto-generated method stub
        return false;
    }

}
