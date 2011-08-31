/**
 * file: drift.js
 *
 * description: This script contains functions that illustrate and highlight
 * drift monitoring functionality. Some of the functionality demonstrated in
 * this script is currently available only via RHQ's remote client (and CLI)
 * at the time of this writing. This script has a dependency on util.js. To
 * load this script's functions into your CLI session execute the following
 * from the CLI shell,
 *
 *   $ login <username> <password>
 *   $ exec -f samples/util.js
 *   $ exec -f samples/drift.js
 *
 * Note that you must login before you can load the scripts. Also please note
 * that if a function is not documented, then it is not intended for public
 * use. It is used only as an internal helper function.
 *
 * author: jsanda@redhat.com
 */

/**
 * description: Generates a snapshot of changes sets belonging to a particular
 * drift configuration of a specific resource. By default all change sets are
 * included in the snapshot. An optional third argument can be specified to
 * limit or control the number of change sets included in the snapshot.
 *
 * Note that a snapshot is an accumulation or aggregation of change sets that
 * provides a view of a resource, or more precisely, all files being monitored
 * for drift, at a particular version or point in time.
 *
 * arguments:
 *   rid: A resource id, expected to be an integer
 *
 *   cname: The drift configuration name as a string
 *
 *   filters: (optional) A map or JS object that limits the number of
 *   change sets included in the snapshot
 *
 * return: A org.rhq.core.domamin.drift.DriftSnapshot object
 *
 * usage:
 *   // generates a snapshot that includes all change sets belonging to the
 *   // mydrift drift configuration.
 *   createSnapshot(123, 'mydrift') 
 *
 *   // generates a snapshot starting a change set version 3 and including
 *   // everything more recent than it.
 *   createSnapshot(123, 'mydrift', {"startVersion": 3})
 *
 *   // generates a snapshot that includes change sets 3 through 5 inclusive.
 *   createSnapshot(123, 'mydrift', {"startVersion": 3, "endVersion": 5})   
 *
 *   // generates a snaphot that includes everything after the specified
 *   // timestamp. Note that the timestamp should be specified in milliseconds.
 *   createSnapshot(123, 'mydrift', {"createdAfter": 1314811734365})
 *
 *   // generates a snapshot that includes all change sets that were created
 *   // between the two timestamps.
 *   createSnapshot(123, 'mydrift', {"createdAfter": 1314811734365, "createdBefore": 9994811734365})
 */
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

/**
 * description: Generates a diff report that is printed to the console. By
 * default this function generates a snapshot diff. The function expects two
 * arguments which should be DriftSnapshot objects. A diff between those two
 * snapshots is generated and printed to the console. A path can be specified
 * as an optional third argument. If a path is a specified as a third argument,
 * then a file diff will be performed using the file that matches the path in
 * each change set. The format will be a unified diff.
 *
 * Note that the snapshots can be from the same or from different resources;
 * however, if they are from different resources, it is assumed that the
 * resources are of the same type.
 *
 * arguments:
 *   s1: A DriftSnapshot object
 *
 *   s2: A DriftSnapshot object
 *
 *   path: (optional) A string that specifies a path that exists in both s1 and
 *   s2.
 *
 * return: This function does not return a useful value. It may be an empty
 * string or it could be null. Instead of returns its results, the function
 * prints them to the CLI console. As such, this function is intended for
 * use from the interactive CLI shell.
 *
 * usage:
 *   // Generates a snapshot diff report
 *   diff(s1, s2)
 *
 *   // Generates a file diff report in the unified format
 *   diff(s1, s2, 'jboss_home/bin/run.conf')
 */
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
    return "";
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

/**
 * description: Generates and returns the drift history for a file being
 * monitored for drift. This function takes three arguments. The first two, the
 * resource id drift configuration name, uniquely identify the drift
 * configuration. The third argument specifies a path that is set up for
 * monitoring by the drift configuration.
 *
 * arguments:
 *   rid: A resource id
 *
 *   configName: A drift configuration name
 *
 *   path: A path that is set up for drift monitoring by the drift
 *   configuration that is identified by the first two arguments. The path
 *   should be specified as relative to the base directory from which
 *   monitoring is done.
 *
 * return: A History object, which is a native JS object. This object contains
 * a few methods for working with the file history:
 *
 *   list: prints a short summary of all versions of the file
 *   view: returns the contents of a particular version of the file
 *   compare: compares two versions of the file and prints a unified diff
 *
 * usage:
 *   $ history = fetchHistory(123, 'mydrift', 'bin/run.conf')
 *   $ history.list()   // prints a summary of all versions of 'bin/run.conf'
 *   $ history.view(1)  // returns the full contents of version 1 of the file
 *   $ history.compare(1, 2)  // generates and prints a unified diff of versions 1 and 2
 */
function fetchHistory(rid, configName, path) {
  function History() {
    var entries = [];

    function findDrift(version) {
      return find(entries, function(drift) { 
        return drift.changeSet.version == version  
      });
    }


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

    this.list = function() {
      var format = java.text.DateFormat.getDateTimeInstance();
      println(path + "\n-----------------------------------");
      foreach(entries, function(drift) {
        println(drift.changeSet.version + "\t" + format.format(drift.ctime)); 
      });
    } 

    this.view = function(version) {
      var drift = findDrift(version);
      if (drift == null) {
        return "Could not find version " + version;
      }
      return DriftManager.getDriftFileBits(drift.newDriftFile.hashId);
    }

    this.compare = function(v1, v2) {
      var d1 = findDrift(v1);
      if (d1 == null) {
        return "Could not find version " + v1;
      }
      var d2 = findDrift(v2);    
      if (d2 == null) {
        return "Could not find version " + v2;
      }
      var fileDiff = DriftManager.generateUnifiedDiff(d1, d2);
      foreach(fileDiff.diff, println);     
    }

    generate();
  }

  return new History();
}
