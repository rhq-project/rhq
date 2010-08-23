package org.rhq.enterprise.server.plugins.groovy

import org.codehaus.groovy.control.CompilerConfiguration

class ClasspathInitializer {

  def initClasspath(String paths, String libDirs, scriptClassLoader) {
    paths.eachLine { scriptClassLoader.addClasspath(it.trim()) }

    libDirs.eachLine { path ->
      def dir = new File(path.trim())
      dir.eachFileMatch( ~/.*\.jar/) { jarFile ->
        scriptClassLoader.addClasspath(jarFile.absolutePath)  
      }
    }
  }
}
