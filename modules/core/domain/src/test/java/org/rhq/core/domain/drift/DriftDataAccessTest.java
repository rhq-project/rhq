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

package org.rhq.core.domain.drift;

import javax.persistence.EntityManager;

import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeMethod;

import org.rhq.core.domain.shared.TransactionCallback;
import org.rhq.core.domain.test.AbstractEJB3Test;

public class DriftDataAccessTest extends AbstractEJB3Test {

    @BeforeMethod(groups = "drift.ejb")
    public final void initDB() {
        if (!inContainer()) {
            return;
        }

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                try {
                    purgeDB();

                } catch (Exception e) {
                    System.out.println("BEFORE METHOD FAILURE, TEST DID NOT RUN!!!");
                    e.printStackTrace();
                    throw e;
                }
            }
        });
    }

    @AfterGroups(groups = "drift.ejb")
    public void resetDB() {
        if (!inContainer()) {
            return;
        }

        executeInTransaction(false, new TransactionCallback() {
            @Override
            public void execute() throws Exception {
                purgeDB();
            }
        });
    }

    private void purgeDB() {
        EntityManager em = getEntityManager();
        em.createQuery("delete from JPADrift ").executeUpdate();
        em.createQuery("delete from JPADriftChangeSet").executeUpdate();
        em.createQuery("delete from JPADriftSet").executeUpdate();
        em.createQuery("delete from JPADriftFile").executeUpdate();
        em.createQuery("delete from DriftDefinition").executeUpdate();
        em.createQuery("delete from DriftDefinitionTemplate").executeUpdate();
    }

}
