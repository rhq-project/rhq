#!/bin/sh
#_PREFIX="../../modules/enterprise/gui/coregui/src/main/webapp/"
_PREFIX="../../dev-container/rhq-server/modules/org/rhq/server-startup/main/deployments/rhq.ear/coregui.war/"
_LIST="listOfBlack.txt"
_LIGHTNESS_GAIN=3000
while read _img; do
  _IMG_PATH="$_PREFIX/$_img"
  echo "converting: $_IMG_PATH..."
  convert -verbose $_IMG_PATH -colorspace HSL -channel B -evaluate add $_LIGHTNESS_GAIN +channel -colorspace sRGB $_IMG_PATH
done < $_LIST
