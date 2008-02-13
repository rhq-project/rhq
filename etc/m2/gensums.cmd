@echo off
::
:: A simple script that generates Apache-style md5 and sha1 checksum
:: files for the specified files.
::
:: NOTE: This batch file requires md5sum, sha1sum, and gawk to be in the
:: PATH. Win32 binaries can be obtained from
:: http://gnuwin32.sourceforge.net/.
::
:: $Id: gensums.cmd 4432 2007-04-23 20:15:41Z ispringer $
::
:loop
if "%1"=="" goto done

echo Processing %1...
echo Writing MD5 sum to %1.md5...
md5sum %1 | gawk "{print $1}" >%1.md5
echo Writing SHA1 sum to %1.sha1...
sha1sum %1 | gawk "{print $1}" >%1.sha1

shift
goto loop
   
:done
