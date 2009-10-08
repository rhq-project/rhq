set _ROOT_DIR=%~dp0
cd %_ROOT_DIR%\dummy1
call mvn clean install
cd %_ROOT_DIR%\dummy2
call mvn clean install
cd %_ROOT_DIR%\dummy3
call mvn clean install
cd %_ROOT_DIR%\plugin1
call mvn clean package
cd %_ROOT_DIR%\plugin2
call mvn clean package
cd %_ROOT_DIR%
cd ..\..\modules\enterprise\agent\target\rhq-agent\plugins
copy %_ROOT_DIR%\plugin1\target\plugin-1.0.jar .
copy %_ROOT_DIR%\plugin2\target\plugin-2.0.jar .
cd %_ROOT_DIR%