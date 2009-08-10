/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

skippedTests = [];

function executeTests(tests) {
    var errors = '';
    var functionName = null;

    for (i in tests) {
        try {
            functionName = tests[i];

            if (isSkippedTest(functionName)) {
                continue;
            }

            this[functionName]();
        }
        catch (e if e.javaException instanceof org.rhq.enterprise.client.utility.ScriptAssertionException) {
            errors = errors + createErrorMsg(functionName, e.javaException.message, e.lineNumber);
        }
        catch (e) {
            errors = errors + createErrorMsg(functionName, e.message, e.lineNumber);
        }
    }
    if (errors.length > 0) {
        throw 'The following tests failed:\n\n' + errors + '\n\n' +
              '----------------------------------------------------\n\n';
    }
}

function isSkippedTest(test) {
    for (i in skippedTests) {
        if (skippedTests[i] == test) {
            return true;
        }
    }
    return false;
}

function createErrorMsg(functionName, msg, lineNumber) {
    return functionName + '() failed:\n' +
           msg + ' at line ' + lineNumber + ' in ' + script + '\n\n';
}

function executeAllTests() {
    var tests = [];
    for (property in this) {
        if (property.indexOf('test') == 0 && (typeof this[property] == 'function') && property != 'test') {
            tests.push(property);
        }
    }
    executeTests(tests);
}

//function test(script) {
//    rhq.exec(script);
//    var errors = executeAllTests();
//    if (errors.length > 0) {
//        println('The following tests failed:\n\n' + errors);
//    }
//    else {
//        println('Tests passed');
//    }
//    var results = executeAllTests();
//    tests = results;
//
//    println('Test Results:');
//    println('----------------------------------------------');
//    println('Passed: ' + results.getPassedTests().size() + ' Failed: ' + results.getFailedTests().size());
//    println('----------------------------------------------');
//}

