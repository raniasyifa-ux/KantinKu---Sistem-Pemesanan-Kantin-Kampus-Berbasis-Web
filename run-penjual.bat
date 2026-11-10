@echo off
cd /d "%~dp0KantinKuApp"
if not exist "out" mkdir out
echo Mengompilasi kode Java...
javac -encoding UTF-8 -cp "lib/*" -d out src/config/*.java src/models/*.java src/dao/impl/*.java src/services/*.java src/util/*.java src/controllers/*.java src/views/antri/*.java src/views/pembeli/*.java src/views/penjual/*.java src/app/*.java
if %ERRORLEVEL% NEQ 0 (
    echo Kompilasi gagal!
    pause
    exit /b %ERRORLEVEL%
)
echo Menjalankan KantinKu (Dashboard Penjual)...
java -cp "out;src;lib/*" app.MainPenjual
