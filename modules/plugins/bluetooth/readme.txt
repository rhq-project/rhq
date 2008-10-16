This plugin is a work in progress.

It is using the Open Source JSR 82 implementation called Blue Cove version 2.0.2. It is only
supported on Windows OS, so this plugin will not work in other OSs. For more information
read the Bluecove-readme.txt that I created in this directory. The text is copied from the 
Bluecove Wiki Documenation page http://code.google.com/p/bluecove/wiki/Documentation

If you have a different Bluetooth Stack implementation for JSR 82, you can change the pom file
to include that jar instead of the BlueCove jars. It should be as simple as just changing the jar
file for the JSR implementation.

Another intention of this plugin is to serve as a Parent Plugin for other Bluetooth Specific Plugins
For instance, I can picture a Nintendo Wii Plugin that extends this plugin and has operations specific
for say getting a Mii downloaded from a Wiimote to your local machine. And later uploading the same
Mii to another or same Wiimote, but maybe hacked and edited to look real cool get some Gold Pants and all.