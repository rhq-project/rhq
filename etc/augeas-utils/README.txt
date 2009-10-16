Augeas Tree Utility

This utiliy generates a simple augeas tree from a configuration file. 
One can then use this information in their code to print various
configuration values from a augeas tree. 
Usage: 
augtree.sh <config file> [<root directory>]

for example
% augtree.sh /etc/hosts 

will print

/files/etc/hosts
  #comment[1] = Do not remove the following line, or various programs
  #comment[2] = that require network functionality will fail.
  1
    ipaddr = 127.0.0.1
    canonical = localhost.localdomain
    alias = localhost
  2
    ipaddr = ::1
    canonical = localhost6.localdomain6
    alias = localhost6
  3
    ipaddr = xx.xx.xx
    canonical = foo.rdu.redhat.com
    alias = foo

One can then use this in aug tool to generate different expressions. For example
%augtool
> get /files/etc/hosts/3/canonical 
 foo.rdu.redhat.com

One can also specify custom root paths, 
for example if we want to test a custom /etc/hosts file, 
create the following directory structure
% mkdir -p /tmp/etc
% cp /etc/hosts /tmp/etc/hosts
change /tmp/etc/hosts to whatever
% augtree.sh /etc/hosts /tmp
will use the etc/hosts relative to /tmp . 
NOTE: /tmp is just an example here you can use whatever root dir you want.


INSTALLATION:
% mvn install

