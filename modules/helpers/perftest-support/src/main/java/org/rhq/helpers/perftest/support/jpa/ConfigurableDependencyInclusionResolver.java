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

import java.util.Set;

import org.rhq.helpers.perftest.support.config.Entity;
import org.rhq.helpers.perftest.support.config.Relationship;
import org.rhq.helpers.perftest.support.config.ExportConfiguration;

/**
 *
 * @author Lukas Krejci
 */
public class ConfigurableDependencyInclusionResolver implements DependencyInclusionResolver {

    private ExportConfiguration edg;

    public ConfigurableDependencyInclusionResolver() {

    }

    public ConfigurableDependencyInclusionResolver(ExportConfiguration edg) {
        this.edg = edg;
    }

    public boolean isValid(Edge edge) {

        Entity directFrom = edg.getEntity(edge.getFrom().getEntity());
        if (directFrom != null) {
            if (directFrom.getIncludeAllDependents() != null
                && directFrom.getIncludeAllDependents().equals(Boolean.TRUE)) {
                return true;
            }
            for (Relationship r : directFrom.getRelationships()) {
                if (r.getSourceField() != null) {
                    if (edge.getFromField() != null && edge.getFromField().getName().equals(r.getSourceField())) {
                        return true;
                    }
                } else {
                    String edgeToField = edge.getToField() == null ? null : edge.getToField().getName();
                    Class<?> target = edge.getTo().getEntity();

                    if (edgeToField != null && edgeToField.equals(r.getTargetField())
                        && target.equals(edg.getClassForEntity(directFrom))) {
                        return true;
                    }
                }
            }
        }

        //check if some of the parents wasn't declared as "includeAllDependents"
        Set<Node> parentFroms = edge.getFrom().getTransitiveParents(true);

        for (Node parentFrom : parentFroms) {
            Entity parentEntity = edg.getEntity(parentFrom.getEntity());
            if (parentEntity != null && parentEntity.getIncludeAllDependents() != null
                && parentEntity.getIncludeAllDependents().equals(Boolean.TRUE)) {
                return true;
            }
        }

        return false;
    }
}
