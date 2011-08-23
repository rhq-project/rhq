// This script has a dependency on rhq/etc/cl-scripts/util.js. The following
// command needs to be run in order to load the functions in util.js:
//
//     exec -f /path/to/rhq/etc/cli-scripts/util.js
//
// Then the same exec command can be run to load functions in this script.

function createSnapshot(rid, cname) {
  var config = findDriftConfig(rid, function(c) { return cname.equals(c.name) });

  var criteria = GenericDriftChangeSetCriteria();
  criteria.addFilterResourceId(rid);
  criteria.addFilterDriftConfigurationId(config.id);

  if (arguments.length > 2) {
    var filters = arguments[2];
    for (key in filters) {
      criteria['addFilter' + key[0].toUpperCase() + key.substring(1)](filters[key]);
    } 
  }

  return DriftManager.createSnapshot(criteria);
}

function findDriftConfig(rid, filter) {
  var criteria = ResourceCriteria();
  criteria.addFilterId(rid);
  criteria.fetchDriftConfigurations(true);
  
  var resources = ResourceManager.findResourcesByCriteria(criteria);
  var resource = resources.get(0);

  return find(resource.driftConfigurations, function(config) {
    return filter(DriftConfiguration(config));
  });
}

function diff(s1, s2) {
  var theDiff = s1.diff(s2);

  if (arguments.length > 2) {
    var path = arguments[2];
   
    if (theDiff.elementsInConflict.size() == 0) {
      // If the snapshot diff reports no files in conflict, then there
      // is no need to call the server to perform the file diff. We can
      // instead return quickly.
      println("There are no differences to report");
      return "";
    } 
    
    var pathFilter = function(entry) { return entry.path == path; };
    var e1 = find(s1.entries, pathFilter);
    var e2 = find(s2.entries, pathFilter);

    var fileDiff = DriftManager.generateUnifiedDiff(e1, e2);
    foreach(fileDiff.diff, println);
  }

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

function fetchHistory(rid, configName, path) {
  function History() {
    var entries = [];

    this.echo = function(msg) {println(msg);}


    var generate = function() {
      entries = [];
      var criteria = GenericDriftCriteria();
      criteria.addFilterResourceIds([rid]);
      criteria.fetchChangeSet(true);
      criteria.addFilterPath(path);

      var drifts = DriftManager.findDriftsByCriteria(criteria);
      foreach(drifts, function(drift) {
        if (drift.changeSet.driftConfiguration.name == configName && 
          drift.path == path) {
            entries.push(drift);
        }
      });

      entries.sort(function(d1, d2) { 
        return d1.changeSet.version <= d2.changeSet.version
      });
    }

    this.display = function() {
      var format = java.text.DateFormat.getDateTimeInstance();
      println(path + "\n-----------------------------------");
      foreach(entries, function(drift) {
        println(drift.changeSet.version + "\t" + format.format(drift.ctime)); 
      });
    } 

    this.compare = function(v1, v2) {
      var d1 = find(entries, function(drift) { 
        return drift.changeSet.version == v1;
      });
      var d2 = find(entries, function(drift) { 
        return drift.changeSet.version == v2;
      });
      
      var fileDiff = DriftManager.generateUnifiedDiff(d1, d2);
      foreach(fileDiff.diff, println);     
    }

    generate();
  }

  return new History();

/*
  var criteria = GenericDriftCriteria();
  criteria.addFilterResourceIds([rid]);
  criteria.fetchChangeSet(true);
  criteria.addFilterPath(path);

  var drifts = DriftManager.findDriftsByCriteria(criteria);
  var history = [];
  foreach(drifts, function(drift) {
    if (drift.changeSet.driftConfiguration.name == configName && 
      drift.path == path) {
      history.push(drift);
    }
  });

  history.sort(function(d1, d2) { 
    return d1.changeSet.version <= d2.changeSet.version
  });

  return history;
*/
}
