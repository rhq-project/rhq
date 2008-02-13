/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.license;

/**
 * Munged error messages and other resource strings used by licensing code. These string are munged because a typical
 * circumvention technique is to search class files (decompiled or not) for strings known to be used by licensing code.
 *
 * <p/>These strings are munged with License.munge
 */
final class LRES {
    // "license.xml"
    static final String LICENSE_LOCATION = "R73u7y2Fk#:";

    // "License Error: "
    static final String HALT_MSG = "=}BS}cW)5,_~mb)";

    // "Unexpected exception while initialzing license file"
    static final String ERR_INIT_EXC = "K7 v:<n4h(]S6nuRjUc}IMx}dS-$\\ojUAQN57w]qTnu7y2/,$:h";

    // "Unexpected error while initialzing license file"
    static final String ERR_INIT_ERROR = "{rqJEh>]dUbu.z,w^?boGG(7[5w}>RZs\\4XE\\CQ\\m $I7Au";

    // "Error reading license file"
    static final String ERR_MSG_INIT = "x1kl@mp% j-]~y=]d[]ls.c/t\\";

    // "Error discovering IP addresses on server"
    static final String ERR_MSG_NETIF = "=)X8/P5gIH8`:Ig=(}J_TL'[]A8;nIo8tP;n)3i/";

    // "org.rhq.enterprise.server.license.LicenseManager"
    static final String JNDI_NAME = "\\hj+]`Gu.}B#/Xe`KC3~Uem)i_<)eD}BS}cuHWUV<U{hqio:.u.";

    // "org.rhq.enterprise.server.license.LicenseManager"
    static final String LOG_NAME = "\\hj+]`Gu.}B#/Xe`KC3~Uem)i_<)eD}BS}cuHWUV<U{hqio:.u.";

    // "org.rhq.enterprise.server.license.LicenseStoreManager"
    static final String STORE_LOG_NAME = "}=,Cwnh<m(R3j4LnkBVa^LcIdKOILu(Rt(@<&%^iO^|=j_@|fhDPd_Oc";

    // "Unable to get initial JNDI context"
    static final String ERR_MSG_JNDIIC = "HwfYs$J@ecpT6J8w.v,'oKgx{RktZwpP>6";

    // "Unable to perform JNDI rebind operation"
    static final String ERR_MSG_JNDIBIND = "w<&mBAPT45Ci/]Ix?o&u\\|s)Ry%O%swCi/YTgwW";

    // "Unable to perform JNDI lookup"
    static final String ERR_MSG_JNDILOOKUP = "M]=Lro|c?mg5!vl@Dy=^,3{fF9=*<";

    // "No licensed IP address found on server"
    static final String ERR_MSG_NOIPS = ":Is`O\"A2/n'ok,\\#r'Xij]7^wsg7\\I<54i/L0x";

    // "Error closing SIGAR library"
    static final String ERR_MSG_SIGARCLOSE = ")kJ?ay W6!I/1*_Cj4Iyr]HJ<ad";

    // "The time-limited license for this software has expired.  Please contact support to renew your license."
    static final String ERR_MSG_EXPIRED = "F,\\mkjq'CO4Gj@'8{fw o)!\\m@9!,c_4?*F61~BWvo,,<ly5CS-@7ziK,FO7WMo,q?/kg2|_6_gtZkc{1F*!'X\\ByqZ.J{fw o)!\\E";

    // "Invalid attribute index"
    static final String MSG_BADINDEX = "+( hW/8jQy*1/\"4mf.])8t9";

    // "Error parsing expiration date"
    static final String MSG_ERRPARSE = "]@aFvKfS@lwIJ|[#Wwvta-?/y,ta[";

    // "Invalid platform count"
    static final String ERR_MSG_BADCOUNT = "\"$hu=]jwQSu*c6*V8:v;)_";

    // "Invalid support level"
    static final String ERR_MSG_BADLEVEL = "HTuQ<Xw414uhv1,w2O %W";

    // "*"
    static final String IP_ANY = "l";

    // "*"
    static final String PLATFORMS_UNLIMITED = "l";

    // "*"
    static final String EXPIRATION_NEVER = "l";

    // "*"
    static final String PLUGIN_ANY = "l";

    // "license"
    static final String TAGNAME_LICENSE = "VrLGr,Q";

    // "license-owner"
    static final String TAGNAME_LICENSEE = "A5` 5K<!yFs2_";

    // "license-expiration"
    static final String TAGNAME_EXPIRATION = "qTnWTkOF<ld^KQ;T1P";

    // "platform-limit"
    static final String TAGNAME_PLATFORMLIMIT = "EDR8z~mLlGU3Tz";

    // "support-level"
    static final String TAGNAME_SUPPORTLEVEL = "_^Ri|_.!:hA2q";

    // "server-ip"
    static final String TAGNAME_SERVERIP = ";GwnQ;l'B";

    // "plugin-enable"
    static final String TAGNAME_PLUGINENABLE = "n:bwUo9Q\\EadS";

    // "address"
    static final String ATTR_ADDRESS = "3s54d,_";

    // "revokes"
    static final String ATTR_REVOKES = "/:B,Yq_";

    // "count"
    static final String ATTR_COUNT = "H8oOX";

    // "name"
    static final String ATTR_NAME = "=NFA";

    // "email"
    static final String ATTR_EMAIL = "RF3rn";

    // "phone"
    static final String ATTR_PHONE = "Co;OD";

    // "yyyy-MM-dd"
    static final String DFORMAT = "\"+#L6dqa}U";

    // "##0"
    static final String NFORMAT = "A:=";

    // "date"
    static final String ATTR_DATE = "'NbA";

    // "level"
    static final String ATTR_LEVEL = ">i`:n";

    // "key"
    static final String ATTR_KEY = "\"EF";

    // "version"
    static final String ATTR_VERSION = "`:I.\\{[";

    // "No value to validate"
    static final String ERR_NOVALUE = "rkwuQ<8=4_@8 hu(]qmf";

    // "Error validating license: key mismatch"
    static final String ERR_VALIDATION = "SbxwX}`+3g'Nb%2Ps`O\"A2/nfoLA67lt4F3P97";

    // "Invalid server-ip element"
    static final String ERR_INVALIDSERVERIP = "LbS2tI.;ark=\\a#X2,[O7n%x|";

    // "Invalid license-owner element"
    static final String ERR_INVALIDLICENSEE = "3]gWr8{_O4h549[3MVIo6_\\fsdowc";

    // "Key validation error"
    static final String ERR_KEYVALIDATION = "VWVjhu=$Xd{xpbIW*Kac";

    // "Key generation error"
    static final String ERR_KEYGENERATION = "VWVj{fb=yd{xpbIW*Kac";

    // "Invalid server-ip count"
    static final String ERR_SERVERIPCOUNT = "+( hW/8jJf~S'J\"xh.G6z$m";

    // "Warning: software license expires soon!  Contact support to renew your license.  Current license expires on "
    static final String WARN_EXPIRATION = "kOl8,.6.|Z\"l6Dt6U~s_towZP1$VO;lPHk+Zej[1k-Zwp%W6JF*%r+FlKaH~M$zoEc +~9K[.=T.+o:c~]~9!}jv15{2}jeTk(Cf.?T\"JZwc";

    // "Error: software license validation failed.  Please contact support to resolve the issue."
    static final String ERR_FAILED_VALIDATION = "7.z|_(I{*;8e:mu)GUV<U{hX:AQ$TEjUc}IjE}dSx&x)~dS:cu)>|o.DYwX*XDGy.j];~I;hy|qG=xw)2/Tc{b +";

    // "Error: software license has become corrupt.  Please contact support to resolve the issue."
    static final String ERR_CORRUPT_VALIDATION = "sz,c{^4mK.;HDyhXdP0=Pm ]]:c4@ BcY=4`K,_]G,HX]kQ=Gm ]V~U,E>8/|/dRK,;-zk)z2K~uqhX8]<I(my)SL";

    // "Error: software license key has become corrupt.  Please contact support to resolve the issue."
    static final String ERR_CORRUPT_KEY = "UmyJKX;|1{_LQc<Ih^iO^|=4:tn;/D~wMO:~`Wwiac{/d_38;JuWdkO;01P_qAy-c-qQac.I,Jj|f|1hQt8y/=4^kJj<&";

    // "Error: software license trial has become corrupt.  Please contact support to resolve the issue."
    static final String ERR_CORRUPT_TRIAL = "T*Kac/z~JymNh1Ww2x:%x~gjmcX hwwQ@.eWR@0f.Gk*Kwh*B4wvSfhpWwia^* E_jJj 2k*{8yvzygka<2r4_wO;Xp~ItY";

    // "Error: trial software licenses must be updated by term licenses only.  Please contact support to solve the issue."
    static final String ERR_TRIAL_VALIDATION = "WX];-\\$(Xf30T.wxbaYIG5>f+:f.E4}9};]5miU}>o&^i7P|\"5^i/ZTEtHit8D.5jgVe?$5.BAY;G5H8t)`&(owo`>,)^}x.T4E48V>D$(5iUr;.7R!";

    // salt used to calculate md5 keys for license terms - gibberish munged or not
    static final String our_salt = "RHQqBr,f{,{XO#P)2di^<}=KaLmhrUB3\"6Q_}E%Z7%d3FP$,E:DN/CEBdW}F\\D)1+5";

    // "Cannot register new platform due to licensing restrictions.  Contact support to license additional platforms.  Current license permits a maximum platform count of "
    static final String ERR_MSG_TOOMANYPLATFORMS = "tR5o~z4. jP|zW.X5SHIdDR8z~m>)()S-zk)GUV<U~osj/{=~wzUV.$k7y+/-'k7jiV.I~biAc{z4wK]qTnW7y2/:)XojUc}Dh)idA._k.N*#-I[bz,S}z4D}BS}cW)i2_Y$,mXi/YD?oN)3-GhEj.c{`4`K)o.Ik8X";

    // "[License owner="
    static final String TOSTRING_START = ",WUV<U~hX|e}=y-";

    // " email="
    static final String EMAIL = "U:e>\\i]";

    // " phone="
    static final String PHONE = "U`P,rq]";

    // " expiration="
    static final String EXPIRATION = "^Q@R},A-sy7I";

    // "never"
    static final String NEVER = "Wi`:I";

    // "any"
    static final String ANY = "L=F";

    // "all"
    static final String ALL = "L`>";

    // " platforms="
    static final String TOSTRING_PLATFORMS = "(0Anw;|w+_/";

    // "unlimited"
    static final String UNLIMITED = "U%R7#5wDo";

    // " serverIPs="
    static final String TOSTRING_SERVERIPS = "(,Q;i ,v*_/";

    // " plugins="
    static final String TOSTRING_PLUGINS = "TBR$-57;x";

    // " support="
    static final String TOSTRING_SUPPORT = "T.T0ny.Xx";

    // "]"
    static final String TOSTRING_END = "m";

    protected static String get(String res) {
        return License.unmunge(res);
    }
}