/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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

package org.rhq.core.domain.search;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.SavedSearchCriteria;
import org.rhq.core.domain.test.AbstractEJB3Test;
import org.testng.annotations.Test;

import javax.persistence.EntityManager;
import java.util.List;

public class SavedSearchTest extends AbstractEJB3Test {

    @Test
    public void testInsert() throws Exception {
        getTransactionManager().begin();
        EntityManager entityMgr = getEntityManager();
        try {
            Subject subject = new Subject("searcher", true, true);
            entityMgr.persist(subject);

            SavedSearch search = new SavedSearch(SearchSubsystem.RESOURCE, "test search", "test pattern", subject);
            search.setGlobal(true);
            entityMgr.persist(search);
            entityMgr.flush();            
        }
        finally {
            getTransactionManager().rollback();
        }
    }    

}
