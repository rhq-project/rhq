module Httpd =
    autoload xfm

(* Helpers *)

(* Newlines can be escaped. *)
let sep = del /([ \t]+(\\\\\n)?)+/ " "
let eol = del /([ \t]*(\\\\\n)?)*\n/ "\n"
  let del_str (s:string) = del s s
let ws = /[ \t]*/
let alnum = /[a-zA-Z0-9_]+/
(* the last character in the non-quoted word must not be a backslash. I guess this is not completely semantically
   correct but at the same time, apache discourages to use backslashes for anything else than
   line breaking so we should be safe here. This restriction is in place to support
   escaped new lines in the sep and eol rules. *)
let word = /\"([^\"\n]|\\\\\")*\"|'([^'\n]|\\\\')*'|[^'" \t\n]*[^'" \t\n\\]/
let secarg = /\"([^\"\n]|\\\\\")*\"|'([^'\n]|\\\\')*'|[^'\" \t\n>]+/
let wskey (k:regexp) = del ws "" . key k
let params (param:regexp) = [ sep . label "param" . store param ]*
let sec (name:string) (body:lens) =
    [ wskey ("<" . name) . params secarg . del_str ">" . eol .
        body . del ws "" . del_str("</" . name . ">") . eol ]

(* Definitions *)
let comment = [ del /([ \t]*(#.*)*)\n/ "#\n" ]

let directive = [ wskey alnum . params word . eol ]

let section (name:string) = sec name (directive|comment)*

let sections (body:lens) = sec "Directory" body
             | sec "DirectoryMatch" body
             | sec "Files" body
             | sec "FilesMatch" body
             | sec "Location" body
             | sec "LocationMatch" body
             | sec "AuthnProviderAlias" body
             | sec "IfDefine" body
             | sec "IfVersion" body
             | sec "Limit" body
             | sec "LimitExcept" body
             | sec "Proxy" body
             | sec "ProxyMatch" body
             | sec "VirtualHost" body
             | sec "IfModule" body


(* What we want ot say is *)
(* let rec body = (directive|comment)* | ifModule body | directory body | ... *)
(* but we can't typecheck that *)

(* FIXME: *)
(* - Nesting of sections *)
let rec lns = (directive | comment | sections lns)*

let filter =
    incl "/etc/httpd/conf/httpd.conf"
let xfm = transform lns filter
