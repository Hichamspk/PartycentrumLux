@echo off
cd /d "%~dp0"
"C:\Program Files\Common Files\Oracle\Java\javapath\java.exe" -jar target\lux-booking-backend-0.0.1-SNAPSHOT.jar > backend.log 2> backend.err.log
