#!/usr/bin/perl

#
# Small script to create release notes from the git short log.
# Parameters are old and new tag. If the new tag is omitted,
# HEAD is used
#
# Output of the script can be pasted into the markup 
# release notes of confluence
#
# Sample command line:
# $ perl etc/scripts/createRelasenotes.pl RHQ_4_4_0 RHQ_4_5_0
#
# Author: Heiko W. Rupp
#


die "Usage createReleasenotes FROM_TAG [TO_TAG]" unless $#ARGV>-1;
$OLD_TAG=$ARGV[0];
$TO_TAG=$ARGV[1];

$COMMAND="git shortlog -e $OLD_TAG..$TO_TAG |";

open(GIT,$COMMAND);

while(<GIT>) {

#   print "IN:  ", $_;
   next if /^$/;
   s/\${/\$\\{/g;
   s/^[\s\[]*BZ\s([0-9]{5,7})\s?(\]? .*)$/      [BZ $1|https:\/\/bugzilla.redhat.com\/show_bug.cgi\?id\=$1] $2/;
   s/^[\s\[]*BZ-([0-9]{5,7})\s?(\]? .*)$/      [BZ $1|https:\/\/bugzilla.redhat.com\/show_bug.cgi\?id\=$1] $2/;
   s/^[\s\[]*BZ([0-9]{5,7})\s?(\]? .*)$/      [BZ $1|https:\/\/bugzilla.redhat.com\/show_bug.cgi\?id\=$1] $2/;
   s/^[\s\[]*Bug\s([0-9]{5,7})(\]? .*)$/      [BZ $1|https:\/\/bugzilla.redhat.com\/show_bug.cgi\?id\=$1] $2/;
   if (/      .*/) {
       s/^      (.*)/** $1/;
   } else {
       s/(.*)/* $1/;
   }
   print $_;
}
