module Httpd =
    autoload xfm
(* FIXME: up to line 737 in httpd.conf *)

(* FIXME: Most of the comparisons in Apache are case insensitive       *)
(* We do not have a way to express that currently; probably need some *)
(* additions to the regexp syntax like /[a-z]+/i                      *)
let sep = Util.del_ws_spc
let eol = del /[ \t]*\n/ "\n"
let number = /[0-9]+/
let on_off = /[Oo](n|ff)/
let langle = Util.del_str "<"
let rangle = Util.del_str ">"
let modname = /[a-zA-Z0-9._]+/
let alnum = /[a-zA-Z0-9]+/
let word = /"([^"\n])*"|'([^'\n])*'|[^'" \t\n]+/
let wordWithNestQuote = /"([^"\n]|(\\\\\"))*"|'([^'\n])*'|[^'" \t\n]+/

(* A that does not end with ">" *)
let secargBody = /\"([^\"\n]|\\\\\")*\"|'([^'\n]|\\\\')*'|[^ \t\n]*[^ \t\n>]/
(* let secarg = (secargBody.(" ".secargBody)?) *)
let secarg = secargBody

(* A key preceded by optional whitespace *)
let wskey (k:regexp) = del /[ \t]*/ "" . key k

(* A key value pair KEY VALUE *)
let kv1 (k:regexp) (v:regexp) =
  [ wskey k . sep . store v . eol ]

let kv2 (k:regexp) (v:regexp) (l2:string) (v2:regexp) =
  [ wskey k . sep . store v . [ sep . label l2 . store word ] . eol ]

let kv2_op (k:regexp) (v:regexp) (l2:string) (v2:regexp) = 
  [ wskey k . sep . store v . [ sep . label l2 . store word ]? . eol ]
  
(* A key followed by a space-separated list of values *)
let kv_arr (k:regexp) (l:string) (v:regexp) =
  [ wskey k . [ sep . label l . store v ]* . eol ]

(* A key followed by a non-empty space separated list of values *)
let kv_arr1 (k:regexp) (l:string) (v:regexp) = 
  [ wskey k . [ sep . label l . store v ]+ . eol ]
  
let kv_val_arr (k:regexp) (v:regexp) (l:string) (v2:regexp) =
  [ wskey k . sep . store v . [ sep . label l . store v2 ]+ . eol ]

let allow_deny (k:string) =
  [ wskey k . sep . Util.del_str "from" .
      [ sep . label "from" . store word ]* . eol ]

let icon =
   let icon_pair =
     Util.del_str "(" . [ label "alttext" . store /[^, \t\n]+/ ] .
     Util.del_str "," . store /[^() \t\n]+/ . Util.del_str ")" in
   [ label "icon" . (store /[^() \t\n]+/|icon_pair) ]

let comment = [ del /([ \t]*(#.*)*)\n/ "#\n" ]



(* Directives with their arguments *)

let addAltByEncoding = kv_val_arr "AddAltByEncoding" word "mime" word 
let addAltByType = kv_val_arr "AddAltByType" word "mime" word  
let addCharset = kv_val_arr "AddCharset" word "extension" word 
let accessFileName = kv_arr "AccessFileName" "filename" word
let acceptFilter = [wskey "AcceptFilter" . [ sep . label "protocol" . store word] . [ sep . label "acceptFilter". store word] . eol ]
let acceptPathInfo = kv1 "AcceptPathInfo" (on_off | "Default")
let addDescription = kv_val_arr "AddDescription" word "file" word
let addEncoding = kv_val_arr "AddEncoding" word "extension" word
let serverAlias = kv_val_arr "ServerAlias" word "hostname" word
let addType = kv_val_arr "AddType" word "name" word
let authType = kv1 "AuthType" ("Basic"|"Digest")
let action = [wskey "Action". [sep . label "action-type" . store word] . 
                              [sep . label "cgi-script". store word].
                              [sep . label "virtual". store /virtual/]? . eol ]
let addAlt = [wskey "AddAlt". [sep . label "string" . store word] . 
                              [sep . label "file". store word]+ . eol ]
let filterProvider = [wskey "FilterProvider". [sep . label "filter" . store word] . 
                              [sep . label "provider". store word].
                              [sep . label "match". store word] . eol ]
let filterTrace = [wskey "FilterTrace" . sep . store word . [ sep . label "level" . store /0|1|2/ ] . eol ]
let addIcon =
  [ wskey "AddIcon" . sep . icon . [ sep . label "name" . store word ]+ . eol ]
let addInputFilter = kv_val_arr "AddInputFilter" word "extension" word 
let addIconByEncodingOrType =
  [ wskey /AddIconBy(Encoding|Type)/ . sep .
      icon . [ sep . label "encoding" . store word ]+ . eol ]
let addLanguage = kv_val_arr "AddLanguage" word "extension" word

let alias = kv2 "Alias" word "directory" word

let aliasMatch = kv_val_arr "AliasMatch" word "file" word
let allow = allow_deny "Allow"
let allowOverride = 
  let directive_re = /All|None|Limit|Options|FileInfo|AuthConfig|Indexes/ in
  kv_arr "AllowOverride" "directive" directive_re
let addHandler = kv_arr1 "AddHandler" "name" word
let addOutputFilter = kv_val_arr "AddOutputFilter" word "name" word 
let addOutputFilterByType = kv_val_arr "AddOutputFilterByType" word "mime" word 
let browserMatch = kv_arr1 "BrowserMatch" "name" word
let browserMatchNoCase = kv_arr1 "BrowserMatchNoCase" "name" word
let customLog = 
  [ wskey "CustomLog" . sep . store word .
      [ sep . label "format" . store word ] .
      [ sep . label "env" . Util.del_str "=" . store /[^= \t\n]+/ ]? . eol ]
let imapMenu = kv1 "ImapMenu" /none|formatted|semiformatted|unformatted/
let indexOrderDefault = kv1 "IndexOrderDefault" /Ascending|Descending Name|Date|Size|Description/
let iSAPICacheFile = kv_arr "ISAPICacheFile" "file" word 
let limitExcept = kv_arr "LimitExcept" "nr" number 
let limitInternalRecursion = kv_arr "LimitInternalRecursion" "nr" number
let nWSSLTrustedCerts =  kv_arr "NWSSLTrustedCerts" "file" word
let nWSSLUpgradeable = kv_arr "NWSSLUpgradeable" "address" word
let multiviewsMatch = kv_val_arr "MultiViewsMatch" /Any|NegotiatedOnly|Filters|Handlers/ "param" /Handlers|Filters/ 
let cGIMapExtension =  kv2 "CGIMapExtension" word "extension" word
let errorDocument = kv2 "ErrorDocument" word "document" word
let filterDeclare = kv2_op "FilterDeclare" word "type" /RESOURCE|CONTENT_SET|PROTOCOL|TRANSCODE|CONNECTION|NETWORK/
let filterProtocol = [ wskey "FilterProtocol". sep .store word . 
                                 [sep . label "provider" . store alnum]?. 
                                 [sep . label "flags" . store /change=yes|change=1:1|byteranges=no|proxy=no|proxy=transform|cache=no/]* . eol ]
let redirect = [ wskey "Redirect" . 
      [ sep . label "status" . store /permanent|temp|seeother|gone/ ]? .
      [ sep . label "urlpath" . store word].
      [ sep . label "url" . store word] . eol ]
let redirectPermanent = [ wskey "RedirectPermanent" . 
      [ sep . label "urlpath" . store word ] .
      [ sep . label "url" . store word] .eol ]
let redirectTemp = [ wskey "RedirectTemp" . 
      [ sep . label "urlpath" . store word ] .
      [ sep . label "url" . store word] . eol ]
let redirectMatch = [ wskey "RedirectMatch" . 
      [ sep . label "status" . store /permanent|temp|seeother|gone/ ]? .
      [ sep . label "regex" . store word].
      [ sep . label "url" . store word] . eol ]
let rLimitCPU = [ wskey "RLimitCPU" . 
      [ sep . label "nr" . store (number|"max") ] .
      [ sep . label "nr" . store (number|"max")]? . eol ]
let rLimitMEM = [ wskey "RLimitMEM" . 
      [ sep . label "nr" . store (number|"max") ] .
      [ sep . label "nr" . store (number|"max")]? . eol ]
let rLimitNPROC= [ wskey "RLimitNPROC" . 
      [ sep . label "nr" . store (number|"max") ] .
      [ sep . label "nr" . store (number|"max")]? . eol ]
let script = [ wskey "Script" . 
      [ sep . label "method" . store word ] .
      [ sep . label "script" . store word] . eol ]
let secureListen = [ wskey "SecureListen" . 
      [ sep . label "address" . store word ] .
      [ sep . label "certName" . store word] .
      [ sep . label "mutual" . store word] . eol] 
let deny = allow_deny "Deny"
let directoryIndex = kv_arr "DirectoryIndex" "url" word
let forceLanguagePriority =
  let priority = [ sep . label "priority" . store /None|Prefer|Fallback/] in
  [ wskey "ForceLanguagePriority" . priority . priority? . eol ]
let group = kv1 "Group" alnum
let satisfy = kv1 "Satisfy" /Any|All/
let setHandler = kv1 "SetHandler" (word|"None")
let indexIgnore = kv_arr "IndexIgnore" "ignore" word
let passEnv = kv_arr "PassEnv" "var" word
let removeCharset = kv_arr "RemoveCharset" "extension" word
let removeEncoding = kv_arr "RemoveEncoding" "extension" word
let removeHandler = kv_arr "RemoveHandler" "extension" word
let removeInputFilter = kv_arr "RemoveInputFilter" "extension" word
let removeLanguage = kv_arr "RemoveLanguage" "extension" word
let removeOutputFilter = kv_arr "RemoveOutputFilter" "extension" word
let removeType = kv_arr "RemoveType" "extension" word
let setEnv = kv2 "SetEnv" alnum "val" word
let unsetEnv =  kv_arr "UnsetEnv" "env" word
let require = kv_arr "Require" "entity" word
let userDir= kv_arr "UserDir" "name" word
let indexOptions =
  let opt_re = /[+-]?(FancyIndexing|FoldersFirst|HTMLTable|IconsAreLinks|IgnoreCase|IgnoreClient|ScanHTMLTitles|Suppress(ColumnSorting|Description|HTMLPreamble|Icon|LastModified|Size|Rules)TrackModified|VersionSort|XHTML|ShowForbidden|None)/ in
  let opt_arg_re = /[+-]?(IconWidth|IconHeight|NameWidth|DescriptionWidth|Type|Charset)/ in
  [ wskey "IndexOptions" .
      ( [ sep . key opt_re ]
        | [ sep . key opt_arg_re . ( Util.del_str "=" . store /[^= \t\n]+/ )? ]
      )* .
      eol
  ]
let languagePriority = kv_arr "LanguagePriority" "lang" word
let loadModule = [ wskey "LoadModule" . sep . store modname .
                     sep . [ label "path" . store word ] . eol ]
let logFormat =
  [ wskey "LogFormat" . sep . store wordWithNestQuote .
      [ sep . label "nickname" . store word ]? . eol ]
let logLevel = kv1 "LogLevel" /emerg|alert|crit|error|warn|notice|info|debug/
let options =
  let opt_re = /[+-]?(None|Indexes|Includes(NOEXEC)?|FollowSymLinks|SymLinksIfOwnerMatch|ExecCGI|MultiViews|RunScripts|All)/ in
  kv_arr "Options" "option" opt_re
let order = kv1 "Order" /[aA]llow,[dD]eny|[dD]eny,[aA]llow|[mM]utual-failure/
let scriptAlias = kv2 "ScriptAlias" word "directory" word
let scriptAliasMatch = [ sep .key "ScriptAliasMatch" . 
      [ sep . label "regex" . store word ] .
      [ sep . label "path" . store word]? . eol ]
let serverSignature = kv1 "ServerSignature" (on_off | "EMail")
let traceEnable =  kv1 "TraceEnable" (on_off |"extended")
let useCanonicalName= kv1 "UseCanonicalName" (on_off |"DNS")
let xBitHack = kv1 "XBitHack" (on_off |"full")
let serverTokens =
  kv1 "ServerTokens" /Major|Minor|Min(imal)?|Prod(uctOnly)?|OS|Full/

let dirWithOnOfNm =    "KeepAlive"
                     | "HostnameLookups"
                     | "EnableSendFile"
                     | "ExtendedStatus"
                     | "CacheNegotiatedDocs"
                     | "EnableMMAP"
                     | "BufferedLogs"
                     | "AuthDefaultAuthoritative"
                     | "AuthzUserAuthoritative"
                     | "AuthBasicProvider"
                     | "AuthzGroupFileAuthoritative"
                     | "AllowEncodedSlashes"
                     | "AuthzDefaultAuthoritative"
                     | "AuthBasicAuthoritative"
                     | "ContentDigest"
                     | "UseCanonicalPhysicalPort"
                     | "ISAPIAppendLogToErrors"
                     | "ISAPIAppendLogToQuery"
                     | "ISAPIFakeAsync"
                     | "ISAPILogNotSupported"
                     | "ModMimeUsePathInfo"
                     | "SeeRequestTail"
                     | "SSIEnableAccess"
                   

let dirWithWordParam = "User"
                   | "TypesConfig"
                   | "ServerRoot"
                   | "ServerName"
                   | "ServerAdmin"
                   | "ReadmeName"
                   | "PidFile"
                   | "Include"
                   | "HeaderName"
                   | "DefaultType"
                   | "ErrorLog"
                   | "AuthUserFile"
                   | "AuthGroupFile"
                   | "DefaultIcon"
                   | "DocumentRoot"
                   | "DefaultLanguage"
                   | "AddDefaultCharset"
                   | "AuthName"
                   | /D[aA][vV]LockDB/
                   | /(MIME|Mime)MagicFile/
                   | "CookieLog"
                   | "DirectorySlash"
                   | "ForceType"
                   | "HeaderName"
                   | "ImapBase"
                   | "ImapDefault"
                   | "IndexHeadInsert"
                   | "IndexStyleSheet"
                   | "NameVirtualHost"
                   | "ScriptInterpreterSource"
                   | "ScriptLog"
                   | "ScriptSock"
                   | "ServerPath"
                   | "ServerRoot"
                   | "SetInputFilter"
                   | "SetOutputFilter"
                   | "SSIEndTag"
                   | "SSIErrorMsg"
                   | "SSIStartTag"
                   | "SSITimeFormat"
                   | "SSIUndefinedEcho"

let dirWithNrParam = "Timeout"
                     | "ThreadsPerChild"
                     | "StartServers"
                     | "ServerLimit"
                     | "MaxSpareServers"
                     | "MaxSpareThreads"
                     | "MinSpareServers"
                     | "MinSpareThreads"
                     | "MaxRequestsPerChild"
                     | "MaxKeepAliveRequests" 
                     | "KeepAliveTimeout"
                     | "MaxClients"
                     | "Listen"
                     | "ISAPIReadAheadBuffer"
                     | "LimitRequestBody"
                     | "LimitRequestFields"
                     | "LimitRequestFieldSize"
                     | "LimitRequestLine"
                     | "LimitXMLRequestBody"
                     | "ScriptLogBuffer"
                     | "ScriptLogLength"
                     | "TimeOut"
                     | "TransferLog"

let directiveWithOnOfParam = kv1 dirWithOnOfNm on_off
let directiveWithWordParam = kv1 dirWithWordParam word
let directiveWithNrParam = kv1 dirWithNrParam number

let directive =  accessFileName
              | addInputFilter
              | addCharset
              | cGIMapExtension
              | directiveWithOnOfParam
              | directiveWithWordParam
              | directiveWithNrParam
              | addEncoding
              | addAltByType
              | addAltByEncoding
              | action
              | authType
              | addLanguage
              | aliasMatch
              | acceptPathInfo
              | acceptFilter
              | addDescription
              | addIcon
              | addAlt
              | addIconByEncodingOrType
              | addType
              | addHandler
              | addOutputFilter
              | alias 
              | allow
              | allowOverride
              | browserMatch
              | browserMatchNoCase
              | customLog
              | deny
              | directoryIndex
              | forceLanguagePriority
              | group
              | indexIgnore
              | indexOptions
              | languagePriority
              | loadModule
              | logFormat
              | logLevel
              | options
              | order
              | scriptAlias
              | serverTokens
              | errorDocument
              | filterDeclare
              | filterProtocol
              | filterProvider
              | filterTrace
              | imapMenu
              | indexOrderDefault
              | iSAPICacheFile
              | limitExcept
              | limitInternalRecursion
              | multiviewsMatch
              | nWSSLTrustedCerts
              | nWSSLUpgradeable
              | passEnv
              | redirect
              | redirectMatch
              | redirectPermanent
              | redirectTemp
              | removeCharset
              | removeHandler
              | removeEncoding
              | removeInputFilter
              | removeLanguage
              | removeOutputFilter
              | removeType
              | require
              | rLimitCPU
              | rLimitMEM
              | rLimitNPROC
              | satisfy
              | script
              | scriptAliasMatch
              | secureListen
              | serverAlias
              | serverSignature
              | setEnv
              | setHandler
              | traceEnable
              | unsetEnv
              | useCanonicalName
              | userDir
              | xBitHack 

(* Containers *)

(* A section with one argument in the <Section ..> tag *)
let sec1 (name:string) (body:lens) =
  [ langle . key name . sep . store secarg . rangle . eol .
      body .
      Util.del_str ("</" . name . ">") . eol
  ]

let secN (name:string) (args:lens) (body:lens) = 
    [ langle . key name . sep . args . rangle . eol . 
        body . 
        Util.del_str("</" . name . ">") . eol ]

(* A helper for <Directory>-like sections that can optionally have ~ preceeding the 
   path argument making httpd interpret it as a regex *)
let pathSpec (l:string) =
    [ label l . Util.del_str "~" . sep ]? . store secarg

let ifModule (body:lens) = sec1 "IfModule" body
let directory (body:lens) = secN "Directory" (pathSpec "Directory_regexp") body
let directoryMatch (body:lens) = sec1 "DirectoryMatch" body
let virtualHost (body:lens) = sec1 "VirtualHost" body
let files (body:lens) = secN "Files" (pathSpec "Files_regexp") body
let filesMatch (body:lens) = sec1 "FilesMatch" body
let location (body:lens) = secN "Location" (pathSpec "Location_regexp") body
let locationMatch (body:lens) = sec1 "LocationMatch" body

let anySection(body:lens) = ifModule body | directory body | directoryMatch body | 
    virtualHost body | files body | filesMatch body | location body | locationMatch body

(* What we want ot say is *)
(* let rec body = (directive|comment)* | ifModule body | directory body | ... *)
(* but we can't typecheck that *)

(* FIXME: *)
(* - Nesting of sections *)
(* - comments inside sections *)
let prim = (directive | comment)
let lns = (prim | anySection prim*)*

let filter =
    incl "/etc/httpd/conf/httpd.conf" .
    Util.stdexcl

let xfm = transform lns filter
