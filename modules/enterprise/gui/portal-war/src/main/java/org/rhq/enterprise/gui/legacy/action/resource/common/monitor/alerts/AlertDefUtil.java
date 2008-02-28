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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts;

import sun.security.pkcs.EncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.alert.AlertDampening;
import org.rhq.core.domain.alert.AlertDefinition;
import org.rhq.core.domain.measurement.MeasurementSchedule;
import org.rhq.core.domain.measurement.MeasurementUnits;
import org.rhq.core.domain.measurement.util.MeasurementConverter;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.enterprise.gui.legacy.Constants;
import org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config.SyslogActionForm;
import org.rhq.enterprise.gui.legacy.beans.AlertConditionBean;
import org.rhq.enterprise.gui.legacy.beans.OptionItem;
import org.rhq.enterprise.gui.legacy.exception.ParameterNotFoundException;
import org.rhq.enterprise.gui.legacy.util.RequestUtils;
import org.rhq.enterprise.server.measurement.util.MeasurementFormatter;
import org.rhq.enterprise.server.operation.OperationManagerLocal;
import org.rhq.enterprise.server.util.LookupUtil;

/**
 * Utility class for dealing with rendering alert definition conditions.
 */
public final class AlertDefUtil {
    private static Log log = LogFactory.getLog(AlertDefUtil.class);

    public static long getSecondsConsideringUnits(Integer duration, AlertDampening.TimeUnits units) {
        return (units.getNumberOfSeconds() * duration);
    }

    /**
     * Converts the list of alert conditions into a list of AlertConditionBean objects.
     *
     * @param subject
     * @param conds   @return List of AlertConditionBean objects
     */
    public static List<AlertConditionBean> getAlertConditionBeanList(Subject subject, HttpServletRequest request,
        Set<AlertCondition> conds, boolean template) {
        List<AlertConditionBean> alertCondBeans = new ArrayList<AlertConditionBean>(conds.size());

        boolean first = true;
        for (AlertCondition cond : conds) {
            String conditionDisplayText = formatAlertConditionForDisplay(cond, request);
            AlertConditionBean alertCondBean = new AlertConditionBean(conditionDisplayText, first);
            alertCondBeans.add(alertCondBean);
            first = false;
        }

        return alertCondBeans;
    }

    @SuppressWarnings("deprecation")
    public static String formatAlertConditionForDisplay(AlertCondition cond, HttpServletRequest request) {
        AlertConditionCategory category = cond.getCategory();

        StringBuffer textValue = new StringBuffer();

        // first format the LHS of the operator
        if (category == AlertConditionCategory.CONTROL) {
            try {
                Subject subject = RequestUtils.getSubject(request);
                Integer resourceTypeId = cond.getAlertDefinition().getResource().getResourceType().getId();
                String operationName = cond.getName();
                OperationManagerLocal operationManager = LookupUtil.getOperationManager();

                OperationDefinition definition = operationManager.getOperationDefinitionByResourceTypeAndName(
                    resourceTypeId, operationName);
                textValue.append(definition.getDisplayName()).append(' ');
            } catch (Exception e) {
                textValue.append(cond.getName()).append(' ');
            }
        } else {
            textValue.append(cond.getName()).append(' ');
        }

        // next format the RHS
        if (category == AlertConditionCategory.CONTROL) {
            textValue.append(cond.getOption());
        } else if ((category == AlertConditionCategory.THRESHOLD) || (category == AlertConditionCategory.BASELINE)) {
            textValue.append(cond.getComparator());
            textValue.append(' ');

            MeasurementSchedule schedule = null;

            MeasurementUnits units;
            double value = cond.getThreshold();
            if (category == AlertConditionCategory.THRESHOLD) {
                units = cond.getMeasurementDefinition().getUnits();
            } else // ( category == AlertConditionCategory.BASELINE )
            {
                units = MeasurementUnits.PERCENTAGE;
            }

            String formatted = MeasurementConverter.format(value, units, true);
            textValue.append(formatted);

            if (category == AlertConditionCategory.BASELINE) {
                textValue.append(" of ");
                textValue.append(MeasurementFormatter.getBaselineText(cond.getOption(), schedule));
            }
        } else if ((category == AlertConditionCategory.CONFIGURATION_PROPERTY)
            || (category == AlertConditionCategory.CHANGE) || (category == AlertConditionCategory.TRAIT)) {
            textValue.append(RequestUtils.message(request, "alert.current.list.ValueChanged"));
        } else if (category == AlertConditionCategory.EVENT) {
            String msgKey = "alert.config.props.CB.LogCondition";
            List<String> args = new ArrayList<String>(2);

            //          args.add( ResourceLogEvent.getLevelString( Integer.parseInt( cond.getName() ) ) );
            if ((cond.getOption() != null) && (cond.getOption().length() > 0)) {
                msgKey += ".StringMatch";
                args.add(cond.getOption());
            }

            textValue = new StringBuffer(RequestUtils.message(request, msgKey, args.toArray()));
        } else if (category == AlertConditionCategory.AVAILABILITY) {
            textValue = new StringBuffer(RequestUtils.message(request, "alert.config.props.CB.Availability",
                new String[] { cond.getOption() }));
        } else {
            // do nothing
        }

        return textValue.toString();
    }

    /**
     * Sets the following request attributes based on what's contained in the AlertConditionValue.
     *
     * <ul>
     *   <li>enableActionsResource - resource bundle key for display</li>
     *   <li>enableActionsHowLong - how long</li>
     *   <li>enableActionsHowLongUnits - units (i.e. -- ALERT_ACTION_ENABLE_UNITS_WEEKS)</li>
     *   <li>enableActionsHowMany - number of times condition occurs</li>
     * </ul>
     *
     * @param request  the http request
     * @param alertDef
     */
    public static void setAlertDampeningRequestAttributes(HttpServletRequest request, AlertDefinition alertDefinition) {
        AlertDampening alertDampening = alertDefinition.getAlertDampening();

        Integer enableActionsHowLong = alertDampening.getPeriod();
        AlertDampening.TimeUnits enableActionsHowLongUnits = alertDampening.getPeriodUnits();

        Integer enableActionsHowMany = alertDampening.getValue();
        AlertDampening.TimeUnits enableActionsHowManyUnits = alertDampening.getValueUnits();

        String enableActionsResource;
        AlertDampening.Category alertDampeningCategory = alertDampening.getCategory();

        if (alertDampeningCategory == AlertDampening.Category.NONE) {
            enableActionsResource = "alert.config.props.CB.DampenNone";
        } else if (alertDampeningCategory == AlertDampening.Category.ONCE) {
            enableActionsResource = "alert.config.props.CB.DampenOnce";
        } else if (alertDampeningCategory == AlertDampening.Category.CONSECUTIVE_COUNT) {
            enableActionsResource = "alert.config.props.CB.DampenConsecutiveCount";
        } else if (alertDampeningCategory == AlertDampening.Category.PARTIAL_COUNT) {
            enableActionsResource = "alert.config.props.CB.DampenPartialCount";
        } else if (alertDampeningCategory == AlertDampening.Category.INVERSE_COUNT) {
            enableActionsResource = "alert.config.props.CB.DampenInverseCount";
        } else if (alertDampeningCategory == AlertDampening.Category.DURATION_COUNT) {
            enableActionsResource = "alert.config.props.CB.DampenDurationCount";
        } else {
            throw new RuntimeException("Alert dampening category " + alertDampeningCategory
                + " does not have a corresponding message in ApplicationResources.properties");
        }

        request.setAttribute("enableActionsResource", enableActionsResource);
        request.setAttribute("enableActionsHowLong", enableActionsHowLong); // {0}
        request.setAttribute("enableActionsHowLongUnits", enableActionsHowLongUnits); // {1}
        request.setAttribute("enableActionsHowMany", enableActionsHowMany); // {2}
        request.setAttribute("enableActionsHowManyUnits", enableActionsHowManyUnits); // {3}
    }

    /**
     * Retrieve the alert definition from either the request or from the bizapp as necessary. First check to see if the
     * alertDef is already in the request attributes. If it is, return it. If not, look for an "ad" parameter and then
     * get the alert definition from the bizapp and return it.
     */
    @Deprecated
    public static AlertDefinition getAlertDefinition(HttpServletRequest request) throws ServletException {
        AlertDefinition alertDefinition = (AlertDefinition) request.getAttribute(Constants.ALERT_DEFINITION_ATTR);

        if (null == alertDefinition) {
            String alertDefinitionParameter = request.getParameter(Constants.ALERT_DEFINITION_PARAM);

            if (null == alertDefinitionParameter) {
                throw new ParameterNotFoundException(Constants.ALERT_DEFINITION_PARAM);
            } else {
                Subject user = RequestUtils.getSubject(request);
                int alertDefinitionId = Integer.parseInt(alertDefinitionParameter);
                alertDefinition = LookupUtil.getAlertDefinitionManager()
                    .getAlertDefinitionById(user, alertDefinitionId);
                request.setAttribute(Constants.ALERT_DEFINITION_ATTR, alertDefinition);

                if (alertDefinition.getResourceType() != null) {
                    RequestUtils.setResourceType(request, alertDefinition.getResourceType());
                } else {
                    RequestUtils.setResource(request, alertDefinition.getResource());
                }

                log.trace("adv.id=" + alertDefinitionId);
            }
        }

        return alertDefinition;
    }

    /**
     * Returns a List of LabelValueBean objects whose labels and values are both set to the string of the control
     * actions for the passed-in resource.
     */
    public static List<OptionItem> getControlActions(Subject subject, Integer id, boolean isAlertTemplate)
        throws Exception {
        List<OptionItem> operations = new ArrayList<OptionItem>();

        OperationManagerLocal operationManager = LookupUtil.getOperationManager();
        List<OperationDefinition> operationDefinitions = null;

        if (isAlertTemplate) {
            operationDefinitions = operationManager.getSupportedResourceTypeOperations(subject, id);
        } else {
            operationDefinitions = operationManager.getSupportedResourceOperations(subject, id);
        }

        for (OperationDefinition definition : operationDefinitions) {
            // as of right now, you can only trigger no-arg operations when an alert fires
            if (definition.getParametersConfigurationDefinition() != null) {
                continue;
            }

            OptionItem next = new OptionItem(definition.getDisplayName(), String.valueOf(definition.getId()));
            operations.add(next);
        }

        return operations;
    }

    /**
     * Get the control action ActionValue and string from an AlertDefinitionValue. If none exists, return null;
     *
     * @param alertDef
     */
    public static ControlActionInfo getControlActionInfo(AlertDefinition alertDef) {
        //        Set<Action> actions = alertDef.getActions();
        //        ControlActionConfig ca = new ControlActionConfig();
        //        for (Action action: actions) {
        //            if ( ca.getImplementor().equals(action.getClassname())) {
        //                ConfigResponse configResponse =
        //                    ConfigResponse.decode( action.getConfig() );
        //                ca.init(configResponse);
        //                return new ControlActionInfo(action,
        //                                             ca.getControlAction());
        //            }
        //        }
        return null;
    }

    public static class ControlActionInfo {
        //        public ControlActionInfo(Action action, String controlAction) {
        //            this.action = action;
        //            this.controlAction = controlAction;
        //        }
        //        public Action action;
        //        public String controlAction;
    }

    //    public static Action getSyslogActionValue(AlertDefinition alertDef) {
    //        Set<Action> actions = alertDef.getActions();
    //        for (Action action: actions) {
    //            if (
    //                    action.getClassname() != null &&
    //                    !action.getClassname().equals("") ) {
    //                try {
    //                    Class clazz = Class.forName( action.getClassname() );
    //                    if ( SyslogActionConfig.class.isAssignableFrom(clazz) ) {
    //                        return action;
    //                    }
    //                } catch (ClassNotFoundException e) {
    //                    // ignore
    //                }
    //            }
    //        }
    //        return null;
    //    }

    public static void prepareSyslogActionForm(AlertDefinition alertDef, SyslogActionForm form)
        throws EncodingException {
        //        Action action = getSyslogActionValue(alertDef);
        //        if (null != action) {
        //            SyslogActionConfig sa = new SyslogActionConfig();
        //            ConfigResponse configResponse =
        //                ConfigResponse.decode( action.getConfig() );
        //            sa.init(configResponse);
        //            form.setAd( alertDef.getId() );
        //            form.setMetaProject( sa.getMeta() );
        //            form.setProject( sa.getProduct() );
        //            form.setVersion( sa.getVersion() );
        //            form.setId( action.getId() );
        //        }
    }
}