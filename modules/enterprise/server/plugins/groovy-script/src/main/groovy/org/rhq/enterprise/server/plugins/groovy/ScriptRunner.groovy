package org.rhq.enterprise.server.plugins.groovy

import org.codehaus.groovy.control.CompilerConfiguration
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.PropertySimple
import org.rhq.enterprise.server.plugin.pc.ControlFacet
import org.rhq.enterprise.server.plugin.pc.ControlResults
import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext
import org.rhq.core.domain.resource.Resource
import org.rhq.core.domain.resource.ResourceType
import org.rhq.enterprise.server.plugin.pc.ScheduledJobInvocationContext

class ScriptRunner implements ServerPluginComponent, ControlFacet {

  Map entityMap = [
      Resource:     Resource.class,
      ResourceType: ResourceType.class
  ]

  void initialize(ServerPluginContext context) {

  }

  void start() {

  }

  void stop() {

  }

  void shutdown() {

  }

  ControlResults invoke(String name, Configuration parameters) {
    def compilerConfig = new CompilerConfiguration()
    compilerConfig.scriptBaseClass = RHQScript.class.name

    def scriptName = parameters.getSimpleValue("script", null)

    def scriptClassLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader, compilerConfig)
    def scriptRoots = new URL[1]
    scriptRoots[0] = new File(scriptName).toURI().toURL()
    def scriptEngine = new GroovyScriptEngine(scriptRoots, scriptClassLoader)
    scriptEngine.config = compilerConfig
    
    def script = (RHQScript) scriptEngine.createScript(scriptName, new Binding())
    // Not sure why but assigning a value to the entityMap property was failing in unit tests.
    // Calling setEntityMap() worked though.
    //script.entityMap = entityMap
    script.setEntityMap(entityMap)
    def scriptResult = script.run()

    ControlResults results = new ControlResults()
    results.complexResults.put(new PropertySimple("results", scriptResult))

    return results;
  }

  void executeScript(ScheduledJobInvocationContext context) {    
  }

}
