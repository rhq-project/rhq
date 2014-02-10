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
package org.rhq.core.domain.bundle.test;

import javax.persistence.EntityManager;

import org.testng.annotations.Test;

import org.rhq.core.domain.authz.Role;
import org.rhq.core.domain.bundle.BundleGroup;
import org.rhq.core.domain.test.AbstractEJB3Test;

@Test
public class BundleGroupTest extends AbstractEJB3Test {

    private static final boolean ENABLED = true;

    @Test(enabled = ENABLED, groups = "integration.ejb3")
    public void Bz1029121Test() throws Throwable {
        // create role and bundle group, then assign them together
        // then delete BG and verify role still exists
        getTransactionManager().begin();
        EntityManager em = getEntityManager();
        try {
            BundleGroup bg = createBundleGroup(em);
            Role role = createRole(em);
            bg.addRole(role);
            em.persist(bg);
            em.persist(role);
            em.remove(bg);
            this.assertNotNull("Role previously assigned to BundleGroups must exist after BG was deleted", em.find(Role.class, role.getId()));       

        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
        finally  {
            getTransactionManager().rollback();
        }
    }

    private BundleGroup createBundleGroup(EntityManager em) {
        BundleGroup bg = new BundleGroup("test-group");
        em.persist(bg);
        return bg;
    }
    private Role createRole(EntityManager em) {
        Role r = new Role("test-role");
        em.persist(r);
        return r;
    }
}
