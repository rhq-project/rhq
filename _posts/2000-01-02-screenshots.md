---
title: "Screenshots"
bg: orange
color: black
fa-icon: camera
---

{% for gallery in site.data.galleries %}
## **{{ gallery.description }}**
<table>
<tr>
    {% for image in gallery.images %}
    {% assign loopindex = forloop.index | modulo: 2 %}
<td style="padding-bottom: 30px;">{{ image.text }}
<a href="{{ gallery.imagefolder }}/{{ image.name }}" data-lightbox="{{ gallery.id }}" title="{{ image.text }}">
          <img style="border:3px solid black;" width="450px" src="./{{ gallery.imagefolder }}/{{ image.thumb }}">
</a></td>
        {% if loopindex == 0 %}
</tr><tr>
        {% endif %}
    {% endfor %}
</tr></table>
{% endfor %}

All screenshots are taken from RHQ 4.13.

