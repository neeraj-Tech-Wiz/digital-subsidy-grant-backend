@echo off
REM ==============================================================================
REM Digital Subsidy and Grant Platform - Scheme Master Module Runner
REM ==============================================================================

if not exist "bin" mkdir "bin"

echo [INFO] Compiling Scheme Master Java Application...
javac -d bin src\main\java\com\digital\subsidy\scheme\enums\*.java src\main\java\com\digital\subsidy\scheme\exception\*.java src\main\java\com\digital\subsidy\scheme\model\*.java src\main\java\com\digital\subsidy\scheme\repository\*.java src\main\java\com\digital\subsidy\scheme\service\*.java src\main\java\com\digital\subsidy\scheme\util\*.java src\main\java\com\digital\subsidy\scheme\runner\*.java src\main\java\com\digital\subsidy\scheme\SchemeMasterApplication.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %ERRORLEVEL%
)

echo [SUCCESS] Compilation complete. Starting Scheme Master Terminal Application...
java -cp bin com.digital.subsidy.scheme.SchemeMasterApplication
