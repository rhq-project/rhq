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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rhq.core.clientapi.util.StringUtil;

public final class License implements Serializable {
    public static final int FEATURE_BASIC = 1;
    public static final int FEATURE_MONITOR = FEATURE_BASIC << 1;
    public static final int FEATURE_ALL = FEATURE_BASIC | FEATURE_MONITOR;

    private String _masterKey = null;
    private String _version = null;
    private String _licenseeName = null;
    private String _licenseeEmail = null;
    private String _licenseePhone = null;
    private long _expiration = 0;
    private int _platformLimit = 0;
    private List _serverIps = new ArrayList();
    private List _plugins = new ArrayList();
    private int _supportLevel = 1;
    private boolean trial = false;

    static final long EXPIRES_NEVER = 9223372036854775307L;
    static final int PLATFORMS_UNLIMITED = 2147483624;

    private static final char[] HEX = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static final String MUNGABLE_CHARS_ALNUM
    // shuffled: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
    = "6JCqLxnjaOSAkyWXsvQ7G4ueNf1hIMgbV2dDEcTU5o9HlRwFPYZBirm8ztK03p";

    // DON'T CHANGE THESE MUNGE MAPS, or you will BREAK our licensing code.
    private static final String MUNGABLE_CHARS
    // shuffled: "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890~!@#$%^&*()_+-=`[]\\{}|;':\",./<>? "
    = "_{my*K|c~1kJ@apv!6l?MF9ZeH\"+#L&NC3Y`>BV0nERiA:DGdqQuh 2S<=WgtOf%r'[\\7s5o}UPT$(^bx)X]/-I4wj8;.z,";

    protected License() {
        _expiration = 0;
    }

    protected String getMasterKey() {
        return _masterKey;
    }

    protected void setMasterKey(String m) {
        _masterKey = m;
    }

    protected String getVersion() {
        return _version;
    }

    protected void setVersion(String v) {
        _version = v;
    }

    public String getLicenseeName() {
        return _licenseeName;
    }

    protected void setLicenseeName(String n) {
        _licenseeName = n;
    }

    public String getLicenseeEmail() {
        return _licenseeEmail;
    }

    protected void setLicenseeEmail(String e) {
        _licenseeEmail = e;
    }

    public String getLicenseePhone() {
        return _licenseePhone;
    }

    protected void setLicenseePhone(String p) {
        _licenseePhone = p;
    }

    protected void setExpiration(long ex) {
        _expiration = ex;
    }

    public boolean getIsPerpetualLicense() {
        return (_expiration == EXPIRES_NEVER);
    }

    public long getLicenseExpiration() {
        return _expiration;
    }

    protected void setPlatformLimit(int l) {
        _platformLimit = l;
    }

    public boolean getPlatformsUnlimited() {
        return (_platformLimit == PLATFORMS_UNLIMITED);
    }

    public int getPlatformLimit() {
        return _platformLimit;
    }

    public int getSupportLevel() {
        return _supportLevel;
    }

    protected void setSupportLevel(int level) {
        _supportLevel = level;
    }

    protected void addServerIp(int count, String addr) {
        if (_serverIps.size() >= count) {
            LicenseManager.doHalt(LRES.ERR_SERVERIPCOUNT);
            return;
        }

        _serverIps.add(addr);
    }

    public boolean getIsAnyIpOk() {
        return (_serverIps.size() > 0) && _serverIps.get(0).equals(LRES.get(LRES.IP_ANY));
    }

    public List getServerIps() {
        return _serverIps;
    }

    public boolean getIsAnyPluginOk() {
        return (_plugins.size() > 0) && _plugins.get(0).equals(LRES.get(LRES.PLUGIN_ANY));
    }

    protected void addPlugin(String plugin) {
        _plugins.add(plugin);
    }

    public List getPlugins() {
        return _plugins;
    }

    public boolean equals(Object o) {
        if (!(o instanceof License)) {
            return false;
        }

        License l = (License) o;
        return getMasterKey().equals(l.getMasterKey());
    }

    public String toString() {
        return LRES.get(LRES.TOSTRING_START)
            + getLicenseeName()
            + LRES.get(LRES.EMAIL)
            + getLicenseeEmail()
            + LRES.get(LRES.PHONE)
            + getLicenseePhone()
            + LRES.get(LRES.EXPIRATION)
            + (getIsPerpetualLicense() ? LRES.get(LRES.NEVER) : ExpirationTag.DFORMAT.format(new java.util.Date(
                _expiration))) + LRES.get(LRES.TOSTRING_PLATFORMS)
            + (getPlatformsUnlimited() ? LRES.get(LRES.UNLIMITED) : String.valueOf(_platformLimit))
            + LRES.get(LRES.TOSTRING_SERVERIPS)
            + (getIsAnyIpOk() ? LRES.get(LRES.ANY) : StringUtil.listToString(_serverIps))
            + LRES.get(LRES.TOSTRING_PLUGINS)
            + (getIsAnyPluginOk() ? LRES.get(LRES.ALL) : StringUtil.listToString(_plugins))
            + LRES.get(LRES.TOSTRING_SUPPORT) + getSupportLevel() + LRES.get(LRES.TOSTRING_END);
    }

    /**
     * A simple one-way hash function for strings *
     */
    static String simpleHash(String s) {
        String reverse = (new StringBuffer(s)).reverse().toString();

        // Munge the string
        String m = munge(s);

        // Ensure the string is long enough to generate enough entropy
        for (int i = 0; m.length() < 1024; i++) {
            String rm = (new StringBuffer(m)).reverse().toString();

            // combine in different ways to increase entropy
            switch ((i + m.length()) % 5) {
            case 0: {
                m = munge(reverse + m + s);
                break;
            }

            case 1: {
                m = munge(s + reverse + rm);
                break;
            }

            case 2: {
                m = munge(rm + reverse);
                break;
            }

            case 3: {
                m = munge(s + m);
                break;
            }

            case 4: {
                m = munge(reverse + rm + m + rm);
                break;
            }
            }
        }

        // Remunge one last time
        m = munge(m);

        // Simple accumulator
        int[] accum = new int[32];
        for (int i = 0; i < 32; i++) {
            accum[i] = 0;
        }

        for (int i = 0; i < m.length(); i++) {
            accum[i % 32] += (int) m.charAt(i);
        }

        // print out as hex, taking the lower 8 bits
        // of each integer in accum, interpret as a hex digit
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < 32; i++) {
            result.append(HEX[accum[i] % 16]);
        }

        return result.toString();
    }

    /**
     * @return The original form of a string that was munged with the 'munge' method. Uses the default munge map.
     */
    static String unmunge(String s) {
        return unmunge(s, MUNGABLE_CHARS);
    }

    static String unmunge(String s, String mungeMap) {
        StringBuffer rstr = new StringBuffer();
        char c;
        int idx;
        int newIdx;
        int mlen = mungeMap.length();
        int lenmod = (s.length() % 37);
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            idx = mungeMap.indexOf(c);
            if (idx == -1) {
                rstr.append(c);
            } else {
                newIdx = (idx - 13 - (i % 7) - lenmod + mlen) % mlen;
                rstr.append(mungeMap.charAt(newIdx));
            }
        }

        return rstr.toString();
    }

    /**
     * Simple munger to make searching for String constants in class files more difficult. Use the unmunge method to get
     * back to the original string. Uses the default munge map.
     */
    static String munge(String s) {
        return munge(s, MUNGABLE_CHARS);
    }

    /**
     * Just like the other munge map, but uses a caller-supplied munge map
     */
    static String munge(String s, String mungeMap) {
        StringBuffer rstr = new StringBuffer();
        char c;
        int idx;
        int newIdx;
        int mlen = mungeMap.length();
        int lenmod = (s.length() % 37);
        for (int i = 0; i < s.length(); i++) {
            c = s.charAt(i);
            idx = mungeMap.indexOf(c);
            if (idx == -1) {
                rstr.append(c);
            } else {
                newIdx = (idx + 13 + (i % 7) + lenmod) % mlen;
                rstr.append(mungeMap.charAt(newIdx));
            }
        }

        return rstr.toString();
    }

    static String shuffle(String s) {
        List chars = new ArrayList();
        for (int i = 0; i < s.length(); i++) {
            chars.add(new Character(s.charAt(i)));
        }

        Collections.shuffle(chars);
        StringBuffer rstr = new StringBuffer();
        for (int i = 0; i < chars.size(); i++) {
            rstr.append(((Character) chars.get(i)).charValue());
        }

        return rstr.toString();
    }

    public boolean isTrial() {
        return trial;
    }

    protected void setTrial(boolean trial) {
        this.trial = trial;
    }
}