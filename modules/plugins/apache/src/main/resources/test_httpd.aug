module Test_httpd =

let directives = "#
# Timeout: The number of seconds before receives and sends time out.
#
Timeout 120

Listen 80
Listen 0.0.0.0:8080
Listen [2001:db8::a00:20ff:fea7:ccea]:80 
Listen 192.170.2.1:8443 https 

#
# KeepAlive: Whether or not to allow persistent connections (more than
# one request per connection). Set to \"Off\" to deactivate.
#
KeepAlive Off
"

let ifmodule = "<IfModule prefork.c>
StartServers       8
MinSpareServers    5
MaxSpareServers   20
ServerLimit      256
MaxClients       256
MaxRequestsPerChild  4000
</IfModule>\n"

let loadModule = "# Example:
# LoadModule foo_module modules/mod_foo.so
#
LoadModule auth_basic_module modules/mod_auth_basic.so
LoadModule auth_digest_module modules/mod_auth_digest.so
LoadModule authn_file_module modules/mod_authn_file.so
"

test Httpd.lns get directives =
  {} {} {} { "Timeout" = "120" } {} 
  { "Listen" { "port" = "80" } }
  { "Listen" { "ip" = "0.0.0.0" } { "port" = "8080" } }
  { "Listen" { "ip" = "[2001:db8::a00:20ff:fea7:ccea]" } { "port" = "80" } }
  { "Listen" { "ip" = "192.170.2.1" } { "port" = "8443" } { "scheme" = "https" } }
  {} {} {} {} {} { "KeepAlive" = "Off" }

test Httpd.lns get ifmodule =
  { "IfModule" = "prefork.c"
      { "StartServers" = "8" }
      { "MinSpareServers" = "5" }
      { "MaxSpareServers" = "20" }
      { "ServerLimit" = "256" }
      { "MaxClients" = "256" }
      { "MaxRequestsPerChild" = "4000" } }

test Httpd.lns get loadModule =
  {} {} {}
  { "LoadModule" = "auth_basic_module"
      { "path" = "modules/mod_auth_basic.so" } }
  { "LoadModule" = "auth_digest_module"
      { "path" = "modules/mod_auth_digest.so" } }
  { "LoadModule" = "authn_file_module"
      { "path" = "modules/mod_authn_file.so" } }

test Httpd.options get "  Options Indexes +ExecCGI\n" =
  { "Options"
      { "option" = "Indexes" }
      { "option" = "+ExecCGI" } }

test Httpd.customLog get " CustomLog gif-requests.log common env=!gif-image\n" =
  { "CustomLog" = "gif-requests.log"
      { "format" = "common" }
      { "env" = "!gif-image" } }

test Httpd.indexOptions get
    " IndexOptions +ScanHTMLTitles -IconsAreLinks FancyIndexing Charset=UTF-8\n"
  =
  { "IndexOptions"
    { "+ScanHTMLTitles" }
    { "-IconsAreLinks" }
    { "FancyIndexing" }
    { "Charset" = "UTF-8" } }

test Httpd.addIconByEncodingOrType get
    "AddIconByEncoding (CMP,/icons/compressed.gif) x-compress x-gzip\n" =
  { "AddIconByEncoding"
      { "icon" = "/icons/compressed.gif"  { "alttext"  = "CMP" } }
      { "encoding" = "x-compress" }
      { "encoding" = "x-gzip" } }
  

test Httpd.alias get
    "Alias /image /ftp/pub/image\n"
    =
    {"Alias" = "/image"
        {"directory" = "/ftp/pub/image" } }

let dir_test = "<Directory \"/var/www/html\">
    Options Indexes FollowSymLinks
</Directory>
"

test Httpd.lns get 
    dir_test
    =
    { "Directory" = "\"/var/www/html\"" 
        { "Options" { "option" = "Indexes" } { "option" = "FollowSymLinks" } } }

test Httpd.restOfDirectives get "RewriteRule ^/auth($|/)(.*)         http://host/auth/$2                [P,L]\n" = 
    { "RewriteRule" {"param" = "^/auth($|/)(.*)" } { "param" = "http://host/auth/$2" } { "param" = "[P,L]" } }

test Httpd.directive get "Include /etc/httpd/conf/vhosts/www.jboss.org-common/www.jboss.org-config.conf\n" =
    { "Include" = "/etc/httpd/conf/vhosts/www.jboss.org-common/www.jboss.org-config.conf" }

test Httpd.lns get "Include \\\n /value\n" = { "Include" = "/value" }
