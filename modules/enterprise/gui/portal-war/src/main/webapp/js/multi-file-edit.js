function ArchiveFile(p,c) {
	  this.path  = p;
	  this.dirty = 0;
	  this.contents = c;
}

/*
 * This is a class for editing a collection of files, represented by a map.
 * There is a list of links in the left column, each which represent a file,
 * a textarea input that changes based on which link is selected
 */
function MultiFileEdit(){
	this.currentIndex = 0;
	this.isDirty = 0;
	this.archive = new Array();
	this.disabled=0;
	
	this.initializeArchive = function(){
		var i=0;
		for (i=0; i <  6 ;i++)
		{
			this.archive[i] = new ArchiveFile("/tmp/file/"+i, "/tmp/file/"+i +" archive = "+ this.archive.length);		 	
		}
	}

	this.setTextAreaValue = function(id)
	{
		if (this.currentIndex % 2 == 0){
			document.getElementById("file-row-"+this.currentIndex).className = "OddRow";	
		}else{
			document.getElementById("file-row-"+this.currentIndex).className = "EvenRow";
				
		}
		
		this.currentIndex = id;
		document.getElementById("testForm").textarea.value = multiFileEdit.archive[this.currentIndex].contents;
		document.getElementById("file-row-"+id).className = "SelectedRow";	
		document.getElementById("testForm").textarea.focus();
		document.getElementById("current-path-span").firstChild.data = multiFileEdit.archive[this.currentIndex].path;
	}
	
	
	this.resetArchive = function(){
		this.initializeArchive();
		this.setTextAreaValue(0);
		this.isDirty = 0; 
		for (i=0; i < this.archive.length ;i++)
		{
			document.getElementById("dirty-span-"+i).style.visibility="hidden";
		}
	}

	function askConfirm(){
		return "You have unsaved changes.";
	}

	
	this.updateArchive = function(){
		
		window.onbeforeunload = askConfirm;
				
		this.isDirty = 1;
		this.archive[this.currentIndex].contents = textarea.value ;	
		this.archive[this.currentIndex].dirty = 1;
		document.getElementById("dirty-span-"+this.currentIndex).style.visibility="visible";
	}	

	this.generateLinks = function(){
		var i=0;
		var style =0;
		var divclass = "";
		document.write("<table BORDER=0 RULES=NONE FRAME=BOX><th><td>Configuration File Paths</td><td></td></th>");	
		for (i=0; i < multiFileEdit.archive.length ;i++){
						
			if (i == 0){
				divclass="SelectedRow"
			}else if (style != 0){
				style = 0;
				divclass="OddRow" 
			}else{
				style = 1;
				divclass="EvenRow" 
			}
			
			document.write("<tr id='file-row-"+i+"'class='"+ divclass +"'>");
			document.write("<td>");
			document.write("<span id='dirty-span-"+i+"' style=\"visibility:hidden;\" > * </span>");
			document.write("</td><td>");
			document.write("<a href=\"#\"  onclick='multiFileEdit.setTextAreaValue("+i+")' >"+ multiFileEdit.archive[i].path);
			document.write("</a>");	
			document.write("</td></tr>");
		}
		document.write("</table>");	
	
	}

	this.drawTable = function(){		
		document.write("<style type=\"text/css\">");
		document.write("td.multi-edit-table {");
		document.write("vertical-align: top;");
		document.write("valign: top;");
		document.write("height: = 300;");
		document.write("}");
		document.write("tr.EvenRow {")
		document.write("background-color:#a4b2b9;");
		document.write("}");
		document.write("tr.OddRow {");
		document.write("background-color:grey;");
		document.write("}");
		document.write("tr.SelectedRow {");
		document.write("background-color:white;");
		document.write("}");
		
		
		document.write("</style>");
		document.write("<table class='multi-edit-table' border='0' cellpadding='0' >");
		document.write("<tbody>");
		document.write("<tr>");
		document.write("<td class='multi-edit-table'   bgcolor='#a4b2b9'>");
		this.generateLinks();
		document.write("</td>");
		document.write("<td>");
		document.write("<div><span id='current-path-span'>Current File Name Goes here</span>  ..................");
		document.write("<a href=\"#\"><img src='/images/download.png'/> Download</a>");
		document.write("<a href=\"#\"  ");
		document.write("onclick=\"multiFileEdit.fullscreen()\"> "); 
		document.write("<img src='/images/fullscreen.png'/> Full Screen</a>")
		document.write("</div>");
		document.write("<textarea id='textarea' "); 
		if (this.disabled){
			document.write("disabled ");
		}
		document.write("onkeyup='multiFileEdit.updateArchive()' cols='80' rows='40'>");				
		document.write(multiFileEdit.archive[0].contents);
		document.write("</textarea>");
		
		document.write("<div>upload New Version</div>");
		document.write("<div>Select A File: <input type='file'/> </div>");
		document.write("</td>");
		document.write("</tr>");
		document.write("</tbody>");
		document.write("</table>");	
		
	}	
	
	this.fullscreen= function()
	{

		
	  var generator=window.open('','full screen','width=1024,height=768,left=0,top=100,screenX=0,screenY=0');
	  generator.document.write('<html><head><title>Popup</title>');
	  generator.document.write('<link rel="stylesheet" href="style.css">');	  
	  generator.document.write('<script language="JavaScript" type="text/javascript" src="multi-file-edit.js"></script>');
	  generator.document.write('</head><body>');	  
	  generator.document.write("<form id='full-screen-edit-form'>");
	  generator.document.write("<div><span id='current-path-span'>" +multiFileEdit.archive[this.currentIndex].path+"</span> ");
	  generator.document.write("<a href=\"javascript:self.close();\">Close</a>..................");		
	  generator.document.write("</div>");
	  generator.document.write("<textarea id='textarea'  onkeyup='update_parent();' ");
	  if (this.disabled){
	     generator.document.write("disabled ");
	   }			  
	  generator.document.write("cols='200' rows='45'>");
	  generator.document.write(multiFileEdit.archive[this.currentIndex].contents);
	  generator.document.write("</textarea>");						
	  generator.document.write("</form>");	 	  
	  generator.document.write('</body></html>');
	  
	  generator.document.getElementById("full-screen-edit-form").textarea.focus();
	  
	  generator.document.close();
	  	  

	}
		
	this.initializeArchive();
}


function update_parent(){
	opener.document.getElementById("testForm").textarea.value =  document.getElementById("full-screen-edit-form").textarea.value;
	opener.multiFileEdit.updateArchive()	
}
