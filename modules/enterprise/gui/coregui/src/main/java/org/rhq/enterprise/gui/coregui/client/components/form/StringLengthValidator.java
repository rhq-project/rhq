/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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

package org.rhq.enterprise.gui.coregui.client.components.form;

import com.smartgwt.client.widgets.form.validator.CustomValidator;
import com.smartgwt.client.widgets.form.validator.LengthRangeValidator;

/**
 * Similar to {@link LengthRangeValidator} except this also checks to see if
 * the value is <code>null</code>.
 * 
 * Note that if the minimum length is 0, the validator will pass the minimum check
 * if the value is null. However, if the value is not allowed to be null,
 * the validator will fail, even if the minimum length is 0.
 * 
 * @author John Mazzitelli
 */
public class StringLengthValidator extends CustomValidator {

    private final Integer min;
    private final Integer max;
    private final Boolean allowForNull;

    public StringLengthValidator(Integer min, Integer max, Boolean allowForNull) {
        this.min = min;
        this.max = max;
        this.allowForNull = allowForNull;
    }

    @Override
    protected boolean condition(Object value) {
        if (allowForNull != null) {
            if (!allowForNull.booleanValue() && value == null) {
                return false;
            }
        }

        int length = (value != null) ? value.toString().length() : 0;

        if (min != null) {
            if (length < min.intValue()) {
                return false;
            }
        }

        if (max != null) {
            if (length > max.intValue()) {
                return false;
            }
        }

        return true;
    }
}
