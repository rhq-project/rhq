/*
* RHQ Management Platform
* Copyright (C) 2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.scheduler.jobs;

import java.util.HashSet;
import java.util.Set;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import org.rhq.core.domain.content.Repo;
import org.rhq.enterprise.server.test.AbstractEJB3Test;

/**
 * @author Jason Dobies
 */
public class RepoSyncJobTest extends AbstractEJB3Test {

    private static final boolean ENABLED = true;

    @BeforeMethod
    public void beforeMethod() throws Exception {
        prepareScheduler();
        prepareForTestAgents();
    }

    @AfterMethod
    public void afterMethod() throws Exception {
        unprepareForTestAgents();
        unprepareScheduler();
    }

    @Test(enabled = ENABLED)
    public void createJobDataMap() throws Exception {
        // Setup
        String repoName = "testRepo";
        Repo repo = new Repo(repoName);

        JobDataMap existingMap = new JobDataMap();
        JobDetail detail = new JobDetail();
        detail.setJobDataMap(existingMap);

        // Test
        JobDataMap nullDetailsMap = RepoSyncJob.createJobDataMap(null, repo);
        JobDataMap existingDetailsMap = RepoSyncJob.createJobDataMap(detail, repo);

        // Verify
        assert nullDetailsMap.getString(RepoSyncJob.KEY_REPO_NAME).equals(repoName);

        assert existingDetailsMap.getString(RepoSyncJob.KEY_REPO_NAME).equals(repoName);
        assert existingDetailsMap == existingMap;
    }

    @Test(enabled = ENABLED)
    public void createJobName() throws Exception {
        // Setup
        Repo repo = new Repo("testRepo");

        // Test
        Set<String> jobNames = new HashSet<String>(1);

        jobNames.add(RepoSyncJob.createJobName(repo));
        jobNames.add(RepoSyncJob.createJobName(repo));
        jobNames.add(RepoSyncJob.createJobName(repo));
        jobNames.add(RepoSyncJob.createJobName(repo));
        jobNames.add(RepoSyncJob.createJobName(repo));

        // Verify
        assert jobNames.size() == 1 : "Number of jobs found: " + jobNames.size();
    }

    @Test(enabled = ENABLED)
    public void createUniqueJobName() throws Exception {
        // Setup
        Repo repo = new Repo("testRepo");

        // Test
        Set<String> jobNames = new HashSet<String>(1);

        jobNames.add(RepoSyncJob.createUniqueJobName(repo));
        Thread.sleep(100); // Need a quick pause to allow for uniqueness
        jobNames.add(RepoSyncJob.createUniqueJobName(repo));
        Thread.sleep(100);
        jobNames.add(RepoSyncJob.createUniqueJobName(repo));
        Thread.sleep(100);
        jobNames.add(RepoSyncJob.createUniqueJobName(repo));
        Thread.sleep(100);
        jobNames.add(RepoSyncJob.createUniqueJobName(repo));

        // Verify
        assert jobNames.size() == 5 : "Number of jobs found: " + jobNames.size();
    }

}
