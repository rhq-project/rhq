package org.rhq.enterprise.server.plugins.groovy

import org.rhq.enterprise.server.plugin.pc.ServerPluginComponent
import org.rhq.enterprise.server.plugin.pc.ServerPluginContext
import org.rhq.enterprise.server.plugin.pc.ControlFacet
import org.rhq.enterprise.server.plugin.pc.ControlResults
import org.rhq.core.domain.configuration.Configuration
import org.rhq.core.domain.configuration.PropertySimple
import org.codehaus.groovy.control.CompilerConfiguration

class ScriptRunner implements ServerPluginComponent, ControlFacet {

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
    
    def scriptResult = scriptEngine.run(scriptName, new Binding())

    ControlResults results = new ControlResults()
    results.complexResults.put(new PropertySimple("results", scriptResult))

    return results;
  }


}
