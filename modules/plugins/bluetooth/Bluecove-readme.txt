
About BlueCove

BlueCove is a LGPL licensed JSR-82 implementation on Java Standard Edition (J2SE) that currently interfaces with the WIDCOMM, BlueSoleil and Microsoft Bluetooth stack. Originally developed by Intel Research and currently maintained by volunteers. The LGPL license allow to link and distribute commercial software with BlueCove.

BlueCove public contirbutions Wiki Blog about Blue Cove
Library API

BlueCove provides an implementation of the JSR 82. Applications should use API defined in JSR-82. See BlueCove JSR-82 API
Requirements

    * WIDCOMM (Broadcom) BTW Stack software version 1.4.2.10 SP5 or above
    * BlueSoleil version 1.6.0 or above
    * Microsoft Bluetooth stack (currently this means Windows XP SP2 or newer and Windows Mobile 2003 or newer)
    * A Bluetooth device supported by the WIDCOMM, BlueSoleil or Microsoft bluetooth stack
    * Java 1.1 or newer for the binary execution, Java 1.4 or newer to compile.
    * Another Bluetooth device to communicate with. See Complete list of the JSR-82 compliant phones 

Limitations

L2CAP support available only on WIDCOMM Stack since BlueCove version 2.0.1.

Due to the Microsoft Bluetooth stack only supporting RFCOMM connections, BlueCove also only supports RFCOMM connections on this stack. The operating system support is currently limited to Windows XP SP2 and newer, because the Microsoft Bluetooth stack is not available on other operating systems.

If someone writes code to support another stack and/or operating system, it will be considered for inclusion. TOSHIBA on Win32 and BlueZ are wellcome!

In spring there was an attempt to make OS X port by Eric Wagner. The code is avalable in SVN.

For more limitations details see stacks.txt or BlueCove supported stacks.
GNU Lesser General Public License

We hope you enjoy using BlueCove. Please note that this is an open-source effort. If you feel the code could use new features or fixes, or the documentation can be improved, please get involved and lend us a hand! The BlueCove developers community welcomes your participation.
Not Implemented functionality

    * RemoteDevice authenticate, authorize and encrypt Not implemented 

Installation

Installation of the binary (already compiled) version of BlueCove is as follows:

   1. Download BlueCove binary release
   2. Add bluecove.jar to your classpath 

For maven2 users see Using maven2 to build application or MIDlet
Runtime configuration

Bluetooth Stack

    If automatic Bluetooth Stack detection is not enough Java System property "bluecove.stack" can be used to force desired Stack Initialization. Values "widcomm", "bluesoleil" or "winsock". By default winsock is selected if available. 

    Another property "bluecove.stack.first" is used optimize stack detection. If -Dbluecove.stack.first=widcomm then widcomm (bluecove.dll) stack is loaded first and if not available then BlueCove will switch to winsock. By default intelbth.dll is loaded first. 

    If multiple stacks are detected they are selected in following order: "winsock", "widcomm", "bluesoleil". Since BlueCove v2.0.1 "bluecove.stack.first" will alter the order of stack selection. 

    If System property is not an option (e.g. when running in Webstart) create text file "bluecove.stack" or "bluecove.stack.first" containing stack name and add this file to BlueCove or Application jar. (Since v2.0.1) 

Native Library location

   1. By default Native Library is extracted from from jar to temporary directory ${java.io.tmpdir}/bluecove_${user.name}_N and loaded from this location.
   2. If you wish to load library (.dll) from another location add this system property -Dbluecove.native.path=/your/path.
   3. If you wish to load library from default location in path e.g. %SystemRoot%\system32 or any other location in %PATH% use -Dbluecove.native.resource=false 

IBM J9 Personal Profile

    To run BlueCove with IBMs J9 Java VM on Win32 or PocketPC add this system property -Dmicroedition.connection.pkgs=com.intel.bluetooth. 

    Tested on

       1. WebSphere Everyplace Micro Environment 5.7.2, CDC 1.0/Foundation 1.0/Personal Profile 1.0 for Windows XP/X86
       2. WebSphere Everyplace Micro Environment 6.1.1, CDC 1.0/Foundation 1.0/Personal Profile 1.0 for Windows XP/X86 

IBM J9 MIDP 2.0 Profile

   1. Copy to bluecove.jar %J9_HOME%\lib\jclMidp20\ext directory
   2. Copy all bluecove dlls to %J9_HOME%\bin directory or add -Dcom.ibm.oti.vm.bootstrap.library.path=%bluecove_dll_path%;%J9_HOME%\bin
   3. run app "%J9_HOME%\bin\j9.exe" -jcl:midp20 -Dmicroedition.connection.pkgs=com.intel.bluetooth -cp target\bctest.jar "-jxe:%J9_HOME%\lib\jclMidp20\jclMidp20.jxe" target\bctest.jad
   4. -Dmicroedition.connection.pkgs=com.intel.bluetooth is optonal if you place bluecove.jar to ext directory (Since v2.0.2) 

    Tested on

       1. WebSphere Everyplace Micro Environment 5.7.2, CLDC 1.1, MIDP 2.0 for Windows XP/X86 

Debug

    If something goes wrong system property -Dbluecove.debug=true will enable prints prints in BlueCove code 

    If System property is not an option (e.g. when running MIDP application in emulator) create text file "bluecove.debug" containing one line 'true' and add this file to BlueCove.jar. (Since v2.0.2) 

    Bluecove log is redirected to log4j when log4j.jar is available in classpath. Debug can be enabled using log4j configuration. (Since v2.0.2) 

Compilation

You need a C++ compiler and JDK. Tested on Visual C++ 2005 Express Edition SP1 and SDK for Windows Vista or Windows Server 2003 R2 Platform SDK.

    VC++ and Windows SDK are available for free download from microsoft.com. We are using for Windows Vista SDK for binary distribution:

        Make sure you have Tools -> Options -> VC++ Directories ->

                "Include files": %ProgramFiles%\Microsoft SDKs\Windows\v6.0\Include
                "Library files": %ProgramFiles%\Microsoft SDKs\Windows\v6.0\lib

    We can't use the same DLL on windows for all implemenations. Since WIDCOMM need to be compile /MD using VC6 and winsock /MT using VC2005

        intelbth.dll build by VC2005 Configuration "Win32 winsock" bluecove.dll build by VC6 Configuration "Win32 Release" 

    Visual C++ 6.0 SP6 used to build bluecove.dll for WIDCOMM Visual Visual C++ 2005 used to build intelbth.dll for winsock and BlueSoleil More detail on building native code src\main\c\intelbth\ReadMe.txt 

    Ant or maven2 are used as the build tool for java. 

   1. Download BlueCove source release
   2. Unzip the source
   3. Run ant or mvn
   4. Go into src\main\c\intelbth
   5. Open intelbth.sln
   6. Compile the project for your platform (e.g. 'Winsock' for 'Win32')
   7. Run ant jar or mvn 

    We don't use ant for official distributions! build.xml only provided for your convenience 

Source

Available as downloadable packages or at the Subversion repository or for each released version.

    Organized in: 

    * src\main\c\intelbth - The native windows JNI dll
    * src\main\java - The implementation of JSR-82 with calls to intelbth
    * src\test\java - Some test programs 

