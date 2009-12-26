package org.rhq.plugins.byteman;

import java.io.File;
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

        // add the script back to the byteman agent if necessary (must do this after the call to getAvailability)
        try {
            addDeployedScript();
        } catch (Throwable t) {
            log.warn("Failed to add managed script to the byteman agent - is it up?", t);
        }

        return;
    }

    protected void addDeployedScript() throws Exception {
        // If the script content is null, that means the Byteman agent does not have our script loaded anymore.
        // In this case, force the Byteman agent to reload our managed script. Note that we only force a
        // reload of our script if it was explicitly created via our parent's create child resource facet.
        // If this script was loaded by some other non-RHQ means, we do not attempt to reload it, we'll just
        // show its availability as DOWN and expect the user to correct the situation manually.
        if (this.scriptContent == null) {
            File scriptsDataDir = this.resourceContext.getParentResourceComponent().getScriptsDataDirectory();
            File scriptFile = new File(this.resourceContext.getResourceKey());
            File scriptParentDir = scriptFile.getParentFile();
            if (scriptParentDir != null && scriptsDataDir.getAbsolutePath().equals(scriptParentDir.getAbsolutePath())) {
                getBytemanClient().addRulesFromFiles(Arrays.asList(scriptFile.getAbsolutePath()));
            }
        }
        return;
    }

    public void stop() {
        this.scriptContent = null;
        this.rules = null;
    }

    public AvailabilityType getAvailability() {
        try {
            // refresh our cache
            Map<String, String> scripts = this.resourceContext.getParentResourceComponent().getAllKnownScripts();
            this.scriptContent = scripts.get(this.resourceContext.getResourceKey());

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
        Map<String, String> allScripts = getBytemanClient().getAllScripts();

        // get the script name and the rules content belonging to that script
        String scriptName = this.resourceContext.getResourceKey();
        String scriptRulesContent = allScripts.get(scriptName);
        if (scriptRulesContent == null) {
            throw new Exception("Cannot delete unknown script [" + scriptName + "]");
        }

        // tell Byteman to delete that script's rules
        Map<String, String> scriptRulesToDelete = new HashMap<String, String>(1);
        scriptRulesToDelete.put(scriptName, scriptRulesContent);
        getBytemanClient().deleteRules(scriptRulesToDelete);

        // attempt to delete the source file
        // note: the script name is usually the full path name to the script file
        File scriptFile = null;
        try {
            scriptFile = new File(scriptName);
            if (scriptFile.isFile()) {
                if (!scriptFile.delete()) {
                    log.warn("Byteman unloaded the script, but the script file [" + scriptFile + "] failed to delete");
                }
            }
        } catch (Exception e) {
            log.warn("Byteman unloaded the script, but the script file [" + scriptFile + "] could not be deleted", e);
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
}
