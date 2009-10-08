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

import org.rhq.core.util.xmlparser.XmlAttr;
import org.rhq.core.util.xmlparser.XmlAttrException;
import org.rhq.core.util.xmlparser.XmlTagInfo;

final class LicenseeTag extends LicenseTermTag {
    protected LicenseeTag(LicenseTag lt) {
        super(lt);
    }

    protected LicenseeTag(LicenseTag lt, String name, String email, String phone) {
        super(lt);
        _name = name;
        _email = email;
        _phone = phone;
    }

    private String _name = null;
    private String _email = null;
    private String _phone = null;

    private static final XmlAttr[] ATTRS = { new XmlAttr(LRES.get(LRES.ATTR_NAME), XmlAttr.REQUIRED),
        new XmlAttr(LRES.get(LRES.ATTR_EMAIL), XmlAttr.REQUIRED),
        new XmlAttr(LRES.get(LRES.ATTR_PHONE), XmlAttr.REQUIRED),
        new XmlAttr(LRES.get(LRES.ATTR_KEY), XmlAttr.REQUIRED) };

    public String getName() {
        return LRES.get(LRES.TAGNAME_LICENSEE);
    }

    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {};
    }

    public XmlAttr[] getAttributes() {
        return ATTRS;
    }

    protected String getLicenseeName() {
        return _name;
    }

    protected String getLicenseeEmail() {
        return _email;
    }

    protected String getLicenseePhone() {
        return _phone;
    }

    public void handleAttribute(int attrNumber, String value) throws XmlAttrException {
        if ((attrNumber < 0) || (attrNumber > (ATTRS.length - 1))) {
            LicenseManager.doHalt(LRES.MSG_BADINDEX);
            throw new IllegalStateException();
        }

        if (attrNumber == 0) {
            _name = value;
        } else if (attrNumber == 1) {
            _email = value;
        } else if (attrNumber == 2) {
            _phone = value;
        } else {
            setKey(value);
        }
    }

    protected void preValidate() {
        if ((_name == null) || (_email == null) || (_phone == null)) {
            LicenseManager.doHalt(LRES.ERR_INVALIDLICENSEE);
            throw new IllegalStateException();
        }
    }

    protected String getValidationComparisonValue() {
        return _name + ":" + _email + ":" + _phone;
    }

    protected void termValidated() {
        getLicense().setLicenseeName(_name);
        getLicense().setLicenseeEmail(_email);
        getLicense().setLicenseePhone(_phone);
    }
}