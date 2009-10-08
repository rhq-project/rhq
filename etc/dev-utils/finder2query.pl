#!/usr/bin/perl

# $Id$

die "Usage: finder2query <prefix> <file>" if $#ARGV<1;

$prefix = $ARGV[0];
$filename = $ARGV[1];

open(THEFILE,$filename) || die "Cannot open file $filename";

$infinder = 0;
$process_line =0;
while(<THEFILE>) {

   chop;
   if ($infinder==1) 
   {
        if ((m/^\s*\*\s*$/)  ||  (/\@(ejb|jboss)/))
        {
            $infinder=0;
            $process_line =1;            
        }
        else 
        {
            $line .= $_;
        }    
    }
    
    if ($process_line==1) 
    {        
        $line =~ /query="([^"]*)"/;
        $query = $1;
        $line =~ /signature="([^"]*)/;
        $signature = $1;
        # now match the name of the finder
        $signature =~ /([\w.]*) ([^(]*)/;
        $fname = $2;
        
        # load parameters
        $signature =~ /\(([^)]*)\)/;
        $args = $1;
        @argarray = split("," , $args);
        $numargs = $#argarray;
        
        for ($i = 0 ; $i <= $numargs  ; $i++) 
        {
            $arg = $argarray[$i];
            # remove leading / trailing whitespace
            $arg =~ s/^[\s]*([\w.]*)[\s]*(\w*)[\s]*$/\1 \2/;
            ($type, $name) = split(/ /,$arg);
            $pos = $i+1;
            $query =~ s/\?$pos/:$name/g;
        }
        # Convert some constructs to improve readability
        $query =~ s/ as / AS /;
        $query =~ s/select /SELECT /;
        $query =~ s/ from / FROM /;
        $query =~ s/ where / WHERE /;
        $query =~ s/ LCASE/ LOWER/;
        $query =~ s/ UCASE/ UPPER/;
        $query =~ s/OBJECT\(([\w]+)\)/\1/;
        $query =~ s/\s+/ /g;
        
        #print "--> sig: $signature  qu: $query\n";
        #print "--> name: $fname query: $query\n";
        
        # if mode == 1 -> jboss query override already processed
        #    so don't put a ejb.finder query in
        if ($modes{$fname}!=1) {
            $queries{$fname} = $query;
        }
        $modes{$fname}=$mode;    
        $process_line = 0;
    }

   if (/\@ejb.finder/ && $infinder == 0) 
   {
	  $infinder = 1;
	  $line = $_;
	  $mode = 0;
	  next;
	  
   }
   if (/\@jboss.query/ && $infinder == 0) 
   {
      #print "===> JBoss  $_\n";
      $infinder = 1;
	  $line = $_;
	  $mode = 1;
	  next;

   }
   

}

# Now that we have all finders output them as named querys

@query_keys = keys %queries;
$multi = 1 if $#query_keys >0;  ## $multi is set when we have >1 query

print "\@NamedQueries(\{\n" if $multi;
$i = $#query_keys;
foreach $query (@query_keys)
{
    print "   " if $multi;
    print "\@NamedQuery(name=\"$prefix.$query\",\n";
    print "      query=\"$queries{$query}\"";
    print "   " if $multi;
    print "\n)";
    print "," if $multi && $i > 0;
    print "\n";
    $i--;
}

print "\})\n" if $multi;
