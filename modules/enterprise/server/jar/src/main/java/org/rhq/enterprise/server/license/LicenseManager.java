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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import javax.naming.InitialContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class LicenseManager implements Runnable, Serializable {
    private static final long serialVersionUID = -3229197855347715264L;

    public static final int FEATURE_MONITOR = License.FEATURE_MONITOR;

    private static final long WARN_EXPIRE_MILLIS = 60L * 60L * 24L * 1000L * 30L;

    private static Log log = LogFactory.getLog(LRES.get(LRES.LOG_NAME));

    private static LicenseManager _instance = new LicenseManager();
    private static volatile boolean _jndiBound = false;

    protected transient InitialContext ic = null;
    protected String JNDI_NAME = LRES.get(LRES.JNDI_NAME);
    private License _license;
    private transient Thread _expirationEnforcementThread = null;

    private boolean licenseLoaded = false;
    private boolean licenseExpired = false;

    private LicenseManager() {
        try {
            ic = new InitialContext();
        } catch (Exception e) {
            doHalt(LRES.ERR_MSG_JNDIIC, e);
        }
    }

    public static LicenseManager instance() {
        if (!_jndiBound) {
            _instance.bind();
        }

        try {
            return (LicenseManager) _instance.ic.lookup(_instance.JNDI_NAME);
        } catch (Exception e) {
            doHalt(LRES.ERR_MSG_JNDILOOKUP, e);
            throw new IllegalStateException();
        }
    }

    private synchronized void bind() {
        if (_jndiBound) {
            return;
        }

        try {
            ic.rebind(JNDI_NAME, _instance);
        } catch (Exception e) {
            doHalt(LRES.ERR_MSG_JNDIBIND, e);
            throw new IllegalStateException();
        }
    }

    /**
     * Tests a license file by running it through the parser. Throws an exception if something fails to parse. Proper
     * exceptions should really be integrated at some point.
     */
    public static void checkLicense(InputStream licenseFile) throws LicenseParsingException,
        UnavailableLicenseException, CorruptLicenseException, UpdateTrialLicenseException {
        LicenseParser lp;
        lp = new LicenseParser();
        License license = lp.parse(licenseFile);

        LicenseStoreManager.store(license);
    }

    /**
     * Read the license data
     */
    private final void initialize(String licenseFile) {
        LicenseParser lp;
        File lfile = new File(licenseFile);
        try {
            if (!lfile.exists()) {
                throw new FileNotFoundException(licenseFile);
            }

            lp = new LicenseParser();
            _license = lp.parse(lfile);
            licenseLoaded = true;
        } catch (Exception e) {
            licenseLoaded = false;
        }
    }

    protected static final void doHalt(String msg, Exception e) {
        doHaltThrowable(msg, e);
    }

    private static final void doHaltThrowable(String msg, Throwable t) {
        if (t != null) {
            log.error(LRES.get(msg) + ": " + t + "\n\n" + LRES.get(LRES.HALT_MSG));
            // t.printStackTrace();
        } else {
            log.error(LRES.get(msg) + "\n\n" + LRES.get(LRES.HALT_MSG));
            // (new Exception("no exception")).printStackTrace();
        }

        // Would be nice to obfuscate the call to "halt" here, otherwise
        // it is something easy to search the classfiles for
        //Runtime.getRuntime().halt(0);

        // should never get here, but just in case
        throw new IllegalStateException();
    }

    protected static final void doHalt(String msg) {
        doHalt(msg, null);
    }

    /**
     * Called by ProductPluginDeployer at startup to enforce the following license provisions: <license-expiration> :
     * ensure that the customer's license has not expired <server-ip> : ensure that at least one of the server's IPs
     * matches one of the licensed IPs.
     */
    public final void doStartupCheck(String licenseFile) {
        try {
            initialize(licenseFile);
            log.info(_license.toString());
            enforceExpiration();
            enforceServerIp();
        } catch (Exception e) {
            //            doHalt(LRES.ERR_INIT_EXC, e);
        } catch (Throwable t) {
            //            doHaltThrowable(LRES.ERR_INIT_ERROR, t);
            throw new IllegalStateException();
        }
    }

    public boolean isLicenseExpired() {
        return licenseExpired;
    }

    public boolean isLicenseLoaded() {
        return licenseLoaded;
    }

    private final void enforceExpiration() {
        licenseExpired = false;

        if ((_license == null) || _license.getIsPerpetualLicense()) {
            return;
        }

        long ex = 0;
        try {
            ex = LicenseStoreManager.store(_license);
        } catch (LicenseException le) {
            licenseExpired = true;
            log.warn(le.getMessage());
        }

        long diff = ex - System.currentTimeMillis();
        if (diff < 0) {
            //            doHalt(LRES.ERR_MSG_EXPIRED);
            //            throw new IllegalStateException();
            licenseExpired = true;
        } else if (diff < WARN_EXPIRE_MILLIS) {
            log.warn(LRES.get(LRES.WARN_EXPIRATION) + ExpirationTag.DFORMAT.format(new Date(ex)));
        }

        // Make sure we continue to check expiration date
        if (_expirationEnforcementThread == null) {
            _expirationEnforcementThread = new Thread(this);
            _expirationEnforcementThread.setDaemon(true);
            _expirationEnforcementThread.start();
        }
    }

    public void run() {
        long millisInDay = 1000 * 60 * 60 * 24;
        while (true) {
            try {
                Thread.sleep(millisInDay);
            } catch (InterruptedException e) {
            }

            enforceExpiration();
        }
    }

    private final void enforceServerIp() {
        //        // Easy out
        //        if (_license == null || _license.getIsAnyIpOk()) return;
        //
        //        List okIps = _license.getServerIps();
        //        Sigar sigar = null;
        //        try {
        //            sigar = new Sigar();
        //
        //            String[] netInterfaces = sigar.getNetInterfaceList();
        //            String ifaceName = null;
        //            NetInterfaceConfig iface = null;
        //            String addr;
        //
        //            for ( int i=0; i<netInterfaces.length; i++ ) {
        //                ifaceName = netInterfaces[i];
        //                iface = sigar.getNetInterfaceConfig(ifaceName);
        //                addr = iface.getAddress();
        //
        //                if (okIps.contains(addr)) return;
        //            }
        //            doHalt(LRES.ERR_MSG_NOIPS);
        //            throw new IllegalStateException();
        //
        //        } catch (SigarException e) {
        //            doHalt(LRES.ERR_MSG_NETIF, e);
        //            throw new IllegalStateException();
        //        } finally {
        //            if (sigar != null) {
        //                sigar.close();
        //            }
        //        }
    }

    public boolean isPluginEnabled(String name) {
        if ((_license == null) || _license.getIsAnyPluginOk()) {
            return true;
        }

        boolean enabled = _license.getPlugins().contains(name);
        return enabled;
    }

    public void enforcePlatformLimit(int count) throws PlatformLimitException {
        if (_license == null) {
            return; // temporarily ignore limits before license is loaded
        }

        if (_license.getPlatformsUnlimited()) {
            return;
        }

        if (count > _license.getPlatformLimit()) {
            String msg = LRES.get(LRES.ERR_MSG_TOOMANYPLATFORMS) + _license.getPlatformLimit();
            throw new PlatformLimitException(msg, _license.getPlatformLimit());
        }
    }

    public void enforceFeatureLimit(int feature) throws FeatureUnavailableException {
        LicenseManager.enforceFeatureLimit(_license, feature);
    }

    public static void enforceFeatureLimit(License license, int feature) throws FeatureUnavailableException {
        if ((license != null) && ((license.getSupportLevel() & feature) == 0)) {
            switch (feature) {
            case FEATURE_MONITOR: {
                throw new FeatureUnavailableException("Monitor feature is not supported");
            }

            default: {
                throw new FeatureUnavailableException("Unknown feature " + feature + " is not supported");
            }
            }
        }
    }

    public License getLicense() {
        return _license;
    }

    public static String getLicenseFileName() {
        return LRES.get(LRES.LICENSE_LOCATION);
    }

    /**
     * @return The expiration time in millis, or -1 if there is no expiration.
     */
    public long getExpiration() throws LicenseException {
        if ((_license == null) || _license.getIsPerpetualLicense()) {
            return -1;
        }

        //return _license.getLicenseExpiration();
        return LicenseStoreManager.store(_license);
    }

    public String getLicenseInfoString() {
        return _license.toString();
    }
}