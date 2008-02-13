#!/bin/perl
#
# $Id$
#

sub checkTransforms 
{
   my $url = shift;   
   print "Before transforms: $url\n";          
   # The portion following the 's' is the transforms value to be tested.
   $url =~ s|\?.*||; 
   print "After transforms: $url\n";
}

checkTransforms "http://localhost:7080/resource/common/monitor/Visibility.do?mode=view&eid=3%3A10187&m=10460";
checkTransforms "http://foobar.com/blah/catalog/displayCategory.jsp";
