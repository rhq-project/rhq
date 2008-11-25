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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.clientapi.descriptor.perspective.Perspective;
import org.rhq.core.clientapi.descriptor.perspective.Task;

/**
 * Implementation of PerspectiveManager functionality.
 */
@Stateless
// @WebService(endpointInterface = "org.rhq.enterprise.server.perspective.PerspectiveManagerRemote")
public class PerspectiveManagerBean implements PerspectiveManagerLocal, PerspectiveManagerRemote {
    private Log log = LogFactory.getLog(PerspectiveManagerBean.class.getName());

    /**
     * {@inheritDoc}
     */
    public List<Perspective> getAllPerspectives() {
        return new LinkedList<Perspective>(PerspectiveCacheService.getInstance().getPerspectivesByName().values());
    }

    /**
     * {@inheritDoc}
     */
    public Perspective getPerspective(String name) {
        return PerspectiveCacheService.getInstance().getPerspectivesByName().get(name);
    }

    /**
     * Get the Task objects that are related to the passed in context name
     *
     * @param  contextName to lookup
     *
     * @return List<Task> found. null if no matches
     */
    public List<Task> getTasks(String contextName) {
        return getTasks(contextName, (Object[]) null);
    }

    /**
     * {@inheritDoc}
     */
    public List<Task> getTasks(String contextName, Object... args) {
        log.debug("getTasks called: " + contextName + " id: " + args);
        Map<String, List<Task>> tasksByContext = PerspectiveCacheService.getInstance().getTasksByContext();
        if (tasksByContext.containsKey(contextName)) {
            log.debug("getTasks tasksByContext containsKey, lets fix the paths");
            List<Task> retval = new LinkedList<Task>();
            List<Task> tasks = tasksByContext.get(contextName);

            // Massage the paths on the fetched Task objects to fill out the
            // parameter values so the user gets a usable path to use
            for (Task t : tasks) {
                Task newTask = new Task();
                newTask.setContext(t.getContext());
                newTask.setDescription(t.getDescription());
                newTask.setName(t.getName());

                // path="/pulp/channels/clone?cid={0}"
                String newPath = MessageFormat.format(t.getPath(), args);

                // String newPath = t.getPath().replaceAll(t.getId(), contextId);
                newTask.setPath(newPath);
                log.debug("getTasks new path: " + newPath);
                retval.add(newTask);
            }

            return retval;
        } else {
            log.debug("getTasks no matches, Returning null");
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Task> getTasksWithArgs(String contextName, Object... args) {
        return getTasks(contextName, args);
    }

    private void listPathFiles(String path) {
        URL defDirUri = getClass().getClassLoader().getResource(path);
        log.debug("listPathFiles.URI: " + defDirUri);
        File defDir = null;
        try {
            defDir = new File(defDirUri.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        File[] files = defDir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File currFile = files[i];
            log.debug("listPathFiles.file: " + currFile);
        }
    }
}