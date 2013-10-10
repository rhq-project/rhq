#!/bin/sh
echo >&2 "ERROR: $0 has been replaced by rhqctl."
cat <<EOF
Please use rhqctl. For help on the new command try:

    rhqctl --help
EOF
exit 1
