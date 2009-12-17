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
  {} {} {} { "Timeout" { "param" = "120" } } {} 
  { "Listen" { "param" = "80" } }
  { "Listen" { "param" = "0.0.0.0:8080" } }
  { "Listen" { "param" = "[2001:db8::a00:20ff:fea7:ccea]:80" } }
  { "Listen" { "param" = "192.170.2.1:8443" } { "param" = "https" } }
  {} {} {} {} {} { "KeepAlive" { "param" = "Off" } }

test Httpd.lns get ifmodule =
  { "<IfModule" { "param" = "prefork.c" }
      { "StartServers" { "param" = "8" } }
      { "MinSpareServers" { "param" = "5" } }
      { "MaxSpareServers" { "param" = "20" } }
      { "ServerLimit" { "param" = "256" } }
      { "MaxClients" { "param" = "256" } }
      { "MaxRequestsPerChild" { "param" = "4000" } } }

test Httpd.lns get loadModule =
  {} {} {}
  { "LoadModule" { "param" = "auth_basic_module" }
      { "param" = "modules/mod_auth_basic.so" } }
  { "LoadModule" { "param" = "auth_digest_module" }
      { "param" = "modules/mod_auth_digest.so" } }
  { "LoadModule" { "param" = "authn_file_module" }
      { "param" = "modules/mod_authn_file.so" } }

test Httpd.directive get "  Options Indexes +ExecCGI\n" =
  { "Options"
      { "param" = "Indexes" }
      { "param" = "+ExecCGI" } }

test Httpd.directive get " CustomLog gif-requests.log common env=!gif-image\n" =
  { "CustomLog" { "param" = "gif-requests.log" }
      { "param" = "common" }
      { "param" = "env=!gif-image" } }

test Httpd.directive get
    " IndexOptions +ScanHTMLTitles -IconsAreLinks FancyIndexing Charset=UTF-8\n"
  =
  { "IndexOptions"
    { "param" = "+ScanHTMLTitles" }
    { "param" = "-IconsAreLinks" }
    { "param" = "FancyIndexing" }
    { "param" = "Charset=UTF-8" } }

let dir_test = "<Directory \"/var/www/html\">
    Options Indexes FollowSymLinks
</Directory>
"

test Httpd.lns get 
    dir_test
    =
    { "<Directory" { "param" = "\"/var/www/html\""  }
        { "Options" { "param" = "Indexes" } { "param" = "FollowSymLinks" } } }

test Httpd.directive get "Include /etc/httpd/conf/vhosts/www.jboss.org-common/www.jboss.org-config.conf\n" =
    { "Include" { "param" = "/etc/httpd/conf/vhosts/www.jboss.org-common/www.jboss.org-config.conf" } }

test Httpd.lns get "Include \\\n /value\n" = { "Include" {"param" =  "/value" } }
