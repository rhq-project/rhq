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
package org.rhq.enterprise.server.license;

import org.rhq.core.util.xmlparser.XmlAttrHandler;
import org.rhq.core.util.xmlparser.XmlTagException;
import org.rhq.core.util.xmlparser.XmlTagExitHandler;
import org.rhq.core.util.xmlparser.XmlTagHandler;

abstract class LicenseTermTag implements XmlTagHandler, XmlAttrHandler, XmlTagExitHandler {
    // This is the super secret 'salt' that we use to calculate
    // our MD5.  We should really use some PKI here instead, but
    // this is a lot simpler and easier to do right now.
    private String our_salt = null;

    private License _license;
    private String _key;

    protected LicenseTermTag(LicenseTag lt) {
        _license = lt.getLicense();
        our_salt = LRES.get(LRES.getOurSalt());
    }

    protected License getLicense() {
        return _license;
    }

    protected void setKey(String key) {
        _key = key;
    }

    // Called before validation begins
    protected void preValidate() {
    }

    // Called to get the value to compare against the key
    protected abstract String getValidationComparisonValue();

    // Called when validation is successful
    protected abstract void termValidated();

    public void exit() throws XmlTagException {
        // Validate a value against a key
        String key = generateKey();
        if (!key.equals(_key)) {
            // System.err.println(getClass().getName() + ": given key("+_key+") doesnt match realkey="+key);
            //LicenseManager.doHalt(LRES.ERR_VALIDATION);
            throw new IllegalStateException();
        }

        termValidated();
    }

    protected String generateKey() {
        String value = getValidationComparisonValue();
        if ((value == null) || (value.length() == 0)) {
            LicenseManager.doHalt(LRES.ERR_NOVALUE);
            throw new IllegalStateException();
        }

        try {
            return LicenseTag.hash(value + our_salt + value.charAt(0) + getName() + "$" + _license.getMasterKey());
        } catch (Exception e) {
            LicenseManager.doHalt(LRES.ERR_KEYVALIDATION, e);
            throw new IllegalStateException();
        }
    }
}