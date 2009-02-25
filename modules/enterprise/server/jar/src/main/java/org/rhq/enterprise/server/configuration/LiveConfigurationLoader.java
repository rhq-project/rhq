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
package org.rhq.enterprise.server.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * @author Ian Springer
 */
public class LiveConfigurationLoader
{
    private static LiveConfigurationLoader ourInstance = new LiveConfigurationLoader();

    private ConfigurationManagerLocal configurationManager = LookupUtil.getConfigurationManager();
    private SubjectManagerLocal subjectManager = LookupUtil.getSubjectManager();

    public static LiveConfigurationLoader getInstance()
    {
        return ourInstance;
    }

    /**
     *
     * @param resources
     * @param timeout the number of seconds before this call should timeout and
     * @return
     * @throws Exception
     */
    public Map<Integer, Configuration> loadLiveResourceConfigurations(final Set<Resource> resources, long timeout)
    {
        try
        {
            FutureTask<Map<Integer, Configuration>> task = new FutureTask(new Callable<Map<Integer, Configuration>>()
            {
                public Map<Integer, Configuration> call() throws Exception
                {
                    return loadLiveResourceConfigurations(resources);
                }
            });
            new Thread(task).start();
            return task.get(timeout, TimeUnit.SECONDS);
        }
        catch (TimeoutException e)
        {
            throw new RuntimeException("Timed out after " + timeout + " seconds while retrieving live Resource configurations.");
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to retrieve live Resource configurations.", e);
        }
    }

    private Map<Integer, Configuration> loadLiveResourceConfigurations(Set<Resource> resources) throws Exception
    {
        Map<Integer, Configuration> liveConfigs = new HashMap();
        for (Resource resource : resources)
        {
            Configuration liveConfig = this.configurationManager.getLiveResourceConfiguration(
                this.subjectManager.getOverlord(), resource.getId(), false);
            if (liveConfig == null)
                throw new Exception("Failed to obtain live Resource configuration for " + resource + ".");
            liveConfigs.put(resource.getId(), liveConfig);
        }
        return liveConfigs;
    }

    private LiveConfigurationLoader()
    {
    }
}
