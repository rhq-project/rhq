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
package org.rhq.enterprise.gui.legacy.action.resource.common.monitor.alerts.config;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;

/**
 * An extension of <code>BaseValidatorForm</code> representing the <em>Add Others</em> form.
 */
public class AddOthersForm extends AddNotificationsForm {
    private Log log = LogFactory.getLog(AddOthersForm.class);

    private String emailAddresses;

    public AddOthersForm() {
        super();
    }

    public String getEmailAddresses() {
        return this.emailAddresses;
    }

    public void setEmailAddresses(String emailAddresses) {
        this.emailAddresses = emailAddresses;
    }

    @Override
    public String toString() {
        StringBuffer s = new StringBuffer(super.toString());
        s.append("ad=" + ad + " ");
        s.append("emailAddresses={");
        s.append(emailAddresses);
        s.append("} ");

        return s.toString();
    }

    @Override
    public void reset(ActionMapping mapping, HttpServletRequest request) {
        this.emailAddresses = "";
        super.reset(mapping, request);
    }

    @Override
    public ActionErrors validate(ActionMapping mapping, HttpServletRequest request) {
        if (!shouldValidate(mapping, request)) {
            return null;
        }

        log.debug("validating email addresses: " + emailAddresses);
        ActionErrors errs = super.validate(mapping, request);
        if ((null == errs) && ((emailAddresses == null) || (emailAddresses.length() == 0))) {
            // A special case, BaseValidatorForm will return null if Ok is
            // clicked and input is null, indicating a PrepareForm
            // action is occurring. This is tricky. However, it also
            // returns null if no validations failed. This is really
            // lame, in my opinion.
            return null;
        } else {
            errs = new ActionErrors();
        }
        String[] emails = emailAddresses.split(",");
        try {

            for (int i = 0; i < emails.length; i++) {

                String email = emails[i];

                int center = email.indexOf('@');

                if (center == -1)
                    throw new AddressException("At-sign is missing.");

                if ((email.length() - center) == 0)
                    throw new AddressException("Domain name is missing.");
            }

            InternetAddress[] addresses = InternetAddress.parse(emailAddresses, true);
        } catch (AddressException e) {
            if (e.getRef() == null) {
                ActionMessage err = new ActionMessage("alert.config.error.InvalidEmailAddresses", e.getMessage());
                errs.add("emailAddresses", err);
            } else {
                ActionMessage err = new ActionMessage("alert.config.error.InvalidEmailAddress", e.getRef(), e
                    .getMessage());
                errs.add("emailAddresses", err);
            }
        }

        return errs;
    }
}

// EOF
