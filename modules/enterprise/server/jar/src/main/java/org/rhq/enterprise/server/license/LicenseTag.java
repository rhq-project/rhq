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

import java.io.IOException;
import org.rhq.core.util.xmlparser.XmlAttr;
import org.rhq.core.util.xmlparser.XmlAttrException;
import org.rhq.core.util.xmlparser.XmlAttrHandler;
import org.rhq.core.util.xmlparser.XmlTagHandler;
import org.rhq.core.util.xmlparser.XmlTagInfo;

final class LicenseTag implements XmlTagHandler, XmlAttrHandler {
    private License _license;

    protected LicenseTag(License lic) {
        _license = lic;
    }

    private static final XmlAttr[] ATTRS = { new XmlAttr(LRES.get(LRES.ATTR_KEY), XmlAttr.REQUIRED),
        new XmlAttr(LRES.get(LRES.ATTR_VERSION), XmlAttr.REQUIRED) };

    public String getName() {
        return LRES.get(LRES.TAGNAME_LICENSE);
    }

    protected License getLicense() {
        return _license;
    }

    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] { new XmlTagInfo(new LicenseeTag(this), XmlTagInfo.REQUIRED),
            new XmlTagInfo(new ExpirationTag(this), XmlTagInfo.REQUIRED),
            new XmlTagInfo(new ServerIpTag(this), XmlTagInfo.ONE_OR_MORE),
            new XmlTagInfo(new PlatformLimitTag(this), XmlTagInfo.REQUIRED),
            new XmlTagInfo(new PluginTag(this), XmlTagInfo.ONE_OR_MORE),
            new XmlTagInfo(new SupportLevelTag(this), XmlTagInfo.REQUIRED), };
    }

    public XmlAttr[] getAttributes() {
        return ATTRS;
    }

    public void handleAttribute(int attrNumber, String value) throws XmlAttrException {
        if (attrNumber == 0) { // "key"
            setMasterKey(value);
        } else if (attrNumber == 1) { // "version"
            setVersion(value);
        } else {
            LicenseManager.doHalt(LRES.MSG_BADINDEX);
            throw new IllegalArgumentException();
        }
    }

    protected void setMasterKey(String s) {
        _license.setMasterKey(s);
    }

    protected void setVersion(String v) {
        _license.setVersion(v);
    }

    protected String generateKey() {
        String our_salt = LRES.get(LRES.getOurSalt());
        try {
            return hash(String.valueOf(our_salt + "@" + System.currentTimeMillis()));
        } catch (Exception e) {
            LicenseManager.doHalt(LRES.ERR_KEYGENERATION, e);
            throw new IllegalStateException();
        }
    }

    protected String getMasterKey() {
        return _license.getMasterKey();
    }

    protected String getVersion() {
        return _license.getVersion();
    }

    protected static String hash(String s) throws IOException {
        // Does not work on RedHat AS 3.0
        // return FileMD5.getDigestFromString(s);

        // Apparently even this has problems on Windows
        // return FileMD5.getPureJavaMD5(s);

        // Let's try something so dumb, so simple,
        // that it just HAS to work on every stinkin' platform.
        return License.simpleHash(s);
    }
}