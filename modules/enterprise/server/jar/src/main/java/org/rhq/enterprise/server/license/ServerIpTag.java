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

final class ServerIpTag extends LicenseTermTag {
    protected ServerIpTag(LicenseTag lt) {
        super(lt);
    }

    private String _address = null;
    private int _count = 0;

    private static final XmlAttr[] ATTRS = { new XmlAttr(LRES.get(LRES.ATTR_ADDRESS), XmlAttr.REQUIRED),
        new XmlAttr(LRES.get(LRES.ATTR_COUNT), XmlAttr.REQUIRED),
        new XmlAttr(LRES.get(LRES.ATTR_KEY), XmlAttr.REQUIRED) };

    public String getName() {
        return LRES.get(LRES.TAGNAME_SERVERIP);
    }

    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {};
    }

    public XmlAttr[] getAttributes() {
        return ATTRS;
    }

    protected int getCount() {
        return _count;
    }

    protected String getAddress() {
        return _address;
    }

    public void handleAttribute(int attrNumber, String value) throws XmlAttrException {
        if ((attrNumber != 0) && (attrNumber != 1) && (attrNumber != 2)) {
            throw new XmlAttrException(LRES.get(LRES.MSG_BADINDEX));
        }

        if (attrNumber == 0) {
            _address = value;
        } else if (attrNumber == 1) {
            try {
                _count = Integer.parseInt(value);
            } catch (Exception e) {
                throw new XmlAttrException(LRES.get(LRES.ERR_MSG_BADCOUNT));
            }
        } else {
            setKey(value);
        }
    }

    protected void preValidate() {
        if ((_address == null) || (_count == 0)) {
            throw new IllegalStateException(LRES.get(LRES.ERR_INVALIDSERVERIP));
        }
    }

    protected String getValidationComparisonValue() {
        return _count + ":" + _address;
    }

    protected void termValidated() {
        if (_address != null) {
            getLicense().addServerIp(_count, _address);
        }
    }
}