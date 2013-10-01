@echo off
set RHQ_SERVER_BIN_DIR_PATH=%~dp0
%RHQ_SERVER_BIN_DIR_PATH%/internal/rhq-installer --encodepassword
endlocal
