/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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

package org.rhq.enterprise.server.plugins.drift.mongodb.dao;

import java.util.List;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.dao.BasicDAO;
import com.google.code.morphia.query.Query;
import com.mongodb.Mongo;

import org.bson.types.ObjectId;

import org.rhq.core.domain.criteria.DriftCriteria;
import org.rhq.enterprise.server.plugins.drift.mongodb.entities.MongoDBChangeSet;

import static java.util.Arrays.asList;

public class ChangeSetDAO extends BasicDAO<MongoDBChangeSet, ObjectId> {

    public ChangeSetDAO(Morphia morphia, Mongo mongo, String db) {
        super(mongo, morphia, db);
    }

    public List<MongoDBChangeSet> findByDriftCriteria(DriftCriteria criteria) {
        Query<MongoDBChangeSet> query = createQuery();

        if (criteria.getFilterResourceIds() != null && criteria.getFilterResourceIds().length > 0) {
            query.field("resourceId").in(asList(criteria.getFilterResourceIds()));
        }

        if (criteria.getFilterCategories() != null && criteria.getFilterCategories().length > 0) {
            query.field("files.category").in(asList(criteria.getFilterCategories()));
        }

        return query.asList();
    }

}
