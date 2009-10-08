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

final class SupportLevelTag extends LicenseTermTag {
    protected SupportLevelTag(LicenseTag lt) {
        super(lt);
    }

    private int _level = 0;
    private String _origValue = null;

    private static final XmlAttr[] ATTRS = { new XmlAttr(LRES.get(LRES.ATTR_LEVEL), XmlAttr.REQUIRED),
        new XmlAttr(LRES.get(LRES.ATTR_KEY), XmlAttr.REQUIRED) };

    public String getName() {
        return LRES.get(LRES.TAGNAME_SUPPORTLEVEL);
    }

    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {};
    }

    public XmlAttr[] getAttributes() {
        return ATTRS;
    }

    protected String getOriginalSupportLevelString() {
        return _origValue;
    }

    public void handleAttribute(int attrNumber, String value) throws XmlAttrException {
        if ((attrNumber != 0) && (attrNumber != 1)) {
            throw new XmlAttrException(LRES.get(LRES.MSG_BADINDEX));
        }

        if (attrNumber == 0) {
            try {
                _level = Integer.parseInt(value);
                _origValue = value;
            } catch (Exception e) {
                throw new XmlAttrException(LRES.get(LRES.ERR_MSG_BADLEVEL));
            }
        } else {
            setKey(value);
        }
    }

    protected String getValidationComparisonValue() {
        return String.valueOf(_level);
    }

    protected void termValidated() {
        getLicense().setSupportLevel(_level);
    }
}