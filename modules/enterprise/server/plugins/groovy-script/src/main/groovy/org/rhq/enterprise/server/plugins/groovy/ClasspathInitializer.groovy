package org.rhq.enterprise.server.plugins.groovy

import org.codehaus.groovy.control.CompilerConfiguration

class ClasspathInitializer {

  def initClasspath(String paths, String libDirs, scriptClassLoader) {
    paths.eachLine { scriptClassLoader.addClasspath(it.trim()) }
  }
}
