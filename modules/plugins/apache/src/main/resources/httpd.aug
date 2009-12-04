module Httpd =
    autoload xfm
(* FIXME: up to line 737 in httpd.conf *)

(* FIXME: Most of the comparisons in Apache are case insensitive       *)
(* We do not have a way to express that currently; probably need some *)
(* additions to the regexp syntax like /[a-z]+/i                      *)

(* This is the list of all httpd directives taken from http://httpd.apache.org/docs/2.2/mod/directives.html *)
(* If we wanted a simple lens, we could make do with a simple generic "directive" rule that would match any directive and
   its arguments + a generic section that would do ditto. But because we want to support rich parsing and parameter naming, 
   we have to create a rule for each directive that we want special treatment for. This brings along an interesting problem
   of coming up with "catch-all" directive that would match any "non-special" directives/sections. 
   A construct along the lines of " /[a-zA-Z0-9]+/ - allSpecialDirectiveNames " doesn't work for some reason because Augeas
   errors out with memory exhaustion error. Thus we have to have a list of all directives there are... *)
let allDirectives = "AcceptFilter"
                  | "AcceptMutex"
                  | "AcceptPathInfo"
                  | "AccessFileName"
                  | "Action"
                  | "AddAlt"
                  | "AddAltByEncoding"
                  | "AddAltByType"
                  | "AddCharset"
                  | "AddDefaultCharset"
                  | "AddDescription"
                  | "AddEncoding"
                  | "AddHandler"
                  | "AddIcon"
                  | "AddIconByEncoding"
                  | "AddIconByType"
                  | "AddInputFilter"
                  | "AddLanguage"
                  | "AddModuleInfo"
                  | "AddOutputFilter"
                  | "AddOutputFilterByType"
                  | "AddType"
                  | "Alias"
                  | "AliasMatch"
                  | "Allow"
                  | "AllowCONNECT"
                  | "AllowEncodedSlashes"
                  | "AllowOverride"
                  | "Anonymous"
                  | "Anonymous_LogEmail"
                  | "Anonymous_MustGiveEmail"
                  | "Anonymous_NoUserID"
                  | "Anonymous_VerifyEmail"
                  | "AuthBasicAuthoritative"
                  | "AuthBasicProvider"
                  | "AuthDBDUserPWQuery"
                  | "AuthDBDUserRealmQuery"
                  | "AuthDBMGroupFile"
                  | "AuthDBMType"
                  | "AuthDBMUserFile"
                  | "AuthDefaultAuthoritative"
                  | "AuthDigestAlgorithm"
                  | "AuthDigestDomain"
                  | "AuthDigestNcCheck"
                  | "AuthDigestNonceFormat"
                  | "AuthDigestNonceLifetime"
                  | "AuthDigestProvider"
                  | "AuthDigestQop"
                  | "AuthDigestShmemSize"
                  | "AuthGroupFile"
                  | "AuthLDAPBindDN"
                  | "AuthLDAPBindPassword"
                  | "AuthLDAPCharsetConfig"
                  | "AuthLDAPCompareDNOnServer"
                  | "AuthLDAPDereferenceAliases"
                  | "AuthLDAPGroupAttribute"
                  | "AuthLDAPGroupAttributeIsDN"
                  | "AuthLDAPRemoteUserAttribute"
                  | "AuthLDAPRemoteUserIsDN"
                  | "AuthLDAPUrl"
                  | "AuthName"
                  | "AuthType"
                  | "AuthUserFile"
                  | "AuthzDBMAuthoritative"
                  | "AuthzDBMType"
                  | "AuthzDefaultAuthoritative"
                  | "AuthzGroupFileAuthoritative"
                  | "AuthzLDAPAuthoritative"
                  | "AuthzOwnerAuthoritative"
                  | "AuthzUserAuthoritative"
                  | "BalancerMember"
                  | "BrowserMatch"
                  | "BrowserMatchNoCase"
                  | "BufferedLogs"
                  | "CacheDefaultExpire"
                  | "CacheDirLength"
                  | "CacheDirLevels"
                  | "CacheDisable"
                  | "CacheEnable"
                  | "CacheFile"
                  | "CacheIgnoreCacheControl"
                  | "CacheIgnoreHeaders"
                  | "CacheIgnoreNoLastMod"
                  | "CacheIgnoreQueryString"
                  | "CacheIgnoreURLSessionIdentifiers"
                  | "CacheLastModifiedFactor"
                  | "CacheMaxExpire"
                  | "CacheMaxFileSize"
                  | "CacheMinFileSize"
                  | "CacheNegotiatedDocs"
                  | "CacheRoot"
                  | "CacheStoreNoStore"
                  | "CacheStorePrivate"
                  | "CGIMapExtension"
                  | "CharsetDefault"
                  | "CharsetOptions"
                  | "CharsetSourceEnc"
                  | "CheckCaseOnly"
                  | "CheckSpelling"
                  | "ChrootDir"
                  | "ContentDigest"
                  | "CookieDomain"
                  | "CookieExpires"
                  | "CookieLog"
                  | "CookieName"
                  | "CookieStyle"
                  | "CookieTracking"
                  | "CoreDumpDirectory"
                  | "CustomLog"
                  | "Dav"
                  | "DavDepthInfinity"
                  | "DavGenericLockDB"
                  | "DavLockDB"
                  | "DavMinTimeout"
                  | "DBDExptime"
                  | "DBDKeep"
                  | "DBDMax"
                  | "DBDMin"
                  | "DBDParams"
                  | "DBDPersist"
                  | "DBDPrepareSQL"
                  | "DBDriver"
                  | "DefaultIcon"
                  | "DefaultLanguage"
                  | "DefaultType"
                  | "DeflateBufferSize"
                  | "DeflateCompressionLevel"
                  | "DeflateFilterNote"
                  | "DeflateMemLevel"
                  | "DeflateWindowSize"
                  | "Deny"
                  | "DirectoryIndex"
                  | "DirectorySlash"
                  | "DocumentRoot"
                  | "DumpIOInput"
                  | "DumpIOLogLevel"
                  | "DumpIOOutput"
                  | "EnableExceptionHook"
                  | "EnableMMAP"
                  | "EnableSendfile"
                  | "ErrorDocument"
                  | "ErrorLog"
                  | "Example"
                  | "ExpiresActive"
                  | "ExpiresByType"
                  | "ExpiresDefault"
                  | "ExtendedStatus"
                  | "ExtFilterDefine"
                  | "ExtFilterOptions"
                  | "FileETag"
                  | "FilterChain"
                  | "FilterDeclare"
                  | "FilterProtocol"
                  | "FilterProvider"
                  | "FilterTrace"
                  | "ForceLanguagePriority"
                  | "ForceType"
                  | "ForensicLog"
                  | "GracefulShutdownTimeout"
                  | "Group"
                  | "Header"
                  | "HeaderName"
                  | "HostnameLookups"
                  | "IdentityCheck"
                  | "IdentityCheckTimeout"
                  | "ImapBase"
                  | "ImapDefault"
                  | "ImapMenu"
                  | "Include"
                  | "IndexHeadInsert"
                  | "IndexIgnore"
                  | "IndexOptions"
                  | "IndexOrderDefault"
                  | "IndexStyleSheet"
                  | "ISAPIAppendLogToErrors"
                  | "ISAPIAppendLogToQuery"
                  | "ISAPICacheFile"
                  | "ISAPIFakeAsync"
                  | "ISAPILogNotSupported"
                  | "ISAPIReadAheadBuffer"
                  | "KeepAlive"
                  | "KeepAliveTimeout"
                  | "LanguagePriority"
                  | "LDAPCacheEntries"
                  | "LDAPCacheTTL"
                  | "LDAPConnectionTimeout"
                  | "LDAPOpCacheEntries"
                  | "LDAPOpCacheTTL"
                  | "LDAPSharedCacheFile"
                  | "LDAPSharedCacheSize"
                  | "LDAPTrustedClientCert"
                  | "LDAPTrustedGlobalCert"
                  | "LDAPTrustedMode"
                  | "LDAPVerifyServerCert"
                  | "LimitInternalRecursion"
                  | "LimitRequestBody"
                  | "LimitRequestFields"
                  | "LimitRequestFieldSize"
                  | "LimitRequestLine"
                  | "LimitXMLRequestBody"
                  | "Listen"
                  | "ListenBackLog"
                  | "LoadFile"
                  | "LoadModule"
                  | "LockFile"
                  | "LogFormat"
                  | "LogLevel"
                  | "MaxClients"
                  | "MaxKeepAliveRequests"
                  | "MaxMemFree"
                  | "MaxRequestsPerChild"
                  | "MaxRequestsPerThread"
                  | "MaxSpareServers"
                  | "MaxSpareThreads"
                  | "MaxThreads"
                  | "MCacheMaxObjectCount"
                  | "MCacheMaxObjectSize"
                  | "MCacheMaxStreamingBuffer"
                  | "MCacheMinObjectSize"
                  | "MCacheRemovalAlgorithm"
                  | "MCacheSize"
                  | "MetaDir"
                  | "MetaFiles"
                  | "MetaSuffix"
                  | "MimeMagicFile"
                  | "MinSpareServers"
                  | "MinSpareThreads"
                  | "MMapFile"
                  | "ModMimeUsePathInfo"
                  | "MultiviewsMatch"
                  | "NameVirtualHost"
                  | "NoProxy"
                  | "NWSSLTrustedCerts"
                  | "NWSSLUpgradeable"
                  | "Options"
                  | "Order"
                  | "PassEnv"
                  | "PidFile"
                  | "ProtocolEcho"
                  | "ProxyBadHeader"
                  | "ProxyBlock"
                  | "ProxyDomain"
                  | "ProxyErrorOverride"
                  | "ProxyFtpDirCharset"
                  | "ProxyIOBufferSize"
                  | "ProxyMaxForwards"
                  | "ProxyPass"
                  | "ProxyPassInterpolateEnv"
                  | "ProxyPassMatch"
                  | "ProxyPassReverse"
                  | "ProxyPassReverseCookieDomain"
                  | "ProxyPassReverseCookiePath"
                  | "ProxyPreserveHost"
                  | "ProxyReceiveBufferSize"
                  | "ProxyRemote"
                  | "ProxyRemoteMatch"
                  | "ProxyRequests"
                  | "ProxySCGIInternalRedirect"
                  | "ProxySCGISendfile"
                  | "ProxySet"
                  | "ProxyStatus"
                  | "ProxyTimeout"
                  | "ProxyVia"
                  | "ReadmeName"
                  | "ReceiveBufferSize"
                  | "Redirect"
                  | "RedirectMatch"
                  | "RedirectPermanent"
                  | "RedirectTemp"
                  | "RemoveCharset"
                  | "RemoveEncoding"
                  | "RemoveHandler"
                  | "RemoveInputFilter"
                  | "RemoveLanguage"
                  | "RemoveOutputFilter"
                  | "RemoveType"
                  | "RequestHeader"
                  | "Require"
                  | "RewriteBase"
                  | "RewriteCond"
                  | "RewriteEngine"
                  | "RewriteLock"
                  | "RewriteLog"
                  | "RewriteLogLevel"
                  | "RewriteMap"
                  | "RewriteOptions"
                  | "RewriteRule"
                  | "RLimitCPU"
                  | "RLimitMEM"
                  | "RLimitNPROC"
                  | "Satisfy"
                  | "ScoreBoardFile"
                  | "Script"
                  | "ScriptAlias"
                  | "ScriptAliasMatch"
                  | "ScriptInterpreterSource"
                  | "ScriptLog"
                  | "ScriptLogBuffer"
                  | "ScriptLogLength"
                  | "ScriptSock"
                  | "SecureListen"
                  | "SeeRequestTail"
                  | "SendBufferSize"
                  | "ServerAdmin"
                  | "ServerAlias"
                  | "ServerLimit"
                  | "ServerName"
                  | "ServerPath"
                  | "ServerRoot"
                  | "ServerSignature"
                  | "ServerTokens"
                  | "SetEnv"
                  | "SetEnvIf"
                  | "SetEnvIfNoCase"
                  | "SetHandler"
                  | "SetInputFilter"
                  | "SetOutputFilter"
                  | "SSIEnableAccess"
                  | "SSIEndTag"
                  | "SSIErrorMsg"
                  | "SSIStartTag"
                  | "SSITimeFormat"
                  | "SSIUndefinedEcho"
                  | "SSLCACertificateFile"
                  | "SSLCACertificatePath"
                  | "SSLCADNRequestFile"
                  | "SSLCADNRequestPath"
                  | "SSLCARevocationFile"
                  | "SSLCARevocationPath"
                  | "SSLCertificateChainFile"
                  | "SSLCertificateFile"
                  | "SSLCertificateKeyFile"
                  | "SSLCipherSuite"
                  | "SSLCryptoDevice"
                  | "SSLEngine"
                  | "SSLHonorCipherOrder"
                  | "SSLMutex"
                  | "SSLOptions"
                  | "SSLPassPhraseDialog"
                  | "SSLProtocol"
                  | "SSLProxyCACertificateFile"
                  | "SSLProxyCACertificatePath"
                  | "SSLProxyCARevocationFile"
                  | "SSLProxyCARevocationPath"
                  | "SSLProxyCheckPeerCN"
                  | "SSLProxyCheckPeerExpire"
                  | "SSLProxyCipherSuite"
                  | "SSLProxyEngine"
                  | "SSLProxyMachineCertificateFile"
                  | "SSLProxyMachineCertificatePath"
                  | "SSLProxyProtocol"
                  | "SSLProxyVerify"
                  | "SSLProxyVerifyDepth"
                  | "SSLRandomSeed"
                  | "SSLRenegBufferSize"
                  | "SSLRequire"
                  | "SSLRequireSSL"
                  | "SSLSessionCache"
                  | "SSLSessionCacheTimeout"
                  | "SSLStrictSNIVHostCheck"
                  | "SSLUserName"
                  | "SSLVerifyClient"
                  | "SSLVerifyDepth"
                  | "StartServers"
                  | "StartThreads"
                  | "Substitute"
                  | "SuexecUserGroup"
                  | "ThreadLimit"
                  | "ThreadsPerChild"
                  | "ThreadStackSize"
                  | "TimeOut"
                  | "TraceEnable"
                  | "TransferLog"
                  | "TypesConfig"
                  | "UnsetEnv"
                  | "UseCanonicalName"
                  | "UseCanonicalPhysicalPort"
                  | "User"
                  | "UserDir"
                  | "VirtualDocumentRoot"
                  | "VirtualDocumentRootIP"
                  | "VirtualScriptAlias"
                  | "VirtualScriptAliasIP"
                  | "Win32DisableAcceptEx"
                  | "XBitHack"

(* Now let the specialization commence *)

(* Newlines can be escaped *)
let sep = del /([ \t]+(\\\\\n)?)+/ " "
let eol = del /([ \t]*(\\\\\n)?)*\n/ "\n"
let number = /[0-9]+/
let on_off = /[Oo](n|ff)/
let langle = del /[ \t]*</ "<"
let rangle = Util.del_str ">"
let modname = /[a-zA-Z0-9._]+/
let alnum = /[a-zA-Z0-9]+/
(* let word = /"([^"\n])*"|'([^'\n])*'|[^'" \t\n]+/ *)
let word = /"([^"\n]|\\\\")*"|'([^'\n])*'|[^'" \t\n]+/
let wordWithNestQuote = word

(* A that does not end with ">" *)
let secargBody = /\"([^\"\n]|\\\\\")*\"|'([^'\n]|\\\\')*'|[^'" \t\n>]+/
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
let serverAlias = kv_arr1 "ServerAlias" "hostname" word
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
      [ sep . key "env" . Util.del_str "=" . store (word - "=") ]? . eol ]
let imapMenu = kv1 "ImapMenu" /none|formatted|semiformatted|unformatted/
let indexOrderDefault = kv1 "IndexOrderDefault" /Ascending|Descending Name|Date|Size|Description/
let iSAPICacheFile = kv_arr "ISAPICacheFile" "file" word 
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
let setEnv = kv2 "SetEnv" word "val" word
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
let scriptAliasMatch = [ wskey "ScriptAliasMatch" . 
      [ sep . label "regex" . store word ] .
      [ sep . label "path" . store word]? . eol ]
let serverSignature = kv1 "ServerSignature" (on_off | "EMail")
let traceEnable =  kv1 "TraceEnable" (on_off |"extended")
let useCanonicalName= kv1 "UseCanonicalName" (on_off |"DNS")
let xBitHack = kv1 "XBitHack" (on_off |"full")
let serverTokens =
  kv1 "ServerTokens" /Major|Minor|Min(imal)?|Prod(uctOnly)?|OS|Full/
let listen = [ wskey "Listen" . sep .
    [ label "ip" . store /([0-9\.]+)|(\[[0-9a-fA-F:]+\])/ . Util.del_str ":" ]? .
    [ label "port" . store number ] .
    [ sep . label "scheme" . store alnum ]? . eol ]

(* this is a list of the names of the directives defined using the special rules above.
   We use this to construct the generic catch-all rule that will match "everything we don't know better *)
let definedDirectiveNames = "AddAltByEncoding"
                          | "AddAltByType"
                          | "AddCharset"
                          | "AccessFileName"
                          | "AcceptFilter"
                          | "AcceptPathInfo"
                          | "AddDescription"
                          | "AddEncoding"
                          | "ServerAlias"
                          | "AddType"
                          | "AuthType"
                          | "Action"
                          | "AddAlt"
                          | "FilterProvider"
                          | "FilterTrace"
                          | "AddIcon"
                          | "AddInputFilter"
                          | /AddIconBy(Encoding|Type)/
                          | "AddLanguage"
                          | "Alias"
                          | "AliasMatch"
                          | "Allow"
                          | "AllowOverride"
                          | "AddHandler"
                          | "AddOutputFilter"
                          | "AddOutputFilterByType"
                          | "BrowserMatch"
                          | "BrowserMatchNoCase"
                          | "CustomLog"
                          | "ImapMenu"
                          | "IndexOrderDefault"
                          | "ISAPICacheFile"
                          | "LimitExcept"
                          | "LimitInternalRecursion"
                          | "NWSSLTrustedCerts"
                          | "NWSSLUpgradeable"
                          | "MultiViewsMatch"
                          | "CGIMapExtension"
                          | "ErrorDocument"
                          | "FilterDeclare"
                          | "FilterProtocol"
                          | "Redirect"
                          | "RedirectPermanent"
                          | "RedirectTemp"
                          | "RedirectMatch"
                          | "RLimitCPU"
                          | "RLimitMEM"
                          | "RLimitNPROC"
                          | "Script"
                          | "SecureListen"
                          | "Deny"
                          | "DirectoryIndex"
                          | "ForceLanguagePriority"
                          | "Group"
                          | "Satisfy"
                          | "SetHandler"
                          | "IndexIgnore"
                          | "PassEnv"
                          | "RemoveCharset"
                          | "RemoveEncoding"
                          | "RemoveHandler"
                          | "RemoveInputFilter"
                          | "RemoveLanguage"
                          | "RemoveOutputFilter"
                          | "RemoveType"
                          | "SetEnv"
                          | "UnsetEnv"
                          | "Require"
                          | "UserDir"
                          | "IndexOptions"
                          | "LanguagePriority"
                          | "LoadModule"
                          | "LogFormat"
                          | "LogLevel"
                          | "Options"
                          | "Order"
                          | "ScriptAlias"
                          | "ScriptAliasMatch"
                          | "ServerSignature"
                          | "TraceEnable"
                          | "UseCanonicalName"
                          | "XBitHack"
                          | "ServerTokens"
                          | "Listen"
    
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
                   | "TransferLog"

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
                     | "ISAPIReadAheadBuffer"
                     | "LimitRequestBody"
                     | "LimitRequestFields"
                     | "LimitRequestFieldSize"
                     | "LimitRequestLine"
                     | "LimitXMLRequestBody"
                     | "ScriptLogBuffer"
                     | "ScriptLogLength"
                     | "TimeOut"

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
              | addOutputFilterByType
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
              | listen
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

let secN (name:string) (args:lens) (body:lens) = 
    [ langle . key name . args . rangle . eol . 
        body . 
        del (/[ \t]*<\// . name . ">") ("</" . name . ">") . eol ]

(* A section with one argument in the <Section ..> tag *)
let sec1 (name:string) (body:lens) = secN name (sep . store secarg) body

(* A helper for <Directory>-like sections that can optionally have ~ preceeding the 
   path argument making httpd interpret it as a regex *)
let pathSpec (l:string) =
    [ sep . label l . Util.del_str "~" ]? . sep . store secarg

(* these are all the sections there are possible in the Apache configuration according to the Apache 2.2 documentation *)
let ifModule (body:lens) = sec1 "IfModule" body
let directory (body:lens) = secN "Directory" (pathSpec "Directory_regexp") body
let directoryMatch (body:lens) = sec1 "DirectoryMatch" body
let virtualHost (body:lens) = sec1 "VirtualHost" body
let files (body:lens) = secN "Files" (pathSpec "Files_regexp") body
let filesMatch (body:lens) = sec1 "FilesMatch" body
let location (body:lens) = secN "Location" (pathSpec "Location_regexp") body
let locationMatch (body:lens) = sec1 "LocationMatch" body
let authnProviderAlias (body:lens) = secN "AuthnProviderAlias" ([sep . label "baseProvider" . store word ] . [sep .  label "alias" . store word ]) body
let ifDefine (body:lens) = sec1 "IfDefine" body
let ifVersion (body:lens) = secN "IfVersion" ([sep . label "operator" . store /[<>=]+/ ] . sep . [label "version" . store /[0-9\.]+/ ]) body
let limit (body:lens) = secN "Limit" 
    [ sep . label "method" . store /GET|POST|PUT|DELETE|CONNECT|OPTIONS|PATCH|PROPFIND|PROPPATCH|MKCOL|COPY|MOVE|LOCK|UNLOCK/ ]+
    body
let limitExcept (body:lens) = secN "LimitExcept" [sep . label "method" . store alnum ]+ body
let proxy (body:lens) = sec1 "Proxy" body
let proxyMatch (body:lens) = sec1 "ProxyMatch" body

let sectionsWithoutVirtualHost (body:lens) = ifModule body | directory body | directoryMatch body | 
    files body | filesMatch body | location body | locationMatch body | authnProviderAlias body |
    ifDefine body | ifVersion body | limit body | limitExcept body | proxy body | proxyMatch body

let restOfDirectives = [ wskey (allDirectives - definedDirectiveNames - dirWithNrParam - dirWithOnOfNm - dirWithWordParam) .
    [ sep . label "param" . store wordWithNestQuote ]* . eol ]

(* What we want ot say is *)
(* let rec body = (directive|comment)* | ifModule body | directory body | ... *)
(* but we can't typecheck that *)

(* FIXME: *)
(* - Nesting of sections *)
(* - comments inside sections *)
let prim = ( directive | restOfDirectives | comment)
let directiveOrSectionWithoutVirtualHost = prim | sectionsWithoutVirtualHost prim*
let directiveOrSection = directiveOrSectionWithoutVirtualHost | virtualHost directiveOrSectionWithoutVirtualHost*
let lns = directiveOrSection*

let filter =
    incl "/etc/httpd/conf/httpd.conf" .
    Util.stdexcl

let xfm = transform lns filter
