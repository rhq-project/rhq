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

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.rhq.core.util.xmlparser.XmlAttr;
import org.rhq.core.util.xmlparser.XmlAttrException;
import org.rhq.core.util.xmlparser.XmlTagInfo;

final class ExpirationTag extends LicenseTermTag {
    static final DateFormat DFORMAT = new SimpleDateFormat(LRES.get(LRES.DFORMAT));
    static final NumberFormat NFORMAT = new DecimalFormat(LRES.get(LRES.NFORMAT));

    static final int PST_OFFSET = -1000 * 60 * 60 * 8;
    static final int DST_OFFSET = 0;
    private boolean trial = false;

    protected ExpirationTag(LicenseTag lt) {
        super(lt);
    }

    private String _origValue = null;
    private long _expMillis = 0;

    private static final XmlAttr[] ATTRS = { new XmlAttr(LRES.get(LRES.ATTR_DATE), XmlAttr.REQUIRED),
        new XmlAttr(LRES.get(LRES.ATTR_KEY), XmlAttr.REQUIRED) };

    public String getName() {
        return LRES.get(LRES.TAGNAME_EXPIRATION);
    }

    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {};
    }

    public XmlAttr[] getAttributes() {
        return ATTRS;
    }

    protected String getOriginalExpirationString() {
        return _origValue;
    }

    public void handleAttribute(int attrNumber, String value) throws XmlAttrException {
        if ((attrNumber != 0) && (attrNumber != 1)) {
            //            LicenseManager.doHalt(LRES.MSG_BADINDEX);
            throw new IllegalArgumentException();
        }

        if (attrNumber == 0) {
            if (value.equals(LRES.get(LRES.EXPIRATION_NEVER))) {
                _expMillis = License.EXPIRES_NEVER;
                _origValue = value;
            } else {
                // cal has today as default time
                Calendar cal = Calendar.getInstance();

                try {
                    // if the expiration parses as a date, set it
                    Date expDate = DFORMAT.parse(value);
                    cal.setTime(expDate);
                } catch (ParseException e) {
                    try {
                        // otherwise it parses as a duration
                        int days = NFORMAT.parse(value).intValue();
                        cal.add(Calendar.DATE, days);
                        trial = true;
                    } catch (ParseException ee) {
                        throw new XmlAttrException(LRES.get(LRES.MSG_ERRPARSE));
                    }
                }

                cal.set(Calendar.HOUR, 23);
                cal.set(Calendar.MINUTE, 59);
                cal.set(Calendar.SECOND, 59);

                // hardcode timezone and DST offsets
                cal.set(Calendar.ZONE_OFFSET, PST_OFFSET);
                cal.set(Calendar.DST_OFFSET, DST_OFFSET);

                _expMillis = cal.getTimeInMillis();
                _origValue = value;
            }
        } else {
            setKey(value);
        }
    }

    protected String getValidationComparisonValue() {
        return String.valueOf(_origValue);
    }

    protected void termValidated() {
        //getLicense().setExpiration(_expMillis);
        getLicense().setTrial(trial);
    }
}