/*
 * Adapted from http://www.davidc.net/programming/java/java-preferences-using-file-backing-store
 * 
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package org.rhq.core.util.preferences;

import java.io.File;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * PreferencesFactory implementation that stores the preferences in a single user-defined file. To use it, set the system
 * property <tt>-Djava.util.prefs.PreferencesFactory=org.rhq.core.util.preferences.FilePreferencesFactory</tt>.
 * <p/>
 * The file defaults to ${user.home}/.fileprefs, but may be overridden with the system property
 * <tt>org.rhq.core.util.preferences.FilePreferencesFactory.file</tt>
 * <p/>
 * Both the system root and user root default to the user.home system property by default.
 * <p/>
 * NOTE: This implementation does not allow a '/' character in the preference property names (although it's fine in
 * the values).
 *
 * @author David Croft (<a href="http://www.davidc.net">www.davidc.net</a>)
 * @author Jay Shaughnessy (adapted from David Croft for RHQ)
 */
public class FilePreferencesFactory implements PreferencesFactory {
    private static final Log log = LogFactory.getLog(FilePreferencesFactory.class);

    public static final String SYSTEM_PROPERTY_FILE = "org.rhq.core.util.preferences.FilePreferencesFactory.file";

    private static File preferencesFile;

    Preferences rootPreferences;

    @Override
    public Preferences systemRoot() {
        return userRoot();
    }

    @Override
    public Preferences userRoot() {
        if (null == rootPreferences) {
            log.debug("Instantiating root preferences");

            rootPreferences = new FilePreferences(null, "");
        }
        return rootPreferences;
    }

    public static File getPreferencesFile() {
        if (null == preferencesFile) {
            String prefsFile = System.getProperty(SYSTEM_PROPERTY_FILE);

            if (null == prefsFile || prefsFile.isEmpty()) {
                prefsFile = System.getProperty("user.home") + File.separator + ".fileprefs";
            }

            preferencesFile = new File(prefsFile).getAbsoluteFile();

            if (log.isDebugEnabled()) {
                log.debug("Preferences file is " + preferencesFile);
            }
        }

        return preferencesFile;
    }
}