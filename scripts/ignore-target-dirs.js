/*
 * Menu: RHQ > Make Maven Targets Derived
 * DOM: http://download.eclipse.org/technology/dash/update/org.eclipse.eclipsemonkey.lang.javascript
 */

function main() {
  var files = resources.filesMatching(".*/pom\\.xml");
  var targetFolder;

  for each( file in files ) {
    if (targetFolder = file.eclipseObject.parent.findMember("target")) {
      targetFolder.setDerived(true);
    }
  }
  
  files = resources.filesMatching(".*/dev-container/jbossas/bin/run.sh");
  for each( file in files ) {
	  file.eclipseObject.parent.parent.parent.setDerived(true);
  }
}