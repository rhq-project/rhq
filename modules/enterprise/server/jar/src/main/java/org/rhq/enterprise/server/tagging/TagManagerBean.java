/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.rhq.enterprise.server.tagging;

import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.TagCriteria;
import org.rhq.core.domain.tagging.Tag;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;

/**
 * @author Greg Hinkle
 */
@Stateless
public class TagManagerBean implements TagManagerLocal {

    private final Log log = LogFactory.getLog(TagManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;



    public PageList<Tag> findTagsByCriteria(Subject subject, TagCriteria criteria) {

        CriteriaQueryGenerator generator = new CriteriaQueryGenerator(criteria);

        CriteriaQueryRunner<Tag> queryRunner = new CriteriaQueryRunner<Tag>(criteria, generator, entityManager);

        return queryRunner.execute();
    }


    public Set<Tag> addTags(Subject subject, Set<Tag> tags) {
        // This isn't efficient, but then how many tags will you really be creating at once
        for (Tag tag : tags) {
            entityManager.persist(tag);
        }
        return tags;
    }

    public void removeTags(Subject subject, Set<Tag> tags) {
        // This isn't efficient, but then how many tags will you really be deleting at once
        for (Tag tag : tags) {
            entityManager.remove(entityManager.find(Tag.class,tag.getId()));
        }
    }
}
