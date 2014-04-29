/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.rhq.enterprise.server.measurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.criteria.CallTimeDataCriteria;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.calltime.CallTimeData;
import org.rhq.core.domain.measurement.calltime.CallTimeDataComposite;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.server.RHQConstants;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheManagerLocal;
import org.rhq.enterprise.server.alert.engine.AlertConditionCacheStats;
import org.rhq.enterprise.server.auth.SubjectManagerLocal;
import org.rhq.enterprise.server.authz.AuthorizationManagerLocal;
import org.rhq.enterprise.server.authz.PermissionException;
import org.rhq.enterprise.server.measurement.instrumentation.MeasurementMonitor;
import org.rhq.enterprise.server.storage.StorageClientManager;
import org.rhq.enterprise.server.util.CriteriaQueryGenerator;
import org.rhq.enterprise.server.util.CriteriaQueryRunner;
import org.rhq.server.metrics.CallTimeDAO;
import org.rhq.server.metrics.CallTimeRow;

import com.google.common.util.concurrent.FutureCallback;

/**
 * The manager for call-time metric data.
 *
 * @author Ian Springer
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class CallTimeDataManagerBean implements CallTimeDataManagerLocal, CallTimeDataManagerRemote {

    private final Log log = LogFactory.getLog(CallTimeDataManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @EJB
    private AuthorizationManagerLocal authorizationManager;

    @EJB
    private CallTimeDataManagerLocal callTimeDataManager;

    @EJB
    private AlertConditionCacheManagerLocal alertConditionCacheManager;

    @EJB
    private StorageClientManager storageClientManager;

    @EJB
    private MeasurementScheduleManagerLocal measurementScheduleManager;

    @EJB
    private SubjectManagerLocal subjectManager;

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void addCallTimeData(@NotNull final Set<CallTimeData> data) {
        if (data.isEmpty()) {
            return;
        }

        final long startTime = System.currentTimeMillis();
        CallTimeDAO dao = storageClientManager.getCallTimeDAO();
        dao.insert(data, new FutureCallback<Object>() {

            @Override
            public void onFailure(Throwable e) {
                log.error("Error persisting calltime data " + data.size(), e);
            }

            @Override
            public void onSuccess(Object result) {
                MeasurementMonitor.getMBean().incrementCallTimeInsertTime(System.currentTimeMillis() - startTime);
                notifyAlertConditionCacheManager("insertCallTimeDataValues",
                        data.toArray(new CallTimeData[data.size()]));
            }
        });

    }

    public PageList<CallTimeDataComposite> findCallTimeDataRawForResource(Subject subject,
                int scheduleId, long beginTime, long endTime, PageControl pageControl)
    {
        MeasurementSchedule schedule = entityManager.find(MeasurementSchedule.class, scheduleId);
        int resourceId = schedule.getResource().getId();
        if (authorizationManager.canViewResource(subject, resourceId) == false) {
            throw new PermissionException("User [" + subject
                + "] does not have permission to view call time data for measurementSchedule[id=" + scheduleId
                + "] and resource[id=" + resourceId + "]");
        }

        CallTimeDAO dao = storageClientManager.getCallTimeDAO();
        List<CallTimeRow> select = dao.select(scheduleId, new Date(beginTime), new Date(endTime));

        // TODO sorting fields
        // TODO paging

        List<CallTimeDataComposite> comps = new ArrayList<CallTimeDataComposite>();
        for (CallTimeRow row : select) {
            comps.add(row.toComposite());
        }

        return new PageList<CallTimeDataComposite>(comps, (int) comps.size(), pageControl);
    }

    @SuppressWarnings("unchecked")
    public PageList<CallTimeDataComposite> findCallTimeDataForResource(Subject subject, int scheduleId, long beginTime,
        long endTime, PageControl pageControl) {

        MeasurementSchedule schedule = entityManager.find(MeasurementSchedule.class, scheduleId);
        if (schedule == null) {
            return new PageList<CallTimeDataComposite>(pageControl);
        }

        int resourceId = schedule.getResource().getId();
        if (authorizationManager.canViewResource(subject, resourceId) == false) {
            throw new PermissionException("User [" + subject
                + "] does not have permission to view call time data for measurementSchedule[id=" + scheduleId
                + "] and resource[id=" + resourceId + "]");
        }

        CallTimeDAO dao = storageClientManager.getCallTimeDAO();
        List<CallTimeRow> select = dao.select(scheduleId, null, new Date(beginTime), new Date(endTime));

        Aggregator a = new Aggregator();
        a.aggregate(select);
        List<CallTimeDataComposite> comps = a.result();

        return new PageList<CallTimeDataComposite>(comps, comps.size(), pageControl);
    }

    static class Aggregator {
        Map<String, List<CallTimeRow>> rows = new TreeMap<String, List<CallTimeRow>>();

        private void aggregate(List<CallTimeRow> select) {
            for (CallTimeRow row : select) {
                String d = row.getDest();
                List<CallTimeRow> list = rows.get(d);
                if (list == null) {
                    rows.put(d, list = new ArrayList<CallTimeRow>());
                }
                list.add(row);
            }
        }

        private List<CallTimeDataComposite> result() {
            List<CallTimeDataComposite> comps = new LinkedList<CallTimeDataComposite>();
            for (Entry<String, List<CallTimeRow>> rowlist : rows.entrySet()) {
                CallTimeRow aggregate = CallTimeRow.aggregate(rowlist.getValue());
                comps.add(aggregate.toComposite());
            }
            return comps;
        }
    }

    public PageList<CallTimeDataComposite> findCallTimeDataForCompatibleGroup(Subject subject, int groupId,
        long beginTime, long endTime, PageControl pageControl) {
        return findCallTimeDataForContext(subject, EntityContext.forGroup(groupId), beginTime, endTime, null,
            pageControl);
    }

    public PageList<CallTimeDataComposite> findCallTimeDataForAutoGroup(Subject subject, int parentResourceId,
        int childResourceTypeId, long beginTime, long endTime, PageControl pageControl) {
        return findCallTimeDataForContext(subject, EntityContext.forAutoGroup(parentResourceId, childResourceTypeId),
            beginTime, endTime, null, pageControl);
    }

    public PageList<CallTimeDataComposite> findCallTimeDataForContext(Subject subject, EntityContext context,
        long beginTime, long endTime, String destination, PageControl pageControl) {

        CallTimeDataCriteria criteria = new CallTimeDataCriteria();
        criteria.addFilterBeginTime(beginTime);
        criteria.addFilterEndTime(endTime);
        if (destination != null && !destination.trim().equals("")) {
            criteria.addFilterDestination(destination);
        }

        criteria.setPageControl(pageControl);

        return findCallTimeDataForContext(subject, context, criteria);
    }

    public PageList<CallTimeDataComposite> findCallTimeDataForContext(Subject subject, EntityContext context,
        CallTimeDataCriteria criteria) {

        /*
        PageControl pageControl = criteria.getPageControlOverrides();
        if (pageControl != null) {
            pageControl.initDefaultOrderingField("SUM(calltimedatavalue.total)/SUM(calltimedatavalue.count)",
                PageOrdering.DESC); // only set if no ordering yet specified
            pageControl.addDefaultOrderingField("calltimedatavalue.key.callDestination", PageOrdering.ASC); // add this to sort, if not already specified
        }
        */

        if (context.type == EntityContext.Type.Resource) {
            criteria.addFilterResourceId(context.resourceId);
        } else if (context.type == EntityContext.Type.ResourceGroup) {
            criteria.addFilterResourceGroupId(context.groupId);
        } else if (context.type == EntityContext.Type.AutoGroup) {
            criteria.addFilterAutoGroupParentResourceId(context.parentResourceId);
            criteria.addFilterAutoGroupResourceTypeId(context.resourceTypeId);
        }
        criteria.setSupportsAddSortId(false);

        MeasurementScheduleCriteria criteria2 = new MeasurementScheduleCriteria(criteria);
        criteria2.addFilterDataType(DataType.CALLTIME);
        PageList<MeasurementSchedule> schedules =
                measurementScheduleManager.findSchedulesByCriteria(subjectManager.getOverlord(), criteria2);

        Aggregator a = new Aggregator();

        for (MeasurementSchedule schedule : schedules) {
            int scheduleId = schedule.getId();
            CallTimeDAO dao = storageClientManager.getCallTimeDAO();
            Long begin = criteria.getFilterBeginTime();
            Date beginD = begin == null ? new Date(0) : new Date(begin);
            Long end = criteria.getFilterBeginTime();
            Date endD = end == null ? new Date(Long.MAX_VALUE) : new Date(end);

            List<CallTimeRow> select = dao.select(scheduleId, criteria.getFilterDestination(), beginD, endD);
            a.aggregate(select);
        }

        List<CallTimeDataComposite> comps = a.result();
        for (Iterator<CallTimeDataComposite> i = comps.iterator(); i.hasNext(); ) {
            Double min = criteria.getFilterMinimum();
            Double max = criteria.getFilterMaximum();
            Double total = criteria.getFilterTotal();
            Long count = criteria.getFilterCount();
            CallTimeDataComposite comp = i.next();
            if (min != null && comp.getMinimum() < min) {
                i.remove();
            }
            if (max != null && comp.getMaximum() > max) {
                i.remove();
            }
            if (total != null && comp.getTotal() < total) {
                i.remove();
            }
            if (count != null && comp.getCount() < count) {
                i.remove();
            }
        }

        Collections.sort(comps, new Comparator<CallTimeDataComposite>() {

            @Override
            public int compare(CallTimeDataComposite o1, CallTimeDataComposite o2) {
                if (o1.getAverage() == o2.getAverage()) { // zero?
                    return o1.getCallDestination().compareTo(o2.getCallDestination());
                }
                // sort by average (descending)
                return o1.getAverage() > o2.getAverage() ? -1 : 1;
            }

        });

        PageList<CallTimeDataComposite> results2 = new PageList<CallTimeDataComposite>(comps, PageControl.getUnlimitedInstance());
        return results2;
    }

    private void notifyAlertConditionCacheManager(String callingMethod, CallTimeData... data) {
        AlertConditionCacheStats stats = alertConditionCacheManager.checkConditions(data);
        if (log.isDebugEnabled()) {
            log.debug(callingMethod + ": " + stats.toString());
        }
    }

}
