/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.server.alert.engine.model;

import java.util.regex.Pattern;

import org.rhq.core.domain.drift.DriftChangeSetCategory;
import org.rhq.enterprise.server.plugin.pc.drift.DriftChangeSetSummary;

public class DriftCacheElement extends AbstractCacheElement<Object> {

    public static final Object UNUSED_CONDITION_VALUE = "Drift Detected";
    private final Pattern driftConfigNameRegex;
    private final Pattern driftPathNameRegex;

    public DriftCacheElement(AlertConditionOperator operator, String driftConfigNameRegexStr,
        String driftPathNameRegexStr, int conditionTriggerId) {
        // our drift processing is special in that we do not have an alert condition value that
        // we need check to determine if our cache element matches a particular value. But we need
        // to give something, so just give static string since its small and we don't create more
        // objects than we need. This means super.alertConditionValue will not be used. 
        super(operator, UNUSED_CONDITION_VALUE, conditionTriggerId);

        try {
            if (driftConfigNameRegexStr != null && driftConfigNameRegexStr.length() > 0) {
                this.driftConfigNameRegex = Pattern.compile(driftConfigNameRegexStr);
            } else {
                this.driftConfigNameRegex = null;
            }

            if (driftPathNameRegexStr != null && driftPathNameRegexStr.length() > 0) {
                this.driftPathNameRegex = Pattern.compile(driftPathNameRegexStr);
            } else {
                this.driftPathNameRegex = null;
            }
        } catch (Exception e) {
            throw new InvalidCacheElementException("Failed to compile regex for drift condition", e);
        }
    }

    @Override
    public boolean matches(Object providedValue, Object... extraParams) {

        if (alertConditionOperator == AlertConditionOperator.CHANGES) {
            DriftChangeSetSummary summary = (DriftChangeSetSummary) extraParams[0];

            if (summary.getCategory() == DriftChangeSetCategory.COVERAGE) {
                return false; // we never alert on coverage reports, we only alert when files have drifted from previous known states
            }

            if (driftConfigNameRegex != null) {
                if (!driftConfigNameRegex.matcher(summary.getDriftConfigurationName()).matches()) {
                    return false; // drift config name did not match, our condition is false so don't alert
                }
            }

            boolean pathNameMatches;
            if (driftPathNameRegex != null) {
                pathNameMatches = false; // assume we don't match anything
                for (String pathname : summary.getDriftPathnames()) {
                    if (driftPathNameRegex.matcher(pathname).matches()) {
                        // at least one file that drifted matches our regex
                        pathNameMatches = true;
                        break;
                    }
                }
            } else {
                pathNameMatches = true; // no regex, so we always match no matter what files drifted
            }

            return pathNameMatches;

        } else {
            throw new UnsupportedAlertConditionOperatorException(getClass().getSimpleName() + " does not support "
                + alertConditionOperator);
        }
    }

    @Override
    public AlertConditionOperator.Type getOperatorSupportsType(AlertConditionOperator operator) {
        if (operator == AlertConditionOperator.CHANGES) {
            return operator.getDefaultType();
        }

        return AlertConditionOperator.Type.NONE;
    }
}
