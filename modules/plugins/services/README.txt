This plugin lists and enables system-config-services related operations. It is still a prototype and thus will not get automatically built. To install it one needs to do the following.
1) yum install dbus-java
2) if on X86_64, ln -s /usr/lib64/libmatthew-java /usr/lib/
3) cd $RHQ_DIR/modules/plugins/services && mvn -Pdev install
