/*
 * RHQ Management Platform
 * Copyright (C) 2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

importPackage(java.io);

/**
 *  The first argument is a string that specifies the base directory in which
 *  file sets will be generated.
 *
 * {
 *   basedir: '/drift/files',
 *   fileSets: [
 *     {numFiles: 500, size: 10, extension: 'jar', dir:}
 *     {numFiles: 750, size: 8, extension: 'jar', dir:}
 *   ]
 * }
 *
 * @param basedir
 * @param fileSpecs
 */

/**
 *
 * @param opts
 *   basedir:
 *   fileSets:
 *     dir
 *     dirLayout
 *     numDirectories
 *     numFiles
 *     size
 *     extension
 */
function createFiles(opts) {
  var basedir =  File(opts.basedir);
  basedir.mkdirs();

  for (var i = 0; i < opts.fileSets.length; ++i) {
    var fileSetOpts = bindFileSetOpts(opts.fileSets[i], i);
    var dir = File(basedir, fileSetOpts.dir);
    dir.mkdir();

    var getSize;

    if (fileSetOpts.size.constructor == Array) {
      var nextSize = 0;
      getSize = function() {
        return fileSetOpts.size[(nextSize++ % fileSetOpts.size.length)]
      }
    } else {
      getSize = function() { return fileSetOpts.size; }
    }

    for (var j = 0; j < fileSetOpts.numFiles; ++j) {
      createFile({
        dir: dir.path,
        name: j + '.' + fileSetOpts.extension,
        size: getSize()
      });
    }
  }
}

function bindFileSetOpts(options, index) {
  var defaultOpts = {
    dir: index,
    dirLayout: 'single',
    numDirectories: 1,
    numFiles: 100,
    size: 4,
    extension: 'gen'
  };

  for (var optName in defaultOpts) {
    if (typeof options[optName] === 'undefined') {
      options[optName] = defaultOpts[optName];
    }
  }

  return options;
}

function fileSets(num, createFileSet) {
  var array = [];
  for (var i = 0; i < num; ++i) {
    array.push(createFileSet());
  }
  return array;
}

/**
 * Takes a map of options that provide specifications for creating the file and
 * writes a file to disk according to those specifications. Unless stated
 * otherwise, each option should be specified as a string.
 *
 * @param opts A map of options that specify how the file is to be generated.
 *   dir: The directory in which the file should be created. (required)
 *   name: The file name (required)
 *   size: The file size which can be specified in bytes, kilobytes, megabytes
 *         or gigabytes, e.g., 10B, 10KB, 10MB, 10GB (required)
 */
function createFile(opts) {
  var file = File(opts.dir, opts.name);
  var random = java.util.Random();
  var sizeObj = parseFileSize(opts.size);
  var calcSize = fileSizeConverter(sizeObj.units);
  var numBytes = calcSize(sizeObj.number);

  withWriter(file, function(writer) {
    for (var i = 0; i < numBytes; i++) {
      writer.write(random.nextInt(128));
    }
  });
}

/**
 * Parses the file size option into an object. The size argument is validated
 * to ensure it has the proper format. An exception is thrown if the syntax is
 * invalid.
 *
 * @param size The file size specified as a string. The format is an integer
 * followed by the units where recognized units are bytes (B), kilobytes (KB),
 * megabytes (MB), and gigabytes(GB). Here are some examples of legal values,
 *
 *   12B    // 12 bytes
 *   12KB   // 12 kilobytes
 *   12MB   // 12 megabytes
 *   12GB   // 12 gigabytes
 *
 * @return An object that contains two properties - number and units. number is
 * the integer portion of the size argument and units is the one or two character
 * units abbreviation.
 */
function parseFileSize(size) {
  var pattern = /^\d+(B|KB|MB|GB)$/;

  if (!pattern.test(size)) {
    throw 'Illegal Syntax: ' + size + ' - file size must be an integer followed by units ' +
        'which is one of B, KB, MB, GB';
  }

  var unitsIndex = size.search(/(B|KB|MB|GB)/);
  return {number: size.substring(0, unitsIndex), units: size.substring(unitsIndex)};
}

function fileSizeConverter(units) {
  var converters = {
    'B': function(size) { return size; },
    'KB': function(size) { return size * 1024; },
    'MB': function(size) { return size * 1024 * 1024; },
    'GB': function(size) { return size * 1024 * 1024 * 1024; }
  }
  return converters[units];
}

function withWriter(file, fn) {
  var writer = BufferedWriter(FileWriter(file));
  try {
    fn(writer);
  } finally {
    writer.close();
  }
}

if (typeof create != 'undefined') {
  println('creating files...');
  var defs = eval('(' + create + ');');
  createFiles(defs);
  println('finished');
}
