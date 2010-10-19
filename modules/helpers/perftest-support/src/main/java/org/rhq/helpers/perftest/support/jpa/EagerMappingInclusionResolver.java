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

package org.rhq.helpers.perftest.support.jpa;

import java.lang.reflect.Field;

import javax.persistence.FetchType;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

/**
 * A {@link DependencyInclusionResolver} implementation that consideres an edge valid
 * if the JPA defines the fetch type as {@link FetchType#EAGER}.
 * <p>
 * Note that this is just an example implementation of the interface with no practical use because
 * of the lack of flexibility of this simple rule.
 * 
 * @author Lukas Krejci
 */
public class EagerMappingInclusionResolver implements DependencyInclusionResolver {

    public boolean isValid(Edge edge) {
        switch (edge.getDependencyType()) {
        case ONE_TO_ONE:
            return analyzeOneToOne(edge);
        case ONE_TO_MANY:
            return analyzeOneToMany(edge);
        case MANY_TO_MANY:
            return analyzeManyToMany(edge);
        default:
            return true;
        }
    }
    
    private boolean analyzeOneToOne(Edge edge) {
        return edge.getFromField().getAnnotation(OneToOne.class).fetch() == FetchType.EAGER;   
    }

    private boolean analyzeOneToMany(Edge edge) {
        Field fromField = edge.getFromField();
        
        return fromField != null && fromField.getAnnotation(OneToMany.class).fetch() == FetchType.EAGER;   
    }

    private boolean analyzeManyToMany(Edge edge) {
        Field fromField = edge.getFromField();
        Field toField = edge.getToField();

        ManyToMany fromAnnotation = fromField.getAnnotation(ManyToMany.class);
        ManyToMany toAnnotation = toField.getAnnotation(ManyToMany.class);

        return (fromAnnotation != null && fromAnnotation.fetch() == FetchType.EAGER)
            || (toAnnotation != null && fromAnnotation.fetch() == FetchType.EAGER);

    }
}
