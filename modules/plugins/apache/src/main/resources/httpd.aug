module Httpd = 
    autoload xfm

(* Helpers *)

(* Newlines can be escaped but I can see no way of making the lens unambigous and support escaped newlines. *)
(* let sep = del /([ \t]+(\\\\\n)?)+/ " " *)
let sep = del /[ \t]+/ " "
let eol = del /[ \t]*\n/ "\n"

let ws = /[ \t]*/
let alnum = /[a-zA-Z0-9_]+/
let word = /\"([^\"\n]|\\\\\")*\"|'([^'\n]|\\\\')*'|[^'" \t\n]+/
let secarg = /\"([^\"\n]|\\\\\")*\"|'([^'\n]|\\\\')*'|[^'" \t\n>]+/
let wskey (k:regexp) = del ws "" . key k
let params (param:regexp) = [ sep . label "param" . store param ]*
let sec (name:string) (body:lens) = 
    [ wskey ("<" . name) . params secarg . Util.del_str ">" . eol . 
        body . del ws "" . Util.del_str("</" . name . ">") . eol ]
    
(* Definitions *)
let comment = [ del /([ \t]*(#.*)*)\n/ "#\n" ]
let directive = [ wskey alnum . params word . eol ]

let section (name:string) = sec name (directive|comment)*
    
let sections = section "IfModule" 
             | section "Directory" 
             | section "DirectoryMatch"
             | section "Files"
             | section "FilesMatch"
             | section "Location"
             | section "LocationMatch"
             | section "AuthnProviderAlias"
             | section "IfDefine"
             | section "IfVersion"
             | section "Limit"
             | section "LimitExcept"
             | section "Proxy"
             | section "ProxyMatch"

let virtualHost = sec "VirtualHost" (directive|comment|sections)*

(* What we want ot say is *)
(* let rec body = (directive|comment)* | ifModule body | directory body | ... *)
(* but we can't typecheck that *)

(* FIXME: *)
(* - Nesting of sections *)
let lns = (directive)* (* | comment | sections | virtualHost)* *)

let filter =
    incl "/etc/httpd/conf/httpd.conf" .
    Util.stdexcl

let xfm = transform lns filter
