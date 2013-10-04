/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
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
package org.rhq.coregui.client.components.form;

import com.smartgwt.client.widgets.form.validator.CustomValidator;

/**
 * Validate if a string value is a valid Java long (an integer between -9,223,372,036,854,775,808 and
 * 9,223,372,036,854,775,807), without actually converting it to a {@link Long}. Since this validator
 * deals with longs, not ints, it should be used in conjunction with a
 * {@link com.smartgwt.client.widgets.form.fields.TextItem}, not a
 * {@link com.smartgwt.client.widgets.form.fields.IntegerItem}.
 *
 * @author Ian Springer
 */
public class IsLongValidator extends CustomValidator {

    public IsLongValidator() {
        setErrorMessage("Must be an integer between -9,223,372,036,854,775,808 and 9,223,372,036,854,775,807.");
    }

    // TODO: If the valid invalid should we revert to the last valid value as SpinnerItem does? I'm not even sure how
    //       to do that.
    @Override
    protected boolean condition(Object value) {
        if (value == null) {
            // null is considered valid
            return true;
        }

        // Convert to a string, with leading and trailing whitespace trimmed.
        String stringValue = value.toString().trim();

        if (stringValue.length() == 0) {
            // 100% whitespace is considered valid
            updateInputValue(stringValue);
            return true;
        }

        boolean isNegative = (stringValue.charAt(0) == '-');

        // "-" is invalid
        if (isNegative && stringValue.length() == 1) {
            return false;
        }

        String baseStringValue = stripOffNegativeSignAndLeadingZeroes(stringValue, isNegative);
        String normalizedValue;
        if (isNegative) {
            normalizedValue = "-" + baseStringValue;
        } else {
            normalizedValue = baseStringValue;
        }
        updateInputValue(normalizedValue);

        // all zeroes, optionally prefixed with negative sign, is valid
        if (baseStringValue.equals("0")) {
            return true;
        }

        // more than 19 characters is invalid
        if (baseStringValue.length() > 19) {
            return false;
        }

        // 19 characters starting with '9' is a special case
        if (baseStringValue.length() == 19 && baseStringValue.charAt(0) == '9') {
            if (isNegative) {
                // must be <= 9223372036854775808
                boolean isValid = baseStringValue
                    .matches("[9][0-2][0-2][0-3][0-3][0-7][0-2][0][0-3][0-6][0-8][0-5][0-4][0-7][0-7][0-5][0-8][0][0-8]");
                return isValid;
            } else {
                // must be <= 9223372036854775807
                boolean isValid = baseStringValue
                    .matches("[9][0-2][0-2][0-3][0-3][0-7][0-2][0][0-3][0-6][0-8][0-5][0-4][0-7][0-7][0-5][0-8][0][0-7]");
                return isValid;
            }
        }

        // 0-19 characters, excluding the 19 characters starting with '9' case handled above
        boolean isValid = baseStringValue.matches("[1-9]?[0-9]{0,18}");
        return isValid;
    }

    private void updateInputValue(String normalizedValue) {
        if (getFormItem() != null) {
            getFormItem().setValue(normalizedValue);
        } else if (getRecord() != null && getDataSourceField() != null) {
            getRecord().setAttribute(getDataSourceField().getName(), normalizedValue);
        }
    }

    private static String stripOffNegativeSignAndLeadingZeroes(String stringValue, boolean negative) {
        int charsToSkip = 0;
        if (negative) {
            charsToSkip++;
        }

        for (int i = charsToSkip; i < stringValue.length() - 1; i++) {
            if (stringValue.charAt(i) == '0') {
                charsToSkip++;
            } else {
                break;
            }
        }

        return stringValue.substring(charsToSkip);
    }

}
