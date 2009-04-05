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

import java.util.prefs.Preferences;

public class LicenseStoreManager {
    private static long CUT_POINT = 424242L;

    // PREFIX_EXPIRATION := munge("com/jboss/jbossnetwork/expiration");
    private static final String PREFIX_EXPIRATION = " ZQVA`#MFBiY+\"M4}pi+F<>}Yr,Fg@;H8";

    // PREFX_DIV := munge("com/jboss/jbossnetwork/quotient");
    private static final String PREFIX_DIV = "uFd>R3\"l?`ECHel-5@EH?2Y_yepz7-J";

    // PREFIX_MOD := munge("com/jboss/jbossnetwork/modulus");
    private static final String PREFIX_MOD = "QMG`ECH6lYnNeZ6/sJnel 3G9_*\\,l";

    // PREFIX_TRIAL := munge("preference/trial");
    private static final String PREFIX_TRIAL = "i,S,=ygsBSFzy^Rd";

    // PREFIX_MAJOR_KEY := munge("major/license/key");
    private static final String PREFIX_MAJOR_KEY = "CA\"1ye2UV<U~gHnS`";

    private LicenseStoreManager() {
        super();
    }

    /**
     * @param  license
     *
     * @return the expiration in the BackingStore, if one exists. Otherwise, it returns the date found in license.
     *         Handles duration licenses different than regular licenses (see Exception descriptions below).
     *
     * @throws UnavailableLicenseException if the BackingStore is not available. It must be available to retrieve the
     *                                     expiration date.
     * @throws CorruptLicenseException     if the expiration date in the backing store was tampered with
     * @throws UpdateTrialLicenseException if the user is trying to update any previously installed license with a trial
     *                                     license, unless the previous one and the current one match (have the same
     *                                     master keys), in which case the previously calculated date will be used
     */
    public static long store(License license) throws UnavailableLicenseException, CorruptLicenseException,
        UpdateTrialLicenseException {
        if (!BackingStore.isAvailable()) {
            throw new UnavailableLicenseException(LRES.get(LRES.ERR_FAILED_VALIDATION));
        }

        // get the various license bits from the backing store
        Preferences prefs = Preferences.userRoot();
        String str_expiration = prefs.get(PREFIX_EXPIRATION, "");
        String str_major_key = prefs.get(PREFIX_MAJOR_KEY, "");
        String str_trial = prefs.get(PREFIX_TRIAL, "");

        long expiration = 0;
        long quotient = 0;
        long modulus = 0;

        boolean trial = false;

        /*
         * Either:
         *
         * 1) RHQ was newly installed, or 2) The user deleted all backing store entries
         *
         * Regardless, it passes the check
         */
        if ("".equals(str_expiration + str_major_key + str_trial)) {
            expiration = storeLicenseBits(license, prefs);
        } else {
            if (license.isTrial()) {
                /*
                 * we can't install a new trial license unless it matches up perfectly with the one we have written to
                 * the store, which means it:   a) the store has record of a trial license   b) the master key values
                 * match up   c) the time hasn't been mangled with
                 */

                /*
                 * we must find the trial value, and ensure that it's true
                 */
                try {
                    trial = ((Integer.parseInt(str_trial) % 2) == 1);
                } catch (NumberFormatException nfe) {
                    throw new CorruptLicenseException(LRES.get(LRES.ERR_CORRUPT_TRIAL));
                }

                if (trial) {
                    /*
                     * can't update a trial license with another trial license, unless it's the same one (using master
                     * key comparison). thus, if the user tries to mess with the master key from a trial license it'll
                     * only break the license features and let us know there was tampering.
                     */
                    if (!license.getMasterKey().equals(str_major_key)) {
                        throw new CorruptLicenseException(LRES.get(LRES.ERR_CORRUPT_KEY));
                    }

                    /*
                     * furthermore, it can only be updated if it's not corrupt. otherwise, the previous date can't be
                     * considered reliable.
                     */
                    try {
                        quotient = decode(prefs.get(PREFIX_DIV, ""));
                        modulus = decode(prefs.get(PREFIX_MOD, ""));
                        expiration = decode(str_expiration);
                    } catch (NumberFormatException nfe) {
                        throw new CorruptLicenseException(LRES.get(LRES.ERR_CORRUPT_VALIDATION));
                    }

                    if (((quotient * CUT_POINT) + modulus) != expiration) {
                        throw new CorruptLicenseException(LRES.get(LRES.ERR_CORRUPT_VALIDATION));
                    }

                    return license.getLicenseExpiration();
                } else {
                    // can't update a real/regular license with a trial one
                    throw new UpdateTrialLicenseException(LRES.get(LRES.ERR_TRIAL_VALIDATION));
                }
            } else {
                /*
                 * the simplest logic: forget about everything in the store if you have a real/regular, date-based
                 * license; all the fancy rules, regulations, restrictions go away for this case
                 */
                expiration = storeLicenseBits(license, prefs);
            }
        }

        return expiration;
    }

    private static long storeLicenseBits(License license, Preferences prefs) {
        long expiration = license.getLicenseExpiration();
        long quotient = expiration / CUT_POINT;
        long modulus = expiration % CUT_POINT;

        prefs.put(PREFIX_EXPIRATION, encode(expiration));
        prefs.put(PREFIX_DIV, encode(quotient));
        prefs.put(PREFIX_MOD, encode(modulus));

        int result = getModRandom(license);
        prefs.put(PREFIX_TRIAL, Integer.toString(result));

        prefs.put(PREFIX_MAJOR_KEY, license.getMasterKey());
        return expiration;
    }

    /*
     * Generate a large random number so that it looks munged on the disk
     *
     * If the license was a trial, make it odd; otherwise, make it even
     */
    private static int getModRandom(License lic) {
        return (2 * (int) (Math.random() * 1000000.0)) + (lic.isTrial() ? 1 : 0);
    }

    private static String encode(long value) {
        String temp = String.valueOf(value);
        StringBuffer buff = new StringBuffer();
        int nextNum = 0;
        for (int i = 0, sz = temp.length(); i < sz; i++) {
            nextNum = (int) (temp.charAt(i) - '0');
            nextNum += 2 * i;
            if ((nextNum % 2) == 0) {
                nextNum += 6;
            } else {
                nextNum += 4;
            }
            while (nextNum > 9) {
                nextNum -= 10;
            }

            buff.append((char) (nextNum + '0'));
        }

        return buff.toString();
    }

    private static long decode(String value) {
        String temp = String.valueOf(value);
        StringBuffer buff = new StringBuffer();
        int nextNum = 0;
        for (int i = 0, sz = temp.length(); i < sz; i++) {
            nextNum = (int) (temp.charAt(i) - '0');
            if ((nextNum % 2) == 0) {
                nextNum -= 6;
            } else {
                nextNum -= 4;
            }

            nextNum -= 2 * i;
            while (nextNum < 0) {
                nextNum += 10;
            }

            buff.append((char) (nextNum + '0'));
        }

        return Long.valueOf(buff.toString()).longValue();
    }
}