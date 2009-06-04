 /*
 * Jopr Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.plugins.jbossas5;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.deployers.spi.management.deploy.DeploymentManager;
import org.jboss.deployers.spi.management.deploy.DeploymentProgress;
import org.jboss.deployers.spi.management.deploy.DeploymentStatus;
import org.jboss.managed.api.ManagedComponent;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.ManagedProperty;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.PropertyList;
import org.rhq.core.domain.configuration.PropertyMap;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.measurement.MeasurementDataNumeric;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.measurement.calltime.CallTimeDataValue;
import org.rhq.core.pluginapi.operation.OperationResult;
import org.rhq.plugins.jbossas5.util.DeploymentUtils;
import org.mc4j.ems.connection.EmsConnection;
import org.mc4j.ems.connection.bean.EmsBean;
import org.mc4j.ems.connection.bean.attribute.EmsAttribute;

 /**
  * A plugin component for managing an EJB3 session bean.
  *
  * @author Greg Hinkle
  * @author Ian Springer
  * @author Heiko W. Rupp
  */
//
// ***TODO*** ManagedComponentComponent is now used for managing session beans. This class will eventually need to
//            subclass that class. It will also need to be reworked to use the PS rather than JMX to retrieve the
//            method invocation call-time stats, once ALR is done implementing those on the JBAS side. (ips, 06/02/09)
//
 public class EJB3BeanComponent extends ManagedComponentComponent {
     private final Log log = LogFactory.getLog(EJB3BeanComponent.class);

     private Map<Integer, CallTimeData> previousRawCallTimeDatas = new HashMap<Integer,CallTimeData>();

     @Override
     public OperationResult invokeOperation(String name, Configuration parameters) throws Exception {
         if ("viewMethodStats".equals(name)) {

             ManagedProperty invocationStatistics = getInvocationStatistics();

             OperationResult result = new OperationResult();
             PropertyList methodList = new PropertyList("methods");
             result.getComplexResults().put(methodList);

             Map<String, Object> stats = getStats(invocationStatistics);
             for (String methodName : stats.keySet()) {
                 Object timeStatistic = stats.get(methodName);

                 Long count = (Long) timeStatistic.getClass().getField("count").get(timeStatistic);
                 Long minTime = (Long) timeStatistic.getClass().getField("minTime").get(timeStatistic);
                 Long maxTime = (Long) timeStatistic.getClass().getField("maxTime").get(timeStatistic);
                 Long totalTime = (Long) timeStatistic.getClass().getField("totalTime").get(timeStatistic);

                 PropertyMap method = new PropertyMap("method", new PropertySimple("methodName", methodName),
                     new PropertySimple("count", count), new PropertySimple("minTime", minTime), new PropertySimple(
                         "maxTime", maxTime), new PropertySimple("totalTime", totalTime));
                 methodList.add(method);
             }

             return result;
         }

         return super.invokeOperation(name, parameters);
     }

     @Override
     public void getValues(MeasurementReport report, Set<MeasurementScheduleRequest> schedules) {
         Set<MeasurementScheduleRequest> numericMetricSchedules = new LinkedHashSet<MeasurementScheduleRequest>();
         for (MeasurementScheduleRequest schedule : schedules) {
             if (schedule.getDataType() == DataType.MEASUREMENT) {
                 numericMetricSchedules.add(schedule);
             } else if (schedule.getName().equals("MethodInvocationTime")) {
                 ManagedProperty invocationStatistics;
                 try {
                     invocationStatistics = getInvocationStatistics();
                 } catch (Exception e) {
                     // This will be fairly common, since only JBossAS 4.2.x provides this operation, so don't log an
                     // error.
                     continue;
                 }
                 try {
                     long lastResetTime = getLastResetTime(invocationStatistics);
                     Map<String, Object> stats = getStats(invocationStatistics);
                     long collectionTime = System.currentTimeMillis();
                     if (!stats.isEmpty()) {
                         CallTimeData callTimeData = createCallTimeData(schedule, stats, new Date(lastResetTime),
                             new Date(collectionTime));
                         report.addData(callTimeData);
                     }
                 } catch (Exception e) {
                     log.error("Failed to retrieve EJB3 call-time data.", e);
                 }
             }
         }


         try {
             super.getValues(report, numericMetricSchedules);
         } catch (Exception e) {
             e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
         }
     }

     private CallTimeData createCallTimeData(MeasurementScheduleRequest schedule, Map<String, Object> stats,
         Date lastResetTime, Date collectionTime) throws Exception {
         CallTimeData previousRawCallTimeData = this.previousRawCallTimeDatas.get(schedule.getScheduleId());
         CallTimeData rawCallTimeData = new CallTimeData(schedule);
         this.previousRawCallTimeDatas.put(schedule.getScheduleId(), rawCallTimeData);
         CallTimeData callTimeData = new CallTimeData(schedule);
         for (String methodName : stats.keySet()) {
             Object timeStatistic = stats.get(methodName);
             long minTime = (Long) timeStatistic.getClass().getField("minTime").get(timeStatistic);
             long maxTime = (Long) timeStatistic.getClass().getField("maxTime").get(timeStatistic);
             long totalTime = (Long) timeStatistic.getClass().getField("totalTime").get(timeStatistic);
             long count = (Long) timeStatistic.getClass().getField("count").get(timeStatistic);
             if (count == 0) {
                 // Don't bother even adding data for this method if the call count is 0.
                 continue;
             }

             rawCallTimeData.addAggregatedCallData(methodName, lastResetTime, collectionTime, minTime, maxTime,
                 totalTime, count);

             // Now compute the adjusted data, which is what we will report back to the server.
             CallTimeDataValue previousValue = (previousRawCallTimeData != null) ? previousRawCallTimeData.getValues()
                 .get(methodName) : null;
             boolean supercedesPrevious = ((previousValue != null) && (previousValue.getBeginTime() == lastResetTime
                 .getTime()));
             Date beginTime = lastResetTime;
             if (supercedesPrevious) {
                 // The data for this method hasn't been reset since the last time we collected it.
                 long countSincePrevious = count - previousValue.getCount();
                 if (countSincePrevious > 0) {
                     // There have been new calls since the last time we collected data
                     // for this method. Adjust the time span to begin at the end of the
                     // time span from the previous collection.
                     beginTime = new Date(previousValue.getEndTime());

                     // Adjust the total and count to reflect the adjusted time span;
                     // do so by subtracting the previous values from the current values.
                     // NOTE: It isn't possible to figure out the minimum and maximum for
                     // the adjusted time span, so just leave them be. If they happen
                     // to have changed since the previous collection, they will be
                     // accurate; otherwise they will not.
                     count = countSincePrevious;
                     totalTime = totalTime - (long) previousValue.getTotal();
                 }
                 // else, the count hasn't changed, so don't bother adjusting the data;
                 // when the JON server sees the data has the same begin time as
                 // previously persisted data, it will replace the previous data with the
                 // updated data (which will basically have a later end time)
             }

             callTimeData.addAggregatedCallData(methodName, beginTime, collectionTime, minTime, maxTime, totalTime,
                 count);
         }

         return callTimeData;
     }

     /**
      * Helper to obtain the statistics from the remote server
      * @return
      * @throws Exception
      */
     private ManagedProperty getInvocationStatistics() throws Exception {

         DeploymentManager deploymentManager = getConnection().getDeploymentManager();
         DeploymentProgress progress;

         String name = getManagedComponent().getName();
         // Get component sub type from original type
         ComponentType ct = getManagedComponent().getType();
         name = name.replace("instance","invocation");
         ManagedComponent invComp = getConnection().getManagementView().getComponent(name,ct);
         if (invComp==null) {
             log.warn("No component with name " + name + " and type " + ct + " found");
             return null;
         }

         ManagedProperty stats = invComp.getProperty("stats");
         ManagedProperty lastResetTime = invComp.getProperty("lastResetTime"); // TODO what to do with this?

         return stats;
     }

     private long getLastResetTime(ManagedProperty invocationStatistics) throws Exception {
         // TODO implement this
         Field field = invocationStatistics.getClass().getField("lastResetTime");
         return (Long) field.get(invocationStatistics);
     }

     private Map<String, Object> getStats(ManagedProperty invocationStatistics) throws Exception {
        // TODO implement this
         return (Map<String, Object>) new HashMap<String,Object>();
     }
 }