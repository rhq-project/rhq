package org.rhq.plugins.byteman;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.jboss.byteman.agent.submit.Submit;

import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.pluginapi.inventory.DeleteResourceFacet;
import org.rhq.core.pluginapi.inventory.ResourceComponent;
import org.rhq.core.pluginapi.inventory.ResourceContext;
import org.rhq.core.util.stream.StreamUtil;

/**
 * Component that represents a script that is deployed in a Byteman agent.
 *
 * @author John Mazzitelli
 */
public class BytemanScriptComponent implements ResourceComponent<BytemanAgentComponent>, DeleteResourceFacet {

    // protected scope so BytemanAgentComponent can see it
    static final String PKG_TYPE_NAME_SCRIPT = "bytemanScript";

    private final Log log = LogFactory.getLog(BytemanScriptComponent.class);

    private ResourceContext<BytemanAgentComponent> resourceContext;
    private String scriptContent; // cached content of the script that this component is managing
    private List<String> rules; // cached data for all individual rules found in the script content

    public void start(ResourceContext<BytemanAgentComponent> context) {
        this.resourceContext = context;
        getAvailability(); // forces the scripts/rules caches to load
    }

    public void stop() {
        this.scriptContent = null;
        this.rules = null;
    }

    public AvailabilityType getAvailability() {
        try {
            // refresh our cache
            Map<String, String> scripts = this.resourceContext.getParentResourceComponent().getAllKnownScripts();
            if (scripts != null) {
                this.scriptContent = scripts.get(this.resourceContext.getResourceKey());
            } else {
                this.scriptContent = null;
            }

            // if we can, ensure the script is loaded in the byteman agent
            try {
                addDeployedScript();
            } catch (Throwable t) {
                log.warn("Failed to add managed script to the byteman agent - is it up?", t);
            }

            if (this.scriptContent != null) {
                try {
                    this.rules = getBytemanClient().splitAllRulesFromScript(this.scriptContent);
                } catch (Exception e) {
                    log.warn("Failed to split the rules from a script - might be a client problem or data corruption");
                    this.rules = null;
                }
                return AvailabilityType.UP;
            } else {
                this.rules = null;
                return AvailabilityType.DOWN;
            }
        } catch (Exception e) {
            return AvailabilityType.DOWN;
        }
    }

    public void deleteResource() throws Exception {
        // we need the most up-to-date info - so ask the byteman agent for the latest data
        Map<String, String> allScripts = getBytemanClient().getAllRules();

        // get the script name and the rules content belonging to that script
        String scriptName = this.resourceContext.getResourceKey();
        String scriptRulesContent = allScripts.get(scriptName);

        // attempt to delete the source file.
        // note: the script name is usually the full path name to the script file
        // note: if there is no script file, ignore this - it was probably a script added
        // via a non-RHQ mechanism. We only manage physical script files that we (RHQ) deployed
        File scriptFile = null;
        try {
            scriptFile = getManagedScriptFile(scriptName);
            if (scriptFile != null && scriptFile.isFile()) {
                if (!scriptFile.delete()) {
                    log.warn("The Byteman script file [" + scriptFile + "] failed to delete");
                }
            }
        } catch (Exception e) {
            log.warn("The Byteman script file [" + scriptFile + "] could not be deleted", e);
        }

        // if the script rule is not loaded, we have nothing else to do.
        // if the script is still loaded, tell Byteman to delete it
        if (scriptRulesContent != null) {
            // tell Byteman to delete that script's rules
            Map<String, String> scriptRulesToDelete = new HashMap<String, String>(1);
            scriptRulesToDelete.put(scriptName, scriptRulesContent);
            getBytemanClient().deleteRules(scriptRulesToDelete);

        }

        return;
    }

    /**
     * Returns a cached copy of all known rules for the script since the last availability check was made.
     *
     * @return the last known set of rules that were loaded in the remote Byteman agent. <code>null</code>
     *         if a problem occurred attempting to get the scripts
     */
    public List<String> getRules() {
        return this.rules;
    }

    public Submit getBytemanClient() {
        return this.resourceContext.getParentResourceComponent().getBytemanClient();
    }

    /**
     * This returns a File representation of a managed script found at the given path.
     * This returns <code>null</code> if the given script path is not a script managed by this plugin.
     * Even if <code>scriptPath</code> points to a valid script file, if it is not managed by this plugin,
     * <code>null</code> will be returned.
     *
     * @param scriptPath path to check to see if its a managed script, to be converted to a File if so
     * @return the File of the managed script, or <code>null</code> if the given path is not a managed script
     */
    protected File getManagedScriptFile(String scriptPath) {
        File scriptsDataDir = this.resourceContext.getParentResourceComponent().getScriptsDataDirectory();
        File scriptFile = new File(scriptPath);
        File scriptParentDir = scriptFile.getParentFile();
        boolean isManaged = scriptParentDir != null
            && scriptsDataDir.getAbsolutePath().equals(scriptParentDir.getAbsolutePath());
        return (isManaged) ? scriptFile : null;
    }

    /**
     * This method will attempt to ensure that the Byteman agent has the managed script
     * loaded. If a user created this script resource via RHQ (i.e. using the create-child-facet
     * of the parent resource), then this method will always try to ensure that script is loaded
     * in the Byteman agent. If it isn't, it reloads it. If it is, this method does nothing.
     * If this script resource represents an externally managed script (that is, the script
     * was added to the Byteman agent via some other mechanism, such as Byteman's CLI tool),
     * this method will do nothing - in this case, this resource component will declare the
     * resource's availability as DOWN.
     *
     * @throws Exception
     */
    protected void addDeployedScript() throws Exception {
        // If the script content is null, that means the Byteman agent does not have our script loaded anymore.
        // In this case, force the Byteman agent to reload our managed script. Note that we only force a
        // reload of our script if it was explicitly created via our parent's create child resource facet.
        // If this script was loaded by some other non-RHQ means, we do not attempt to reload it, we'll just
        // show its availability as DOWN and expect the user to correct the situation manually.
        if (this.scriptContent == null) {
            File scriptFile = getManagedScriptFile(this.resourceContext.getResourceKey());
            if (scriptFile != null) {
                getBytemanClient().addRulesFromFiles(Arrays.asList(scriptFile.getAbsolutePath()));
                this.scriptContent = new String(StreamUtil.slurp(new FileInputStream(scriptFile)));
            }
        }
        return;
    }
}
