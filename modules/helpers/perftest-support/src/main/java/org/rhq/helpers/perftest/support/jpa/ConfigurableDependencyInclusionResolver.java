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

import org.rhq.helpers.perftest.support.config.Entity;
import org.rhq.helpers.perftest.support.config.ExportConfiguration;
import org.rhq.helpers.perftest.support.config.Relationship;

/**
 * An implementation of {@link DependencyInclusionResolver} interface that can be configured
 * to include/exclude relationships by being provided a dependency graph in the form of {@link ExportConfiguration} instance.
 * 
 * @author Lukas Krejci
 */
public class ConfigurableDependencyInclusionResolver implements DependencyInclusionResolver {

    private ExportConfiguration edg;

    /**
     * @param edg the dependency graph of allowed entities and relations among them.
     */
    public ConfigurableDependencyInclusionResolver(ExportConfiguration edg) {
        this.edg = edg;
    }

    /**
     * An edge (relationship) is considered valid based on the dependency graph provided in this instance's
     * constructor.
     * These are the naming conventions used in the rules below:
     * <ul>
     * <li>Source Entity is an {@link Entity} in the {@link ExportConfiguration configuration} that corresponds
     * to the "from node" {@link Edge#getFrom()} of the provided edge of the full dependency graph.
     * <li>Target Entity is the {@link Entity} corresponding to the {@link Edge#getTo()}.
     * <li>Source relationship is the {@link Relationship} defined on the source entity that corresponds to the {@link Edge#getFromField()} (i.e. has the same name).
     * <li>Target relationship is the {@link Relationship} on the target entity corresponding to the {@link Edge#getToField()}. 
     * </ul>
     * 
     * <p>
     * An edge is considered valid if at least one of the following conditions is true:
     * <ul>
     * <li>The configuration specifies to include all explicit dependent entities and the edge has a non-null {@link Edge#getFromField() from field} (i.e. the dependency is explicitly defined).
     * <li>The source entity specifies to include all fields ({@link Entity#isIncludeAllFields()}).
     * <li>The target entity specifies to include all fields ({@link Entity#isIncludeAllFields()}).
     * <li>The source relationship is defined.
     * <li>The target relationship is defined.
     * </ul>
     * 
     * @param edge the edge from the full entity dependency graph to check the validity of.
     * @return true if valid (i.e. to be included in the output), false otherwise.
     */
    public boolean isValid(Edge edge) {
        Entity from = edg.getEntity(edge.getFrom().getEntity());
        Entity to = edg.getEntity(edge.getTo().getEntity());

        //check if we should include all the explicitly defined dependent entities
        //implicitly.
        if (edg.isIncludeExplicitDependentsImplicitly()) {
            if (edge.getFromField() != null) {
                //this is a candidate for implicit inclusion, but check if there
                //aren't some explicit inclusion rules configured first.
                if (from == null) {
                    //k, there aren't
                    return true;
                }
            }
        }

        if (from != null) {
            //we have an explicit configuration for this entity, let's see if the edge matches
            return isValid(from, edge.getFromField());
        }

        if (to != null) {
            return isValid(to, edge.getToField());
        }

        return false;
    }

    private boolean isValid(Entity entity, Field field) {
        if (field == null) {
            return false;
        }

        for (Relationship relationship : entity.getRelationships()) {
            String fieldName = relationship.getField();
            if (field.getName().equals(fieldName)) {
                return !relationship.isExclude();
            }
        }

        //check the includeAll flag as the last so that explicit exclusions can
        //override it in the loop above.
        return entity.isIncludeAllFields();
    }
}
