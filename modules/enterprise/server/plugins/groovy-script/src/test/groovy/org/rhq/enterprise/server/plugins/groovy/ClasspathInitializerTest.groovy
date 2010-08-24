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

//  @Test
//  void addJarsInEachLibDirToClasspath() {
//
//  }

}
