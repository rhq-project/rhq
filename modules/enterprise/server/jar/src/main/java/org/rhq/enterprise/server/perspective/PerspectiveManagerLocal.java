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
import org.rhq.core.clientapi.descriptor.perspective.Perspective;
import org.rhq.core.clientapi.descriptor.perspective.Task;

/**
 * PerspectiveManager - interface to storage and retrieval of Perspectives
 *
 * @version $Rev$
 */
@Local
public interface PerspectiveManagerLocal {
    /**
     * Get the list of all the Perspectives the user has access to
     *
     * @return List<Perspective> of Perspective objects user has access too
     */
    public List<Perspective> getAllPerspectives();

    /**
     * Get a named Perspective
     *
     * @param  name of Perspective desired
     *
     * @return Perspective looked up
     */
    public Perspective getPerspective(String name);

    /**
     * Get a list of Task items that are relative to the Context name passed in.
     *
     * @param  contextName to lookup
     * @param  Object      array to parameterize for the Tasks returned.
     *
     * @return List<Task> that match. null if none found or defined that relate to this type of object passed in.
     */
    public List<Task> getTasks(String contextName, Object... args);

    /**
     * Get a list of Task items that are relative to the Context name passed in.
     *
     * @param  contextName to lookup
     *
     * @return List<Task> that match. null if none found or defined that relate to this type of object passed in.
     */
    public List<Task> getTasks(String contextName);
}