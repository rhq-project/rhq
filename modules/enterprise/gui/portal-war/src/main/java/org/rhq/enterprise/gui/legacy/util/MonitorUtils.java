package org.rhq.enterprise.gui.legacy.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.rhq.enterprise.server.measurement.uibean.MetricDisplaySummary;
import org.rhq.enterprise.gui.legacy.NumberConstants;

import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.core.clientapi.util.units.DateFormatter.DateSpecifics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.util.LabelValueBean;
import org.apache.struts.util.MessageResources;

import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.core.domain.measurement.composite.MeasurementNumericValueAndUnits;
import org.rhq.core.domain.measurement.composite.MeasurementValueAndUnits;

public class MonitorUtils {

    private static Log log = LogFactory.getLog(MonitorUtils.class.getName());
    public static final String RO = "ro";
    public static final String LASTN = "lastN";
    public static final String UNIT = "unit";
    public static final String BEGIN = "begin";
    public static final String END = "end";

    public static final int DEFAULT_CURRENTHEALTH_LASTN = 8;

    public static final Boolean DEFAULT_VALUE_RANGE_RO = Boolean.FALSE;
    public static final Integer DEFAULT_VALUE_RANGE_LASTN = 8;
    public static final Integer DEFAULT_VALUE_RANGE_UNIT = 3;

    public static final int UNIT_COLLECTION_POINTS = 1;
    public static final int UNIT_MINUTES = 2;
    public static final int UNIT_HOURS = 3;
    public static final int UNIT_DAYS = 4;

    public static final int THRESHOLD_BASELINE_VALUE = 1;
    public static final String THRESHOLD_BASELINE_LABEL = "Baseline";
    public static final int THRESHOLD_HIGH_RANGE_VALUE = 2;
    public static final String THRESHOLD_HIGH_RANGE_LABEL = "HighRange";
    public static final int THRESHOLD_LOW_RANGE_VALUE = 3;
    public static final String THRESHOLD_LOW_RANGE_LABEL = "LowRange";

    public static final int THRESHOLD_UNDER_VALUE = 1;
    public static final int THRESHOLD_OVER_VALUE = 2;

    /**
     * Method calculateTimeFrame
     * <p/>
     * Returns a two element<code>List</code> of <code>Long</code> objects representing the begin and end times (in
     * milliseconds since the epoch) of the timeframe. Returns null instead if the time unit is indicated as
     * <code>UNIT_COLLECTION_POINTS</code>.
     *
     * @param lastN the number of time units in the time frame
     * @param unit  the unit of time (as defined by <code>UNIT_*</code> constants
     * @return List
     */
    public static List<Long> calculateTimeFrame(int lastN, int unit) {
        List<Long> l = new ArrayList<Long>(0);

        if (unit == UNIT_COLLECTION_POINTS) {
            return null;
        }

        long now = System.currentTimeMillis();

        long retrospective = lastN;
        switch (unit) {
        case UNIT_MINUTES:
            retrospective *= NumberConstants.MINUTES;
            break;
        case UNIT_HOURS:
            retrospective *= NumberConstants.HOURS;
            break;
        case UNIT_DAYS:
            retrospective *= NumberConstants.DAYS;
            break;
        default:
            retrospective = -1;
            break;
        }

        l.add(new Long(now - retrospective));
        l.add(new Long(now));

        return l;
    }

    /**
     * Method getSubMiniTabs
     * <p/>
     * Returns a list of <code>SubMiniTab</code> objects to be displayed below the Monitor mini tabs. The properties of
     * each <code>SubMiniTab</code> will be set by examining the corresponding <code>AppdefResourceTypeValue</code>:
     * <p/>
     * <ul> <li>id: resource type id <li>name: resource type name <li>count: number of resources of this type (defaults
     * to 0) <li>selected: if this type is the one identified by the selectedId (if any) </ul>
     *
     * @param resourceTypes  a <code>List</code> of <code>AppdefResourceTypeValue</code> objects
     * @param resourceCounts a <code>Map</code> of resource counts keyed by resource type name
     * @param selectedId     a <code>Integer</code> identifying the particular resource type that is being currently
     *                       viewed 8 @return List
     */
    public static List getSubMiniTabs(List resourceTypes, Map resourceCounts, Integer selectedId) {
        List subtabs = new ArrayList();

        //      if (resourceTypes != null & resourceCounts != null)
        //      {
        //         for (Iterator i = resourceTypes.iterator(); i.hasNext();)
        //         {
        //            AppdefResourceTypeValue type =
        //                  (AppdefResourceTypeValue)i.next();
        //
        //            SubMiniTab subtab = new SubMiniTab();
        //            subtab.setName(type.getName());
        //
        //            Integer typeId = type.getId();
        //            subtab.setId(AppdefEntityConstants.APPDEF_TYPE_SERVICE
        //                  + ":" + typeId.toString());
        //            subtab.setSelected(isSubMiniTabSelected(typeId, selectedId));
        //
        //            Object count = resourceCounts.get(type.getName());
        //            if (count == null)
        //            {
        //               count = new Integer(0);
        //            }
        //            subtab.setCount(count.toString());
        //
        //            subtabs.add(subtab);
        //         }
        //      }

        return subtabs;
    }

    private static Boolean isSubMiniTabSelected(Integer tabId, Integer selectedId) {
        return new Boolean(selectedId != null && selectedId.intValue() == tabId.intValue());
    }

    /**
     * Method findDefaultChildResourceId
     * <p/>
     * Return the id of the first child resource type (according to whatever order in which the BizApp lists them) for
     * which the parent resource has one or more defined child resources.
     *
     * @param resourceTypes  a <code>List</code> of <code>AppdefResourceTypeValue</code> objects
     * @param resourceCounts a <code>Map</code> of resource counts keyed by resource type name
     * @return Integer
     */
    public static Integer findDefaultChildResourceId(List resourceTypes, Map resourceCounts) {
        //      if (resourceTypes != null && resourceCounts != null)
        //      {
        //         Iterator i = resourceTypes.iterator();
        //         while (i.hasNext())
        //         {
        //            AppdefResourceTypeValue type =
        //                  (AppdefResourceTypeValue)i.next();
        //
        //            Integer count = (Integer)resourceCounts.get(type.getName());
        //            if (count != null && count.intValue() > 0)
        //            {
        //               return type.getId();
        //            }
        //         }
        //      }

        return null;
    }

    /**
     * Method findServiceTypes.
     * <p/>
     * Given a List of services associated with an application or server, filter through them for the  service types so
     * we can link to showing selections by type. The returned List has the ServiceTypeValue as elements.
     * <p/>
     * This should eventually get pushed into the bizapp
     *
     * @param services
     * @return List
     */
    public static List findServiceTypes(List services, Boolean internal) {
        TreeMap serviceTypeSet = new TreeMap();
        //      for (Iterator i = services.iterator(); i.hasNext();)
        //      {
        //         AppdefResourceValue svcCandidate = (AppdefResourceValue)i.next();
        //         int thisType = svcCandidate.getEntityId().getType();
        //         if (thisType == AppdefEntityConstants.APPDEF_TYPE_SERVICE ||
        //               thisType == AppdefEntityConstants.APPDEF_TYPE_GROUP)
        //         {
        //            AppdefResourceValue service = (AppdefResourceValue)svcCandidate;
        //            if (service != null && service.getAppdefResourceTypeValue() != null)
        //            {
        //               // if we don't have a group that is a compat group of services, then this
        //               // better throw a ClassCastException
        //               ServiceTypeValue svcType = (ServiceTypeValue)service.getAppdefResourceTypeValue();
        //               if (internal == null)
        //               {
        //                  // if we're not discriminating between internal and deployed, we'll just
        //                  // return them all
        //                  serviceTypeSet.put(svcType.getName(), svcType);
        //               }
        //               else if (internal != null && new Boolean(svcType.getIsInternal()).equals(internal))
        //               {
        //                  serviceTypeSet.put(svcType.getName(), svcType);
        //               }
        //            }
        //         }
        //         else
        //         {
        //            throw new IllegalStateException("Did not get a valid service: " + svcCandidate);
        //         }
        //      }
        return new ArrayList(serviceTypeSet.values());
    }

    public static List findServerTypes(List servers) {
        TreeMap serverTypeSet = new TreeMap();
        //      for (Iterator i = servers.iterator(); i.hasNext();)
        //      {
        //         ServerValue thisAppSvc = (ServerValue)i.next();
        //         if (thisAppSvc != null && thisAppSvc.getServerType() != null)
        //         {
        //            ServerTypeValue svcType = thisAppSvc.getServerType();
        //            serverTypeSet.put(svcType.getName(), svcType);
        //         }
        //      }
        return new ArrayList(serverTypeSet.values());
    }

    /**
     * Sometimes, it's useful to just get a dump of all of the metrics returned by the backend.
     *
     * @param logger
     * @param metrics a Map keyed on the category (String), values are List's of MetricDisplaySummary beans
     */
    public static void traceMetricDisplaySummaryMap(Log logger, Map metrics) {
        logger.trace("Dumping metric map (from " + MonitorUtils.class.getName() + "):");
        for (Iterator categoryIter = metrics.keySet().iterator(); categoryIter.hasNext();) {
            String categoryName = (String) categoryIter.next();
            logger.trace("Category: " + categoryName);
            int i = 0;
            Collection metricList = (Collection) metrics.get(categoryName);
            for (Iterator iter = metricList.iterator(); iter.hasNext();) {
                MetricDisplaySummary summaryBean = (MetricDisplaySummary) iter.next();
                ++i;
                logger.trace("\t " + i + ": " + summaryBean);
            }
        }
    }

    public static void traceResourceTypeDisplaySummaryList(Log logger, List healths) {
        logger.trace("Dumping list of ResourceTypeDisplaySummary's (from " + MonitorUtils.class.getName() + "):");
        if (healths == null) {
            logger.trace("'healths' list was null!");
            return;
        }
        if (healths.size() < 1) {
            logger.trace("'healths' list was empty!");
            return;
        }
        int i = 0;
        for (Object health : healths) {
            //         ResourceTypeDisplaySummary summaryBean = (ResourceTypeDisplaySummary)health;
            //         ++i;
            //         logger.trace("\t" + i + ":" + summaryBean);
        }
    }

    public static Integer formatMetrics(List<MetricDisplaySummary> metricDisplaySummaries, Locale userLocale,
        MessageResources msgs) {
        Integer resourceCount = null;
        try {
            for (MetricDisplaySummary metricDisplaySummary : metricDisplaySummaries) {
                if (Double.isNaN(metricDisplaySummary.getAvgMetric().getValue())) {
                    continue;
                }
                if (resourceCount == null)
                    resourceCount = metricDisplaySummary.getAvailUp();
                // the formatting subsystem doesn't interpret
                // units set to empty strings as "no units" so
                // we'll explicity set it so
                if (metricDisplaySummary.getUnits().length() < 1) {
                    metricDisplaySummary.setUnits(MeasurementUnits.NONE.name());
                }
                MeasurementValueAndUnits[] formattedValues;
                if (msgs.isPresent(userLocale, Constants.UNIT_FORMAT_PREFIX_KEY + metricDisplaySummary.getUnits())) {
                    // this means that there's a whole song and dance for formatting this type of thing
                    // a certain way (hopefully in a way that some descendant of java.test.Format will
                    // help with)
                    String fmtString = msgs.getMessage(userLocale, Constants.UNIT_FORMAT_PREFIX_KEY
                        + metricDisplaySummary.getUnits());
                    if (metricDisplaySummary.getUnits().equals(MeasurementUnits.EPOCH_MILLISECONDS.name())) {
                        DateSpecifics dateSpecifics = new DateSpecifics();
                        dateSpecifics.setDateFormat(new SimpleDateFormat(fmtString, userLocale));

                        // TODO jmarques - figure out how to add dateSpecifics in here
                        formattedValues = getMeasurementValueAndUnits(metricDisplaySummary.getMetricValueDoubles(),
                            metricDisplaySummary.getUnits());
                        // dateSpecifics);
                    } else {
                        formattedValues = getMeasurementValueAndUnits(metricDisplaySummary.getMetricValueDoubles(),
                            metricDisplaySummary.getUnits());
                    }
                } else {
                    formattedValues = getMeasurementValueAndUnits(metricDisplaySummary.getMetricValueDoubles(),
                        metricDisplaySummary.getUnits());
                }
                String[] keys = metricDisplaySummary.getMetricKeys();
                if (keys.length != formattedValues.length) {
                    throw new IllegalStateException("Formatting metrics failed.");
                }
                for (int i = 0; i < keys.length; i++) {
                    metricDisplaySummary.getMetric(keys[i]).setValueFmt(formattedValues[i].toString());
                }
            }
        } catch (IllegalArgumentException e) { // catch and rethrow for debug/logging only
            if (log.isDebugEnabled()) {
                log.debug("formatting metrics failed due to IllegalArgumentException: ", e);
            }
            throw e;
        }
        return resourceCount;
    }

    /**
     * Formats the passed summary. The userLocale is currently ignored
     * @param summary  MetricDisplaySummary with some values
     * @param userLocale ignored
     */
    public static void formatSimpleMetrics(MetricDisplaySummary summary, Locale userLocale) {
        // Don't try to format placeholder NaN values.
        if (!Double.isNaN(summary.getAvgMetric().getValue())) {
            if (summary.getUnits().length() < 1) {
                summary.setUnits(MeasurementUnits.NONE.name());
            }
            String[] formattedValues = MeasurementConverter.formatToSignificantPrecision(summary.getMetricValueDoubles(),
                MeasurementUnits.valueOf(summary.getUnits()), true);
            String[] metricKeys = summary.getMetricKeys();
            for (int i = 0; i < metricKeys.length; i++) {
                summary.getMetric(metricKeys[i]).setValueFmt(formattedValues[i]);
            }
        }
    }

    public static MeasurementValueAndUnits[] getMeasurementValueAndUnits(double[] metricValueDoubles, String units) {
        MeasurementValueAndUnits[] valueAndUnitsList = new MeasurementValueAndUnits[metricValueDoubles.length];

        int i = 0;
        for (Double metricValue : metricValueDoubles) {
            valueAndUnitsList[i] = new MeasurementNumericValueAndUnits(metricValue, MeasurementUnits.valueOf(units));
            i++;
        }

        return valueAndUnitsList;
    }

    public static List<LabelValueBean> getThresholdMenu() {
        List<LabelValueBean> items = new ArrayList<LabelValueBean>();
        String label = null, value = null;

        label = MonitorUtils.THRESHOLD_BASELINE_LABEL;
        value = String.valueOf(MonitorUtils.THRESHOLD_BASELINE_VALUE);
        items.add(new LabelValueBean(label, value));

        label = MonitorUtils.THRESHOLD_HIGH_RANGE_LABEL;
        value = String.valueOf(MonitorUtils.THRESHOLD_HIGH_RANGE_VALUE);
        items.add(new LabelValueBean(label, value));

        label = MonitorUtils.THRESHOLD_LOW_RANGE_LABEL;
        value = String.valueOf(MonitorUtils.THRESHOLD_LOW_RANGE_VALUE);
        items.add(new LabelValueBean(label, value));

        return items;
    }
}
