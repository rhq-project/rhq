/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.bundle.ant.task;

import org.apache.tools.ant.Task;

import org.rhq.bundle.ant.BundleAntProject;

/**
 * This is the base class for all custom bundle tasks. It provides all bundle tasks
 * access to the encompassing bundle ant project object so this task can report back to the 
 * project object things that the task is doing.
 *
 * As new tasks are created by extending this task object, developers must make sure
 * they add the new tasks to the bundle-ant-tasks.properties file.
 * 
 * @author John Mazzitelli
 */
public abstract class AbstractBundleTask extends Task {

    /**
     * Returns the specific {@link BundleAntProject} object that is invoking this task.
     * This task can call methods on the returned project object to inform the project
     * of things this task is doing.
     * 
     * @return the bundle Ant project object
     */
    @Override
    public BundleAntProject getProject() {
        return (BundleAntProject) super.getProject();
    }
}