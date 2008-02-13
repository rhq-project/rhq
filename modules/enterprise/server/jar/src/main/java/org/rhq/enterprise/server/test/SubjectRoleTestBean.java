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
package org.rhq.enterprise.server.test;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.authz.Role;
import org.rhq.enterprise.server.RHQConstants;

@Stateless
public class SubjectRoleTestBean implements SubjectRoleTestBeanLocal {
    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    public void createRolesAndUsers(int roleCount, int usersInRoleCount) {
        long createTime = System.currentTimeMillis();
        for (int i = 0; i < roleCount; i++) {
            Role role = new Role("role" + i + "-" + createTime);
            entityManager.persist(role);

            for (int j = 0; j < usersInRoleCount; j++) {
                Subject subject = new Subject("subject" + i + "-" + j + "-" + createTime, true, false);
                entityManager.persist(subject);

                role.addSubject(subject);
                entityManager.merge(subject);
            }

            entityManager.merge(role);
        }
    }
}