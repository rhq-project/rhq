package org.rhq.enterprise.server.rest.reporting;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.domain.alert.Alert;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertConditionLog;
import org.rhq.core.domain.alert.AlertConditionOperator;
import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.criteria.AlertCriteria;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.alert.AlertManagerLocal;
import org.rhq.enterprise.server.rest.AbstractRestBean;
import org.rhq.enterprise.server.rest.SetCallerInterceptor;
import org.rhq.enterprise.server.util.CriteriaQuery;
import org.rhq.enterprise.server.util.CriteriaQueryExecutor;

import static org.rhq.enterprise.server.rest.reporting.ReportHelper.cleanForCSV;
import static org.rhq.enterprise.server.rest.reporting.ReportHelper.formatDateTime;

@Interceptors(SetCallerInterceptor.class)
@Stateless
public class RecentAlertHandler extends AbstractRestBean implements RecentAlertLocal {

    private final Log log = LogFactory.getLog(RecentAlertHandler.class);

    @EJB
    private AlertManagerLocal alertManager;

    @Override
    public StreamingOutput recentAlerts(final String alertPriority, UriInfo uriInfo, final HttpServletRequest request,
        HttpHeaders headers) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream stream) throws IOException, WebApplicationException {
                final AlertCriteria criteria = new AlertCriteria();

                List<AlertPriority> alertPriorityList = new ArrayList<AlertPriority>(10);
                String alertPriorities[] = alertPriority.split(",");
                for ( String alertPriorityValue : alertPriorities) {
                    log.info("Alert Priority Filter set for: " + alertPriorityValue);
                    alertPriorityList.add(AlertPriority.valueOf(alertPriorityValue.toUpperCase()));
                }
                criteria.addFilterPriorities(alertPriorityList.toArray(new AlertPriority[alertPriorityList.size()]));

                CriteriaQueryExecutor<Alert, AlertCriteria> queryExecutor =
                        new CriteriaQueryExecutor<Alert, AlertCriteria>() {
                            @Override
                            public PageList<Alert> execute(AlertCriteria criteria) {

                                return alertManager.findAlertsByCriteria(caller, criteria);
                            }
                        };

                CriteriaQuery<Alert, AlertCriteria> query =
                        new CriteriaQuery<Alert, AlertCriteria>(criteria, queryExecutor);

                stream.write((getHeader() + "\n").getBytes());
                for (Alert alert : query) {
                    String record = toCSV(alert)  + "\n";
                    stream.write(record.getBytes());
                }

            }
            private String toCSV(Alert alert) {
                return formatDateTime(alert.getCtime()) + "," +
                        cleanForCSV(alert.getAlertDefinition().getName()) + "," +
                        getConditionText(alert) + ", " +
                        alert.getAlertDefinition().getPriority() + "," +
                        getStatus(alert) + "," +
                        cleanForCSV(alert.getAlertDefinition().getResource().getName()) + "," +
                        cleanForCSV(ReportHelper.parseAncestry(alert.getAlertDefinition().getResource().getAncestry()))
                        + "," + getDetailsURL(alert);
            }

            private String getHeader(){
                return "Creation Time,Name,Condition Text,Priority,Status,Resource,Ancestry,Details URL";
            }

            private String getStatus(Alert alert) {
                if (alert.getAcknowledgeTime() == null || alert.getAcknowledgeTime() < 0) {
                    return "No Ack";
                }
                return "Ack (" + alert.getAcknowledgingSubject() + ")";
            }

            private String getDetailsURL(Alert alert) {
                String protocol;
                if (request.isSecure()) {
                    protocol = "https";
                } else {
                    protocol = "http";
                }

                return protocol + "://" + request.getServerName() + ":" + request.getServerPort() +
                    "/coregui/#Resource/" + alert.getAlertDefinition().getResource().getId() + "/Alerts/History/" +
                    alert.getId();
            }

            private String getConditionText(Alert alert) {
                Set<AlertConditionLog> conditionLogs = alert.getConditionLogs();
                String conditionText = null;
                String conditionValue;

                if (conditionLogs.size() > 1) {
                    conditionText = "Multiple Conditions";
                } else if (conditionLogs.size() == 1) {
                    AlertConditionLog conditionLog = conditionLogs.iterator().next();
                    AlertCondition condition = conditionLog.getCondition();
                    conditionText = formatCondition(condition);
                    conditionValue = conditionLog.getValue();
                    if (condition.getMeasurementDefinition() != null) {
                        try {
                            conditionValue = MeasurementConverter.format(conditionLog.getValue(),
                                condition.getMeasurementDefinition().getUnits());
                        } catch (Exception e) {
                            // the condition log value was probably not a number (most likely a trait). Ignore this exception.
                            // even if any other errors occur trying to format the value, ignore this and just use the raw value string
                        }
                    }
                } else {
                    conditionText = "No Conditions";
                    conditionValue = "--";
                }

                return conditionText;
            }

            private String formatCondition(AlertCondition condition) {
                StringBuilder builder = new StringBuilder();
                AlertConditionCategory category = condition.getCategory();
                AlertConditionOperator operator;
                String formattedThreshold;

                switch (category) {
                    case AVAILABILITY:
                        builder.append("Availability [");
                        operator = AlertConditionOperator.valueOf(condition.getName().toUpperCase());
                        switch (operator) {
                            case AVAIL_GOES_DISABLED:
                                builder.append("Goes disabled");
                                break;
                            case AVAIL_GOES_DOWN:
                                builder.append("Goes down");
                                break;
                            case AVAIL_GOES_UNKNOWN:
                                builder.append("Goes unknown");
                                break;
                            case AVAIL_GOES_UP:
                                builder.append("Goes up");
                                break;
                            case AVAIL_GOES_NOT_UP:
                                builder.append("Goes not up");
                                break;
                            default:
                                builder.append("*ERROR*");
                        }
                        builder.append("]");
                        break;
                    case AVAIL_DURATION:
                        builder.append("Availability Duration [");
                        operator = AlertConditionOperator.valueOf(condition.getName().toUpperCase());
                        switch (operator) {
                            case AVAIL_DURATION_DOWN:
                                builder.append("Stays Down");
                                break;
                            case AVAIL_DURATION_NOT_UP:
                                builder.append("Stays Not Up");
                                break;
                            default:
                                builder.append("*ERROR*");
                        }
                        builder.append(" For ");

                        String value = String.valueOf(Integer.valueOf(condition.getOption()) / 60);
                        String formatted = MeasurementConverter.format(value, MeasurementUnits.MINUTES);

                        builder.append(formatted).append("]");
                        break;
                    case THRESHOLD:
                        MeasurementUnits units = condition.getMeasurementDefinition().getUnits();
                        formattedThreshold = MeasurementConverter.format(condition.getThreshold(), units, true);

                        if (condition.getOption() == null) {
                            builder.append("Metric Value Threshold [")
                                .append(condition.getName())
                                .append(" ")
                                .append(condition.getComparator())
                                .append(" ")
                                .append(formattedThreshold)
                                .append("]");
                        } else {
                            // this is a calltime threshold condition
                            builder.append("Call Time Value Threshold [");
                            if (condition.getMeasurementDefinition() != null) {
                                builder.append(condition.getMeasurementDefinition().getDisplayName()).append(" ");
                            }
                            builder.append(condition.getOption()) // MIN, MAX, AVG (never null)
                                .append(" ")
                                .append(condition.getComparator())  // <, >, =
                                .append(" ")
                                .append(formattedThreshold)
                                .append("]");
                            if (condition.getName() != null && condition.getName().length() > 0) {
                                builder.append(" with call destination matching '")
                                    .append(condition.getName())
                                    .append("'");
                            }
                        }
                        break;
                    case BASELINE:
                        formattedThreshold = MeasurementConverter.format(condition.getThreshold(),
                            MeasurementUnits.PERCENTAGE, true);
                        builder.append("Metric Value Baseline [")
                            .append(condition.getName())
                            .append(" ")
                            .append(condition.getComparator())
                            .append(" ")
                            .append(formattedThreshold)
                            .append(" of ")
                            .append(condition.getOption())
                            .append("]");
                        break;
                    case CHANGE:
                        if (condition.getOption() == null) {
                            builder.append("Metric Value Change [")
                                .append(condition.getName())
                                .append(" ")
                                .append("]");
                        } else {
                            // this is a calltime change condition
                            formattedThreshold = MeasurementConverter.format(condition.getThreshold(),
                                MeasurementUnits.PERCENTAGE, true);
                            builder.append("Call Time Value Changes [");
                            if (condition.getMeasurementDefinition() != null) {
                                builder.append(condition.getMeasurementDefinition().getDisplayName()).append(" ");
                            }
                            builder.append(condition.getOption()) // MIN, MAX, AVG (never null)
                                .append(" ")
                                .append(getCalltimeChangeComparator(condition.getComparator()))
                                .append(" by at least ")
                                .append(formattedThreshold)
                                .append("]");
                            if (condition.getName() != null && condition.getName().length() > 0) {
                                builder.append(" with call destination matching '")
                                    .append(condition.getName())
                                    .append("'");
                            }
                        }
                        break;
                    case TRAIT:
                        builder.append("Trait Change [")
                            .append(condition.getName())
                            .append("]");
                        break;
                    case CONTROL:
                        builder.append("Operation Execution [")
                            .append(condition.getName())
                            .append("] with result status [")
                            .append(condition.getOption())
                            .append("]");
                        break;
                    case RESOURCE_CONFIG:
                        builder.append("Resource Configuration Change");
                        break;
                    case EVENT:
                        builder.append("Event Detection [")
                            .append(condition.getName())
                            .append("]");
                        if (condition.getOption() != null && condition.getOption().length() > 0) {
                            builder.append(" with event source matching '")
                                .append(condition.getOption())
                                .append("'");
                        }
                        break;
                    case DRIFT:
                        String configNameRegex = condition.getName();
                        String pathNameRegex = condition.getOption();
                        if (configNameRegex == null || configNameRegex.length() == 0) {
                            if (pathNameRegex == null || pathNameRegex.length() == 0) {
                                // neither a config name regex nor path regex was specified
                                builder.append("Drift Detection");
                            } else {
                                // a path name regex was specified, but not a config name regex
                                builder.append("Drift Detection for files that match \"")
                                    .append(pathNameRegex)
                                    .append("\"");
                            }
                        } else {
                            if (pathNameRegex == null || pathNameRegex.length() == 0) {
                                // a config name regex was specified, but not a path name regex
                                builder.append("Drift Detection for drift definition [")
                                    .append(configNameRegex)
                                    .append("]");
                            } else {
                                // both a config name regex and a path regex was specified
                                builder.append("Drift Detection for files that match \"")
                                    .append(pathNameRegex)
                                    .append("\" and for drift detection [")
                                    .append(configNameRegex)
                                    .append("]");
                            }
                        }
                        break;
                    case RANGE:
                        String metricName = condition.getName();
                        MeasurementUnits metricUnits = condition.getMeasurementDefinition().getUnits();
                        double loValue = condition.getThreshold();
                        String formattedLoValue = MeasurementConverter.format(loValue, metricUnits, true);
                        String formattedHiValue = condition.getOption();
                        try {
                            double hiValue = Double.parseDouble(formattedHiValue);
                            formattedHiValue = MeasurementConverter.format(hiValue, metricUnits, true);
                        } catch (Exception e) {
                            formattedHiValue = "?[" + formattedHiValue + "]?"; // signify something is wrong with the value
                        }

                        // < means "inside the range", > means "outside the range" - exclusive
                        // <= means "inside the range", >= means "outside the range" - inclusive

                        if (condition.getComparator().equals("<")) {
                            // Metric Value Range: [{0}] between [{1}] and [{2}], exclusive
                            builder.append("Metric Value Range: [")
                                .append(metricName)
                                .append("] between ")
                                .append(formattedLoValue)
                                .append("] and [")
                                .append(formattedHiValue)
                                .append("], exclusive");
                        } else if (condition.getComparator().equals(">")) {
                            // Metric Value Range: [{0}] outside [{1}] and [{2}], exclusive
                            builder.append("Metric Value Range: [")
                                .append(metricName)
                                .append("] outside [")
                                .append(formattedLoValue)
                                .append("] and [")
                                .append(formattedHiValue)
                                .append("], exclusive");
                        } else if (condition.getComparator().equals("<=")) {
                            // Metric Value Range: [{0}] between [{1}] and [{2}], inclusive
                            builder.append("Metric Value Range: [")
                                .append(metricName)
                                .append("] between [")
                                .append(formattedLoValue)
                                .append("] and [")
                                .append(formattedHiValue)
                                .append("], inclusive");
                        } else if (condition.getComparator().equals(">=")) {
                            // Metric Value Range: [{0}] outside [{1}] and [{2}], inclusive
                            builder.append("Metric Value Range: [")
                                .append(metricName)
                                .append("] outside [")
                                .append(formattedLoValue)
                                .append("] and [")
                                .append(formattedHiValue)
                                .append("], inclusive");
                        } else {
                            builder.append("BAD COMPARATOR! Report this bug: " + condition.getComparator());
                        }
                        break;
                    default:
                        // Invalid condition category - please report this as a bug: {0}
                        builder.append("Invalid condition category - please report this as a bug: ")
                            .append(category.getName());
                }
                return builder.toString();
            }

            private String getCalltimeChangeComparator(String comparator) {
                if ("HI".equals(comparator)) {
                    return "Grows";
                } else if ("LO".equals(comparator)) {
                    return "Shrinks";
                } else { // CH
                    return "Changes";
                }
            }

        };

    }

}
