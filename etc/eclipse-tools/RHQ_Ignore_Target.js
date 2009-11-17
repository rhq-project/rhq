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
    else if (targetFolder = file.eclipseObject.parent.findMember("dev-container")) {
      targetFolder.setDerived(true);      
    }
  }
}