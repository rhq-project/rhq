package org.rhq.plugins.jdbctrace;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.util.stream.StreamUtil;
import org.rhq.plugins.byteman.BytemanAgentComponent;

/**
 * A utility that performs things that both the discovery component and resource component
 * need. Be aware that this class will exist in the discovery and resource classloaders, so
 * be careful when modifying this class.
 * 
 * @author John Mazzitelli
 */
public class JdbcTracerUtil {
    private final Log log = LogFactory.getLog(JdbcTracerUtil.class);

    public static final String PLUGINCONFIG_ENABLED = "enabled";
    public static final String PLUGINCONFIG_SCRIPTNAME = "scriptName";
    public static final String DEFAULT_JDBC_TRACER_SCRIPT_NAME = "jdbctrace-rules.txt";
    public static final String DEFAULT_JDBC_TRACER_HELPER_JAR = "rhq-jdbctrace-plugin-helper.jar";

    /**
     * Given the byteman agent resource where the JDBC tracer resource is hosted,
     * returns the file where the jdbc trace rules script file should exist.
     * 
     * @param bytemanAgentComponent resource context of the byteman agent resource
     * @param scriptName the name of the jdbc rules script file (not path name, just the short file name)
     * @return the file where the script should be
     * 
     * @throws Exception
     */
    public File getJdbcTraceRulesScriptFile(BytemanAgentComponent bytemanAgentComponent, String scriptName) {
        File dataDir = bytemanAgentComponent.getResourceDataDirectory("jdbctrace");
        dataDir.mkdirs();
        File scriptFile = new File(dataDir, scriptName.replace('/', '-').replace('\\', '-')); // don't want it in subdirectory
        return scriptFile;
    }

    /**
     * Given the parent byteman agent resource where the JDBC tracer resource is hosted,
     * this will extract the jdbc trace rules script file and store it in a persisted data directory
     * 
     * @param bytemanAgentComponent byteman agent resource
     * @param scriptName the name of the jdbc rules script file (not path name, just the short file name)
     * @return the file where the script was extracted
     * 
     * @throws Exception
     */
    public File extractJdbcTraceRulesScriptFile(BytemanAgentComponent bytemanAgentComponent, String scriptName)
        throws Exception {

        // extract the script file from our plugin jar into our parent byteman component's data directory
        File scriptFile = getJdbcTraceRulesScriptFile(bytemanAgentComponent, scriptName);

        InputStream resourceAsStream = getClass().getResourceAsStream("/" + scriptName);
        if (resourceAsStream == null) {
            throw new Exception("Cannot find JDBC tracer rules file from classloader");
        }
        StreamUtil.copy(resourceAsStream, new FileOutputStream(scriptFile), true);

        log.debug("Extracted jdbc trace script file from plugin jar to [" + scriptFile.getAbsolutePath() + "]");

        return scriptFile;
    }

    /**
     * Given the byteman agent resource where the JDBC tracer resource is hosted,
     * returns the file where the helper jar should exist.
     *
     * @param bytemanAgentComponent resource context of the byteman agent resource
     * @param jarFileName the short name of the helper jar file 
     * @return the file where the helper jar should be
     *
     * @throws Exception
     */
    public File getHelperJarFile(BytemanAgentComponent bytemanAgentComponent, String jarFileName) {
        File dataDir = bytemanAgentComponent.getResourceDataDirectory("jdbctrace");
        dataDir.mkdirs();
        File scriptFile = new File(dataDir, jarFileName);
        return scriptFile;
    }

    /**
     * Given the parent byteman agent resource where the JDBC tracer resource is hosted,
     * this will extract the helper jar file that contains classes needed by the rules.
     * The helper jar is persisted in a data directory which can then be loaded in the byteman agent VM.
     *
     * @param bytemanAgentComponent byteman agent resource
     * @param jarFileName the short name of the helper jar file 
     * @return the extracted helper jar file
     * 
     * @throws Exception
     */
    public File extractHelperJarFile(BytemanAgentComponent bytemanAgentComponent, String jarFileName) throws Exception {

        // extract the helper jar from our plugin jar into our parent byteman component's data directory
        File helperFile = getHelperJarFile(bytemanAgentComponent, jarFileName);

        InputStream resourceAsStream = getClass().getResourceAsStream("/helper/" + jarFileName);
        if (resourceAsStream == null) {
            throw new Exception("Cannot find JDBC helper jar file from classloader");
        }
        StreamUtil.copy(resourceAsStream, new FileOutputStream(helperFile), true);

        log.debug("Extracted helper jar file from plugin jar to [" + helperFile.getAbsolutePath() + "]");

        return helperFile;
    }
}
