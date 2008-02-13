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
package org.rhq.core.domain.resource.test;

import javax.persistence.Query;
import org.testng.annotations.Test;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.test.AbstractEJB3Test;

/**
 * Tests the query of JBNADM-1690
 *
 * @author Heiko W. Rupp
 */
public class ResourceTypeJIRA1690 extends AbstractEJB3Test {
    @Test
    public void testSimple() throws Exception {
        getTransactionManager().begin();

        javax.persistence.EntityManager em = getEntityManager();

        Query q = em.createNamedQuery(ResourceType.QUERY_FIND_CHILDREN_BY_CATEGORY);
        q.setParameter(1, 50);
        q.setParameter(2, "categoryName");
        q.setParameter(3, 50);
        q.setParameter(4, "categoryName");
        q.getResultList();

        getTransactionManager().rollback();
    }

    @Test
    public void testWithAdmin() throws Exception {
        getTransactionManager().begin();

        javax.persistence.EntityManager em = getEntityManager();

        Query q = em.createNamedQuery(ResourceType.QUERY_FIND_CHILDREN_BY_CATEGORY + "_admin");
        q.setParameter(1, 50);
        q.setParameter(2, "categoryName");
        q.setParameter(3, 50);
        q.setParameter(4, "categoryName");
        q.getResultList();

        getTransactionManager().rollback();
    }
}