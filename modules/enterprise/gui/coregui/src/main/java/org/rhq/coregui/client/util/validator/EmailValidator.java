/*
 * RHQ Management Platform
 * Copyright (C) 2005-2014 Red Hat, Inc.
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
 * along with this program; if not, write to the Free Software Foundation, Inc,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */
package org.rhq.coregui.client.util.validator;

import com.smartgwt.client.widgets.form.validator.RegExpValidator;

/**
 * SmartGWT form validator that validates EmailAddresses against RegExp.
 *
 * @author Michael Burman
 */
public class EmailValidator extends RegExpValidator {

    private static final String EMAIL_ADDRESS_REGEXP = "^([a-zA-Z0-9_.\\-+])+@([a-zA-Z0-9\\-])+(\\.([a-zA-Z0-9\\-])+)*$";

    public EmailValidator() {
        super(EMAIL_ADDRESS_REGEXP);
    }
}
