@echo off
REM ==============================================================================
REM Digital Subsidy and Grant Platform - Scheme Master Test Suite
REM ==============================================================================

if not exist "bin" mkdir "bin"

echo [INFO] Compiling Scheme Master Application and Test Suite...
javac -d bin src\main\java\com\digital\subsidy\scheme\enums\*.java src\main\java\com\digital\subsidy\scheme\exception\*.java src\main\java\com\digital\subsidy\scheme\model\*.java src\main\java\com\digital\subsidy\scheme\repository\*.java src\main\java\com\digital\subsidy\scheme\service\*.java src\main\java\com\digital\subsidy\scheme\util\*.java src\main\java\com\digital\subsidy\scheme\runner\*.java src\main\java\com\digital\subsidy\scheme\SchemeMasterApplication.java src\test\java\com\digital\subsidy\scheme\SchemeMasterServiceTest.java

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Test compilation failed!
    exit /b %ERRORLEVEL%
)

echo [INFO] Running Automated Tests...
java -cp bin com.digital.subsidy.scheme.SchemeMasterServiceTest
