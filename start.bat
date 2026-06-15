@echo off
title E-commerce TCC - GestorPed

echo ===================================================
echo        Iniciando E-commerce TCC (GestorPed)
echo ===================================================
echo.

echo [1/2] Abrindo o Frontend no navegador (Modo Anonimo)...
:: Forca a abertura em janela anonima para limpar o cache/token antigo
start chrome --incognito "%~dp0index.html" || start msedge -inprivate "%~dp0index.html" || start index.html

echo [2/2] Iniciando o Backend (Spring Boot)...
echo.
mvn spring-boot:run