#!/bin/sh

echo "Present working directory"
echo "-------------------------"
pwd
echo

echo "Command-line arguments"
echo "----------------------"
while [ $# -gt 0 ]; do
  echo "$1"
  shift
done
echo

echo "Environment variables"
echo "---------------------"
env
echo

echo "stdout/stderr interleave test"
echo "-----------------------------"
echo "1 (stdout)"
echo "2 (stderr)" >&2
echo "3 (stdout)"
echo "4 (stderr)" >&2
echo

echo "Exiting with exit code 42..."
exit 42
