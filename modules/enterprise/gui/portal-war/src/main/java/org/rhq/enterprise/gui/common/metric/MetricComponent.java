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
package org.rhq.enterprise.gui.common.metric;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;

import org.rhq.core.gui.util.FacesComponentUtility;
import org.rhq.enterprise.gui.legacy.WebUser;
import org.rhq.enterprise.gui.util.EnterpriseFacesContextUtility;
import org.rhq.enterprise.server.measurement.MeasurementPreferences;
import org.rhq.enterprise.server.measurement.MeasurementPreferences.MetricRangePreferences;

/**
 * @author Fady Matar
 */
public class MetricComponent extends UIComponentBase {

    public final static String VALUE = "metricComponentValue";
    public final static String UNIT = "metricComponentUnit";
    public final static String OPTION_LIST_ATTRIBUTE = "optionList";

    public enum TimeUnit {
        MINUTES("Minutes", "m", 2, 60000L), //
        HOURS("Hours", "h", 3, 3600000L), //
        DAYS("Days", "d", 4, 86400000L);

        private String displayName;
        private String optionListToken;
        private int metricUnitOrdinal;
        private long millisInUnit;

        private TimeUnit(String displayName, String optionListToken, int ordinal, long millisInUnit) {
            this.displayName = displayName;
            this.optionListToken = optionListToken;
            this.metricUnitOrdinal = ordinal;
            this.millisInUnit = millisInUnit;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static TimeUnit getUnitByOptionToken(String optionListToken) {
            for (TimeUnit unit : TimeUnit.values()) {
                if (unit.optionListToken.equals(optionListToken)) {
                    return unit;
                }
            }
            throw new IllegalArgumentException("'" + optionListToken + "' is not a recognized Metric option");
        }

        public static TimeUnit getUnitByMetricOrdinal(int ordinal) {
            for (TimeUnit unit : TimeUnit.values()) {
                if (unit.metricUnitOrdinal == ordinal) {
                    return unit;
                }
            }
            throw new IllegalArgumentException("'" + ordinal + "' is not a recognized Metric ordinal value");
        }

        public long getMillisInUnit() {
            return millisInUnit;
        }

        public int getMetricUntilOrdinal() {
            return metricUnitOrdinal;
        }

    }

    private int value;
    private String unit;
    private String optionList;
    private TimeUnit[] unitOptions;

    public MetricComponent() {
        WebUser user = EnterpriseFacesContextUtility.getWebUser();
        MeasurementPreferences preferences = user.getMeasurementPreferences();
        MetricRangePreferences rangePreferences = preferences.getMetricRangePreferences();
        value = rangePreferences.lastN;
        unit = TimeUnit.getUnitByMetricOrdinal(rangePreferences.unit).name();
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getOptionList() {
        if (optionList == null) {
            optionList = FacesComponentUtility.getExpressionAttribute(this, OPTION_LIST_ATTRIBUTE, String.class);
        }
        return optionList;
    }

    public void setOptionList(String optionList) {
        this.optionList = optionList;
    }

    public TimeUnit[] getUnitOptions() {
        if (unitOptions == null) {
            String[] options = getOptionList().split(",");
            unitOptions = new TimeUnit[options.length];
            int i = 0;
            for (String option : options) {
                unitOptions[i++] = TimeUnit.getUnitByOptionToken(option);
            }
        }
        return unitOptions;
    }

    public void setUnitOptions(TimeUnit[] unitOptions) {
        this.unitOptions = unitOptions;
    }

    public long getMillis() {
        return value * TimeUnit.valueOf(unit).getMillisInUnit();
    }

    public static final String COMPONENT_TYPE = "org.jboss.on.Metric";
    public static final String COMPONENT_FAMILY = "org.jboss.on.Time";

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[5];
        state[0] = super.saveState(facesContext);
        state[1] = this.value;
        state[2] = this.unit;
        state[3] = this.optionList;
        state[4] = this.unitOptions;
        return state;
    }

    @Override
    public void restoreState(FacesContext context, Object stateValues) {
        Object[] state = (Object[]) stateValues;
        super.restoreState(context, state[0]);
        this.value = (Integer) state[1];
        this.unit = (String) state[2];
        this.optionList = (String) state[3];
        this.unitOptions = (TimeUnit[]) state[4];
    }
}
