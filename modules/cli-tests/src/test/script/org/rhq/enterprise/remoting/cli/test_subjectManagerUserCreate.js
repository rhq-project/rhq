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

/**
 * By default generates users jon1..jon20 and assigns them to the "All Resources Role".  Existing users
 * with a generated name will be deleted and recreated.
 *  
 * This test script creates test users on the server running at localhost 7080. To affect a different
 * database change the variables:
 *    testHost
 *    testPort
 * 
 * To change the assigned Role change the variable:
 *    testRole
 *    
 * To change the username or number of generated users change the variables: 
 *    testUsername
 *    testCount
 *
 * To change the default required field values change the variables:
 *    testEmail
 *    testFirst
 *    testLast
 *    testPassword
 */

var testHost     = 'localhost';
var testPort     = '7080';

var testRole     = 'All Resources Role';

var testUsername = 'jon';
var testCount    = 20;

var testEmail    = 'test@test.com';
var testFirst    = 'test';
var testLast     = 'user';
var testPassword = 'redhat';


var TestsEnabled = true;

var subject = rhq.login('rhqadmin', 'rhqadmin', testHost, testPort);

executeAllTests();

rhq.logout();

function testUserCreation() {
   if ( !TestsEnabled ) {
      return;
   }
   
   var c = new RoleCriteria();
   c.addFilterName( testRole );   
   var roles = RoleManager.findRolesByCriteria( c );   
   Assert.assertTrue( (1 == roles.size()), "testRole should exist");
   Assert.assertEquals(testRole, roles.get(0).getName());
   var role = roles.get(0);
   var rolesArray = new Array(1);
   rolesArray[0] = role.getId();
   
   c = new SubjectCriteria();
   c.setStrict( true );
   c.fetchRoles( true );
   var newSubject = new Subject();
   newSubject.setEmailAddress( testEmail );
   newSubject.setFirstName( testFirst );
   newSubject.setLastName( testLast );
   newSubject.setFactive(true);
   newSubject.setFsystem(false);
   
   for( i=1; i <= testCount; ++i ) {
      var newUsername = testUsername + i;
      c.addFilterName( newUsername );      
      var users = SubjectManager.findSubjectsByCriteria(c);      
      if ( !users.isEmpty() ) {
         print( "\n recreating existing user: " + newUsername );
         var subjectArray = new Array(1);
         subjectArray[0] = users.get(0).getId();
         SubjectManager.deleteSubjects( subjectArray );
         users = SubjectManager.findSubjectsByCriteria(c);
         Assert.assertTrue( users.isEmpty() );
      }
         
      newSubject.setName( newUsername );
      var s = SubjectManager.createSubject( newSubject );
      Assert.assertNotNull( s );
      SubjectManager.createPrincipal( newUsername, testPassword );
      RoleManager.addRolesToSubject( s.getId(), rolesArray );
      users = SubjectManager.findSubjectsByCriteria(c);
      Assert.assertEquals( 1, users.size() );
      Assert.assertTrue( ( 1 == users.get(0).getRoles().size()));
      Assert.assertTrue( users.get(0).getRoles().contains( role ) );
   }
}
