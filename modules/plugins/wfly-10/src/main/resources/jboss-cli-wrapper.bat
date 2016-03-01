@echo off

rem
rem RHQ Management Platform
rem Copyright (C) 2014 Red Hat, Inc.
rem All rights reserved.
rem
rem This program is free software; you can redistribute it and/or modify
rem it under the terms of the GNU General Public License as published by
rem the Free Software Foundation version 2 of the License.
rem
rem This program is distributed in the hope that it will be useful,
rem but WITHOUT ANY WARRANTY; without even the implied warranty of
rem MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
rem GNU General Public License for more details.
rem
rem You should have received a copy of the GNU General Public License
rem along with this program; if not, write to the Free Software Foundation, Inc.,
rem 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
rem

call %*
"%COMSPEC%" /C exit %errorlevel% >nul
