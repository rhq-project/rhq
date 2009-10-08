

/* jquery-1.2.3.js */


(function(){if(window.jQuery)var _jQuery=window.jQuery;var jQuery=window.jQuery=function(selector,context){return new jQuery.prototype.init(selector,context);};if(window.$)var _$=window.$;window.$=jQuery;var quickExpr=/^[^<]*(<(.|\s)+>)[^>]*$|^#(\w+)$/;var isSimple=/^.[^:#\[\.]*$/;jQuery.fn=jQuery.prototype={init:function(selector,context){selector=selector||document;if(selector.nodeType){this[0]=selector;this.length=1;return this;}else if(typeof selector=="string"){var match=quickExpr.exec(selector);if(match&&(match[1]||!context)){if(match[1])selector=jQuery.clean([match[1]],context);else{var elem=document.getElementById(match[3]);if(elem)if(elem.id!=match[3])return jQuery().find(selector);else{this[0]=elem;this.length=1;return this;}else
selector=[];}}else
return new jQuery(context).find(selector);}else if(jQuery.isFunction(selector))return new jQuery(document)[jQuery.fn.ready?"ready":"load"](selector);return this.setArray(selector.constructor==Array&&selector||(selector.jquery||selector.length&&selector!=window&&!selector.nodeType&&selector[0]!=undefined&&selector[0].nodeType)&&jQuery.makeArray(selector)||[selector]);},jquery:"1.2.3",size:function(){return this.length;},length:0,get:function(num){return num==undefined?jQuery.makeArray(this):this[num];},pushStack:function(elems){var ret=jQuery(elems);ret.prevObject=this;return ret;},setArray:function(elems){this.length=0;Array.prototype.push.apply(this,elems);return this;},each:function(callback,args){return jQuery.each(this,callback,args);},index:function(elem){var ret=-1;this.each(function(i){if(this==elem)ret=i;});return ret;},attr:function(name,value,type){var options=name;if(name.constructor==String)if(value==undefined)return this.length&&jQuery[type||"attr"](this[0],name)||undefined;else{options={};options[name]=value;}return this.each(function(i){for(name in options)jQuery.attr(type?this.style:this,name,jQuery.prop(this,options[name],type,i,name));});},css:function(key,value){if((key=='width'||key=='height')&&parseFloat(value)<0)value=undefined;return this.attr(key,value,"curCSS");},text:function(text){if(typeof text!="object"&&text!=null)return this.empty().append((this[0]&&this[0].ownerDocument||document).createTextNode(text));var ret="";jQuery.each(text||this,function(){jQuery.each(this.childNodes,function(){if(this.nodeType!=8)ret+=this.nodeType!=1?this.nodeValue:jQuery.fn.text([this]);});});return ret;},wrapAll:function(html){if(this[0])jQuery(html,this[0].ownerDocument).clone().insertBefore(this[0]).map(function(){var elem=this;while(elem.firstChild)elem=elem.firstChild;return elem;}).append(this);return this;},wrapInner:function(html){return this.each(function(){jQuery(this).contents().wrapAll(html);});},wrap:function(html){return this.each(function(){jQuery(this).wrapAll(html);});},append:function(){return this.domManip(arguments,true,false,function(elem){if(this.nodeType==1)this.appendChild(elem);});},prepend:function(){return this.domManip(arguments,true,true,function(elem){if(this.nodeType==1)this.insertBefore(elem,this.firstChild);});},before:function(){return this.domManip(arguments,false,false,function(elem){this.parentNode.insertBefore(elem,this);});},after:function(){return this.domManip(arguments,false,true,function(elem){this.parentNode.insertBefore(elem,this.nextSibling);});},end:function(){return this.prevObject||jQuery([]);},find:function(selector){var elems=jQuery.map(this,function(elem){return jQuery.find(selector,elem);});return this.pushStack(/[^+>] [^+>]/.test(selector)||selector.indexOf("..")>-1?jQuery.unique(elems):elems);},clone:function(events){var ret=this.map(function(){if(jQuery.browser.msie&&!jQuery.isXMLDoc(this)){var clone=this.cloneNode(true),container=document.createElement("div");container.appendChild(clone);return jQuery.clean([container.innerHTML])[0];}else
return this.cloneNode(true);});var clone=ret.find("*").andSelf().each(function(){if(this[expando]!=undefined)this[expando]=null;});if(events===true)this.find("*").andSelf().each(function(i){if(this.nodeType==3)return;var events=jQuery.data(this,"events");for(var type in events)for(var handler in events[type])jQuery.event.add(clone[i],type,events[type][handler],events[type][handler].data);});return ret;},filter:function(selector){return this.pushStack(jQuery.isFunction(selector)&&jQuery.grep(this,function(elem,i){return selector.call(elem,i);})||jQuery.multiFilter(selector,this));},not:function(selector){if(selector.constructor==String)if(isSimple.test(selector))return this.pushStack(jQuery.multiFilter(selector,this,true));else
selector=jQuery.multiFilter(selector,this);var isArrayLike=selector.length&&selector[selector.length-1]!==undefined&&!selector.nodeType;return this.filter(function(){return isArrayLike?jQuery.inArray(this,selector)<0:this!=selector;});},add:function(selector){return!selector?this:this.pushStack(jQuery.merge(this.get(),selector.constructor==String?jQuery(selector).get():selector.length!=undefined&&(!selector.nodeName||jQuery.nodeName(selector,"form"))?selector:[selector]));},is:function(selector){return selector?jQuery.multiFilter(selector,this).length>0:false;},hasClass:function(selector){return this.is("."+selector);},val:function(value){if(value==undefined){if(this.length){var elem=this[0];if(jQuery.nodeName(elem,"select")){var index=elem.selectedIndex,values=[],options=elem.options,one=elem.type=="select-one";if(index<0)return null;for(var i=one?index:0,max=one?index+1:options.length;i<max;i++){var option=options[i];if(option.selected){value=jQuery.browser.msie&&!option.attributes.value.specified?option.text:option.value;if(one)return value;values.push(value);}}return values;}else
return(this[0].value||"").replace(/\r/g,"");}return undefined;}return this.each(function(){if(this.nodeType!=1)return;if(value.constructor==Array&&/radio|checkbox/.test(this.type))this.checked=(jQuery.inArray(this.value,value)>=0||jQuery.inArray(this.name,value)>=0);else if(jQuery.nodeName(this,"select")){var values=value.constructor==Array?value:[value];jQuery("option",this).each(function(){this.selected=(jQuery.inArray(this.value,values)>=0||jQuery.inArray(this.text,values)>=0);});if(!values.length)this.selectedIndex=-1;}else
this.value=value;});},html:function(value){return value==undefined?(this.length?this[0].innerHTML:null):this.empty().append(value);},replaceWith:function(value){return this.after(value).remove();},eq:function(i){return this.slice(i,i+1);},slice:function(){return this.pushStack(Array.prototype.slice.apply(this,arguments));},map:function(callback){return this.pushStack(jQuery.map(this,function(elem,i){return callback.call(elem,i,elem);}));},andSelf:function(){return this.add(this.prevObject);},data:function(key,value){var parts=key.split(".");parts[1]=parts[1]?"."+parts[1]:"";if(value==null){var data=this.triggerHandler("getData"+parts[1]+"!",[parts[0]]);if(data==undefined&&this.length)data=jQuery.data(this[0],key);return data==null&&parts[1]?this.data(parts[0]):data;}else
return this.trigger("setData"+parts[1]+"!",[parts[0],value]).each(function(){jQuery.data(this,key,value);});},removeData:function(key){return this.each(function(){jQuery.removeData(this,key);});},domManip:function(args,table,reverse,callback){var clone=this.length>1,elems;return this.each(function(){if(!elems){elems=jQuery.clean(args,this.ownerDocument);if(reverse)elems.reverse();}var obj=this;if(table&&jQuery.nodeName(this,"table")&&jQuery.nodeName(elems[0],"tr"))obj=this.getElementsByTagName("tbody")[0]||this.appendChild(this.ownerDocument.createElement("tbody"));var scripts=jQuery([]);jQuery.each(elems,function(){var elem=clone?jQuery(this).clone(true)[0]:this;if(jQuery.nodeName(elem,"script")){scripts=scripts.add(elem);}else{if(elem.nodeType==1)scripts=scripts.add(jQuery("script",elem).remove());callback.call(obj,elem);}});scripts.each(evalScript);});}};jQuery.prototype.init.prototype=jQuery.prototype;function evalScript(i,elem){if(elem.src)jQuery.ajax({url:elem.src,async:false,dataType:"script"});else
jQuery.globalEval(elem.text||elem.textContent||elem.innerHTML||"");if(elem.parentNode)elem.parentNode.removeChild(elem);}jQuery.extend=jQuery.fn.extend=function(){var target=arguments[0]||{},i=1,length=arguments.length,deep=false,options;if(target.constructor==Boolean){deep=target;target=arguments[1]||{};i=2;}if(typeof target!="object"&&typeof target!="function")target={};if(length==1){target=this;i=0;}for(;i<length;i++)if((options=arguments[i])!=null)for(var name in options){if(target===options[name])continue;if(deep&&options[name]&&typeof options[name]=="object"&&target[name]&&!options[name].nodeType)target[name]=jQuery.extend(target[name],options[name]);else if(options[name]!=undefined)target[name]=options[name];}return target;};var expando="jQuery"+(new Date()).getTime(),uuid=0,windowData={};var exclude=/z-?index|font-?weight|opacity|zoom|line-?height/i;jQuery.extend({noConflict:function(deep){window.$=_$;if(deep)window.jQuery=_jQuery;return jQuery;},isFunction:function(fn){return!!fn&&typeof fn!="string"&&!fn.nodeName&&fn.constructor!=Array&&/function/i.test(fn+"");},isXMLDoc:function(elem){return elem.documentElement&&!elem.body||elem.tagName&&elem.ownerDocument&&!elem.ownerDocument.body;},globalEval:function(data){data=jQuery.trim(data);if(data){var head=document.getElementsByTagName("head")[0]||document.documentElement,script=document.createElement("script");script.type="text/javascript";if(jQuery.browser.msie)script.text=data;else
script.appendChild(document.createTextNode(data));head.appendChild(script);head.removeChild(script);}},nodeName:function(elem,name){return elem.nodeName&&elem.nodeName.toUpperCase()==name.toUpperCase();},cache:{},data:function(elem,name,data){elem=elem==window?windowData:elem;var id=elem[expando];if(!id)id=elem[expando]=++uuid;if(name&&!jQuery.cache[id])jQuery.cache[id]={};if(data!=undefined)jQuery.cache[id][name]=data;return name?jQuery.cache[id][name]:id;},removeData:function(elem,name){elem=elem==window?windowData:elem;var id=elem[expando];if(name){if(jQuery.cache[id]){delete jQuery.cache[id][name];name="";for(name in jQuery.cache[id])break;if(!name)jQuery.removeData(elem);}}else{try{delete elem[expando];}catch(e){if(elem.removeAttribute)elem.removeAttribute(expando);}delete jQuery.cache[id];}},each:function(object,callback,args){if(args){if(object.length==undefined){for(var name in object)if(callback.apply(object[name],args)===false)break;}else
for(var i=0,length=object.length;i<length;i++)if(callback.apply(object[i],args)===false)break;}else{if(object.length==undefined){for(var name in object)if(callback.call(object[name],name,object[name])===false)break;}else
for(var i=0,length=object.length,value=object[0];i<length&&callback.call(value,i,value)!==false;value=object[++i]){}}return object;},prop:function(elem,value,type,i,name){if(jQuery.isFunction(value))value=value.call(elem,i);return value&&value.constructor==Number&&type=="curCSS"&&!exclude.test(name)?value+"px":value;},className:{add:function(elem,classNames){jQuery.each((classNames||"").split(/\s+/),function(i,className){if(elem.nodeType==1&&!jQuery.className.has(elem.className,className))elem.className+=(elem.className?" ":"")+className;});},remove:function(elem,classNames){if(elem.nodeType==1)elem.className=classNames!=undefined?jQuery.grep(elem.className.split(/\s+/),function(className){return!jQuery.className.has(classNames,className);}).join(" "):"";},has:function(elem,className){return jQuery.inArray(className,(elem.className||elem).toString().split(/\s+/))>-1;}},swap:function(elem,options,callback){var old={};for(var name in options){old[name]=elem.style[name];elem.style[name]=options[name];}callback.call(elem);for(var name in options)elem.style[name]=old[name];},css:function(elem,name,force){if(name=="width"||name=="height"){var val,props={position:"absolute",visibility:"hidden",display:"block"},which=name=="width"?["Left","Right"]:["Top","Bottom"];function getWH(){val=name=="width"?elem.offsetWidth:elem.offsetHeight;var padding=0,border=0;jQuery.each(which,function(){padding+=parseFloat(jQuery.curCSS(elem,"padding"+this,true))||0;border+=parseFloat(jQuery.curCSS(elem,"border"+this+"Width",true))||0;});val-=Math.round(padding+border);}if(jQuery(elem).is(":visible"))getWH();else
jQuery.swap(elem,props,getWH);return Math.max(0,val);}return jQuery.curCSS(elem,name,force);},curCSS:function(elem,name,force){var ret;function color(elem){if(!jQuery.browser.safari)return false;var ret=document.defaultView.getComputedStyle(elem,null);return!ret||ret.getPropertyValue("color")=="";}if(name=="opacity"&&jQuery.browser.msie){ret=jQuery.attr(elem.style,"opacity");return ret==""?"1":ret;}if(jQuery.browser.opera&&name=="display"){var save=elem.style.outline;elem.style.outline="0 solid black";elem.style.outline=save;}if(name.match(/float/i))name=styleFloat;if(!force&&elem.style&&elem.style[name])ret=elem.style[name];else if(document.defaultView&&document.defaultView.getComputedStyle){if(name.match(/float/i))name="float";name=name.replace(/([A-Z])/g,"-$1").toLowerCase();var getComputedStyle=document.defaultView.getComputedStyle(elem,null);if(getComputedStyle&&!color(elem))ret=getComputedStyle.getPropertyValue(name);else{var swap=[],stack=[];for(var a=elem;a&&color(a);a=a.parentNode)stack.unshift(a);for(var i=0;i<stack.length;i++)if(color(stack[i])){swap[i]=stack[i].style.display;stack[i].style.display="block";}ret=name=="display"&&swap[stack.length-1]!=null?"none":(getComputedStyle&&getComputedStyle.getPropertyValue(name))||"";for(var i=0;i<swap.length;i++)if(swap[i]!=null)stack[i].style.display=swap[i];}if(name=="opacity"&&ret=="")ret="1";}else if(elem.currentStyle){var camelCase=name.replace(/\-(\w)/g,function(all,letter){return letter.toUpperCase();});ret=elem.currentStyle[name]||elem.currentStyle[camelCase];if(!/^\d+(px)?$/i.test(ret)&&/^\d/.test(ret)){var style=elem.style.left,runtimeStyle=elem.runtimeStyle.left;elem.runtimeStyle.left=elem.currentStyle.left;elem.style.left=ret||0;ret=elem.style.pixelLeft+"px";elem.style.left=style;elem.runtimeStyle.left=runtimeStyle;}}return ret;},clean:function(elems,context){var ret=[];context=context||document;if(typeof context.createElement=='undefined')context=context.ownerDocument||context[0]&&context[0].ownerDocument||document;jQuery.each(elems,function(i,elem){if(!elem)return;if(elem.constructor==Number)elem=elem.toString();if(typeof elem=="string"){elem=elem.replace(/(<(\w+)[^>]*?)\/>/g,function(all,front,tag){return tag.match(/^(abbr|br|col|img|input|link|meta|param|hr|area|embed)$/i)?all:front+"></"+tag+">";});var tags=jQuery.trim(elem).toLowerCase(),div=context.createElement("div");var wrap=!tags.indexOf("<opt")&&[1,"<select multiple='multiple'>","</select>"]||!tags.indexOf("<leg")&&[1,"<fieldset>","</fieldset>"]||tags.match(/^<(thead|tbody|tfoot|colg|cap)/)&&[1,"<table>","</table>"]||!tags.indexOf("<tr")&&[2,"<table><tbody>","</tbody></table>"]||(!tags.indexOf("<td")||!tags.indexOf("<th"))&&[3,"<table><tbody><tr>","</tr></tbody></table>"]||!tags.indexOf("<col")&&[2,"<table><tbody></tbody><colgroup>","</colgroup></table>"]||jQuery.browser.msie&&[1,"div<div>","</div>"]||[0,"",""];div.innerHTML=wrap[1]+elem+wrap[2];while(wrap[0]--)div=div.lastChild;if(jQuery.browser.msie){var tbody=!tags.indexOf("<table")&&tags.indexOf("<tbody")<0?div.firstChild&&div.firstChild.childNodes:wrap[1]=="<table>"&&tags.indexOf("<tbody")<0?div.childNodes:[];for(var j=tbody.length-1;j>=0;--j)if(jQuery.nodeName(tbody[j],"tbody")&&!tbody[j].childNodes.length)tbody[j].parentNode.removeChild(tbody[j]);if(/^\s/.test(elem))div.insertBefore(context.createTextNode(elem.match(/^\s*/)[0]),div.firstChild);}elem=jQuery.makeArray(div.childNodes);}if(elem.length===0&&(!jQuery.nodeName(elem,"form")&&!jQuery.nodeName(elem,"select")))return;if(elem[0]==undefined||jQuery.nodeName(elem,"form")||elem.options)ret.push(elem);else
ret=jQuery.merge(ret,elem);});return ret;},attr:function(elem,name,value){if(!elem||elem.nodeType==3||elem.nodeType==8)return undefined;var fix=jQuery.isXMLDoc(elem)?{}:jQuery.props;if(name=="selected"&&jQuery.browser.safari)elem.parentNode.selectedIndex;if(fix[name]){if(value!=undefined)elem[fix[name]]=value;return elem[fix[name]];}else if(jQuery.browser.msie&&name=="style")return jQuery.attr(elem.style,"cssText",value);else if(value==undefined&&jQuery.browser.msie&&jQuery.nodeName(elem,"form")&&(name=="action"||name=="method"))return elem.getAttributeNode(name).nodeValue;else if(elem.tagName){if(value!=undefined){if(name=="type"&&jQuery.nodeName(elem,"input")&&elem.parentNode)throw"type property can't be changed";elem.setAttribute(name,""+value);}if(jQuery.browser.msie&&/href|src/.test(name)&&!jQuery.isXMLDoc(elem))return elem.getAttribute(name,2);return elem.getAttribute(name);}else{if(name=="opacity"&&jQuery.browser.msie){if(value!=undefined){elem.zoom=1;elem.filter=(elem.filter||"").replace(/alpha\([^)]*\)/,"")+(parseFloat(value).toString()=="NaN"?"":"alpha(opacity="+value*100+")");}return elem.filter&&elem.filter.indexOf("opacity=")>=0?(parseFloat(elem.filter.match(/opacity=([^)]*)/)[1])/100).toString():"";}name=name.replace(/-([a-z])/ig,function(all,letter){return letter.toUpperCase();});if(value!=undefined)elem[name]=value;return elem[name];}},trim:function(text){return(text||"").replace(/^\s+|\s+$/g,"");},makeArray:function(array){var ret=[];if(typeof array!="array")for(var i=0,length=array.length;i<length;i++)ret.push(array[i]);else
ret=array.slice(0);return ret;},inArray:function(elem,array){for(var i=0,length=array.length;i<length;i++)if(array[i]==elem)return i;return-1;},merge:function(first,second){if(jQuery.browser.msie){for(var i=0;second[i];i++)if(second[i].nodeType!=8)first.push(second[i]);}else
for(var i=0;second[i];i++)first.push(second[i]);return first;},unique:function(array){var ret=[],done={};try{for(var i=0,length=array.length;i<length;i++){var id=jQuery.data(array[i]);if(!done[id]){done[id]=true;ret.push(array[i]);}}}catch(e){ret=array;}return ret;},grep:function(elems,callback,inv){var ret=[];for(var i=0,length=elems.length;i<length;i++)if(!inv&&callback(elems[i],i)||inv&&!callback(elems[i],i))ret.push(elems[i]);return ret;},map:function(elems,callback){var ret=[];for(var i=0,length=elems.length;i<length;i++){var value=callback(elems[i],i);if(value!==null&&value!=undefined){if(value.constructor!=Array)value=[value];ret=ret.concat(value);}}return ret;}});var userAgent=navigator.userAgent.toLowerCase();jQuery.browser={version:(userAgent.match(/.+(?:rv|it|ra|ie)[\/: ]([\d.]+)/)||[])[1],safari:/webkit/.test(userAgent),opera:/opera/.test(userAgent),msie:/msie/.test(userAgent)&&!/opera/.test(userAgent),mozilla:/mozilla/.test(userAgent)&&!/(compatible|webkit)/.test(userAgent)};var styleFloat=jQuery.browser.msie?"styleFloat":"cssFloat";jQuery.extend({boxModel:!jQuery.browser.msie||document.compatMode=="CSS1Compat",props:{"for":"htmlFor","class":"className","float":styleFloat,cssFloat:styleFloat,styleFloat:styleFloat,innerHTML:"innerHTML",className:"className",value:"value",disabled:"disabled",checked:"checked",readonly:"readOnly",selected:"selected",maxlength:"maxLength",selectedIndex:"selectedIndex",defaultValue:"defaultValue",tagName:"tagName",nodeName:"nodeName"}});jQuery.each({parent:function(elem){return elem.parentNode;},parents:function(elem){return jQuery.dir(elem,"parentNode");},next:function(elem){return jQuery.nth(elem,2,"nextSibling");},prev:function(elem){return jQuery.nth(elem,2,"previousSibling");},nextAll:function(elem){return jQuery.dir(elem,"nextSibling");},prevAll:function(elem){return jQuery.dir(elem,"previousSibling");},siblings:function(elem){return jQuery.sibling(elem.parentNode.firstChild,elem);},children:function(elem){return jQuery.sibling(elem.firstChild);},contents:function(elem){return jQuery.nodeName(elem,"iframe")?elem.contentDocument||elem.contentWindow.document:jQuery.makeArray(elem.childNodes);}},function(name,fn){jQuery.fn[name]=function(selector){var ret=jQuery.map(this,fn);if(selector&&typeof selector=="string")ret=jQuery.multiFilter(selector,ret);return this.pushStack(jQuery.unique(ret));};});jQuery.each({appendTo:"append",prependTo:"prepend",insertBefore:"before",insertAfter:"after",replaceAll:"replaceWith"},function(name,original){jQuery.fn[name]=function(){var args=arguments;return this.each(function(){for(var i=0,length=args.length;i<length;i++)jQuery(args[i])[original](this);});};});jQuery.each({removeAttr:function(name){jQuery.attr(this,name,"");if(this.nodeType==1)this.removeAttribute(name);},addClass:function(classNames){jQuery.className.add(this,classNames);},removeClass:function(classNames){jQuery.className.remove(this,classNames);},toggleClass:function(classNames){jQuery.className[jQuery.className.has(this,classNames)?"remove":"add"](this,classNames);},remove:function(selector){if(!selector||jQuery.filter(selector,[this]).r.length){jQuery("*",this).add(this).each(function(){jQuery.event.remove(this);jQuery.removeData(this);});if(this.parentNode)this.parentNode.removeChild(this);}},empty:function(){jQuery(">*",this).remove();while(this.firstChild)this.removeChild(this.firstChild);}},function(name,fn){jQuery.fn[name]=function(){return this.each(fn,arguments);};});jQuery.each(["Height","Width"],function(i,name){var type=name.toLowerCase();jQuery.fn[type]=function(size){return this[0]==window?jQuery.browser.opera&&document.body["client"+name]||jQuery.browser.safari&&window["inner"+name]||document.compatMode=="CSS1Compat"&&document.documentElement["client"+name]||document.body["client"+name]:this[0]==document?Math.max(Math.max(document.body["scroll"+name],document.documentElement["scroll"+name]),Math.max(document.body["offset"+name],document.documentElement["offset"+name])):size==undefined?(this.length?jQuery.css(this[0],type):null):this.css(type,size.constructor==String?size:size+"px");};});var chars=jQuery.browser.safari&&parseInt(jQuery.browser.version)<417?"(?:[\\w*_-]|\\\\.)":"(?:[\\w\u0128-\uFFFF*_-]|\\\\.)",quickChild=new RegExp("^>\\s*("+chars+"+)"),quickID=new RegExp("^("+chars+"+)(#)("+chars+"+)"),quickClass=new RegExp("^([#.]?)("+chars+"*)");jQuery.extend({expr:{"":function(a,i,m){return m[2]=="*"||jQuery.nodeName(a,m[2]);},"#":function(a,i,m){return a.getAttribute("id")==m[2];},":":{lt:function(a,i,m){return i<m[3]-0;},gt:function(a,i,m){return i>m[3]-0;},nth:function(a,i,m){return m[3]-0==i;},eq:function(a,i,m){return m[3]-0==i;},first:function(a,i){return i==0;},last:function(a,i,m,r){return i==r.length-1;},even:function(a,i){return i%2==0;},odd:function(a,i){return i%2;},"first-child":function(a){return a.parentNode.getElementsByTagName("*")[0]==a;},"last-child":function(a){return jQuery.nth(a.parentNode.lastChild,1,"previousSibling")==a;},"only-child":function(a){return!jQuery.nth(a.parentNode.lastChild,2,"previousSibling");},parent:function(a){return a.firstChild;},empty:function(a){return!a.firstChild;},contains:function(a,i,m){return(a.textContent||a.innerText||jQuery(a).text()||"").indexOf(m[3])>=0;},visible:function(a){return"hidden"!=a.type&&jQuery.css(a,"display")!="none"&&jQuery.css(a,"visibility")!="hidden";},hidden:function(a){return"hidden"==a.type||jQuery.css(a,"display")=="none"||jQuery.css(a,"visibility")=="hidden";},enabled:function(a){return!a.disabled;},disabled:function(a){return a.disabled;},checked:function(a){return a.checked;},selected:function(a){return a.selected||jQuery.attr(a,"selected");},text:function(a){return"text"==a.type;},radio:function(a){return"radio"==a.type;},checkbox:function(a){return"checkbox"==a.type;},file:function(a){return"file"==a.type;},password:function(a){return"password"==a.type;},submit:function(a){return"submit"==a.type;},image:function(a){return"image"==a.type;},reset:function(a){return"reset"==a.type;},button:function(a){return"button"==a.type||jQuery.nodeName(a,"button");},input:function(a){return/input|select|textarea|button/i.test(a.nodeName);},has:function(a,i,m){return jQuery.find(m[3],a).length;},header:function(a){return/h\d/i.test(a.nodeName);},animated:function(a){return jQuery.grep(jQuery.timers,function(fn){return a==fn.elem;}).length;}}},parse:[new RegExp("^(\[) *@?([\w-]+) *([!*$^~=]*) *('?\"?)(.*?)\4 *\]"),new RegExp("^(:)([\w-]+)\(\"?'?(.*?(\(.*?\))?[^(]*?)\"?'?\)"),new RegExp("^([:.#]*)("+chars+"+)")],multiFilter:function(expr,elems,not){var old,cur=[];while(expr&&expr!=old){old=expr;var f=jQuery.filter(expr,elems,not);expr=f.t.replace(/^\s*,\s*/,"");cur=not?elems=f.r:jQuery.merge(cur,f.r);}return cur;},find:function(t,context){if(typeof t!="string")return[t];if(context&&context.nodeType!=1&&context.nodeType!=9)return[];context=context||document;var ret=[context],done=[],last,nodeName;while(t&&last!=t){var r=[];last=t;t=jQuery.trim(t);var foundToken=false;var re=quickChild;var m=re.exec(t);if(m){nodeName=m[1].toUpperCase();for(var i=0;ret[i];i++)for(var c=ret[i].firstChild;c;c=c.nextSibling)if(c.nodeType==1&&(nodeName=="*"||c.nodeName.toUpperCase()==nodeName))r.push(c);ret=r;t=t.replace(re,"");if(t.indexOf(" ")==0)continue;foundToken=true;}else{re=/^([>+~])\s*(\w*)/i;if((m=re.exec(t))!=null){r=[];var merge={};nodeName=m[2].toUpperCase();m=m[1];for(var j=0,rl=ret.length;j<rl;j++){var n=m=="~"||m=="+"?ret[j].nextSibling:ret[j].firstChild;for(;n;n=n.nextSibling)if(n.nodeType==1){var id=jQuery.data(n);if(m=="~"&&merge[id])break;if(!nodeName||n.nodeName.toUpperCase()==nodeName){if(m=="~")merge[id]=true;r.push(n);}if(m=="+")break;}}ret=r;t=jQuery.trim(t.replace(re,""));foundToken=true;}}if(t&&!foundToken){if(!t.indexOf(",")){if(context==ret[0])ret.shift();done=jQuery.merge(done,ret);r=ret=[context];t=" "+t.substr(1,t.length);}else{var re2=quickID;var m=re2.exec(t);if(m){m=[0,m[2],m[3],m[1]];}else{re2=quickClass;m=re2.exec(t);}m[2]=m[2].replace(/\\/g,"");var elem=ret[ret.length-1];if(m[1]=="#"&&elem&&elem.getElementById&&!jQuery.isXMLDoc(elem)){var oid=elem.getElementById(m[2]);if((jQuery.browser.msie||jQuery.browser.opera)&&oid&&typeof oid.id=="string"&&oid.id!=m[2])oid=jQuery('[@id="'+m[2]+'"]',elem)[0];ret=r=oid&&(!m[3]||jQuery.nodeName(oid,m[3]))?[oid]:[];}else{for(var i=0;ret[i];i++){var tag=m[1]=="#"&&m[3]?m[3]:m[1]!=""||m[0]==""?"*":m[2];if(tag=="*"&&ret[i].nodeName.toLowerCase()=="object")tag="param";r=jQuery.merge(r,ret[i].getElementsByTagName(tag));}if(m[1]==".")r=jQuery.classFilter(r,m[2]);if(m[1]=="#"){var tmp=[];for(var i=0;r[i];i++)if(r[i].getAttribute("id")==m[2]){tmp=[r[i]];break;}r=tmp;}ret=r;}t=t.replace(re2,"");}}if(t){var val=jQuery.filter(t,r);ret=r=val.r;t=jQuery.trim(val.t);}}if(t)ret=[];if(ret&&context==ret[0])ret.shift();done=jQuery.merge(done,ret);return done;},classFilter:function(r,m,not){m=" "+m+" ";var tmp=[];for(var i=0;r[i];i++){var pass=(" "+r[i].className+" ").indexOf(m)>=0;if(!not&&pass||not&&!pass)tmp.push(r[i]);}return tmp;},filter:function(t,r,not){var last;while(t&&t!=last){last=t;var p=jQuery.parse,m;for(var i=0;p[i];i++){m=p[i].exec(t);if(m){t=t.substring(m[0].length);m[2]=m[2].replace(/\\/g,"");break;}}if(!m)break;if(m[1]==":"&&m[2]=="not")r=isSimple.test(m[3])?jQuery.filter(m[3],r,true).r:jQuery(r).not(m[3]);else if(m[1]==".")r=jQuery.classFilter(r,m[2],not);else if(m[1]=="["){var tmp=[],type=m[3];for(var i=0,rl=r.length;i<rl;i++){var a=r[i],z=a[jQuery.props[m[2]]||m[2]];if(z==null||/href|src|selected/.test(m[2]))z=jQuery.attr(a,m[2])||'';if((type==""&&!!z||type=="="&&z==m[5]||type=="!="&&z!=m[5]||type=="^="&&z&&!z.indexOf(m[5])||type=="$="&&z.substr(z.length-m[5].length)==m[5]||(type=="*="||type=="~=")&&z.indexOf(m[5])>=0)^not)tmp.push(a);}r=tmp;}else if(m[1]==":"&&m[2]=="nth-child"){var merge={},tmp=[],test=/(-?)(\d*)n((?:\+|-)?\d*)/.exec(m[3]=="even"&&"2n"||m[3]=="odd"&&"2n+1"||!/\D/.test(m[3])&&"0n+"+m[3]||m[3]),first=(test[1]+(test[2]||1))-0,last=test[3]-0;for(var i=0,rl=r.length;i<rl;i++){var node=r[i],parentNode=node.parentNode,id=jQuery.data(parentNode);if(!merge[id]){var c=1;for(var n=parentNode.firstChild;n;n=n.nextSibling)if(n.nodeType==1)n.nodeIndex=c++;merge[id]=true;}var add=false;if(first==0){if(node.nodeIndex==last)add=true;}else if((node.nodeIndex-last)%first==0&&(node.nodeIndex-last)/first>=0)add=true;if(add^not)tmp.push(node);}r=tmp;}else{var fn=jQuery.expr[m[1]];if(typeof fn=="object")fn=fn[m[2]];if(typeof fn=="string")fn=eval("false||function(a,i){return "+fn+";}");r=jQuery.grep(r,function(elem,i){return fn(elem,i,m,r);},not);}}return{r:r,t:t};},dir:function(elem,dir){var matched=[];var cur=elem[dir];while(cur&&cur!=document){if(cur.nodeType==1)matched.push(cur);cur=cur[dir];}return matched;},nth:function(cur,result,dir,elem){result=result||1;var num=0;for(;cur;cur=cur[dir])if(cur.nodeType==1&&++num==result)break;return cur;},sibling:function(n,elem){var r=[];for(;n;n=n.nextSibling){if(n.nodeType==1&&(!elem||n!=elem))r.push(n);}return r;}});jQuery.event={add:function(elem,types,handler,data){if(elem.nodeType==3||elem.nodeType==8)return;if(jQuery.browser.msie&&elem.setInterval!=undefined)elem=window;if(!handler.guid)handler.guid=this.guid++;if(data!=undefined){var fn=handler;handler=function(){return fn.apply(this,arguments);};handler.data=data;handler.guid=fn.guid;}var events=jQuery.data(elem,"events")||jQuery.data(elem,"events",{}),handle=jQuery.data(elem,"handle")||jQuery.data(elem,"handle",function(){var val;if(typeof jQuery=="undefined"||jQuery.event.triggered)return val;val=jQuery.event.handle.apply(arguments.callee.elem,arguments);return val;});handle.elem=elem;jQuery.each(types.split(/\s+/),function(index,type){var parts=type.split(".");type=parts[0];handler.type=parts[1];var handlers=events[type];if(!handlers){handlers=events[type]={};if(!jQuery.event.special[type]||jQuery.event.special[type].setup.call(elem)===false){if(elem.addEventListener)elem.addEventListener(type,handle,false);else if(elem.attachEvent)elem.attachEvent("on"+type,handle);}}handlers[handler.guid]=handler;jQuery.event.global[type]=true;});elem=null;},guid:1,global:{},remove:function(elem,types,handler){if(elem.nodeType==3||elem.nodeType==8)return;var events=jQuery.data(elem,"events"),ret,index;if(events){if(types==undefined||(typeof types=="string"&&types.charAt(0)=="."))for(var type in events)this.remove(elem,type+(types||""));else{if(types.type){handler=types.handler;types=types.type;}jQuery.each(types.split(/\s+/),function(index,type){var parts=type.split(".");type=parts[0];if(events[type]){if(handler)delete events[type][handler.guid];else
for(handler in events[type])if(!parts[1]||events[type][handler].type==parts[1])delete events[type][handler];for(ret in events[type])break;if(!ret){if(!jQuery.event.special[type]||jQuery.event.special[type].teardown.call(elem)===false){if(elem.removeEventListener)elem.removeEventListener(type,jQuery.data(elem,"handle"),false);else if(elem.detachEvent)elem.detachEvent("on"+type,jQuery.data(elem,"handle"));}ret=null;delete events[type];}}});}for(ret in events)break;if(!ret){var handle=jQuery.data(elem,"handle");if(handle)handle.elem=null;jQuery.removeData(elem,"events");jQuery.removeData(elem,"handle");}}},trigger:function(type,data,elem,donative,extra){data=jQuery.makeArray(data||[]);if(type.indexOf("!")>=0){type=type.slice(0,-1);var exclusive=true;}if(!elem){if(this.global[type])jQuery("*").add([window,document]).trigger(type,data);}else{if(elem.nodeType==3||elem.nodeType==8)return undefined;var val,ret,fn=jQuery.isFunction(elem[type]||null),event=!data[0]||!data[0].preventDefault;if(event)data.unshift(this.fix({type:type,target:elem}));data[0].type=type;if(exclusive)data[0].exclusive=true;if(jQuery.isFunction(jQuery.data(elem,"handle")))val=jQuery.data(elem,"handle").apply(elem,data);if(!fn&&elem["on"+type]&&elem["on"+type].apply(elem,data)===false)val=false;if(event)data.shift();if(extra&&jQuery.isFunction(extra)){ret=extra.apply(elem,val==null?data:data.concat(val));if(ret!==undefined)val=ret;}if(fn&&donative!==false&&val!==false&&!(jQuery.nodeName(elem,'a')&&type=="click")){this.triggered=true;try{elem[type]();}catch(e){}}this.triggered=false;}return val;},handle:function(event){var val;event=jQuery.event.fix(event||window.event||{});var parts=event.type.split(".");event.type=parts[0];var handlers=jQuery.data(this,"events")&&jQuery.data(this,"events")[event.type],args=Array.prototype.slice.call(arguments,1);args.unshift(event);for(var j in handlers){var handler=handlers[j];args[0].handler=handler;args[0].data=handler.data;if(!parts[1]&&!event.exclusive||handler.type==parts[1]){var ret=handler.apply(this,args);if(val!==false)val=ret;if(ret===false){event.preventDefault();event.stopPropagation();}}}if(jQuery.browser.msie)event.target=event.preventDefault=event.stopPropagation=event.handler=event.data=null;return val;},fix:function(event){var originalEvent=event;event=jQuery.extend({},originalEvent);event.preventDefault=function(){if(originalEvent.preventDefault)originalEvent.preventDefault();originalEvent.returnValue=false;};event.stopPropagation=function(){if(originalEvent.stopPropagation)originalEvent.stopPropagation();originalEvent.cancelBubble=true;};if(!event.target)event.target=event.srcElement||document;if(event.target.nodeType==3)event.target=originalEvent.target.parentNode;if(!event.relatedTarget&&event.fromElement)event.relatedTarget=event.fromElement==event.target?event.toElement:event.fromElement;if(event.pageX==null&&event.clientX!=null){var doc=document.documentElement,body=document.body;event.pageX=event.clientX+(doc&&doc.scrollLeft||body&&body.scrollLeft||0)-(doc.clientLeft||0);event.pageY=event.clientY+(doc&&doc.scrollTop||body&&body.scrollTop||0)-(doc.clientTop||0);}if(!event.which&&((event.charCode||event.charCode===0)?event.charCode:event.keyCode))event.which=event.charCode||event.keyCode;if(!event.metaKey&&event.ctrlKey)event.metaKey=event.ctrlKey;if(!event.which&&event.button)event.which=(event.button&1?1:(event.button&2?3:(event.button&4?2:0)));return event;},special:{ready:{setup:function(){bindReady();return;},teardown:function(){return;}},mouseenter:{setup:function(){if(jQuery.browser.msie)return false;jQuery(this).bind("mouseover",jQuery.event.special.mouseenter.handler);return true;},teardown:function(){if(jQuery.browser.msie)return false;jQuery(this).unbind("mouseover",jQuery.event.special.mouseenter.handler);return true;},handler:function(event){if(withinElement(event,this))return true;arguments[0].type="mouseenter";return jQuery.event.handle.apply(this,arguments);}},mouseleave:{setup:function(){if(jQuery.browser.msie)return false;jQuery(this).bind("mouseout",jQuery.event.special.mouseleave.handler);return true;},teardown:function(){if(jQuery.browser.msie)return false;jQuery(this).unbind("mouseout",jQuery.event.special.mouseleave.handler);return true;},handler:function(event){if(withinElement(event,this))return true;arguments[0].type="mouseleave";return jQuery.event.handle.apply(this,arguments);}}}};jQuery.fn.extend({bind:function(type,data,fn){return type=="unload"?this.one(type,data,fn):this.each(function(){jQuery.event.add(this,type,fn||data,fn&&data);});},one:function(type,data,fn){return this.each(function(){jQuery.event.add(this,type,function(event){jQuery(this).unbind(event);return(fn||data).apply(this,arguments);},fn&&data);});},unbind:function(type,fn){return this.each(function(){jQuery.event.remove(this,type,fn);});},trigger:function(type,data,fn){return this.each(function(){jQuery.event.trigger(type,data,this,true,fn);});},triggerHandler:function(type,data,fn){if(this[0])return jQuery.event.trigger(type,data,this[0],false,fn);return undefined;},toggle:function(){var args=arguments;return this.click(function(event){this.lastToggle=0==this.lastToggle?1:0;event.preventDefault();return args[this.lastToggle].apply(this,arguments)||false;});},hover:function(fnOver,fnOut){return this.bind('mouseenter',fnOver).bind('mouseleave',fnOut);},ready:function(fn){bindReady();if(jQuery.isReady)fn.call(document,jQuery);else
jQuery.readyList.push(function(){return fn.call(this,jQuery);});return this;}});jQuery.extend({isReady:false,readyList:[],ready:function(){if(!jQuery.isReady){jQuery.isReady=true;if(jQuery.readyList){jQuery.each(jQuery.readyList,function(){this.apply(document);});jQuery.readyList=null;}jQuery(document).triggerHandler("ready");}}});var readyBound=false;function bindReady(){if(readyBound)return;readyBound=true;if(document.addEventListener&&!jQuery.browser.opera)document.addEventListener("DOMContentLoaded",jQuery.ready,false);if(jQuery.browser.msie&&window==top)(function(){if(jQuery.isReady)return;try{document.documentElement.doScroll("left");}catch(error){setTimeout(arguments.callee,0);return;}jQuery.ready();})();if(jQuery.browser.opera)document.addEventListener("DOMContentLoaded",function(){if(jQuery.isReady)return;for(var i=0;i<document.styleSheets.length;i++)if(document.styleSheets[i].disabled){setTimeout(arguments.callee,0);return;}jQuery.ready();},false);if(jQuery.browser.safari){var numStyles;(function(){if(jQuery.isReady)return;if(document.readyState!="loaded"&&document.readyState!="complete"){setTimeout(arguments.callee,0);return;}if(numStyles===undefined)numStyles=jQuery("style, link[rel=stylesheet]").length;if(document.styleSheets.length!=numStyles){setTimeout(arguments.callee,0);return;}jQuery.ready();})();}jQuery.event.add(window,"load",jQuery.ready);}jQuery.each(("blur,focus,load,resize,scroll,unload,click,dblclick,"+"mousedown,mouseup,mousemove,mouseover,mouseout,change,select,"+"submit,keydown,keypress,keyup,error").split(","),function(i,name){jQuery.fn[name]=function(fn){return fn?this.bind(name,fn):this.trigger(name);};});var withinElement=function(event,elem){var parent=event.relatedTarget;while(parent&&parent!=elem)try{parent=parent.parentNode;}catch(error){parent=elem;}return parent==elem;};jQuery(window).bind("unload",function(){jQuery("*").add(document).unbind();});jQuery.fn.extend({load:function(url,params,callback){if(jQuery.isFunction(url))return this.bind("load",url);var off=url.indexOf(" ");if(off>=0){var selector=url.slice(off,url.length);url=url.slice(0,off);}callback=callback||function(){};var type="GET";if(params)if(jQuery.isFunction(params)){callback=params;params=null;}else{params=jQuery.param(params);type="POST";}var self=this;jQuery.ajax({url:url,type:type,dataType:"html",data:params,complete:function(res,status){if(status=="success"||status=="notmodified")self.html(selector?jQuery("<div/>").append(res.responseText.replace(/<script(.|\s)*?\/script>/g,"")).find(selector):res.responseText);self.each(callback,[res.responseText,status,res]);}});return this;},serialize:function(){return jQuery.param(this.serializeArray());},serializeArray:function(){return this.map(function(){return jQuery.nodeName(this,"form")?jQuery.makeArray(this.elements):this;}).filter(function(){return this.name&&!this.disabled&&(this.checked||/select|textarea/i.test(this.nodeName)||/text|hidden|password/i.test(this.type));}).map(function(i,elem){var val=jQuery(this).val();return val==null?null:val.constructor==Array?jQuery.map(val,function(val,i){return{name:elem.name,value:val};}):{name:elem.name,value:val};}).get();}});jQuery.each("ajaxStart,ajaxStop,ajaxComplete,ajaxError,ajaxSuccess,ajaxSend".split(","),function(i,o){jQuery.fn[o]=function(f){return this.bind(o,f);};});var jsc=(new Date).getTime();jQuery.extend({get:function(url,data,callback,type){if(jQuery.isFunction(data)){callback=data;data=null;}return jQuery.ajax({type:"GET",url:url,data:data,success:callback,dataType:type});},getScript:function(url,callback){return jQuery.get(url,null,callback,"script");},getJSON:function(url,data,callback){return jQuery.get(url,data,callback,"json");},post:function(url,data,callback,type){if(jQuery.isFunction(data)){callback=data;data={};}return jQuery.ajax({type:"POST",url:url,data:data,success:callback,dataType:type});},ajaxSetup:function(settings){jQuery.extend(jQuery.ajaxSettings,settings);},ajaxSettings:{global:true,type:"GET",timeout:0,contentType:"application/x-www-form-urlencoded",processData:true,async:true,data:null,username:null,password:null,accepts:{xml:"application/xml, text/xml",html:"text/html",script:"text/javascript, application/javascript",json:"application/json, text/javascript",text:"text/plain",_default:"*/*"}},lastModified:{},ajax:function(s){var jsonp,jsre=/=\?(&|$)/g,status,data;s=jQuery.extend(true,s,jQuery.extend(true,{},jQuery.ajaxSettings,s));if(s.data&&s.processData&&typeof s.data!="string")s.data=jQuery.param(s.data);if(s.dataType=="jsonp"){if(s.type.toLowerCase()=="get"){if(!s.url.match(jsre))s.url+=(s.url.match(/\?/)?"&":"?")+(s.jsonp||"callback")+"=?";}else if(!s.data||!s.data.match(jsre))s.data=(s.data?s.data+"&":"")+(s.jsonp||"callback")+"=?";s.dataType="json";}if(s.dataType=="json"&&(s.data&&s.data.match(jsre)||s.url.match(jsre))){jsonp="jsonp"+jsc++;if(s.data)s.data=(s.data+"").replace(jsre,"="+jsonp+"$1");s.url=s.url.replace(jsre,"="+jsonp+"$1");s.dataType="script";window[jsonp]=function(tmp){data=tmp;success();complete();window[jsonp]=undefined;try{delete window[jsonp];}catch(e){}if(head)head.removeChild(script);};}if(s.dataType=="script"&&s.cache==null)s.cache=false;if(s.cache===false&&s.type.toLowerCase()=="get"){var ts=(new Date()).getTime();var ret=s.url.replace(/(\?|&)_=.*?(&|$)/,"$1_="+ts+"$2");s.url=ret+((ret==s.url)?(s.url.match(/\?/)?"&":"?")+"_="+ts:"");}if(s.data&&s.type.toLowerCase()=="get"){s.url+=(s.url.match(/\?/)?"&":"?")+s.data;s.data=null;}if(s.global&&!jQuery.active++)jQuery.event.trigger("ajaxStart");if((!s.url.indexOf("http")||!s.url.indexOf("//"))&&s.dataType=="script"&&s.type.toLowerCase()=="get"){var head=document.getElementsByTagName("head")[0];var script=document.createElement("script");script.src=s.url;if(s.scriptCharset)script.charset=s.scriptCharset;if(!jsonp){var done=false;script.onload=script.onreadystatechange=function(){if(!done&&(!this.readyState||this.readyState=="loaded"||this.readyState=="complete")){done=true;success();complete();head.removeChild(script);}};}head.appendChild(script);return undefined;}var requestDone=false;var xml=window.ActiveXObject?new ActiveXObject("Microsoft.XMLHTTP"):new XMLHttpRequest();xml.open(s.type,s.url,s.async,s.username,s.password);try{if(s.data)xml.setRequestHeader("Content-Type",s.contentType);if(s.ifModified)xml.setRequestHeader("If-Modified-Since",jQuery.lastModified[s.url]||"Thu, 01 Jan 1970 00:00:00 GMT");xml.setRequestHeader("X-Requested-With","XMLHttpRequest");xml.setRequestHeader("Accept",s.dataType&&s.accepts[s.dataType]?s.accepts[s.dataType]+", */*":s.accepts._default);}catch(e){}if(s.beforeSend)s.beforeSend(xml);if(s.global)jQuery.event.trigger("ajaxSend",[xml,s]);var onreadystatechange=function(isTimeout){if(!requestDone&&xml&&(xml.readyState==4||isTimeout=="timeout")){requestDone=true;if(ival){clearInterval(ival);ival=null;}status=isTimeout=="timeout"&&"timeout"||!jQuery.httpSuccess(xml)&&"error"||s.ifModified&&jQuery.httpNotModified(xml,s.url)&&"notmodified"||"success";if(status=="success"){try{data=jQuery.httpData(xml,s.dataType);}catch(e){status="parsererror";}}if(status=="success"){var modRes;try{modRes=xml.getResponseHeader("Last-Modified");}catch(e){}if(s.ifModified&&modRes)jQuery.lastModified[s.url]=modRes;if(!jsonp)success();}else
jQuery.handleError(s,xml,status);complete();if(s.async)xml=null;}};if(s.async){var ival=setInterval(onreadystatechange,13);if(s.timeout>0)setTimeout(function(){if(xml){xml.abort();if(!requestDone)onreadystatechange("timeout");}},s.timeout);}try{xml.send(s.data);}catch(e){jQuery.handleError(s,xml,null,e);}if(!s.async)onreadystatechange();function success(){if(s.success)s.success(data,status);if(s.global)jQuery.event.trigger("ajaxSuccess",[xml,s]);}function complete(){if(s.complete)s.complete(xml,status);if(s.global)jQuery.event.trigger("ajaxComplete",[xml,s]);if(s.global&&!--jQuery.active)jQuery.event.trigger("ajaxStop");}return xml;},handleError:function(s,xml,status,e){if(s.error)s.error(xml,status,e);if(s.global)jQuery.event.trigger("ajaxError",[xml,s,e]);},active:0,httpSuccess:function(r){try{return!r.status&&location.protocol=="file:"||(r.status>=200&&r.status<300)||r.status==304||r.status==1223||jQuery.browser.safari&&r.status==undefined;}catch(e){}return false;},httpNotModified:function(xml,url){try{var xmlRes=xml.getResponseHeader("Last-Modified");return xml.status==304||xmlRes==jQuery.lastModified[url]||jQuery.browser.safari&&xml.status==undefined;}catch(e){}return false;},httpData:function(r,type){var ct=r.getResponseHeader("content-type");var xml=type=="xml"||!type&&ct&&ct.indexOf("xml")>=0;var data=xml?r.responseXML:r.responseText;if(xml&&data.documentElement.tagName=="parsererror")throw"parsererror";if(type=="script")jQuery.globalEval(data);if(type=="json")data=eval("("+data+")");return data;},param:function(a){var s=[];if(a.constructor==Array||a.jquery)jQuery.each(a,function(){s.push(encodeURIComponent(this.name)+"="+encodeURIComponent(this.value));});else
for(var j in a)if(a[j]&&a[j].constructor==Array)jQuery.each(a[j],function(){s.push(encodeURIComponent(j)+"="+encodeURIComponent(this));});else
s.push(encodeURIComponent(j)+"="+encodeURIComponent(a[j]));return s.join("&").replace(/%20/g,"+");}});jQuery.fn.extend({show:function(speed,callback){return speed?this.animate({height:"show",width:"show",opacity:"show"},speed,callback):this.filter(":hidden").each(function(){this.style.display=this.oldblock||"";if(jQuery.css(this,"display")=="none"){var elem=jQuery("<"+this.tagName+" />").appendTo("body");this.style.display=elem.css("display");if(this.style.display=="none")this.style.display="block";elem.remove();}}).end();},hide:function(speed,callback){return speed?this.animate({height:"hide",width:"hide",opacity:"hide"},speed,callback):this.filter(":visible").each(function(){this.oldblock=this.oldblock||jQuery.css(this,"display");this.style.display="none";}).end();},_toggle:jQuery.fn.toggle,toggle:function(fn,fn2){return jQuery.isFunction(fn)&&jQuery.isFunction(fn2)?this._toggle(fn,fn2):fn?this.animate({height:"toggle",width:"toggle",opacity:"toggle"},fn,fn2):this.each(function(){jQuery(this)[jQuery(this).is(":hidden")?"show":"hide"]();});},slideDown:function(speed,callback){return this.animate({height:"show"},speed,callback);},slideUp:function(speed,callback){return this.animate({height:"hide"},speed,callback);},slideToggle:function(speed,callback){return this.animate({height:"toggle"},speed,callback);},fadeIn:function(speed,callback){return this.animate({opacity:"show"},speed,callback);},fadeOut:function(speed,callback){return this.animate({opacity:"hide"},speed,callback);},fadeTo:function(speed,to,callback){return this.animate({opacity:to},speed,callback);},animate:function(prop,speed,easing,callback){var optall=jQuery.speed(speed,easing,callback);return this[optall.queue===false?"each":"queue"](function(){if(this.nodeType!=1)return false;var opt=jQuery.extend({},optall);var hidden=jQuery(this).is(":hidden"),self=this;for(var p in prop){if(prop[p]=="hide"&&hidden||prop[p]=="show"&&!hidden)return jQuery.isFunction(opt.complete)&&opt.complete.apply(this);if(p=="height"||p=="width"){opt.display=jQuery.css(this,"display");opt.overflow=this.style.overflow;}}if(opt.overflow!=null)this.style.overflow="hidden";opt.curAnim=jQuery.extend({},prop);jQuery.each(prop,function(name,val){var e=new jQuery.fx(self,opt,name);if(/toggle|show|hide/.test(val))e[val=="toggle"?hidden?"show":"hide":val](prop);else{var parts=val.toString().match(/^([+-]=)?([\d+-.]+)(.*)$/),start=e.cur(true)||0;if(parts){var end=parseFloat(parts[2]),unit=parts[3]||"px";if(unit!="px"){self.style[name]=(end||1)+unit;start=((end||1)/e.cur(true))*start;self.style[name]=start+unit;}if(parts[1])end=((parts[1]=="-="?-1:1)*end)+start;e.custom(start,end,unit);}else
e.custom(start,val,"");}});return true;});},queue:function(type,fn){if(jQuery.isFunction(type)||(type&&type.constructor==Array)){fn=type;type="fx";}if(!type||(typeof type=="string"&&!fn))return queue(this[0],type);return this.each(function(){if(fn.constructor==Array)queue(this,type,fn);else{queue(this,type).push(fn);if(queue(this,type).length==1)fn.apply(this);}});},stop:function(clearQueue,gotoEnd){var timers=jQuery.timers;if(clearQueue)this.queue([]);this.each(function(){for(var i=timers.length-1;i>=0;i--)if(timers[i].elem==this){if(gotoEnd)timers[i](true);timers.splice(i,1);}});if(!gotoEnd)this.dequeue();return this;}});var queue=function(elem,type,array){if(!elem)return undefined;type=type||"fx";var q=jQuery.data(elem,type+"queue");if(!q||array)q=jQuery.data(elem,type+"queue",array?jQuery.makeArray(array):[]);return q;};jQuery.fn.dequeue=function(type){type=type||"fx";return this.each(function(){var q=queue(this,type);q.shift();if(q.length)q[0].apply(this);});};jQuery.extend({speed:function(speed,easing,fn){var opt=speed&&speed.constructor==Object?speed:{complete:fn||!fn&&easing||jQuery.isFunction(speed)&&speed,duration:speed,easing:fn&&easing||easing&&easing.constructor!=Function&&easing};opt.duration=(opt.duration&&opt.duration.constructor==Number?opt.duration:{slow:600,fast:200}[opt.duration])||400;opt.old=opt.complete;opt.complete=function(){if(opt.queue!==false)jQuery(this).dequeue();if(jQuery.isFunction(opt.old))opt.old.apply(this);};return opt;},easing:{linear:function(p,n,firstNum,diff){return firstNum+diff*p;},swing:function(p,n,firstNum,diff){return((-Math.cos(p*Math.PI)/2)+0.5)*diff+firstNum;}},timers:[],timerId:null,fx:function(elem,options,prop){this.options=options;this.elem=elem;this.prop=prop;if(!options.orig)options.orig={};}});jQuery.fx.prototype={update:function(){if(this.options.step)this.options.step.apply(this.elem,[this.now,this]);(jQuery.fx.step[this.prop]||jQuery.fx.step._default)(this);if(this.prop=="height"||this.prop=="width")this.elem.style.display="block";},cur:function(force){if(this.elem[this.prop]!=null&&this.elem.style[this.prop]==null)return this.elem[this.prop];var r=parseFloat(jQuery.css(this.elem,this.prop,force));return r&&r>-10000?r:parseFloat(jQuery.curCSS(this.elem,this.prop))||0;},custom:function(from,to,unit){this.startTime=(new Date()).getTime();this.start=from;this.end=to;this.unit=unit||this.unit||"px";this.now=this.start;this.pos=this.state=0;this.update();var self=this;function t(gotoEnd){return self.step(gotoEnd);}t.elem=this.elem;jQuery.timers.push(t);if(jQuery.timerId==null){jQuery.timerId=setInterval(function(){var timers=jQuery.timers;for(var i=0;i<timers.length;i++)if(!timers[i]())timers.splice(i--,1);if(!timers.length){clearInterval(jQuery.timerId);jQuery.timerId=null;}},13);}},show:function(){this.options.orig[this.prop]=jQuery.attr(this.elem.style,this.prop);this.options.show=true;this.custom(0,this.cur());if(this.prop=="width"||this.prop=="height")this.elem.style[this.prop]="1px";jQuery(this.elem).show();},hide:function(){this.options.orig[this.prop]=jQuery.attr(this.elem.style,this.prop);this.options.hide=true;this.custom(this.cur(),0);},step:function(gotoEnd){var t=(new Date()).getTime();if(gotoEnd||t>this.options.duration+this.startTime){this.now=this.end;this.pos=this.state=1;this.update();this.options.curAnim[this.prop]=true;var done=true;for(var i in this.options.curAnim)if(this.options.curAnim[i]!==true)done=false;if(done){if(this.options.display!=null){this.elem.style.overflow=this.options.overflow;this.elem.style.display=this.options.display;if(jQuery.css(this.elem,"display")=="none")this.elem.style.display="block";}if(this.options.hide)this.elem.style.display="none";if(this.options.hide||this.options.show)for(var p in this.options.curAnim)jQuery.attr(this.elem.style,p,this.options.orig[p]);}if(done&&jQuery.isFunction(this.options.complete))this.options.complete.apply(this.elem);return false;}else{var n=t-this.startTime;this.state=n/this.options.duration;this.pos=jQuery.easing[this.options.easing||(jQuery.easing.swing?"swing":"linear")](this.state,n,0,1,this.options.duration);this.now=this.start+((this.end-this.start)*this.pos);this.update();}return true;}};jQuery.fx.step={scrollLeft:function(fx){fx.elem.scrollLeft=fx.now;},scrollTop:function(fx){fx.elem.scrollTop=fx.now;},opacity:function(fx){jQuery.attr(fx.elem.style,"opacity",fx.now);},_default:function(fx){fx.elem.style[fx.prop]=fx.now+fx.unit;}};jQuery.fn.offset=function(){var left=0,top=0,elem=this[0],results;if(elem)with(jQuery.browser){var parent=elem.parentNode,offsetChild=elem,offsetParent=elem.offsetParent,doc=elem.ownerDocument,safari2=safari&&parseInt(version)<522&&!/adobeair/i.test(userAgent),fixed=jQuery.css(elem,"position")=="fixed";if(elem.getBoundingClientRect){var box=elem.getBoundingClientRect();add(box.left+Math.max(doc.documentElement.scrollLeft,doc.body.scrollLeft),box.top+Math.max(doc.documentElement.scrollTop,doc.body.scrollTop));add(-doc.documentElement.clientLeft,-doc.documentElement.clientTop);}else{add(elem.offsetLeft,elem.offsetTop);while(offsetParent){add(offsetParent.offsetLeft,offsetParent.offsetTop);if(mozilla&&!/^t(able|d|h)$/i.test(offsetParent.tagName)||safari&&!safari2)border(offsetParent);if(!fixed&&jQuery.css(offsetParent,"position")=="fixed")fixed=true;offsetChild=/^body$/i.test(offsetParent.tagName)?offsetChild:offsetParent;offsetParent=offsetParent.offsetParent;}while(parent&&parent.tagName&&!/^body|html$/i.test(parent.tagName)){if(!/^inline|table.*$/i.test(jQuery.css(parent,"display")))add(-parent.scrollLeft,-parent.scrollTop);if(mozilla&&jQuery.css(parent,"overflow")!="visible")border(parent);parent=parent.parentNode;}if((safari2&&(fixed||jQuery.css(offsetChild,"position")=="absolute"))||(mozilla&&jQuery.css(offsetChild,"position")!="absolute"))add(-doc.body.offsetLeft,-doc.body.offsetTop);if(fixed)add(Math.max(doc.documentElement.scrollLeft,doc.body.scrollLeft),Math.max(doc.documentElement.scrollTop,doc.body.scrollTop));}results={top:top,left:left};}function border(elem){add(jQuery.curCSS(elem,"borderLeftWidth",true),jQuery.curCSS(elem,"borderTopWidth",true));}function add(l,t){left+=parseInt(l)||0;top+=parseInt(t)||0;}return results;};})();

/* platform.js */




SimileAjax.jQuery=jQuery.noConflict(true);
if(typeof window["$"]=="undefined"){
window.$=SimileAjax.jQuery;
}

SimileAjax.Platform.os={
isMac:false,
isWin:false,
isWin32:false,
isUnix:false
};
SimileAjax.Platform.browser={
isIE:false,
isNetscape:false,
isMozilla:false,
isFirefox:false,
isOpera:false,
isSafari:false,

majorVersion:0,
minorVersion:0
};

(function(){
var an=navigator.appName.toLowerCase();
var ua=navigator.userAgent.toLowerCase();


SimileAjax.Platform.os.isMac=(ua.indexOf('mac')!=-1);
SimileAjax.Platform.os.isWin=(ua.indexOf('win')!=-1);
SimileAjax.Platform.os.isWin32=SimileAjax.Platform.isWin&&(
ua.indexOf('95')!=-1||
ua.indexOf('98')!=-1||
ua.indexOf('nt')!=-1||
ua.indexOf('win32')!=-1||
ua.indexOf('32bit')!=-1
);
SimileAjax.Platform.os.isUnix=(ua.indexOf('x11')!=-1);


SimileAjax.Platform.browser.isIE=(an.indexOf("microsoft")!=-1);
SimileAjax.Platform.browser.isNetscape=(an.indexOf("netscape")!=-1);
SimileAjax.Platform.browser.isMozilla=(ua.indexOf("mozilla")!=-1);
SimileAjax.Platform.browser.isFirefox=(ua.indexOf("firefox")!=-1);
SimileAjax.Platform.browser.isOpera=(an.indexOf("opera")!=-1);
SimileAjax.Platform.browser.isSafari=(an.indexOf("safari")!=-1);

var parseVersionString=function(s){
var a=s.split(".");
SimileAjax.Platform.browser.majorVersion=parseInt(a[0]);
SimileAjax.Platform.browser.minorVersion=parseInt(a[1]);
};
var indexOf=function(s,sub,start){
var i=s.indexOf(sub,start);
return i>=0?i:s.length;
};

if(SimileAjax.Platform.browser.isMozilla){
var offset=ua.indexOf("mozilla/");
if(offset>=0){
parseVersionString(ua.substring(offset+8,indexOf(ua," ",offset)));
}
}
if(SimileAjax.Platform.browser.isIE){
var offset=ua.indexOf("msie ");
if(offset>=0){
parseVersionString(ua.substring(offset+5,indexOf(ua,";",offset)));
}
}
if(SimileAjax.Platform.browser.isNetscape){
var offset=ua.indexOf("rv:");
if(offset>=0){
parseVersionString(ua.substring(offset+3,indexOf(ua,")",offset)));
}
}
if(SimileAjax.Platform.browser.isFirefox){
var offset=ua.indexOf("firefox/");
if(offset>=0){
parseVersionString(ua.substring(offset+8,indexOf(ua," ",offset)));
}
}

if(!("localeCompare"in String.prototype)){
String.prototype.localeCompare=function(s){
if(this<s)return-1;
else if(this>s)return 1;
else return 0;
};
}
})();

SimileAjax.Platform.getDefaultLocale=function(){
return SimileAjax.Platform.clientLocale;
};

/* ajax.js */



SimileAjax.ListenerQueue=function(wildcardHandlerName){
this._listeners=[];
this._wildcardHandlerName=wildcardHandlerName;
};

SimileAjax.ListenerQueue.prototype.add=function(listener){
this._listeners.push(listener);
};

SimileAjax.ListenerQueue.prototype.remove=function(listener){
var listeners=this._listeners;
for(var i=0;i<listeners.length;i++){
if(listeners[i]==listener){
listeners.splice(i,1);
break;
}
}
};

SimileAjax.ListenerQueue.prototype.fire=function(handlerName,args){
var listeners=[].concat(this._listeners);
for(var i=0;i<listeners.length;i++){
var listener=listeners[i];
if(handlerName in listener){
try{
listener[handlerName].apply(listener,args);
}catch(e){
SimileAjax.Debug.exception("Error firing event of name "+handlerName,e);
}
}else if(this._wildcardHandlerName!=null&&
this._wildcardHandlerName in listener){
try{
listener[this._wildcardHandlerName].apply(listener,[handlerName]);
}catch(e){
SimileAjax.Debug.exception("Error firing event of name "+handlerName+" to wildcard handler",e);
}
}
}
};



/* data-structure.js */


SimileAjax.Set=function(a){
this._hash={};
this._count=0;

if(a instanceof Array){
for(var i=0;i<a.length;i++){
this.add(a[i]);
}
}else if(a instanceof SimileAjax.Set){
this.addSet(a);
}
}


SimileAjax.Set.prototype.add=function(o){
if(!(o in this._hash)){
this._hash[o]=true;
this._count++;
return true;
}
return false;
}


SimileAjax.Set.prototype.addSet=function(set){
for(var o in set._hash){
this.add(o);
}
}


SimileAjax.Set.prototype.remove=function(o){
if(o in this._hash){
delete this._hash[o];
this._count--;
return true;
}
return false;
}


SimileAjax.Set.prototype.removeSet=function(set){
for(var o in set._hash){
this.remove(o);
}
}


SimileAjax.Set.prototype.retainSet=function(set){
for(var o in this._hash){
if(!set.contains(o)){
delete this._hash[o];
this._count--;
}
}
}


SimileAjax.Set.prototype.contains=function(o){
return(o in this._hash);
}


SimileAjax.Set.prototype.size=function(){
return this._count;
}


SimileAjax.Set.prototype.toArray=function(){
var a=[];
for(var o in this._hash){
a.push(o);
}
return a;
}


SimileAjax.Set.prototype.visit=function(f){
for(var o in this._hash){
if(f(o)==true){
break;
}
}
}


SimileAjax.SortedArray=function(compare,initialArray){
this._a=(initialArray instanceof Array)?initialArray:[];
this._compare=compare;
};

SimileAjax.SortedArray.prototype.add=function(elmt){
var sa=this;
var index=this.find(function(elmt2){
return sa._compare(elmt2,elmt);
});

if(index<this._a.length){
this._a.splice(index,0,elmt);
}else{
this._a.push(elmt);
}
};

SimileAjax.SortedArray.prototype.remove=function(elmt){
var sa=this;
var index=this.find(function(elmt2){
return sa._compare(elmt2,elmt);
});

while(index<this._a.length&&this._compare(this._a[index],elmt)==0){
if(this._a[index]==elmt){
this._a.splice(index,1);
return true;
}else{
index++;
}
}
return false;
};

SimileAjax.SortedArray.prototype.removeAll=function(){
this._a=[];
};

SimileAjax.SortedArray.prototype.elementAt=function(index){
return this._a[index];
};

SimileAjax.SortedArray.prototype.length=function(){
return this._a.length;
};

SimileAjax.SortedArray.prototype.find=function(compare){
var a=0;
var b=this._a.length;

while(a<b){
var mid=Math.floor((a+b)/2);
var c=compare(this._a[mid]);
if(mid==a){
return c<0?a+1:a;
}else if(c<0){
a=mid;
}else{
b=mid;
}
}
return a;
};

SimileAjax.SortedArray.prototype.getFirst=function(){
return(this._a.length>0)?this._a[0]:null;
};

SimileAjax.SortedArray.prototype.getLast=function(){
return(this._a.length>0)?this._a[this._a.length-1]:null;
};



SimileAjax.EventIndex=function(unit){
var eventIndex=this;

this._unit=(unit!=null)?unit:SimileAjax.NativeDateUnit;
this._events=new SimileAjax.SortedArray(
function(event1,event2){
return eventIndex._unit.compare(event1.getStart(),event2.getStart());
}
);
this._idToEvent={};
this._indexed=true;
};

SimileAjax.EventIndex.prototype.getUnit=function(){
return this._unit;
};

SimileAjax.EventIndex.prototype.getEvent=function(id){
return this._idToEvent[id];
};

SimileAjax.EventIndex.prototype.add=function(evt){
this._events.add(evt);
this._idToEvent[evt.getID()]=evt;
this._indexed=false;
};

SimileAjax.EventIndex.prototype.removeAll=function(){
this._events.removeAll();
this._idToEvent={};
this._indexed=false;
};

SimileAjax.EventIndex.prototype.getCount=function(){
return this._events.length();
};

SimileAjax.EventIndex.prototype.getIterator=function(startDate,endDate){
if(!this._indexed){
this._index();
}
return new SimileAjax.EventIndex._Iterator(this._events,startDate,endDate,this._unit);
};

SimileAjax.EventIndex.prototype.getReverseIterator=function(startDate,endDate){
if(!this._indexed){
this._index();
}
return new SimileAjax.EventIndex._ReverseIterator(this._events,startDate,endDate,this._unit);
};

SimileAjax.EventIndex.prototype.getAllIterator=function(){
return new SimileAjax.EventIndex._AllIterator(this._events);
};

SimileAjax.EventIndex.prototype.getEarliestDate=function(){
var evt=this._events.getFirst();
return(evt==null)?null:evt.getStart();
};

SimileAjax.EventIndex.prototype.getLatestDate=function(){
var evt=this._events.getLast();
if(evt==null){
return null;
}

if(!this._indexed){
this._index();
}

var index=evt._earliestOverlapIndex;
var date=this._events.elementAt(index).getEnd();
for(var i=index+1;i<this._events.length();i++){
date=this._unit.later(date,this._events.elementAt(i).getEnd());
}

return date;
};

SimileAjax.EventIndex.prototype._index=function(){


var l=this._events.length();
for(var i=0;i<l;i++){
var evt=this._events.elementAt(i);
evt._earliestOverlapIndex=i;
}

var toIndex=1;
for(var i=0;i<l;i++){
var evt=this._events.elementAt(i);
var end=evt.getEnd();

toIndex=Math.max(toIndex,i+1);
while(toIndex<l){
var evt2=this._events.elementAt(toIndex);
var start2=evt2.getStart();

if(this._unit.compare(start2,end)<0){
evt2._earliestOverlapIndex=i;
toIndex++;
}else{
break;
}
}
}
this._indexed=true;
};

SimileAjax.EventIndex._Iterator=function(events,startDate,endDate,unit){
this._events=events;
this._startDate=startDate;
this._endDate=endDate;
this._unit=unit;

this._currentIndex=events.find(function(evt){
return unit.compare(evt.getStart(),startDate);
});
if(this._currentIndex-1>=0){
this._currentIndex=this._events.elementAt(this._currentIndex-1)._earliestOverlapIndex;
}
this._currentIndex--;

this._maxIndex=events.find(function(evt){
return unit.compare(evt.getStart(),endDate);
});

this._hasNext=false;
this._next=null;
this._findNext();
};

SimileAjax.EventIndex._Iterator.prototype={
hasNext:function(){return this._hasNext;},
next:function(){
if(this._hasNext){
var next=this._next;
this._findNext();

return next;
}else{
return null;
}
},
_findNext:function(){
var unit=this._unit;
while((++this._currentIndex)<this._maxIndex){
var evt=this._events.elementAt(this._currentIndex);
if(unit.compare(evt.getStart(),this._endDate)<0&&
unit.compare(evt.getEnd(),this._startDate)>0){

this._next=evt;
this._hasNext=true;
return;
}
}
this._next=null;
this._hasNext=false;
}
};

SimileAjax.EventIndex._ReverseIterator=function(events,startDate,endDate,unit){
this._events=events;
this._startDate=startDate;
this._endDate=endDate;
this._unit=unit;

this._minIndex=events.find(function(evt){
return unit.compare(evt.getStart(),startDate);
});
if(this._minIndex-1>=0){
this._minIndex=this._events.elementAt(this._minIndex-1)._earliestOverlapIndex;
}

this._maxIndex=events.find(function(evt){
return unit.compare(evt.getStart(),endDate);
});

this._currentIndex=this._maxIndex;
this._hasNext=false;
this._next=null;
this._findNext();
};

SimileAjax.EventIndex._ReverseIterator.prototype={
hasNext:function(){return this._hasNext;},
next:function(){
if(this._hasNext){
var next=this._next;
this._findNext();

return next;
}else{
return null;
}
},
_findNext:function(){
var unit=this._unit;
while((--this._currentIndex)>=this._minIndex){
var evt=this._events.elementAt(this._currentIndex);
if(unit.compare(evt.getStart(),this._endDate)<0&&
unit.compare(evt.getEnd(),this._startDate)>0){

this._next=evt;
this._hasNext=true;
return;
}
}
this._next=null;
this._hasNext=false;
}
};

SimileAjax.EventIndex._AllIterator=function(events){
this._events=events;
this._index=0;
};

SimileAjax.EventIndex._AllIterator.prototype={
hasNext:function(){
return this._index<this._events.length();
},
next:function(){
return this._index<this._events.length()?
this._events.elementAt(this._index++):null;
}
};

/* date-time.js */



SimileAjax.DateTime=new Object();

SimileAjax.DateTime.MILLISECOND=0;
SimileAjax.DateTime.SECOND=1;
SimileAjax.DateTime.MINUTE=2;
SimileAjax.DateTime.HOUR=3;
SimileAjax.DateTime.DAY=4;
SimileAjax.DateTime.WEEK=5;
SimileAjax.DateTime.MONTH=6;
SimileAjax.DateTime.YEAR=7;
SimileAjax.DateTime.DECADE=8;
SimileAjax.DateTime.CENTURY=9;
SimileAjax.DateTime.MILLENNIUM=10;

SimileAjax.DateTime.EPOCH=-1;
SimileAjax.DateTime.ERA=-2;


SimileAjax.DateTime.gregorianUnitLengths=[];
(function(){
var d=SimileAjax.DateTime;
var a=d.gregorianUnitLengths;

a[d.MILLISECOND]=1;
a[d.SECOND]=1000;
a[d.MINUTE]=a[d.SECOND]*60;
a[d.HOUR]=a[d.MINUTE]*60;
a[d.DAY]=a[d.HOUR]*24;
a[d.WEEK]=a[d.DAY]*7;
a[d.MONTH]=a[d.DAY]*31;
a[d.YEAR]=a[d.DAY]*365;
a[d.DECADE]=a[d.YEAR]*10;
a[d.CENTURY]=a[d.YEAR]*100;
a[d.MILLENNIUM]=a[d.YEAR]*1000;
})();

SimileAjax.DateTime._dateRegexp=new RegExp(
"^(-?)([0-9]{4})("+[
"(-?([0-9]{2})(-?([0-9]{2}))?)",
"(-?([0-9]{3}))",
"(-?W([0-9]{2})(-?([1-7]))?)"
].join("|")+")?$"
);
SimileAjax.DateTime._timezoneRegexp=new RegExp(
"Z|(([-+])([0-9]{2})(:?([0-9]{2}))?)$"
);
SimileAjax.DateTime._timeRegexp=new RegExp(
"^([0-9]{2})(:?([0-9]{2})(:?([0-9]{2})(\.([0-9]+))?)?)?$"
);


SimileAjax.DateTime.setIso8601Date=function(dateObject,string){


var d=string.match(SimileAjax.DateTime._dateRegexp);
if(!d){
throw new Error("Invalid date string: "+string);
}

var sign=(d[1]=="-")?-1:1;
var year=sign*d[2];
var month=d[5];
var date=d[7];
var dayofyear=d[9];
var week=d[11];
var dayofweek=(d[13])?d[13]:1;

dateObject.setUTCFullYear(year);
if(dayofyear){
dateObject.setUTCMonth(0);
dateObject.setUTCDate(Number(dayofyear));
}else if(week){
dateObject.setUTCMonth(0);
dateObject.setUTCDate(1);
var gd=dateObject.getUTCDay();
var day=(gd)?gd:7;
var offset=Number(dayofweek)+(7*Number(week));

if(day<=4){
dateObject.setUTCDate(offset+1-day);
}else{
dateObject.setUTCDate(offset+8-day);
}
}else{
if(month){
dateObject.setUTCDate(1);
dateObject.setUTCMonth(month-1);
}
if(date){
dateObject.setUTCDate(date);
}
}

return dateObject;
};


SimileAjax.DateTime.setIso8601Time=function(dateObject,string){


var d=string.match(SimileAjax.DateTime._timeRegexp);
if(!d){
SimileAjax.Debug.warn("Invalid time string: "+string);
return false;
}
var hours=d[1];
var mins=Number((d[3])?d[3]:0);
var secs=(d[5])?d[5]:0;
var ms=d[7]?(Number("0."+d[7])*1000):0;

dateObject.setUTCHours(hours);
dateObject.setUTCMinutes(mins);
dateObject.setUTCSeconds(secs);
dateObject.setUTCMilliseconds(ms);

return dateObject;
};


SimileAjax.DateTime.timezoneOffset=new Date().getTimezoneOffset();


SimileAjax.DateTime.setIso8601=function(dateObject,string){


var offset=null;
var comps=(string.indexOf("T")==-1)?string.split(" "):string.split("T");

SimileAjax.DateTime.setIso8601Date(dateObject,comps[0]);
if(comps.length==2){

var d=comps[1].match(SimileAjax.DateTime._timezoneRegexp);
if(d){
if(d[0]=='Z'){
offset=0;
}else{
offset=(Number(d[3])*60)+Number(d[5]);
offset*=((d[2]=='-')?1:-1);
}
comps[1]=comps[1].substr(0,comps[1].length-d[0].length);
}

SimileAjax.DateTime.setIso8601Time(dateObject,comps[1]);
}
if(offset==null){
offset=dateObject.getTimezoneOffset();
}
dateObject.setTime(dateObject.getTime()+offset*60000);

return dateObject;
};


SimileAjax.DateTime.parseIso8601DateTime=function(string){
try{
return SimileAjax.DateTime.setIso8601(new Date(0),string);
}catch(e){
return null;
}
};


SimileAjax.DateTime.parseGregorianDateTime=function(o){
if(o==null){
return null;
}else if(o instanceof Date){
return o;
}

var s=o.toString();
if(s.length>0&&s.length<8){
var space=s.indexOf(" ");
if(space>0){
var year=parseInt(s.substr(0,space));
var suffix=s.substr(space+1);
if(suffix.toLowerCase()=="bc"){
year=1-year;
}
}else{
var year=parseInt(s);
}

var d=new Date(0);
d.setUTCFullYear(year);

return d;
}

try{
return new Date(Date.parse(s));
}catch(e){
return null;
}
};


SimileAjax.DateTime.roundDownToInterval=function(date,intervalUnit,timeZone,multiple,firstDayOfWeek){
var timeShift=timeZone*
SimileAjax.DateTime.gregorianUnitLengths[SimileAjax.DateTime.HOUR];

var date2=new Date(date.getTime()+timeShift);
var clearInDay=function(d){
d.setUTCMilliseconds(0);
d.setUTCSeconds(0);
d.setUTCMinutes(0);
d.setUTCHours(0);
};
var clearInYear=function(d){
clearInDay(d);
d.setUTCDate(1);
d.setUTCMonth(0);
};

switch(intervalUnit){
case SimileAjax.DateTime.MILLISECOND:
var x=date2.getUTCMilliseconds();
date2.setUTCMilliseconds(x-(x%multiple));
break;
case SimileAjax.DateTime.SECOND:
date2.setUTCMilliseconds(0);

var x=date2.getUTCSeconds();
date2.setUTCSeconds(x-(x%multiple));
break;
case SimileAjax.DateTime.MINUTE:
date2.setUTCMilliseconds(0);
date2.setUTCSeconds(0);

var x=date2.getUTCMinutes();
date2.setTime(date2.getTime()-
(x%multiple)*SimileAjax.DateTime.gregorianUnitLengths[SimileAjax.DateTime.MINUTE]);
break;
case SimileAjax.DateTime.HOUR:
date2.setUTCMilliseconds(0);
date2.setUTCSeconds(0);
date2.setUTCMinutes(0);

var x=date2.getUTCHours();
date2.setUTCHours(x-(x%multiple));
break;
case SimileAjax.DateTime.DAY:
clearInDay(date2);
break;
case SimileAjax.DateTime.WEEK:
clearInDay(date2);
var d=(date2.getUTCDay()+7-firstDayOfWeek)%7;
date2.setTime(date2.getTime()-
d*SimileAjax.DateTime.gregorianUnitLengths[SimileAjax.DateTime.DAY]);
break;
case SimileAjax.DateTime.MONTH:
clearInDay(date2);
date2.setUTCDate(1);

var x=date2.getUTCMonth();
date2.setUTCMonth(x-(x%multiple));
break;
case SimileAjax.DateTime.YEAR:
clearInYear(date2);

var x=date2.getUTCFullYear();
date2.setUTCFullYear(x-(x%multiple));
break;
case SimileAjax.DateTime.DECADE:
clearInYear(date2);
date2.setUTCFullYear(Math.floor(date2.getUTCFullYear()/10)*10);
break;
case SimileAjax.DateTime.CENTURY:
clearInYear(date2);
date2.setUTCFullYear(Math.floor(date2.getUTCFullYear()/100)*100);
break;
case SimileAjax.DateTime.MILLENNIUM:
clearInYear(date2);
date2.setUTCFullYear(Math.floor(date2.getUTCFullYear()/1000)*1000);
break;
}

date.setTime(date2.getTime()-timeShift);
};


SimileAjax.DateTime.roundUpToInterval=function(date,intervalUnit,timeZone,multiple,firstDayOfWeek){
var originalTime=date.getTime();
SimileAjax.DateTime.roundDownToInterval(date,intervalUnit,timeZone,multiple,firstDayOfWeek);
if(date.getTime()<originalTime){
date.setTime(date.getTime()+
SimileAjax.DateTime.gregorianUnitLengths[intervalUnit]*multiple);
}
};


SimileAjax.DateTime.incrementByInterval=function(date,intervalUnit,timeZone){
timeZone=(typeof timeZone=='undefined')?0:timeZone;

var timeShift=timeZone*
SimileAjax.DateTime.gregorianUnitLengths[SimileAjax.DateTime.HOUR];

var date2=new Date(date.getTime()+timeShift);

switch(intervalUnit){
case SimileAjax.DateTime.MILLISECOND:
date2.setTime(date2.getTime()+1)
break;
case SimileAjax.DateTime.SECOND:
date2.setTime(date2.getTime()+1000);
break;
case SimileAjax.DateTime.MINUTE:
date2.setTime(date2.getTime()+
SimileAjax.DateTime.gregorianUnitLengths[SimileAjax.DateTime.MINUTE]);
break;
case SimileAjax.DateTime.HOUR:
date2.setTime(date2.getTime()+
SimileAjax.DateTime.gregorianUnitLengths[SimileAjax.DateTime.HOUR]);
break;
case SimileAjax.DateTime.DAY:
date2.setUTCDate(date2.getUTCDate()+1);
break;
case SimileAjax.DateTime.WEEK:
date2.setUTCDate(date2.getUTCDate()+7);
break;
case SimileAjax.DateTime.MONTH:
date2.setUTCMonth(date2.getUTCMonth()+1);
break;
case SimileAjax.DateTime.YEAR:
date2.setUTCFullYear(date2.getUTCFullYear()+1);
break;
case SimileAjax.DateTime.DECADE:
date2.setUTCFullYear(date2.getUTCFullYear()+10);
break;
case SimileAjax.DateTime.CENTURY:
date2.setUTCFullYear(date2.getUTCFullYear()+100);
break;
case SimileAjax.DateTime.MILLENNIUM:
date2.setUTCFullYear(date2.getUTCFullYear()+1000);
break;
}

date.setTime(date2.getTime()-timeShift);
};


SimileAjax.DateTime.removeTimeZoneOffset=function(date,timeZone){
return new Date(date.getTime()+
timeZone*SimileAjax.DateTime.gregorianUnitLengths[SimileAjax.DateTime.HOUR]);
};


SimileAjax.DateTime.getTimezone=function(){
var d=new Date().getTimezoneOffset();
return d/-60;
};


/* debug.js */



SimileAjax.Debug={
silent:false
};

SimileAjax.Debug.log=function(msg){
var f;
if("console"in window&&"log"in window.console){
f=function(msg2){
console.log(msg2);
}
}else{
f=function(msg2){
if(!SimileAjax.Debug.silent){
alert(msg2);
}
}
}
SimileAjax.Debug.log=f;
f(msg);
};

SimileAjax.Debug.warn=function(msg){
var f;
if("console"in window&&"warn"in window.console){
f=function(msg2){
console.warn(msg2);
}
}else{
f=function(msg2){
if(!SimileAjax.Debug.silent){
alert(msg2);
}
}
}
SimileAjax.Debug.warn=f;
f(msg);
};

SimileAjax.Debug.exception=function(e,msg){
var f,params=SimileAjax.parseURLParameters();
if(params.errors=="throw"||SimileAjax.params.errors=="throw"){
f=function(e2,msg2){
throw(e2);
};
}else if("console"in window&&"error"in window.console){
f=function(e2,msg2){
if(msg2!=null){
console.error(msg2+" %o",e2);
}else{
console.error(e2);
}
throw(e2);
};
}else{
f=function(e2,msg2){
if(!SimileAjax.Debug.silent){
alert("Caught exception: "+msg2+"\n\nDetails: "+("description"in e2?e2.description:e2));
}
throw(e2);
};
}
SimileAjax.Debug.exception=f;
f(e,msg);
};

SimileAjax.Debug.objectToString=function(o){
return SimileAjax.Debug._objectToString(o,"");
};

SimileAjax.Debug._objectToString=function(o,indent){
var indent2=indent+" ";
if(typeof o=="object"){
var s="{";
for(n in o){
s+=indent2+n+": "+SimileAjax.Debug._objectToString(o[n],indent2)+"\n";
}
s+=indent+"}";
return s;
}else if(typeof o=="array"){
var s="[";
for(var n=0;n<o.length;n++){
s+=SimileAjax.Debug._objectToString(o[n],indent2)+"\n";
}
s+=indent+"]";
return s;
}else{
return o;
}
};


/* dom.js */



SimileAjax.DOM=new Object();

SimileAjax.DOM.registerEventWithObject=function(elmt,eventName,obj,handlerName){
SimileAjax.DOM.registerEvent(elmt,eventName,function(elmt2,evt,target){
return obj[handlerName].call(obj,elmt2,evt,target);
});
};

SimileAjax.DOM.registerEvent=function(elmt,eventName,handler){
var handler2=function(evt){
evt=(evt)?evt:((event)?event:null);
if(evt){
var target=(evt.target)?
evt.target:((evt.srcElement)?evt.srcElement:null);
if(target){
target=(target.nodeType==1||target.nodeType==9)?
target:target.parentNode;
}

return handler(elmt,evt,target);
}
return true;
}

if(SimileAjax.Platform.browser.isIE){
elmt.attachEvent("on"+eventName,handler2);
}else{
elmt.addEventListener(eventName,handler2,false);
}
};

SimileAjax.DOM.getPageCoordinates=function(elmt){
var left=0;
var top=0;

if(elmt.nodeType!=1){
elmt=elmt.parentNode;
}

var elmt2=elmt;
while(elmt2!=null){
left+=elmt2.offsetLeft;
top+=elmt2.offsetTop;
elmt2=elmt2.offsetParent;
}

var body=document.body;
while(elmt!=null&&elmt!=body){
if("scrollLeft"in elmt){
left-=elmt.scrollLeft;
top-=elmt.scrollTop;
}
elmt=elmt.parentNode;
}

return{left:left,top:top};
};

SimileAjax.DOM.getSize=function(elmt){
var w=this.getStyle(elmt,"width");
var h=this.getStyle(elmt,"height");
if(w.indexOf("px")>-1)w=w.replace("px","");
if(h.indexOf("px")>-1)h=h.replace("px","");
return{
w:w,
h:h
}
}

SimileAjax.DOM.getStyle=function(elmt,styleProp){
if(elmt.currentStyle){
var style=elmt.currentStyle[styleProp];
}else if(window.getComputedStyle){
var style=document.defaultView.getComputedStyle(elmt,null).getPropertyValue(styleProp);
}else{
var style="";
}
return style;
}

SimileAjax.DOM.getEventRelativeCoordinates=function(evt,elmt){
if(SimileAjax.Platform.browser.isIE){
if(evt.type=="mousewheel"){
var coords=SimileAjax.DOM.getPageCoordinates(elmt);
return{
x:evt.clientX-coords.left,
y:evt.clientY-coords.top
};
}else{
return{
x:evt.offsetX,
y:evt.offsetY
};
}
}else{
var coords=SimileAjax.DOM.getPageCoordinates(elmt);

if((evt.type=="DOMMouseScroll")&&
SimileAjax.Platform.browser.isFirefox&&
(SimileAjax.Platform.browser.majorVersion==2)){


return{
x:evt.screenX-coords.left,
y:evt.screenY-coords.top
};
}else{
return{
x:evt.pageX-coords.left,
y:evt.pageY-coords.top
};
}
}
};

SimileAjax.DOM.getEventPageCoordinates=function(evt){
if(SimileAjax.Platform.browser.isIE){
return{
x:evt.clientX+document.body.scrollLeft,
y:evt.clientY+document.body.scrollTop
};
}else{
return{
x:evt.pageX,
y:evt.pageY
};
}
};

SimileAjax.DOM.hittest=function(x,y,except){
return SimileAjax.DOM._hittest(document.body,x,y,except);
};

SimileAjax.DOM._hittest=function(elmt,x,y,except){
var childNodes=elmt.childNodes;
outer:for(var i=0;i<childNodes.length;i++){
var childNode=childNodes[i];
for(var j=0;j<except.length;j++){
if(childNode==except[j]){
continue outer;
}
}

if(childNode.offsetWidth==0&&childNode.offsetHeight==0){

var hitNode=SimileAjax.DOM._hittest(childNode,x,y,except);
if(hitNode!=childNode){
return hitNode;
}
}else{
var top=0;
var left=0;

var node=childNode;
while(node){
top+=node.offsetTop;
left+=node.offsetLeft;
node=node.offsetParent;
}

if(left<=x&&top<=y&&(x-left)<childNode.offsetWidth&&(y-top)<childNode.offsetHeight){
return SimileAjax.DOM._hittest(childNode,x,y,except);
}else if(childNode.nodeType==1&&childNode.tagName=="TR"){

var childNode2=SimileAjax.DOM._hittest(childNode,x,y,except);
if(childNode2!=childNode){
return childNode2;
}
}
}
}
return elmt;
};

SimileAjax.DOM.cancelEvent=function(evt){
evt.returnValue=false;
evt.cancelBubble=true;
if("preventDefault"in evt){
evt.preventDefault();
}
};

SimileAjax.DOM.appendClassName=function(elmt,className){
var classes=elmt.className.split(" ");
for(var i=0;i<classes.length;i++){
if(classes[i]==className){
return;
}
}
classes.push(className);
elmt.className=classes.join(" ");
};

SimileAjax.DOM.createInputElement=function(type){
var div=document.createElement("div");
div.innerHTML="<input type='"+type+"' />";

return div.firstChild;
};

SimileAjax.DOM.createDOMFromTemplate=function(template){
var result={};
result.elmt=SimileAjax.DOM._createDOMFromTemplate(template,result,null);

return result;
};

SimileAjax.DOM._createDOMFromTemplate=function(templateNode,result,parentElmt){
if(templateNode==null){

return null;
}else if(typeof templateNode!="object"){
var node=document.createTextNode(templateNode);
if(parentElmt!=null){
parentElmt.appendChild(node);
}
return node;
}else{
var elmt=null;
if("tag"in templateNode){
var tag=templateNode.tag;
if(parentElmt!=null){
if(tag=="tr"){
elmt=parentElmt.insertRow(parentElmt.rows.length);
}else if(tag=="td"){
elmt=parentElmt.insertCell(parentElmt.cells.length);
}
}
if(elmt==null){
elmt=tag=="input"?
SimileAjax.DOM.createInputElement(templateNode.type):
document.createElement(tag);

if(parentElmt!=null){
parentElmt.appendChild(elmt);
}
}
}else{
elmt=templateNode.elmt;
if(parentElmt!=null){
parentElmt.appendChild(elmt);
}
}

for(var attribute in templateNode){
var value=templateNode[attribute];

if(attribute=="field"){
result[value]=elmt;

}else if(attribute=="className"){
elmt.className=value;
}else if(attribute=="id"){
elmt.id=value;
}else if(attribute=="title"){
elmt.title=value;
}else if(attribute=="type"&&elmt.tagName=="input"){

}else if(attribute=="style"){
for(n in value){
var v=value[n];
if(n=="float"){
n=SimileAjax.Platform.browser.isIE?"styleFloat":"cssFloat";
}
elmt.style[n]=v;
}
}else if(attribute=="children"){
for(var i=0;i<value.length;i++){
SimileAjax.DOM._createDOMFromTemplate(value[i],result,elmt);
}
}else if(attribute!="tag"&&attribute!="elmt"){
elmt.setAttribute(attribute,value);
}
}
return elmt;
}
}

SimileAjax.DOM._cachedParent=null;
SimileAjax.DOM.createElementFromString=function(s){
if(SimileAjax.DOM._cachedParent==null){
SimileAjax.DOM._cachedParent=document.createElement("div");
}
SimileAjax.DOM._cachedParent.innerHTML=s;
return SimileAjax.DOM._cachedParent.firstChild;
};

SimileAjax.DOM.createDOMFromString=function(root,s,fieldElmts){
var elmt=typeof root=="string"?document.createElement(root):root;
elmt.innerHTML=s;

var dom={elmt:elmt};
SimileAjax.DOM._processDOMChildrenConstructedFromString(dom,elmt,fieldElmts!=null?fieldElmts:{});

return dom;
};

SimileAjax.DOM._processDOMConstructedFromString=function(dom,elmt,fieldElmts){
var id=elmt.id;
if(id!=null&&id.length>0){
elmt.removeAttribute("id");
if(id in fieldElmts){
var parentElmt=elmt.parentNode;
parentElmt.insertBefore(fieldElmts[id],elmt);
parentElmt.removeChild(elmt);

dom[id]=fieldElmts[id];
return;
}else{
dom[id]=elmt;
}
}

if(elmt.hasChildNodes()){
SimileAjax.DOM._processDOMChildrenConstructedFromString(dom,elmt,fieldElmts);
}
};

SimileAjax.DOM._processDOMChildrenConstructedFromString=function(dom,elmt,fieldElmts){
var node=elmt.firstChild;
while(node!=null){
var node2=node.nextSibling;
if(node.nodeType==1){
SimileAjax.DOM._processDOMConstructedFromString(dom,node,fieldElmts);
}
node=node2;
}
};


/* graphics.js */



SimileAjax.Graphics=new Object();


SimileAjax.Graphics.pngIsTranslucent=(!SimileAjax.Platform.browser.isIE)||(SimileAjax.Platform.browser.majorVersion>6);


SimileAjax.Graphics._createTranslucentImage1=function(url,verticalAlign){
var elmt=document.createElement("img");
elmt.setAttribute("src",url);
if(verticalAlign!=null){
elmt.style.verticalAlign=verticalAlign;
}
return elmt;
};
SimileAjax.Graphics._createTranslucentImage2=function(url,verticalAlign){
var elmt=document.createElement("img");
elmt.style.width="1px";
elmt.style.height="1px";
elmt.style.filter="progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"+url+"', sizingMethod='image')";
elmt.style.verticalAlign=(verticalAlign!=null)?verticalAlign:"middle";
return elmt;
};


SimileAjax.Graphics.createTranslucentImage=SimileAjax.Graphics.pngIsTranslucent?
SimileAjax.Graphics._createTranslucentImage1:
SimileAjax.Graphics._createTranslucentImage2;

SimileAjax.Graphics._createTranslucentImageHTML1=function(url,verticalAlign){
return"<img src=\""+url+"\""+
(verticalAlign!=null?" style=\"vertical-align: "+verticalAlign+";\"":"")+
" />";
};
SimileAjax.Graphics._createTranslucentImageHTML2=function(url,verticalAlign){
var style=
"width: 1px; height: 1px; "+
"filter:progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"+url+"', sizingMethod='image');"+
(verticalAlign!=null?" vertical-align: "+verticalAlign+";":"");

return"<img src='"+url+"' style=\""+style+"\" />";
};


SimileAjax.Graphics.createTranslucentImageHTML=SimileAjax.Graphics.pngIsTranslucent?
SimileAjax.Graphics._createTranslucentImageHTML1:
SimileAjax.Graphics._createTranslucentImageHTML2;


SimileAjax.Graphics.setOpacity=function(elmt,opacity){
if(SimileAjax.Platform.browser.isIE){
elmt.style.filter="progid:DXImageTransform.Microsoft.Alpha(Style=0,Opacity="+opacity+")";
}else{
var o=(opacity/100).toString();
elmt.style.opacity=o;
elmt.style.MozOpacity=o;
}
};


SimileAjax.Graphics._bubbleMargins={
top:33,
bottom:42,
left:33,
right:40
}


SimileAjax.Graphics._arrowOffsets={
top:0,
bottom:9,
left:1,
right:8
}

SimileAjax.Graphics._bubblePadding=15;
SimileAjax.Graphics._bubblePointOffset=6;
SimileAjax.Graphics._halfArrowWidth=18;


SimileAjax.Graphics.createBubbleForContentAndPoint=function(div,pageX,pageY,contentWidth,orientation){
if(typeof contentWidth!="number"){
contentWidth=300;
}

div.style.position="absolute";
div.style.left="-5000px";
div.style.top="0px";
div.style.width=contentWidth+"px";
document.body.appendChild(div);

window.setTimeout(function(){
var width=div.scrollWidth+10;
var height=div.scrollHeight+10;

var bubble=SimileAjax.Graphics.createBubbleForPoint(pageX,pageY,width,height,orientation);

document.body.removeChild(div);
div.style.position="static";
div.style.left="";
div.style.top="";
div.style.width=width+"px";
bubble.content.appendChild(div);
},200);
};


SimileAjax.Graphics.createBubbleForPoint=function(pageX,pageY,contentWidth,contentHeight,orientation){
function getWindowDims(){
if(typeof window.innerHeight=='number'){
return{w:window.innerWidth,h:window.innerHeight};
}else if(document.documentElement&&document.documentElement.clientHeight){
return{
w:document.documentElement.clientWidth,
h:document.documentElement.clientHeight
};
}else if(document.body&&document.body.clientHeight){
return{
w:document.body.clientWidth,
h:document.body.clientHeight
};
}
}

var close=function(){
if(!bubble._closed){
document.body.removeChild(bubble._div);
bubble._doc=null;
bubble._div=null;
bubble._content=null;
bubble._closed=true;
}
}
var bubble={
_closed:false
};

var dims=getWindowDims();
var docWidth=dims.w;
var docHeight=dims.h;

var margins=SimileAjax.Graphics._bubbleMargins;
contentWidth=parseInt(contentWidth,10);
contentHeight=parseInt(contentHeight,10);
var bubbleWidth=margins.left+contentWidth+margins.right;
var bubbleHeight=margins.top+contentHeight+margins.bottom;

var pngIsTranslucent=SimileAjax.Graphics.pngIsTranslucent;
var urlPrefix=SimileAjax.urlPrefix;

var setImg=function(elmt,url,width,height){
elmt.style.position="absolute";
elmt.style.width=width+"px";
elmt.style.height=height+"px";
if(pngIsTranslucent){
elmt.style.background="url("+url+")";
}else{
elmt.style.filter="progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"+url+"', sizingMethod='crop')";
}
}
var div=document.createElement("div");
div.style.width=bubbleWidth+"px";
div.style.height=bubbleHeight+"px";
div.style.position="absolute";
div.style.zIndex=1000;

var layer=SimileAjax.WindowManager.pushLayer(close,true,div);
bubble._div=div;
bubble.close=function(){SimileAjax.WindowManager.popLayer(layer);}

var divInner=document.createElement("div");
divInner.style.width="100%";
divInner.style.height="100%";
divInner.style.position="relative";
div.appendChild(divInner);

var createImg=function(url,left,top,width,height){
var divImg=document.createElement("div");
divImg.style.left=left+"px";
divImg.style.top=top+"px";
setImg(divImg,url,width,height);
divInner.appendChild(divImg);
}

createImg(urlPrefix+"images/bubble-top-left.png",0,0,margins.left,margins.top);
createImg(urlPrefix+"images/bubble-top.png",margins.left,0,contentWidth,margins.top);
createImg(urlPrefix+"images/bubble-top-right.png",margins.left+contentWidth,0,margins.right,margins.top);

createImg(urlPrefix+"images/bubble-left.png",0,margins.top,margins.left,contentHeight);
createImg(urlPrefix+"images/bubble-right.png",margins.left+contentWidth,margins.top,margins.right,contentHeight);

createImg(urlPrefix+"images/bubble-bottom-left.png",0,margins.top+contentHeight,margins.left,margins.bottom);
createImg(urlPrefix+"images/bubble-bottom.png",margins.left,margins.top+contentHeight,contentWidth,margins.bottom);
createImg(urlPrefix+"images/bubble-bottom-right.png",margins.left+contentWidth,margins.top+contentHeight,margins.right,margins.bottom);

var divClose=document.createElement("div");
divClose.style.left=(bubbleWidth-margins.right+SimileAjax.Graphics._bubblePadding-16-2)+"px";
divClose.style.top=(margins.top-SimileAjax.Graphics._bubblePadding+1)+"px";
divClose.style.cursor="pointer";
setImg(divClose,urlPrefix+"images/close-button.png",16,16);
SimileAjax.WindowManager.registerEventWithObject(divClose,"click",bubble,"close");
divInner.appendChild(divClose);

var divContent=document.createElement("div");
divContent.style.position="absolute";
divContent.style.left=margins.left+"px";
divContent.style.top=margins.top+"px";
divContent.style.width=contentWidth+"px";
divContent.style.height=contentHeight+"px";
divContent.style.overflow="auto";
divContent.style.background="white";
divInner.appendChild(divContent);
bubble.content=divContent;

(function(){
if(pageX-SimileAjax.Graphics._halfArrowWidth-SimileAjax.Graphics._bubblePadding>0&&
pageX+SimileAjax.Graphics._halfArrowWidth+SimileAjax.Graphics._bubblePadding<docWidth){

var left=pageX-Math.round(contentWidth/2)-margins.left;
left=pageX<(docWidth/2)?
Math.max(left,-(margins.left-SimileAjax.Graphics._bubblePadding)):
Math.min(left,docWidth+(margins.right-SimileAjax.Graphics._bubblePadding)-bubbleWidth);

if((orientation&&orientation=="top")||(!orientation&&(pageY-SimileAjax.Graphics._bubblePointOffset-bubbleHeight>0))){
var divImg=document.createElement("div");

divImg.style.left=(pageX-SimileAjax.Graphics._halfArrowWidth-left)+"px";
divImg.style.top=(margins.top+contentHeight)+"px";
setImg(divImg,urlPrefix+"images/bubble-bottom-arrow.png",37,margins.bottom);
divInner.appendChild(divImg);

div.style.left=left+"px";
div.style.top=(pageY-SimileAjax.Graphics._bubblePointOffset-bubbleHeight+
SimileAjax.Graphics._arrowOffsets.bottom)+"px";

return;
}else if((orientation&&orientation=="bottom")||(!orientation&&(pageY+SimileAjax.Graphics._bubblePointOffset+bubbleHeight<docHeight))){
var divImg=document.createElement("div");

divImg.style.left=(pageX-SimileAjax.Graphics._halfArrowWidth-left)+"px";
divImg.style.top="0px";
setImg(divImg,urlPrefix+"images/bubble-top-arrow.png",37,margins.top);
divInner.appendChild(divImg);

div.style.left=left+"px";
div.style.top=(pageY+SimileAjax.Graphics._bubblePointOffset-
SimileAjax.Graphics._arrowOffsets.top)+"px";

return;
}
}

var top=pageY-Math.round(contentHeight/2)-margins.top;
top=pageY<(docHeight/2)?
Math.max(top,-(margins.top-SimileAjax.Graphics._bubblePadding)):
Math.min(top,docHeight+(margins.bottom-SimileAjax.Graphics._bubblePadding)-bubbleHeight);

if((orientation&&orientation=="left")||(!orientation&&(pageX-SimileAjax.Graphics._bubblePointOffset-bubbleWidth>0))){
var divImg=document.createElement("div");

divImg.style.left=(margins.left+contentWidth)+"px";
divImg.style.top=(pageY-SimileAjax.Graphics._halfArrowWidth-top)+"px";
setImg(divImg,urlPrefix+"images/bubble-right-arrow.png",margins.right,37);
divInner.appendChild(divImg);

div.style.left=(pageX-SimileAjax.Graphics._bubblePointOffset-bubbleWidth+
SimileAjax.Graphics._arrowOffsets.right)+"px";
div.style.top=top+"px";
}else if((orientation&&orientation=="right")||(!orientation&&(pageX-SimileAjax.Graphics._bubblePointOffset-bubbleWidth<docWidth))){
var divImg=document.createElement("div");

divImg.style.left="0px";
divImg.style.top=(pageY-SimileAjax.Graphics._halfArrowWidth-top)+"px";
setImg(divImg,urlPrefix+"images/bubble-left-arrow.png",margins.left,37);
divInner.appendChild(divImg);

div.style.left=(pageX+SimileAjax.Graphics._bubblePointOffset-
SimileAjax.Graphics._arrowOffsets.left)+"px";
div.style.top=top+"px";
}
})();

document.body.appendChild(div);

return bubble;
};


SimileAjax.Graphics.createMessageBubble=function(doc){
var containerDiv=doc.createElement("div");
if(SimileAjax.Graphics.pngIsTranslucent){
var topDiv=doc.createElement("div");
topDiv.style.height="33px";
topDiv.style.background="url("+SimileAjax.urlPrefix+"images/message-top-left.png) top left no-repeat";
topDiv.style.paddingLeft="44px";
containerDiv.appendChild(topDiv);

var topRightDiv=doc.createElement("div");
topRightDiv.style.height="33px";
topRightDiv.style.background="url("+SimileAjax.urlPrefix+"images/message-top-right.png) top right no-repeat";
topDiv.appendChild(topRightDiv);

var middleDiv=doc.createElement("div");
middleDiv.style.background="url("+SimileAjax.urlPrefix+"images/message-left.png) top left repeat-y";
middleDiv.style.paddingLeft="44px";
containerDiv.appendChild(middleDiv);

var middleRightDiv=doc.createElement("div");
middleRightDiv.style.background="url("+SimileAjax.urlPrefix+"images/message-right.png) top right repeat-y";
middleRightDiv.style.paddingRight="44px";
middleDiv.appendChild(middleRightDiv);

var contentDiv=doc.createElement("div");
middleRightDiv.appendChild(contentDiv);

var bottomDiv=doc.createElement("div");
bottomDiv.style.height="55px";
bottomDiv.style.background="url("+SimileAjax.urlPrefix+"images/message-bottom-left.png) bottom left no-repeat";
bottomDiv.style.paddingLeft="44px";
containerDiv.appendChild(bottomDiv);

var bottomRightDiv=doc.createElement("div");
bottomRightDiv.style.height="55px";
bottomRightDiv.style.background="url("+SimileAjax.urlPrefix+"images/message-bottom-right.png) bottom right no-repeat";
bottomDiv.appendChild(bottomRightDiv);
}else{
containerDiv.style.border="2px solid #7777AA";
containerDiv.style.padding="20px";
containerDiv.style.background="white";
SimileAjax.Graphics.setOpacity(containerDiv,90);

var contentDiv=doc.createElement("div");
containerDiv.appendChild(contentDiv);
}

return{
containerDiv:containerDiv,
contentDiv:contentDiv
};
};




SimileAjax.Graphics.createAnimation=function(f,from,to,duration,cont){
return new SimileAjax.Graphics._Animation(f,from,to,duration,cont);
};

SimileAjax.Graphics._Animation=function(f,from,to,duration,cont){
this.f=f;
this.cont=(typeof cont=="function")?cont:function(){};

this.from=from;
this.to=to;
this.current=from;

this.duration=duration;
this.start=new Date().getTime();
this.timePassed=0;
};


SimileAjax.Graphics._Animation.prototype.run=function(){
var a=this;
window.setTimeout(function(){a.step();},50);
};


SimileAjax.Graphics._Animation.prototype.step=function(){
this.timePassed+=50;

var timePassedFraction=this.timePassed/this.duration;
var parameterFraction=-Math.cos(timePassedFraction*Math.PI)/2+0.5;
var current=parameterFraction*(this.to-this.from)+this.from;

try{
this.f(current,current-this.current);
}catch(e){
}
this.current=current;

if(this.timePassed<this.duration){
this.run();
}else{
this.f(this.to,0);
this["cont"]();
}
};




SimileAjax.Graphics.createStructuredDataCopyButton=function(image,width,height,createDataFunction){
var div=document.createElement("div");
div.style.position="relative";
div.style.display="inline";
div.style.width=width+"px";
div.style.height=height+"px";
div.style.overflow="hidden";
div.style.margin="2px";

if(SimileAjax.Graphics.pngIsTranslucent){
div.style.background="url("+image+") no-repeat";
}else{
div.style.filter="progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"+image+"', sizingMethod='image')";
}

var style;
if(SimileAjax.Platform.browser.isIE){
style="filter:alpha(opacity=0)";
}else{
style="opacity: 0";
}
div.innerHTML="<textarea rows='1' autocomplete='off' value='none' style='"+style+"' />";

var textarea=div.firstChild;
textarea.style.width=width+"px";
textarea.style.height=height+"px";
textarea.onmousedown=function(evt){
evt=(evt)?evt:((event)?event:null);
if(evt.button==2){
textarea.value=createDataFunction();
textarea.select();
}
};

return div;
};

SimileAjax.Graphics.getFontRenderingContext=function(elmt,width){
return new SimileAjax.Graphics._FontRenderingContext(elmt,width);
};

SimileAjax.Graphics._FontRenderingContext=function(elmt,width){
this._elmt=elmt;
this._elmt.style.visibility="hidden";
if(typeof width=="string"){
this._elmt.style.width=width;
}else if(typeof width=="number"){
this._elmt.style.width=width+"px";
}
};

SimileAjax.Graphics._FontRenderingContext.prototype.dispose=function(){
this._elmt=null;
};

SimileAjax.Graphics._FontRenderingContext.prototype.update=function(){
this._elmt.innerHTML="A";
this._lineHeight=this._elmt.offsetHeight;
};

SimileAjax.Graphics._FontRenderingContext.prototype.computeSize=function(text){
this._elmt.innerHTML=text;
return{
width:this._elmt.offsetWidth,
height:this._elmt.offsetHeight
};
};

SimileAjax.Graphics._FontRenderingContext.prototype.getLineHeight=function(){
return this._lineHeight;
};


/* history.js */



SimileAjax.History={
maxHistoryLength:10,
historyFile:"__history__.html",
enabled:true,

_initialized:false,
_listeners:new SimileAjax.ListenerQueue(),

_actions:[],
_baseIndex:0,
_currentIndex:0,

_plainDocumentTitle:document.title
};

SimileAjax.History.formatHistoryEntryTitle=function(actionLabel){
return SimileAjax.History._plainDocumentTitle+" {"+actionLabel+"}";
};

SimileAjax.History.initialize=function(){
if(SimileAjax.History._initialized){
return;
}

if(SimileAjax.History.enabled){
var iframe=document.createElement("iframe");
iframe.id="simile-ajax-history";
iframe.style.position="absolute";
iframe.style.width="10px";
iframe.style.height="10px";
iframe.style.top="0px";
iframe.style.left="0px";
iframe.style.visibility="hidden";
iframe.src=SimileAjax.History.historyFile+"?0";

document.body.appendChild(iframe);
SimileAjax.DOM.registerEvent(iframe,"load",SimileAjax.History._handleIFrameOnLoad);

SimileAjax.History._iframe=iframe;
}
SimileAjax.History._initialized=true;
};

SimileAjax.History.addListener=function(listener){
SimileAjax.History.initialize();

SimileAjax.History._listeners.add(listener);
};

SimileAjax.History.removeListener=function(listener){
SimileAjax.History.initialize();

SimileAjax.History._listeners.remove(listener);
};

SimileAjax.History.addAction=function(action){
SimileAjax.History.initialize();

SimileAjax.History._listeners.fire("onBeforePerform",[action]);
window.setTimeout(function(){
try{
action.perform();
SimileAjax.History._listeners.fire("onAfterPerform",[action]);

if(SimileAjax.History.enabled){
SimileAjax.History._actions=SimileAjax.History._actions.slice(
0,SimileAjax.History._currentIndex-SimileAjax.History._baseIndex);

SimileAjax.History._actions.push(action);
SimileAjax.History._currentIndex++;

var diff=SimileAjax.History._actions.length-SimileAjax.History.maxHistoryLength;
if(diff>0){
SimileAjax.History._actions=SimileAjax.History._actions.slice(diff);
SimileAjax.History._baseIndex+=diff;
}

try{
SimileAjax.History._iframe.contentWindow.location.search=
"?"+SimileAjax.History._currentIndex;
}catch(e){

var title=SimileAjax.History.formatHistoryEntryTitle(action.label);
document.title=title;
}
}
}catch(e){
SimileAjax.Debug.exception(e,"Error adding action {"+action.label+"} to history");
}
},0);
};

SimileAjax.History.addLengthyAction=function(perform,undo,label){
SimileAjax.History.addAction({
perform:perform,
undo:undo,
label:label,
uiLayer:SimileAjax.WindowManager.getBaseLayer(),
lengthy:true
});
};

SimileAjax.History._handleIFrameOnLoad=function(){


try{
var q=SimileAjax.History._iframe.contentWindow.location.search;
var c=(q.length==0)?0:Math.max(0,parseInt(q.substr(1)));

var finishUp=function(){
var diff=c-SimileAjax.History._currentIndex;
SimileAjax.History._currentIndex+=diff;
SimileAjax.History._baseIndex+=diff;

SimileAjax.History._iframe.contentWindow.location.search="?"+c;
};

if(c<SimileAjax.History._currentIndex){
SimileAjax.History._listeners.fire("onBeforeUndoSeveral",[]);
window.setTimeout(function(){
while(SimileAjax.History._currentIndex>c&&
SimileAjax.History._currentIndex>SimileAjax.History._baseIndex){

SimileAjax.History._currentIndex--;

var action=SimileAjax.History._actions[SimileAjax.History._currentIndex-SimileAjax.History._baseIndex];

try{
action.undo();
}catch(e){
SimileAjax.Debug.exception(e,"History: Failed to undo action {"+action.label+"}");
}
}

SimileAjax.History._listeners.fire("onAfterUndoSeveral",[]);
finishUp();
},0);
}else if(c>SimileAjax.History._currentIndex){
SimileAjax.History._listeners.fire("onBeforeRedoSeveral",[]);
window.setTimeout(function(){
while(SimileAjax.History._currentIndex<c&&
SimileAjax.History._currentIndex-SimileAjax.History._baseIndex<SimileAjax.History._actions.length){

var action=SimileAjax.History._actions[SimileAjax.History._currentIndex-SimileAjax.History._baseIndex];

try{
action.perform();
}catch(e){
SimileAjax.Debug.exception(e,"History: Failed to redo action {"+action.label+"}");
}

SimileAjax.History._currentIndex++;
}

SimileAjax.History._listeners.fire("onAfterRedoSeveral",[]);
finishUp();
},0);
}else{
var index=SimileAjax.History._currentIndex-SimileAjax.History._baseIndex-1;
var title=(index>=0&&index<SimileAjax.History._actions.length)?
SimileAjax.History.formatHistoryEntryTitle(SimileAjax.History._actions[index].label):
SimileAjax.History._plainDocumentTitle;

SimileAjax.History._iframe.contentWindow.document.title=title;
document.title=title;
}
}catch(e){

}
};

SimileAjax.History.getNextUndoAction=function(){
try{
var index=SimileAjax.History._currentIndex-SimileAjax.History._baseIndex-1;
return SimileAjax.History._actions[index];
}catch(e){
return null;
}
};

SimileAjax.History.getNextRedoAction=function(){
try{
var index=SimileAjax.History._currentIndex-SimileAjax.History._baseIndex;
return SimileAjax.History._actions[index];
}catch(e){
return null;
}
};


/* html.js */



SimileAjax.HTML=new Object();

SimileAjax.HTML._e2uHash={};
(function(){
var e2uHash=SimileAjax.HTML._e2uHash;
e2uHash['nbsp']='\u00A0[space]';
e2uHash['iexcl']='\u00A1';
e2uHash['cent']='\u00A2';
e2uHash['pound']='\u00A3';
e2uHash['curren']='\u00A4';
e2uHash['yen']='\u00A5';
e2uHash['brvbar']='\u00A6';
e2uHash['sect']='\u00A7';
e2uHash['uml']='\u00A8';
e2uHash['copy']='\u00A9';
e2uHash['ordf']='\u00AA';
e2uHash['laquo']='\u00AB';
e2uHash['not']='\u00AC';
e2uHash['shy']='\u00AD';
e2uHash['reg']='\u00AE';
e2uHash['macr']='\u00AF';
e2uHash['deg']='\u00B0';
e2uHash['plusmn']='\u00B1';
e2uHash['sup2']='\u00B2';
e2uHash['sup3']='\u00B3';
e2uHash['acute']='\u00B4';
e2uHash['micro']='\u00B5';
e2uHash['para']='\u00B6';
e2uHash['middot']='\u00B7';
e2uHash['cedil']='\u00B8';
e2uHash['sup1']='\u00B9';
e2uHash['ordm']='\u00BA';
e2uHash['raquo']='\u00BB';
e2uHash['frac14']='\u00BC';
e2uHash['frac12']='\u00BD';
e2uHash['frac34']='\u00BE';
e2uHash['iquest']='\u00BF';
e2uHash['Agrave']='\u00C0';
e2uHash['Aacute']='\u00C1';
e2uHash['Acirc']='\u00C2';
e2uHash['Atilde']='\u00C3';
e2uHash['Auml']='\u00C4';
e2uHash['Aring']='\u00C5';
e2uHash['AElig']='\u00C6';
e2uHash['Ccedil']='\u00C7';
e2uHash['Egrave']='\u00C8';
e2uHash['Eacute']='\u00C9';
e2uHash['Ecirc']='\u00CA';
e2uHash['Euml']='\u00CB';
e2uHash['Igrave']='\u00CC';
e2uHash['Iacute']='\u00CD';
e2uHash['Icirc']='\u00CE';
e2uHash['Iuml']='\u00CF';
e2uHash['ETH']='\u00D0';
e2uHash['Ntilde']='\u00D1';
e2uHash['Ograve']='\u00D2';
e2uHash['Oacute']='\u00D3';
e2uHash['Ocirc']='\u00D4';
e2uHash['Otilde']='\u00D5';
e2uHash['Ouml']='\u00D6';
e2uHash['times']='\u00D7';
e2uHash['Oslash']='\u00D8';
e2uHash['Ugrave']='\u00D9';
e2uHash['Uacute']='\u00DA';
e2uHash['Ucirc']='\u00DB';
e2uHash['Uuml']='\u00DC';
e2uHash['Yacute']='\u00DD';
e2uHash['THORN']='\u00DE';
e2uHash['szlig']='\u00DF';
e2uHash['agrave']='\u00E0';
e2uHash['aacute']='\u00E1';
e2uHash['acirc']='\u00E2';
e2uHash['atilde']='\u00E3';
e2uHash['auml']='\u00E4';
e2uHash['aring']='\u00E5';
e2uHash['aelig']='\u00E6';
e2uHash['ccedil']='\u00E7';
e2uHash['egrave']='\u00E8';
e2uHash['eacute']='\u00E9';
e2uHash['ecirc']='\u00EA';
e2uHash['euml']='\u00EB';
e2uHash['igrave']='\u00EC';
e2uHash['iacute']='\u00ED';
e2uHash['icirc']='\u00EE';
e2uHash['iuml']='\u00EF';
e2uHash['eth']='\u00F0';
e2uHash['ntilde']='\u00F1';
e2uHash['ograve']='\u00F2';
e2uHash['oacute']='\u00F3';
e2uHash['ocirc']='\u00F4';
e2uHash['otilde']='\u00F5';
e2uHash['ouml']='\u00F6';
e2uHash['divide']='\u00F7';
e2uHash['oslash']='\u00F8';
e2uHash['ugrave']='\u00F9';
e2uHash['uacute']='\u00FA';
e2uHash['ucirc']='\u00FB';
e2uHash['uuml']='\u00FC';
e2uHash['yacute']='\u00FD';
e2uHash['thorn']='\u00FE';
e2uHash['yuml']='\u00FF';
e2uHash['quot']='\u0022';
e2uHash['amp']='\u0026';
e2uHash['lt']='\u003C';
e2uHash['gt']='\u003E';
e2uHash['OElig']='';
e2uHash['oelig']='\u0153';
e2uHash['Scaron']='\u0160';
e2uHash['scaron']='\u0161';
e2uHash['Yuml']='\u0178';
e2uHash['circ']='\u02C6';
e2uHash['tilde']='\u02DC';
e2uHash['ensp']='\u2002';
e2uHash['emsp']='\u2003';
e2uHash['thinsp']='\u2009';
e2uHash['zwnj']='\u200C';
e2uHash['zwj']='\u200D';
e2uHash['lrm']='\u200E';
e2uHash['rlm']='\u200F';
e2uHash['ndash']='\u2013';
e2uHash['mdash']='\u2014';
e2uHash['lsquo']='\u2018';
e2uHash['rsquo']='\u2019';
e2uHash['sbquo']='\u201A';
e2uHash['ldquo']='\u201C';
e2uHash['rdquo']='\u201D';
e2uHash['bdquo']='\u201E';
e2uHash['dagger']='\u2020';
e2uHash['Dagger']='\u2021';
e2uHash['permil']='\u2030';
e2uHash['lsaquo']='\u2039';
e2uHash['rsaquo']='\u203A';
e2uHash['euro']='\u20AC';
e2uHash['fnof']='\u0192';
e2uHash['Alpha']='\u0391';
e2uHash['Beta']='\u0392';
e2uHash['Gamma']='\u0393';
e2uHash['Delta']='\u0394';
e2uHash['Epsilon']='\u0395';
e2uHash['Zeta']='\u0396';
e2uHash['Eta']='\u0397';
e2uHash['Theta']='\u0398';
e2uHash['Iota']='\u0399';
e2uHash['Kappa']='\u039A';
e2uHash['Lambda']='\u039B';
e2uHash['Mu']='\u039C';
e2uHash['Nu']='\u039D';
e2uHash['Xi']='\u039E';
e2uHash['Omicron']='\u039F';
e2uHash['Pi']='\u03A0';
e2uHash['Rho']='\u03A1';
e2uHash['Sigma']='\u03A3';
e2uHash['Tau']='\u03A4';
e2uHash['Upsilon']='\u03A5';
e2uHash['Phi']='\u03A6';
e2uHash['Chi']='\u03A7';
e2uHash['Psi']='\u03A8';
e2uHash['Omega']='\u03A9';
e2uHash['alpha']='\u03B1';
e2uHash['beta']='\u03B2';
e2uHash['gamma']='\u03B3';
e2uHash['delta']='\u03B4';
e2uHash['epsilon']='\u03B5';
e2uHash['zeta']='\u03B6';
e2uHash['eta']='\u03B7';
e2uHash['theta']='\u03B8';
e2uHash['iota']='\u03B9';
e2uHash['kappa']='\u03BA';
e2uHash['lambda']='\u03BB';
e2uHash['mu']='\u03BC';
e2uHash['nu']='\u03BD';
e2uHash['xi']='\u03BE';
e2uHash['omicron']='\u03BF';
e2uHash['pi']='\u03C0';
e2uHash['rho']='\u03C1';
e2uHash['sigmaf']='\u03C2';
e2uHash['sigma']='\u03C3';
e2uHash['tau']='\u03C4';
e2uHash['upsilon']='\u03C5';
e2uHash['phi']='\u03C6';
e2uHash['chi']='\u03C7';
e2uHash['psi']='\u03C8';
e2uHash['omega']='\u03C9';
e2uHash['thetasym']='\u03D1';
e2uHash['upsih']='\u03D2';
e2uHash['piv']='\u03D6';
e2uHash['bull']='\u2022';
e2uHash['hellip']='\u2026';
e2uHash['prime']='\u2032';
e2uHash['Prime']='\u2033';
e2uHash['oline']='\u203E';
e2uHash['frasl']='\u2044';
e2uHash['weierp']='\u2118';
e2uHash['image']='\u2111';
e2uHash['real']='\u211C';
e2uHash['trade']='\u2122';
e2uHash['alefsym']='\u2135';
e2uHash['larr']='\u2190';
e2uHash['uarr']='\u2191';
e2uHash['rarr']='\u2192';
e2uHash['darr']='\u2193';
e2uHash['harr']='\u2194';
e2uHash['crarr']='\u21B5';
e2uHash['lArr']='\u21D0';
e2uHash['uArr']='\u21D1';
e2uHash['rArr']='\u21D2';
e2uHash['dArr']='\u21D3';
e2uHash['hArr']='\u21D4';
e2uHash['forall']='\u2200';
e2uHash['part']='\u2202';
e2uHash['exist']='\u2203';
e2uHash['empty']='\u2205';
e2uHash['nabla']='\u2207';
e2uHash['isin']='\u2208';
e2uHash['notin']='\u2209';
e2uHash['ni']='\u220B';
e2uHash['prod']='\u220F';
e2uHash['sum']='\u2211';
e2uHash['minus']='\u2212';
e2uHash['lowast']='\u2217';
e2uHash['radic']='\u221A';
e2uHash['prop']='\u221D';
e2uHash['infin']='\u221E';
e2uHash['ang']='\u2220';
e2uHash['and']='\u2227';
e2uHash['or']='\u2228';
e2uHash['cap']='\u2229';
e2uHash['cup']='\u222A';
e2uHash['int']='\u222B';
e2uHash['there4']='\u2234';
e2uHash['sim']='\u223C';
e2uHash['cong']='\u2245';
e2uHash['asymp']='\u2248';
e2uHash['ne']='\u2260';
e2uHash['equiv']='\u2261';
e2uHash['le']='\u2264';
e2uHash['ge']='\u2265';
e2uHash['sub']='\u2282';
e2uHash['sup']='\u2283';
e2uHash['nsub']='\u2284';
e2uHash['sube']='\u2286';
e2uHash['supe']='\u2287';
e2uHash['oplus']='\u2295';
e2uHash['otimes']='\u2297';
e2uHash['perp']='\u22A5';
e2uHash['sdot']='\u22C5';
e2uHash['lceil']='\u2308';
e2uHash['rceil']='\u2309';
e2uHash['lfloor']='\u230A';
e2uHash['rfloor']='\u230B';
e2uHash['lang']='\u2329';
e2uHash['rang']='\u232A';
e2uHash['loz']='\u25CA';
e2uHash['spades']='\u2660';
e2uHash['clubs']='\u2663';
e2uHash['hearts']='\u2665';
e2uHash['diams']='\u2666';
})();

SimileAjax.HTML.deEntify=function(s){
var e2uHash=SimileAjax.HTML._e2uHash;

var re=/&(\w+?);/;
while(re.test(s)){
var m=s.match(re);
s=s.replace(re,e2uHash[m[1]]);
}
return s;
};

/* json.js */





SimileAjax.JSON=new Object();

(function(){
var m={
'\b':'\\b',
'\t':'\\t',
'\n':'\\n',
'\f':'\\f',
'\r':'\\r',
'"':'\\"',
'\\':'\\\\'
};
var s={
array:function(x){
var a=['['],b,f,i,l=x.length,v;
for(i=0;i<l;i+=1){
v=x[i];
f=s[typeof v];
if(f){
v=f(v);
if(typeof v=='string'){
if(b){
a[a.length]=',';
}
a[a.length]=v;
b=true;
}
}
}
a[a.length]=']';
return a.join('');
},
'boolean':function(x){
return String(x);
},
'null':function(x){
return"null";
},
number:function(x){
return isFinite(x)?String(x):'null';
},
object:function(x){
if(x){
if(x instanceof Array){
return s.array(x);
}
var a=['{'],b,f,i,v;
for(i in x){
v=x[i];
f=s[typeof v];
if(f){
v=f(v);
if(typeof v=='string'){
if(b){
a[a.length]=',';
}
a.push(s.string(i),':',v);
b=true;
}
}
}
a[a.length]='}';
return a.join('');
}
return'null';
},
string:function(x){
if(/["\\\x00-\x1f]/.test(x)){
x=x.replace(/([\x00-\x1f\\"])/g,function(a,b){
var c=m[b];
if(c){
return c;
}
c=b.charCodeAt();
return'\\u00'+
Math.floor(c/16).toString(16)+
(c%16).toString(16);
});
}
return'"'+x+'"';
}
};

SimileAjax.JSON.toJSONString=function(o){
if(o instanceof Object){
return s.object(o);
}else if(o instanceof Array){
return s.array(o);
}else{
return o.toString();
}
};

SimileAjax.JSON.parseJSON=function(){
try{
return!(/[^,:{}\[\]0-9.\-+Eaeflnr-u \n\r\t]/.test(
this.replace(/"(\\.|[^"\\])*"/g,'')))&&
eval('('+this+')');
}catch(e){
return false;
}
};
})();


/* string.js */



String.prototype.trim=function(){
return this.replace(/^\s+|\s+$/g,'');
};

String.prototype.startsWith=function(prefix){
return this.length>=prefix.length&&this.substr(0,prefix.length)==prefix;
};

String.prototype.endsWith=function(suffix){
return this.length>=suffix.length&&this.substr(this.length-suffix.length)==suffix;
};

String.substitute=function(s,objects){
var result="";
var start=0;
while(start<s.length-1){
var percent=s.indexOf("%",start);
if(percent<0||percent==s.length-1){
break;
}else if(percent>start&&s.charAt(percent-1)=="\\"){
result+=s.substring(start,percent-1)+"%";
start=percent+1;
}else{
var n=parseInt(s.charAt(percent+1));
if(isNaN(n)||n>=objects.length){
result+=s.substring(start,percent+2);
}else{
result+=s.substring(start,percent)+objects[n].toString();
}
start=percent+2;
}
}

if(start<s.length){
result+=s.substring(start);
}
return result;
};


/* units.js */





SimileAjax.NativeDateUnit=new Object();



SimileAjax.NativeDateUnit.makeDefaultValue=function(){

return new Date();

};



SimileAjax.NativeDateUnit.cloneValue=function(v){

return new Date(v.getTime());

};



SimileAjax.NativeDateUnit.getParser=function(format){

if(typeof format=="string"){

format=format.toLowerCase();

}

return(format=="iso8601"||format=="iso 8601")?

SimileAjax.DateTime.parseIso8601DateTime:

SimileAjax.DateTime.parseGregorianDateTime;

};



SimileAjax.NativeDateUnit.parseFromObject=function(o){

return SimileAjax.DateTime.parseGregorianDateTime(o);

};



SimileAjax.NativeDateUnit.toNumber=function(v){

return v.getTime();

};



SimileAjax.NativeDateUnit.fromNumber=function(n){

return new Date(n);

};



SimileAjax.NativeDateUnit.compare=function(v1,v2){

var n1,n2;

if(typeof v1=="object"){

n1=v1.getTime();

}else{

n1=Number(v1);

}

if(typeof v2=="object"){

n2=v2.getTime();

}else{

n2=Number(v2);

}



return n1-n2;

};



SimileAjax.NativeDateUnit.earlier=function(v1,v2){

return SimileAjax.NativeDateUnit.compare(v1,v2)<0?v1:v2;

};



SimileAjax.NativeDateUnit.later=function(v1,v2){

return SimileAjax.NativeDateUnit.compare(v1,v2)>0?v1:v2;

};



SimileAjax.NativeDateUnit.change=function(v,n){

return new Date(v.getTime()+n);

};





/* window-manager.js */




SimileAjax.WindowManager={
_initialized:false,
_listeners:[],

_draggedElement:null,
_draggedElementCallback:null,
_dropTargetHighlightElement:null,
_lastCoords:null,
_ghostCoords:null,
_draggingMode:"",
_dragging:false,

_layers:[]
};

SimileAjax.WindowManager.initialize=function(){
if(SimileAjax.WindowManager._initialized){
return;
}

SimileAjax.DOM.registerEvent(document.body,"mousedown",SimileAjax.WindowManager._onBodyMouseDown);
SimileAjax.DOM.registerEvent(document.body,"mousemove",SimileAjax.WindowManager._onBodyMouseMove);
SimileAjax.DOM.registerEvent(document.body,"mouseup",SimileAjax.WindowManager._onBodyMouseUp);
SimileAjax.DOM.registerEvent(document,"keydown",SimileAjax.WindowManager._onBodyKeyDown);
SimileAjax.DOM.registerEvent(document,"keyup",SimileAjax.WindowManager._onBodyKeyUp);

SimileAjax.WindowManager._layers.push({index:0});

SimileAjax.WindowManager._historyListener={
onBeforeUndoSeveral:function(){},
onAfterUndoSeveral:function(){},
onBeforeUndo:function(){},
onAfterUndo:function(){},

onBeforeRedoSeveral:function(){},
onAfterRedoSeveral:function(){},
onBeforeRedo:function(){},
onAfterRedo:function(){}
};
SimileAjax.History.addListener(SimileAjax.WindowManager._historyListener);

SimileAjax.WindowManager._initialized=true;
};

SimileAjax.WindowManager.getBaseLayer=function(){
SimileAjax.WindowManager.initialize();
return SimileAjax.WindowManager._layers[0];
};

SimileAjax.WindowManager.getHighestLayer=function(){
SimileAjax.WindowManager.initialize();
return SimileAjax.WindowManager._layers[SimileAjax.WindowManager._layers.length-1];
};

SimileAjax.WindowManager.registerEventWithObject=function(elmt,eventName,obj,handlerName,layer){
SimileAjax.WindowManager.registerEvent(
elmt,
eventName,
function(elmt2,evt,target){
return obj[handlerName].call(obj,elmt2,evt,target);
},
layer
);
};

SimileAjax.WindowManager.registerEvent=function(elmt,eventName,handler,layer){
if(layer==null){
layer=SimileAjax.WindowManager.getHighestLayer();
}

var handler2=function(elmt,evt,target){
if(SimileAjax.WindowManager._canProcessEventAtLayer(layer)){
SimileAjax.WindowManager._popToLayer(layer.index);
try{
handler(elmt,evt,target);
}catch(e){
SimileAjax.Debug.exception(e);
}
}
SimileAjax.DOM.cancelEvent(evt);
return false;
}

SimileAjax.DOM.registerEvent(elmt,eventName,handler2);
};

SimileAjax.WindowManager.pushLayer=function(f,ephemeral,elmt){
var layer={onPop:f,index:SimileAjax.WindowManager._layers.length,ephemeral:(ephemeral),elmt:elmt};
SimileAjax.WindowManager._layers.push(layer);

return layer;
};

SimileAjax.WindowManager.popLayer=function(layer){
for(var i=1;i<SimileAjax.WindowManager._layers.length;i++){
if(SimileAjax.WindowManager._layers[i]==layer){
SimileAjax.WindowManager._popToLayer(i-1);
break;
}
}
};

SimileAjax.WindowManager.popAllLayers=function(){
SimileAjax.WindowManager._popToLayer(0);
};

SimileAjax.WindowManager.registerForDragging=function(elmt,callback,layer){
SimileAjax.WindowManager.registerEvent(
elmt,
"mousedown",
function(elmt,evt,target){
SimileAjax.WindowManager._handleMouseDown(elmt,evt,callback);
},
layer
);
};

SimileAjax.WindowManager._popToLayer=function(level){
while(level+1<SimileAjax.WindowManager._layers.length){
try{
var layer=SimileAjax.WindowManager._layers.pop();
if(layer.onPop!=null){
layer.onPop();
}
}catch(e){
}
}
};

SimileAjax.WindowManager._canProcessEventAtLayer=function(layer){
if(layer.index==(SimileAjax.WindowManager._layers.length-1)){
return true;
}
for(var i=layer.index+1;i<SimileAjax.WindowManager._layers.length;i++){
if(!SimileAjax.WindowManager._layers[i].ephemeral){
return false;
}
}
return true;
};

SimileAjax.WindowManager.cancelPopups=function(evt){
var evtCoords=(evt)?SimileAjax.DOM.getEventPageCoordinates(evt):{x:-1,y:-1};

var i=SimileAjax.WindowManager._layers.length-1;
while(i>0&&SimileAjax.WindowManager._layers[i].ephemeral){
var layer=SimileAjax.WindowManager._layers[i];
if(layer.elmt!=null){
var elmt=layer.elmt;
var elmtCoords=SimileAjax.DOM.getPageCoordinates(elmt);
if(evtCoords.x>=elmtCoords.left&&evtCoords.x<(elmtCoords.left+elmt.offsetWidth)&&
evtCoords.y>=elmtCoords.top&&evtCoords.y<(elmtCoords.top+elmt.offsetHeight)){
break;
}
}
i--;
}
SimileAjax.WindowManager._popToLayer(i);
};

SimileAjax.WindowManager._onBodyMouseDown=function(elmt,evt,target){
if(!("eventPhase"in evt)||evt.eventPhase==evt.BUBBLING_PHASE){
SimileAjax.WindowManager.cancelPopups(evt);
}
};

SimileAjax.WindowManager._handleMouseDown=function(elmt,evt,callback){
SimileAjax.WindowManager._draggedElement=elmt;
SimileAjax.WindowManager._draggedElementCallback=callback;
SimileAjax.WindowManager._lastCoords={x:evt.clientX,y:evt.clientY};

SimileAjax.DOM.cancelEvent(evt);
return false;
};

SimileAjax.WindowManager._onBodyKeyDown=function(elmt,evt,target){
if(SimileAjax.WindowManager._dragging){
if(evt.keyCode==27){
SimileAjax.WindowManager._cancelDragging();
}else if((evt.keyCode==17||evt.keyCode==16)&&SimileAjax.WindowManager._draggingMode!="copy"){
SimileAjax.WindowManager._draggingMode="copy";

var img=SimileAjax.Graphics.createTranslucentImage(SimileAjax.urlPrefix+"images/copy.png");
img.style.position="absolute";
img.style.left=(SimileAjax.WindowManager._ghostCoords.left-16)+"px";
img.style.top=(SimileAjax.WindowManager._ghostCoords.top)+"px";
document.body.appendChild(img);

SimileAjax.WindowManager._draggingModeIndicatorElmt=img;
}
}
};

SimileAjax.WindowManager._onBodyKeyUp=function(elmt,evt,target){
if(SimileAjax.WindowManager._dragging){
if(evt.keyCode==17||evt.keyCode==16){
SimileAjax.WindowManager._draggingMode="";
if(SimileAjax.WindowManager._draggingModeIndicatorElmt!=null){
document.body.removeChild(SimileAjax.WindowManager._draggingModeIndicatorElmt);
SimileAjax.WindowManager._draggingModeIndicatorElmt=null;
}
}
}
};

SimileAjax.WindowManager._onBodyMouseMove=function(elmt,evt,target){
if(SimileAjax.WindowManager._draggedElement!=null){
var callback=SimileAjax.WindowManager._draggedElementCallback;

var lastCoords=SimileAjax.WindowManager._lastCoords;
var diffX=evt.clientX-lastCoords.x;
var diffY=evt.clientY-lastCoords.y;

if(!SimileAjax.WindowManager._dragging){
if(Math.abs(diffX)>5||Math.abs(diffY)>5){
try{
if("onDragStart"in callback){
callback.onDragStart();
}

if("ghost"in callback&&callback.ghost){
var draggedElmt=SimileAjax.WindowManager._draggedElement;

SimileAjax.WindowManager._ghostCoords=SimileAjax.DOM.getPageCoordinates(draggedElmt);
SimileAjax.WindowManager._ghostCoords.left+=diffX;
SimileAjax.WindowManager._ghostCoords.top+=diffY;

var ghostElmt=draggedElmt.cloneNode(true);
ghostElmt.style.position="absolute";
ghostElmt.style.left=SimileAjax.WindowManager._ghostCoords.left+"px";
ghostElmt.style.top=SimileAjax.WindowManager._ghostCoords.top+"px";
ghostElmt.style.zIndex=1000;
SimileAjax.Graphics.setOpacity(ghostElmt,50);

document.body.appendChild(ghostElmt);
callback._ghostElmt=ghostElmt;
}

SimileAjax.WindowManager._dragging=true;
SimileAjax.WindowManager._lastCoords={x:evt.clientX,y:evt.clientY};

document.body.focus();
}catch(e){
SimileAjax.Debug.exception("WindowManager: Error handling mouse down",e);
SimileAjax.WindowManager._cancelDragging();
}
}
}else{
try{
SimileAjax.WindowManager._lastCoords={x:evt.clientX,y:evt.clientY};

if("onDragBy"in callback){
callback.onDragBy(diffX,diffY);
}

if("_ghostElmt"in callback){
var ghostElmt=callback._ghostElmt;

SimileAjax.WindowManager._ghostCoords.left+=diffX;
SimileAjax.WindowManager._ghostCoords.top+=diffY;

ghostElmt.style.left=SimileAjax.WindowManager._ghostCoords.left+"px";
ghostElmt.style.top=SimileAjax.WindowManager._ghostCoords.top+"px";
if(SimileAjax.WindowManager._draggingModeIndicatorElmt!=null){
var indicatorElmt=SimileAjax.WindowManager._draggingModeIndicatorElmt;

indicatorElmt.style.left=(SimileAjax.WindowManager._ghostCoords.left-16)+"px";
indicatorElmt.style.top=SimileAjax.WindowManager._ghostCoords.top+"px";
}

if("droppable"in callback&&callback.droppable){
var coords=SimileAjax.DOM.getEventPageCoordinates(evt);
var target=SimileAjax.DOM.hittest(
coords.x,coords.y,
[SimileAjax.WindowManager._ghostElmt,
SimileAjax.WindowManager._dropTargetHighlightElement
]
);
target=SimileAjax.WindowManager._findDropTarget(target);

if(target!=SimileAjax.WindowManager._potentialDropTarget){
if(SimileAjax.WindowManager._dropTargetHighlightElement!=null){
document.body.removeChild(SimileAjax.WindowManager._dropTargetHighlightElement);

SimileAjax.WindowManager._dropTargetHighlightElement=null;
SimileAjax.WindowManager._potentialDropTarget=null;
}

var droppable=false;
if(target!=null){
if((!("canDropOn"in callback)||callback.canDropOn(target))&&
(!("canDrop"in target)||target.canDrop(SimileAjax.WindowManager._draggedElement))){

droppable=true;
}
}

if(droppable){
var border=4;
var targetCoords=SimileAjax.DOM.getPageCoordinates(target);
var highlight=document.createElement("div");
highlight.style.border=border+"px solid yellow";
highlight.style.backgroundColor="yellow";
highlight.style.position="absolute";
highlight.style.left=targetCoords.left+"px";
highlight.style.top=targetCoords.top+"px";
highlight.style.width=(target.offsetWidth-border*2)+"px";
highlight.style.height=(target.offsetHeight-border*2)+"px";
SimileAjax.Graphics.setOpacity(highlight,30);
document.body.appendChild(highlight);

SimileAjax.WindowManager._potentialDropTarget=target;
SimileAjax.WindowManager._dropTargetHighlightElement=highlight;
}
}
}
}
}catch(e){
SimileAjax.Debug.exception("WindowManager: Error handling mouse move",e);
SimileAjax.WindowManager._cancelDragging();
}
}

SimileAjax.DOM.cancelEvent(evt);
return false;
}
};

SimileAjax.WindowManager._onBodyMouseUp=function(elmt,evt,target){
if(SimileAjax.WindowManager._draggedElement!=null){
try{
if(SimileAjax.WindowManager._dragging){
var callback=SimileAjax.WindowManager._draggedElementCallback;
if("onDragEnd"in callback){
callback.onDragEnd();
}
if("droppable"in callback&&callback.droppable){
var dropped=false;

var target=SimileAjax.WindowManager._potentialDropTarget;
if(target!=null){
if((!("canDropOn"in callback)||callback.canDropOn(target))&&
(!("canDrop"in target)||target.canDrop(SimileAjax.WindowManager._draggedElement))){

if("onDropOn"in callback){
callback.onDropOn(target);
}
target.ondrop(SimileAjax.WindowManager._draggedElement,SimileAjax.WindowManager._draggingMode);

dropped=true;
}
}

if(!dropped){

}
}
}
}finally{
SimileAjax.WindowManager._cancelDragging();
}

SimileAjax.DOM.cancelEvent(evt);
return false;
}
};

SimileAjax.WindowManager._cancelDragging=function(){
var callback=SimileAjax.WindowManager._draggedElementCallback;
if("_ghostElmt"in callback){
var ghostElmt=callback._ghostElmt;
document.body.removeChild(ghostElmt);

delete callback._ghostElmt;
}
if(SimileAjax.WindowManager._dropTargetHighlightElement!=null){
document.body.removeChild(SimileAjax.WindowManager._dropTargetHighlightElement);
SimileAjax.WindowManager._dropTargetHighlightElement=null;
}
if(SimileAjax.WindowManager._draggingModeIndicatorElmt!=null){
document.body.removeChild(SimileAjax.WindowManager._draggingModeIndicatorElmt);
SimileAjax.WindowManager._draggingModeIndicatorElmt=null;
}

SimileAjax.WindowManager._draggedElement=null;
SimileAjax.WindowManager._draggedElementCallback=null;
SimileAjax.WindowManager._potentialDropTarget=null;
SimileAjax.WindowManager._dropTargetHighlightElement=null;
SimileAjax.WindowManager._lastCoords=null;
SimileAjax.WindowManager._ghostCoords=null;
SimileAjax.WindowManager._draggingMode="";
SimileAjax.WindowManager._dragging=false;
};

SimileAjax.WindowManager._findDropTarget=function(elmt){
while(elmt!=null){
if("ondrop"in elmt&&(typeof elmt.ondrop)=="function"){
break;
}
elmt=elmt.parentNode;
}
return elmt;
};


/* xmlhttp.js */



SimileAjax.XmlHttp=new Object();


SimileAjax.XmlHttp._onReadyStateChange=function(xmlhttp,fError,fDone){
switch(xmlhttp.readyState){





case 4:
try{
if(xmlhttp.status==0
||xmlhttp.status==200
){
if(fDone){
fDone(xmlhttp);
}
}else{
if(fError){
fError(
xmlhttp.statusText,
xmlhttp.status,
xmlhttp
);
}
}
}catch(e){
SimileAjax.Debug.exception("XmlHttp: Error handling onReadyStateChange",e);
}
break;
}
};


SimileAjax.XmlHttp._createRequest=function(){
if(SimileAjax.Platform.browser.isIE){
var programIDs=[
"Msxml2.XMLHTTP",
"Microsoft.XMLHTTP",
"Msxml2.XMLHTTP.4.0"
];
for(var i=0;i<programIDs.length;i++){
try{
var programID=programIDs[i];
var f=function(){
return new ActiveXObject(programID);
};
var o=f();






SimileAjax.XmlHttp._createRequest=f;

return o;
}catch(e){

}
}

}

try{
var f=function(){
return new XMLHttpRequest();
};
var o=f();






SimileAjax.XmlHttp._createRequest=f;

return o;
}catch(e){
throw new Error("Failed to create an XMLHttpRequest object");
}
};


SimileAjax.XmlHttp.get=function(url,fError,fDone){
var xmlhttp=SimileAjax.XmlHttp._createRequest();

xmlhttp.open("GET",url,true);
xmlhttp.onreadystatechange=function(){
SimileAjax.XmlHttp._onReadyStateChange(xmlhttp,fError,fDone);
};
xmlhttp.send(null);
};


SimileAjax.XmlHttp.post=function(url,body,fError,fDone){
var xmlhttp=SimileAjax.XmlHttp._createRequest();

xmlhttp.open("POST",url,true);
xmlhttp.onreadystatechange=function(){
SimileAjax.XmlHttp._onReadyStateChange(xmlhttp,fError,fDone);
};
xmlhttp.send(body);
};

SimileAjax.XmlHttp._forceXML=function(xmlhttp){
try{
xmlhttp.overrideMimeType("text/xml");
}catch(e){
xmlhttp.setrequestheader("Content-Type","text/xml");
}
};