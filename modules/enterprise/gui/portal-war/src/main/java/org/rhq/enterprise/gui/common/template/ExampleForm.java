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
package org.rhq.enterprise.gui.common.template;

import javax.servlet.http.HttpServletRequest;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.rhq.enterprise.gui.legacy.action.BaseValidatorForm;

/**
 * A subclass of <code>BaseValidatorForm</code> representing the <em>Control</em> form data Customize as you see fit.
 *
 * @see BaseValidatorForm
 */
public class ExampleForm extends BaseValidatorForm {
    private String exampleProperty;

    public String getExampleProperty() {
        return this.exampleProperty;
    }

    public void setExampleProperty(String a) {
        this.exampleProperty = a;
    }

    /**
     * Resets all fields to values valid for validation. Calls super.reset() to insure that parent classes' fields are
     * initialialized validly.
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.exampleProperty = null;
        super.reset(mapping, request);
    }

    /**
     * Validates the form's fields in a custom way. XXX Delete this method if no custom validation outside of
     * validation.xml needs to be done.
     */
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if (!shouldValidate(mapping, request)) {
            return null;
        }

        ActionErrors errs = super.validate(mapping, request);
        if (errs == null) {
            errs = new ActionErrors();
        }

        // custom validation rules

        if (errs.size() == 0) {
            return null;
        }

        return errs;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append("exampleProperty= ").append(exampleProperty);

        return super.toString() + buf.toString();
    }
}