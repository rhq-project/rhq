#!/bin/perl
#
# $Id: test-rt-dontLog.pl 7299 2007-10-30 15:32:15Z ispringer $
#

# Set the below var to the dontLog regular expression you want to test out.
$DONTLOG = "^/omax/(?!(catalog/letterLanding)|(catalog/displayCategory)|(profile/login))";

sub checkDontLog 
{
   my $url = shift;
   print $url; 
   my $dontLog = ($url =~ m:$DONTLOG:) ? "true" : "false";
   print " -> $dontLog\n";   
}

checkDontLog "/omax/catalog/letterLanding.jsp";
checkDontLog "/omax/catalog/displayCategory.jsp";
checkDontLog "/omax/profile/login.jsp";
checkDontLog "/omax/catalog/blah.jsp";
checkDontLog "/omax/other/foo.html";
