<#ftl >
<#--
/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
-->
<#-- @ftlvariable name="var" type="java.util.List<org.rhq.enterprise.server.rest.domain.Link>" -->
<html>
<body>
<h2>RHQ Rest API</h2>

Check out the <a href="https://github.com/rhq-project/samples/tree/master/rest-api">RHQ samples project</a>
for samples in various programming languages and
<a href="https://github.com/pilhuhn/RHQpocket">RHQpocket</a> as an example of a
mobile client (Android) of the API.
<ul>
<#list var as var>
 <li>
    <a href="/rest/${var.href}">${var.rel}</a>
 </li>
</#list>
</ul>
<hr/>
<h2>Misc examples (using <a href="https://github.com/mbostock/d3">D3.js</a>)</h2>
Those examples use the powerful D3 library and are rendering the graphs in SVG.
They expect some resources and schedules being present from an initial import with
an empty database (meaning resource ids and schedule ids starting at 10001).
<ul>
    <li><a href="/rest-examples/bars_simple.html">Display avg metrics as bars</a></li>
    <li><a href="/rest-examples/stacked1.html">Metrics as stacked bars</a> in SVG</li>
    <li><a href="/rest-examples/stacked3.html">Metrics as stacked bars</a> in pure HTML + (a little) CSS</li>
    <li><a href="/rest-examples/stacked2.html">Metrics as rects using stacked bars from above</a></li>
    <li><a href="/rest-examples/whisker.html">Whisker chart 1</a></li>
    <li><a href="/rest-examples/whisker2.html">Multiple Whisker charts</a></li>
    <li><a href="/rest-examples/tree.html">Resource tree</a></li>
    <li><a href="/rest-examples/raw_graph.html">7 days of raw metrics as dot- or line- chart</a></li>
    <li><a href="/rest-examples/raw_graph7.html">7 days of raw metrics as dot- or line- chart - comparing daily values</a></li>
</ul>
</body>
</html>
