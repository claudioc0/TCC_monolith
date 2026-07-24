@echo off
rem Inicia o servidor Spring Boot e abre o index.html no navegador
setlocal
pushd "%~dp0"
echo Iniciando o servidor Spring Boot...
start "ecommerce-monolith" cmd /k "mvn spring-boot:run"
echo Aguardando o servidor iniciar em http://localhost:8080 ...
set /a retries=0
:waitloop
  powershell -Command "try { $tcp = New-Object System.Net.Sockets.TcpClient('localhost',8080); $tcp.Close(); exit 0 } catch { exit 1 }"
  if %errorlevel%==0 goto started
  if %retries% GEQ 30 (
    echo Erro: servidor nao respondeu em 30 segundos.
    echo Verifique se o Maven iniciou corretamente e se a porta 8080 esta disponivel.
    popd
    endlocal
    exit /b 1
  )
  timeout /t 2 /nobreak >nul
  set /a retries+=1
goto waitloop
:started
echo Servidor iniciado em http://localhost:8080
start "" "http://localhost:8080/index.html"
popd
endlocal
