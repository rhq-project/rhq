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
 *
 * @author Lukas Krejci
 */
public class ConfigurableDependencyInclusionResolver implements DependencyInclusionResolver {

    private ExportConfiguration edg;

    public ConfigurableDependencyInclusionResolver(ExportConfiguration edg) {
        this.edg = edg;
    }

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
        if (entity.isIncludeAllFields()) {
            return true;
        }

        if (field == null) {
            return false;
        }
        
        for (Relationship relationship : entity.getRelationships()) {
            String fieldName = relationship.getField();
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }

        return false;
    }
}
