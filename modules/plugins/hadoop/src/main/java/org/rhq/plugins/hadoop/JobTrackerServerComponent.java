/*
 * RHQ Management Platform
 * Copyright (C) 2005-2012 Red Hat, Inc.
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

package org.rhq.plugins.hadoop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rhq.core.domain.content.transfer.ResourcePackageDetails;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.resource.CreateResourceStatus;
import org.rhq.core.pluginapi.content.ContentContext;
import org.rhq.core.pluginapi.content.ContentServices;
import org.rhq.core.pluginapi.event.EventContext;
import org.rhq.core.pluginapi.event.EventPoller;
import org.rhq.core.pluginapi.event.log.LogFileEventPoller;
import org.rhq.core.pluginapi.inventory.CreateChildResourceFacet;
import org.rhq.core.pluginapi.inventory.CreateResourceReport;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.plugins.hadoop.calltime.HadoopEventAndCalltimeDelegate;
import org.rhq.plugins.hadoop.calltime.JobSummary;

/**
 * Resource component for Hadoop JobTracker.
 *
 * @author Lukas Krejci
 */
public class JobTrackerServerComponent extends HadoopServerComponent implements CreateChildResourceFacet {

    private static final String SYNTHETIC_METRICS_PREFIX = "_synthetic_";
    private static final String JOB_DURATION_METRIC_NAME = "_synthetic_jobDuration";
    private static final String JOB_PRE_START_DELAY_METRIC_NAME = "_synthetic_jobPreStartDelay";
    private static final String JOB_SUCCESS_RATE_METRIC_NAME = "_synthetic_jobSuccessRate";

    private static final String DEFAULT_JOB_STORAGE_NAME = "__dataDir";    
    private static final String JOB_STORAGE_PROP_NAME = "jobStorage";
    
    private Map<String, Map<String, Set<JobSummary>>> unprocessedCalltimeMeasurements = new HashMap<String, Map<String,Set<JobSummary>>>();

    private HadoopEventAndCalltimeDelegate logProcessor;

    @Override
    @SuppressWarnings({ "rawtypes" })
    public void start(ResourceContext context) throws InvalidPluginConfigurationException, Exception {
        super.start(context);

        Set<MeasurementDefinition> measDefinitions = context.getResourceType().getMetricDefinitions();
        for (MeasurementDefinition measDefinition : measDefinitions) {
            if (measDefinition.getDataType() == DataType.CALLTIME) {
                unprocessedCalltimeMeasurements.put(measDefinition.getName(), new HashMap<String, Set<JobSummary>>());
            }
        }

        //TODO restore unprocessed calltime data from persistent storage in data dir.
    }

    @Override
    public void stop() {
        //TODO save unprocessed calltime data to persistent storage in data dir.
        super.stop();
    }

    public File getJobJarDataDir() {
        String dataDirName = getResourceContext().getPluginConfiguration().getSimpleValue(JOB_STORAGE_PROP_NAME, DEFAULT_JOB_STORAGE_NAME);
        
        File dataDir = null;
        
        if (DEFAULT_JOB_STORAGE_NAME.equals(dataDirName)) {
            dataDir = new File(getResourceContext().getDataDirectory(), "jobJars");
            dataDir.mkdirs();
        } else {            
            dataDir = new File(dataDirName);
            if (!dataDir.isAbsolute()) {
                File hadoopHome = getHomeDir();
                
                dataDir = new File(hadoopHome, dataDirName);
            }
        }
        
        return dataDir;
    }
    
    @Override
    public CreateResourceReport createResource(CreateResourceReport report) {
        if (!JobJarComponent.CONTENT_TYPE_NAME.equals(report.getPackageDetails().getKey().getPackageTypeName())) {
            report.setStatus(CreateResourceStatus.FAILURE);
            report.setErrorMessage("Unknown content type");
            return report;
        }
        
        File dataDir = getJobJarDataDir();
        
        ResourcePackageDetails packageDetails = report.getPackageDetails();
        
        File jobJar = new File(dataDir, packageDetails.getFileName());
        
        FileOutputStream jobJarStream = null;
        try {
            jobJarStream = new FileOutputStream(jobJar); 
        } catch (FileNotFoundException e) {
            report.setErrorMessage("Could not create the job jar file on the agent: " + e.getMessage());
            return report;
        }
        
        ContentContext contentContext = getResourceContext().getContentContext();
        ContentServices contentServices = contentContext.getContentServices();
        contentServices.downloadPackageBitsForChildResource(contentContext, JobJarComponent.RESOURCE_TYPE_NAME, packageDetails.getKey(), jobJarStream);
        
        try {
            jobJarStream.close();
        } catch (IOException e) {
            //hmmm, do I care?
        }
        
        report.setResourceKey(jobJar.getAbsolutePath());
        report.setResourceName(jobJar.getName());
        
        report.setStatus(CreateResourceStatus.SUCCESS);
        
        return report;
    }

    @Override
    public ResourceContext<?> getResourceContext() {
        // TODO Auto-generated method stub
        return super.getResourceContext();
    }
    
    @Override
    protected void handleMetric(MeasurementReport report, MeasurementScheduleRequest request) throws Exception {
        if (request.getName().startsWith(SYNTHETIC_METRICS_PREFIX)) {
            if (logProcessor != null) {
                updateUnprocessedMeasurements();
                Map<String, Set<JobSummary>> pendingJobs = unprocessedCalltimeMeasurements.get(request.getName());
                report.addData(createCalltimeData(request, pendingJobs));
                pendingJobs.clear();
            }
        } else {
            super.handleMetric(report, request);
        }
    }

    @Override
    protected EventPoller createNewEventPoller(EventContext eventContext, File logFile) {
        logProcessor = new HadoopEventAndCalltimeDelegate(LOG_EVENT_TYPE, logFile);
        return new LogFileEventPoller(eventContext, LOG_EVENT_TYPE, logFile, logProcessor);
    }

    @Override
    protected void discardPoller() {
        logProcessor = null;
    }

    private void updateUnprocessedMeasurements() {
        if (logProcessor == null) {
            return;
        }
        
        Set<JobSummary> newJobs = logProcessor.drainAccumulatedJobs();

        //the job summaries are the base for all (currently both) types of calltime metrics
        //we therefore add the newJobs to all members of the unprocessed map..
        for (Map.Entry<String, Map<String, Set<JobSummary>>> e : unprocessedCalltimeMeasurements.entrySet()) {
            Map<String, Set<JobSummary>> jobsByName = e.getValue();

            for (JobSummary newJob : newJobs) {
                Set<JobSummary> unprocessed = jobsByName.get(newJob.getJobName());
                if (unprocessed == null) {
                    unprocessed = new HashSet<JobSummary>();
                    jobsByName.put(newJob.getJobName(), unprocessed);
                }
                unprocessed.add(newJob);
            }
        }
    }

    private CallTimeData
        createCalltimeData(MeasurementScheduleRequest request, Map<String, Set<JobSummary>> pendingJobs) {
        CallTimeData ret = new CallTimeData(request);

        String metricName = request.getName();

        for (Map.Entry<String, Set<JobSummary>> e : pendingJobs.entrySet()) {
            String jobName = e.getKey();
            Set<JobSummary> jobs = e.getValue();

            if (JOB_DURATION_METRIC_NAME.equals(metricName)) {
                initJobDurationData(ret, jobName, jobs);
            } else if (JOB_PRE_START_DELAY_METRIC_NAME.equals(metricName)) {
                initPreStartDelayData(ret, jobName, jobs);
            } else if (JOB_SUCCESS_RATE_METRIC_NAME.equals(metricName)) {
                initSuccessRateData(ret, jobName, jobs);
            }
        }

        return ret;
    }

    private void initJobDurationData(CallTimeData data, String jobName, Set<JobSummary> pendingJobs) {
        long beginTime = Long.MAX_VALUE;
        long endTime = 0;
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        long count = pendingJobs.size();

        for (JobSummary job : pendingJobs) {
            if (job.getStartTime() < beginTime) {
                beginTime = job.getStartTime();
            }
            
            if (job.getEndTime() > endTime) {
                endTime = job.getEndTime();
            }
            
            long duration = job.getEndTime() - job.getStartTime();
            
            totalTime += duration;
            
            if (duration < minTime) {
                minTime = duration;
            }
            
            if (duration > maxTime) {
                maxTime = duration;
            }
        }
        
        data.addAggregatedCallData(jobName, new Date(beginTime), new Date(endTime), minTime, maxTime, totalTime, count);
    }

    private void initPreStartDelayData(CallTimeData data, String jobName, Set<JobSummary> pendingJobs) {
        long beginTime = Long.MAX_VALUE;
        long endTime = 0;
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        long count = pendingJobs.size();

        for (JobSummary job : pendingJobs) {
            if (job.getSubmitTime() < beginTime) {
                beginTime = job.getSubmitTime();
            }
            
            if (job.getStartTime() > endTime) {
                endTime = job.getStartTime();
            }
            
            long duration = job.getStartTime() - job.getSubmitTime();
            
            totalTime += duration;
            
            if (duration < minTime) {
                minTime = duration;
            }
            
            if (duration > maxTime) {
                maxTime = duration;
            }
        }
        
        data.addAggregatedCallData(jobName, new Date(beginTime), new Date(endTime), minTime, maxTime, totalTime, count);
    }

    private void initSuccessRateData(CallTimeData data, String jobName, Set<JobSummary> pendingJobs) {
        long beginTime = Long.MAX_VALUE;
        long endTime = 0;
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        long count = pendingJobs.size();

        for (JobSummary job : pendingJobs) {
            if (job.getStartTime() < beginTime) {
                beginTime = job.getStartTime();
            }
            
            if (job.getEndTime() > endTime) {
                endTime = job.getEndTime();
            }
            
            long duration = job.isSucceeded() ? 1 : 0;
            
            totalTime += duration;
            
            if (duration < minTime) {
                minTime = duration;
            }
            
            if (duration > maxTime) {
                maxTime = duration;
            }
        }
        
        data.addAggregatedCallData(jobName, new Date(beginTime), new Date(endTime), minTime, maxTime, totalTime, count);
    }
}
