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
package org.rhq.enterprise.server.resource.group;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.resource.group.LdapGroup;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;

/**
 * A manager that provides methods for creating, updating, deleting, and querying {@link LdapGroup}s.
 *
 * @author Simeon Pinder
 */
@Local
public interface LdapGroupManagerLocal {

    PageList<LdapGroup> findLdapGroupsByRole(int roleId, PageControl pageControl);

    PageList<LdapGroup> findLdapGroups(PageControl pc);

    void setLdapGroupsOnRole(Subject subject, int roleId, Set<LdapGroup> groups);

    void addLdapGroupsToRole(Subject subject, int roleId, List<String> groupIds);

    void removeLdapGroupsFromRole(Subject subject, int roleId, int[] groupIds);

    void assignRolesToLdapSubject(int subjectId, List<String> ldapGroupNames);

    Set<Map<String, String>> findAvailableGroups();

    Set<Map<String, String>> findAvailableGroupsStatus();

    Set<String> findAvailableGroupsFor(String userName);

    Map<String, String> findLdapUserDetails(String userName);

    Boolean ldapServerRequiresAttention();
}