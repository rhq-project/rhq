#!/bin/sh
_LOG_FILE=/var/www/html/rhqLog

# initial copy
echo "<html><h1>#jboss-on</h1><table>"`cat $1 | grep "PRIVMSG #jboss-on" | grep -v "SAFELIST" | grep -v "TARGMAX" | grep -v ">>>PRIVMSG" | awk '{$2=substr($2, 5, index($2, "!") - 5); $3=""; $4="";  print strftime("%b %d %H:%M:%S  %Y", substr($1, 0, 10))$0;}' | awk '{$1="<nobr>"$1; $3=$3"</nobr>"; $4=""; $5="</td><td><span style=\"color:red;\">"$5"</span></td><td>"; $6=substr($6, 2); print "<tr><td>"$0"</td></tr>"}' | sed 's/\(https\?:\/\/[^ <]*\)/<a href="\1">\1<\/a>/'` > $_LOG_FILE

# listen and add new changes
tail -f $1 | grep --line-buffered "PRIVMSG #jboss-on" | grep --line-buffered -v "SAFELIST" | grep --line-buffered -v "TARGMAX" | grep --line-buffered -v ">>>PRIVMSG" | awk '{$2=substr($2, 5, index($2, "!") - 5); $3=""; $4="";  print strftime("%b %d %H:%M:%S  %Y", substr($1, 0, 10))$0;system("")}' | awk '{$1="<nobr>"$1; $3=$3"</nobr>"; $4=""; $5="</td><td><span style=\"color:red;\">"$5"</span></td><td>"; $6=substr($6, 2); print "<tr><td>"$0"</td></tr>";system("")}' | sed 's/\(https\?:\/\/[^ <]*\)/<a href="\1">\1<\/a>/' >> $_LOG_FILE
