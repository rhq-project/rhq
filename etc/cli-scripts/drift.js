// This script has a dependency on rhq/etc/cl-scripts/util.js. The following
// command needs to be run in order to load the functions in util.js:
//
//     exec -f /path/to/rhq/etc/cli-scripts/util.js
//
// Then the same exec command can be run to load functions in this script.

function createSnapshot(rid, cname) {
  var config = findDriftConfig(rid, function(c) { return cname.equals(c.name) });

  var criteria = BasicDriftChangeSetCriteria();
  criteria.addFilterResourceId(rid);
  criteria.addFilterDriftConfigurationId(config.id);

  return DriftServer.createSnapshot(criteria);
}

function findDriftConfig(rid, fn) {
  var criteria = ResourceCriteria();
  criteria.addFilterId(rid);
  criteria.fetchDriftConfigurations(true);
  
  var resources = ResourceManager.findResourcesByCriteria(criteria);
  var resource = resources.get(0);

  return find(resource.driftConfigurations, function(config) {
    return fn(DriftConfiguration(config));
  });
}

function diff(s1, s2) {
  var theDiff = s1.diff(s2);

  function printEntry(entry) {
    println(entry.newDriftFile.hashId + '\t' + entry.path);
  }

  function report(header, elements) {
    println(header + ':');
    foreach(elements, printEntry);
    println('\n');
  }

  report('elements in conflict', theDiff.elementsInConflict);
  report('elements not in left', theDiff.elementsNotInLeft);
  report('elements not in right', theDiff.elementsNotInRight);
}

function filterEntry(props) {
  return function(entry) {
    var filteredEntry = {};
    foreach(props, function(prop) {
      if (entry[prop]) {
        filteredEntry[prop] = entry[prop];
      }
    });
    return filteredEntry;
  }
}
