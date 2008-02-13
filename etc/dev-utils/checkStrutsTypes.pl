#!/usr/bin/perl

# Root of the RHQ source tree
$RHQ="/jon/jonHEAD/jon";

chdir $RHQ || die "Can not chdir to the root of the source tree: $! \n";

##########  no need to change things below ##########################
$PORTAL="modules/enterprise/gui/portal-war/src/main";
$JAVA="java/";
$WEBAPP="webapp/";

chdir($PORTAL) || die "Can't chdir to the portal-war : $!";

open(CONFIG, "$WEBAPP/WEB-INF/struts-config.xml") || die "Cant open the struts-config: $!";
$line = 0;

while(<CONFIG>) {
	$line++ ;
	if ($_ =~ /^.*type="(.*)".*$/ )
	{
		$type = $1;
		$path = $type;
		$path =~ s/\./\//g;        # create dir path from FQDN
		$path = $path . ".java";   # we need the source file
		$x = stat("$JAVA$path");   # check if it exists
		if (!$x) {
			print "$line : $type \n";
		}
	}
}
