#!perl

use strict;
use warnings;
use SNMP;

my $host = '127.0.0.1';
my $port = 1610;
for (@ARGV) {
    if (/\./) {
	$host = $_;
    }
    else {
	$port = $_;
    }
}

print "Connecting to $host:$port\n";

#adjust this directory if needed
SNMP::addMibDirs('./mibs');

SNMP::loadModules('WWW-MIB', 'COVALENT-APACHE-STATUSv2-MIB');

my @server_flags = (
   '',
   '.', #1 dead
   'S', #2 starting
   '_', #3 ready
   'R', #4 reading
   'W', #5 write
   'K', #6 keepalive 
   'L', #7 log
   'D', #8 dns
   'G', #9 graceful
   'I', #10 accepting
);

my $DEAD = 1;
my $STARTING = 2;
my $READY = 3;
my $IDLE = 10;
my $MAX_LINE = 64;
my $KBYTE = 1024;
my $MBYTE = 1048576;
my $GBYTE = 1073741824;

my $session =
    SNMP::Session->new(DestHost   => $host,
		       Version    => 2,
		       Community  => 'public',
		       RemotePort => $port);

unless (defined $session) {
    die "Failed to create SNMP::Session";
}

my $services = $session->gettable('wwwServiceTable');
my $process = get_process();
get_thread($process);

my $generation = $process->[1]->{'ctApacheProcessStatusGeneration'};
my $description = $session->get('wwwServiceDescription.1');
my $uptime = $session->get('sysUpTime.0') / 100; #convert jiffies to sec
my $server_uptime = format_time($uptime);
my $hostname = $session->get('sysName.0');

print "Apache Server Status for $hostname\n\n";
print "Server Version: $description\n";
#Server Built: N/A

print "_" x $MAX_LINE, "\n\n";

print "Current Time: " . scalar localtime() . "\n";
#Restart Time: N/A
print "Parent Server Generation: $generation\n";
print "Server uptime: $server_uptime\n"; 
#CPU Usage: N/A
traffic();
scoreboard($process);
status($process);

#not seen in server-status
print "VirtualHost stats...\n";
requests();
responses();

sub service_info {
    my $ix = shift;
    my $name = $services->{$ix}->{'wwwServiceName'};
    my $port = $services->{$ix}->{'wwwServiceProtocol'};
    $port =~ s/tcp\.//;
    return "$name:$port";
}

sub traffic {
    my $table = $session->gettable('wwwSummaryTable');
    my $access = 0;
    my $bcount = 0;
    my $kbcount = 0;

    iter_threads($process, sub {
	my($proc, $thread) = @_;
	$bcount += $thread->{'ctApacheThreadStatusRequestBytes'};
	$access += $thread->{'ctApacheThreadStatusAccesses'};
	if ($bcount > $KBYTE) {
	    $kbcount += ($bcount >> 10);
	    $bcount = $bcount & 0x3ff;
	}
    });

    print "Total accesses: $access - Total Traffic: ", 
          format_kbytes($kbcount), "\n"; 

    my $rate = sprintf "%.3g", $access / $uptime;
    my $per_sec = format_bytes($KBYTE * $kbcount / $uptime);
    my $per_req = $access ? #prevent illegal division by zero
        format_bytes($KBYTE * $kbcount / $access) : 0;

    print "$rate requests/sec - $per_sec/second - $per_req/request\n\n";
}

sub requests {
    my $table = $session->gettable('wwwRequestInTable');

    for my $oid (sort keys %$table) {
	my $data = $table->{$oid};
	my $ix = $data->{'wwwServiceIndex'};
	my $info = service_info($ix);
	my $method = $data->{'wwwRequestInIndex'};
	my $bytes  = $data->{'wwwRequestInBytes'};
	my $num    = $data->{'wwwRequestInRequests'};
	next unless $num; #no requests for this method
	print "$info: $num $method requests, $bytes bytes\n";
    }
}

sub responses {
    my $table = $session->gettable('wwwResponseOutTable');

    for my $oid (sort keys %$table) {
	my $data = $table->{$oid};
	my $ix = $data->{'wwwServiceIndex'};
	my $info = service_info($ix);
	my $method = $data->{'wwwResponseOutIndex'};
	my $bytes  = $data->{'wwwResponseOutBytes'};
	my $num    = $data->{'wwwResponseOutResponses'};
	next unless $num; #no responses for this method
	print "$info: $num $method responses, $bytes bytes\n";
    }
}

sub get_process {
    my $table = $session->gettable('ctApacheProcessStatusTable');
    my @process;
    while (my($oid, $data) = each %$table) {
	my $ix = $data->{'ctApacheProcessStatusIndex'};
	$process[$ix] = $data;
    }
    return \@process;
}

sub get_thread {
    my $process = shift;
    my $table = $session->gettable('ctApacheThreadStatusTable');

    while (my($oid, $data) = each %$table) {
	my $process_ix = $data->{'ctApacheProcessStatusIndex'};
	my $thread_ix  = $data->{'ctApacheThreadStatusIndex'};
	my $threads = \@{ $process->[$process_ix]->{THREADS} };
	$threads->[$thread_ix] = $data;
    }
}

#iterate over each process and each thread within
sub iter_threads {
    my($process, $callback) = @_;

    my $num_process = scalar @$process;
    for (my $i=1; $i<$num_process; $i++) {
	my $proc = $process->[$i];
	my $threads = $proc->{THREADS};

	my $num_threads = scalar @$threads;

	for (my $j=1; $j<$num_threads; $j++) {
	    $callback->($proc, $threads->[$j]);
	}
    }
}

sub scoreboard {
    my $process = shift;

    my(@scoreboard, $busy, $ready);
    $busy = $ready = 0;

    iter_threads($process, sub {
	my($proc, $thread) = @_;
	my $pid    = $proc->{'ctApacheProcessStatusProcessId'};
	my $status = $thread->{'ctApacheThreadStatusOperStatus'};
	push @scoreboard, $server_flags[$status] || '?';

	if ($pid) {
	    if ($status == $READY) {
		$ready++;
	    }
	    elsif (($status != $DEAD) &&
		   ($status != $STARTING) &&
		   ($status != $IDLE))
	    {
		$busy++;
	    }
	}
    });

    print "$busy requests currently being processed, $ready idle workers\n";

    for (my $i=0; $i<=$#scoreboard; $i++) {
	print $scoreboard[$i];
	if (($i % $MAX_LINE) == ($MAX_LINE - 1)) {
	    print "\n";
	}
    }
    print "\n";
}

sub status {
    my $process = shift;

    #snmp does not provide 'CPU', 'SS', 'VHost', 'Req' fields
    print "Srv Pid Acc M Conn Child Slot Client Request\n";

    iter_threads($process, sub {
	my($proc, $thread) = @_;
	my $status       = $thread->{'ctApacheThreadStatusOperStatus'};
	my $access_count = $thread->{'ctApacheThreadStatusAccesses'};

	if (($access_count == 0) &&
	    (($status == $DEAD) || ($status == $READY)))
	{
	    return;
	}

	my $ix              = $proc->{'ctApacheProcessStatusIndex'} - 1;
	my $gen             = $proc->{'ctApacheProcessStatusGeneration'};
	my $pid             = $proc->{'ctApacheProcessStatusProcessId'};

	my $conn_count      = $thread->{'ctApacheThreadStatusConnCount'};
	my $my_access_count = $thread->{'ctApacheThreadStatusRequests'};
	my $conn_bytes      = $thread->{'ctApacheThreadStatusConnBytes'};
	my $my_bytes        = $thread->{'ctApacheThreadStatusRequestBytes'};
	my $bytes           = $thread->{'ctApacheThreadStatusBytes'};
	my $client          = $thread->{'ctApacheThreadStatusClient'};
	my $request         = $thread->{'ctApacheThreadStatusRequest'};

	my $mode = $server_flags[$status] || '?';
	my $acc   = "$conn_count/$my_access_count/$access_count";
	my $conn  = sprintf "%-1.1f", $conn_bytes / $KBYTE;
	my $child = sprintf "%-2.2f", $my_bytes / $MBYTE;
	my $slot  = sprintf "%-2.2f", $bytes / $MBYTE;

	print "$ix-$gen $pid $acc $mode $conn $child $slot $client $request\n";
    });

    print "\n";
}

sub tformat {
    my($num, $what) = @_;
    if ($num == 0) {
	return "";
    }
    my $data = "$num $what";
    if ($num != 1) {
	$data .= "s";
    }
    return "$data ";
}

sub format_time {
    my $tsecs = shift;
    my($days, $hrs, $mins, $secs);

    $secs = int($tsecs % 60);
    $tsecs /= 60;
    $mins = int($tsecs % 60);
    $tsecs /= 60;
    $hrs = int($tsecs % 24);
    $days = int($tsecs / 24);

    return
	tformat($days, 'day') .
	tformat($hrs, 'hour') .
	tformat($mins, 'minute') .
	tformat($secs, 'second');
}

sub format_bytes {
    my $bytes = shift;

    if ($bytes < (5 * $KBYTE)) {
        return sprintf "%d B", $bytes;
    }
    elsif ($bytes < ($MBYTE / 2)) {
        return sprintf "%.1f kB", $bytes / $KBYTE;
    }
    elsif ($bytes < ($GBYTE / 2)) {
        return sprintf "%.1f MB", $bytes / $MBYTE;
    }
    else {
        return sprintf "%.1f GB", $bytes / $GBYTE;
    }
}

sub format_kbytes {
    my $kbytes = shift;
    if ($kbytes < $KBYTE) {
        return sprintf "%d kB", $kbytes;
    }
    elsif ($kbytes < $MBYTE) {
        return sprintf("%.1f MB", $kbytes / $KBYTE);
    }
    else {
        return sprintf "%.1f GB", $kbytes / $MBYTE;
    }
}
