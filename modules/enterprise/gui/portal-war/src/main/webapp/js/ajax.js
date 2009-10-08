/*
 * This is public domain of mine
 */
function getXMLHttpRequest()
{
    var xmlhttp = false;
    /*@cc_on @*/
    /*@if (@_jscript_version >= 5)
   try {
       xmlhttp = new ActiveXObject("Msxml2.XMLHTTP");
   } catch (e) {
       try {
           xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
       } catch (E) {
           xmlhttp = false;
       }
   }
   @end @*/
    if (!xmlhttp && typeof XMLHttpRequest != 'undefined') {
        xmlhttp = new XMLHttpRequest();
    }

    return xmlhttp;
}