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

import javax.ejb.Local;

@Local
public interface SubjectRoleTestBeanLocal {
    /**
     * creates roleCount*usersInRoleCount number of objects in the system
     *
     * @param roleCount        the number of roles to create
     * @param usersInRoleCount the number of users to create and attach to each role
     */
    public void createRolesAndUsers(int roleCount, int usersInRoleCount);
}