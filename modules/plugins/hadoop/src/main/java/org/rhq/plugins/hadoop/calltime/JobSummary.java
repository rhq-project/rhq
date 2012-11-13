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

package org.rhq.plugins.hadoop.calltime;

import java.io.Serializable;

/**
 * Description of a Hadoop map-reduce job.
 *
 * @author Lukas Krejci
 */
public class JobSummary implements Serializable {
    
    private static final long serialVersionUID = 1L;

    public static final String EXPECTED_LOGGER = "org.apache.hadoop.mapred.JobInProgress$JobSummary";
    
    private String jobId;
    private String jobName;
    private long startTime;
    private long endTime;
    private long submitTime;
    private int mapTasks;
    private int reduceTasks;    
    private boolean succeeded;
    
    public static JobSummary parseJobSummaryLogEntry(String logEntry) {
        //2012-08-03 13:55:45,224 INFO org.apache.hadoop.mapred.JobInProgress$JobSummary: jobId=job_201208031353_0001,submitTime=1343994869505,
        //launchTime=1343994869801,firstMapTaskLaunchTime=1343994878611,firstReduceTaskLaunchTime=1343994884642,
        //firstJobSetupTaskLaunchTime=1343994872548,firstJobCleanupTaskLaunchTime=1343994938904,finishTime=1343994944924,numMaps=16,
        //numSlotsPerMap=1,numReduces=1,numSlotsPerReduce=1,user=lkrejci,queue=default,status=SUCCEEDED,mapSlotSeconds=76,reduceSlotsSeconds=53,
        //clusterMapCapacity=2,clusterReduceCapacity=2,jobName=grep-search
        
        JobSummary ret = new JobSummary();
        
        int loggerIdx = logEntry.indexOf(EXPECTED_LOGGER);
        if (loggerIdx < 0) {
            return null;
        }
        
        loggerIdx += EXPECTED_LOGGER.length() + 2; //account for the colon and go 1 past it
        
        String[] props = logEntry.substring(loggerIdx).split(",");
        
        for(String prop : props) {
            String[] keyValue = prop.split("=");
            
            if (keyValue.length != 2) {
                continue;
            }
            
            String key = keyValue[0];
            String value = keyValue[1];
            
            if ("jobId".equals(key)) {
                ret.setJobId(value);
            } else if ("submitTime".equals(key)) {
                ret.setSubmitTime(Long.parseLong(value));
            } else if ("launchTime".equals(key)) {
                ret.setStartTime(Long.parseLong(value));
            } else if ("finishTime".equals(key)) {
                ret.setEndTime(Long.parseLong(value));
            } else if ("numMaps".equals(key)) {
                ret.setMapTasks(Integer.parseInt(value));
            } else if ("numReduces".equals(key)) {
                ret.setReduceTasks(Integer.parseInt(value));
            } else if ("status".equals(key)) {
                ret.setSucceeded("SUCCEEDED".equals(value));
            } else if ("jobName".equals(key)) {
                ret.setJobName(value);
            }
        }
        
        return ret;
    }
    
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getMapTasks() {
        return mapTasks;
    }

    public void setMapTasks(int mapTasks) {
        this.mapTasks = mapTasks;
    }

    public int getReduceTasks() {
        return reduceTasks;
    }

    public void setReduceTasks(int reduceTasks) {
        this.reduceTasks = reduceTasks;
    }

    public boolean isSucceeded() {
        return succeeded;
    }
    
    public void setSucceeded(boolean succeeded) {
        this.succeeded = succeeded;
    }

    public long getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(long submitTime) {
        this.submitTime = submitTime;
    }
    
    @Override
    public int hashCode() {
        return jobId == null ? 0 : jobId.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        
        if (!(o instanceof JobSummary)) {
            return false;
        }
        
        JobSummary other = (JobSummary) o;
        
        return getJobId() == null ? (other.getJobId() == null) : (getJobId().equals(other.getJobId()));
    }
}
