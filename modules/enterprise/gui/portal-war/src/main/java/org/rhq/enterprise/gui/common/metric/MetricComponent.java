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

/**
 * @author Fady Matar
 */
public class MetricComponent extends UIComponentBase {

    public final static String VALUE = "metricComponentValue";
    public final static String UNIT = "metricComponentUnit";
    public final static String OPTION_LIST_ATTRIBUTE = "optionList";

    public enum TimeUnit {
        MINUTES("Minutes", "m", 2), //
        HOURS("Hours", "h", 3), //
        DAYS("Days", "d", 4);

        private String displayName;
        private String optionListToken;
        private int metricUnitOrdinal;

        private TimeUnit(String displayName, String optionListToken, int ordinal) {
            this.displayName = displayName;
            this.optionListToken = optionListToken;
            this.metricUnitOrdinal = ordinal;
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

        public int getMetricUntilOrdinal() {
            return metricUnitOrdinal;
        }

    }

    private String optionList;
    private TimeUnit[] unitOptions;

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

    public static final String COMPONENT_TYPE = "org.jboss.on.Metric";
    public static final String COMPONENT_FAMILY = "org.jboss.on.Time";

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[3];
        state[0] = super.saveState(facesContext);
        state[1] = this.optionList;
        state[2] = this.unitOptions;
        return state;
    }

    @Override
    public void restoreState(FacesContext context, Object stateValues) {
        Object[] state = (Object[]) stateValues;
        super.restoreState(context, state[0]);
        this.optionList = (String) state[1];
        this.unitOptions = (TimeUnit[]) state[2];
    }
}
