package org.rhq.enterprise.server.plugins.groovy

import org.testng.annotations.Test

import static org.testng.Assert.*

class ClasspathInitializerTest {

  @Test
  void addPathsToClasspathInSpecifiedOrder() {
    def paths = """
    /foo/scripts
    /foo/classes
    /foo/lib/dep1.jar
    /foo/lib/dep2.jar
    """.trim()

    def expected = ['/foo/scripts', '/foo/classes', '/foo/lib/dep1.jar', '/foo/lib/dep2.jar']
    def actual = []

    def classLoader = [addClasspath: {String path -> actual << path }]

    new ClasspathInitializer().initClasspath(paths, '', classLoader)

    assertEquals(actual, expected, "Failed to add paths or add them in correct order to classpath")
  }

  @Test
  void addJarsInEachLibDirToClasspath() {
    def libDir = this.class.getResource('.').toURI().path + "test-libs"

    def actual = new HashSet()

    def classLoader = [addClasspath: {String path -> actual << path }]

    new ClasspathInitializer().initClasspath('', libDir, classLoader)

    assertTrue(actual.contains("$libDir/dep1.jar".toString()), "Failed to add JAR <dep1.jar> file to classpath")
    assertTrue(actual.contains("$libDir/dep2.jar".toString()), "Failed to add JAR file <dep2.jar> to classpath")
  }

}
