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
package org.rhq.enterprise.server.perspective;

import java.util.List;

import javax.ejb.Local;

import org.rhq.core.domain.auth.Subject;

@Local
public interface PerspectiveManagerLocal {

    /**
     * Return the core menu for the specified subject. Depending on their inventory and roles the
     * core menu for one subject1 could differ from that of subject2.
     * 
     * Subsequent calls will return the same core menu for the same Subject. In other words, it does
     * not change during a user session.
     * 
     * @param subject
     * @return
     */
    List<MenuItem> getCoreMenu(Subject subject) throws PerspectiveException;
}