#
# RHQ Management Platform
# Copyright (C) 2005-2013 Red Hat, Inc.
# All rights reserved.
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation version 2 of the License.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#

import os
import operator
from xml.dom import minidom

def extract_plugin(descriptor):
   prefix = "rhq/modules/plugins/"
   descriptor = descriptor.replace(prefix,"")
   return descriptor[:descriptor.index("/")]

to_parse = []
to_parse.append("rhq/modules/plugins")

plugin_descriptors = []

while len(to_parse) != 0:
   current_folder = to_parse.pop(0)
   for current_file in os.listdir(current_folder):
      if current_file != "target":
         if os.path.isdir(os.path.join(current_folder,current_file)):
            to_parse.append(os.path.join(current_folder,current_file))
         else:
            if current_file.endswith("rhq-plugin.xml"):
               plugin_descriptors.append(os.path.join(current_folder,current_file))

final_tally = []

for descriptor in plugin_descriptors:
   xml_doc = minidom.parse(descriptor)
   metric_list = xml_doc.getElementsByTagName("metric")
   summary_metrics = 0
   detail_metrics = 0
   for metric in metric_list:
      if (metric.attributes.has_key("dataType") and metric.attributes["dataType"].value == "measurement") or \
          not metric.attributes.has_key("dataType"):
         if metric.attributes.has_key("displayType") and metric.attributes["displayType"].value == "summary":
            summary_metrics += 1
         else:
            detail_metrics +=1

   final_tally.append((extract_plugin(descriptor), summary_metrics, detail_metrics))


index1 = operator.itemgetter(1,2)
final_tally.sort(key=index1, reverse=True)
for (plugin_name, summary_metrics,detail_metrics) in final_tally :
   print "|",plugin_name,"|",summary_metrics,"|",detail_metrics,"|", summary_metrics + detail_metrics, "|"
