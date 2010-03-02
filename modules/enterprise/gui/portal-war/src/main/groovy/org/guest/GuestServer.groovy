package org.guest

class GuestServer {

  String scriptsDir

  String suiteFile

  String outputDir

  def executeTests() {
    def basedir = '/Users/john/Development/redhat/imanage/rhq/modules/enterprise/server/jar'
    scriptsDir = "$basedir/src/test/script"
    suiteFile = "$basedir/src/test/resources/guest-testng.xml"
    outputDir = "$basedir/target"

    def classLoader = new GroovyClassLoader(Thread.currentThread().contextClassLoader)    
    def testRunnerScriptURL = classLoader.getResource("org/guest/GuestTestRunner.groovy")
    def scriptEngine = new GroovyScriptEngine(".", classLoader)
    def binding = new Binding([
        'scriptsDir': scriptsDir,
        'suiteFile': suiteFile,
        'outputDir': outputDir,
        'scriptsClassLoader': classLoader
    ])

    scriptEngine.run(testRunnerScriptURL.toURI().path, binding)
  }

}
  