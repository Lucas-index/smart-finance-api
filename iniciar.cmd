@echo off
rem Atalho do dia a dia: duplo clique neste arquivo (Windows). Sobe a API com o Groq e abre a tela no navegador.
setlocal
cd /d "%~dp0"

if not exist chave.txt goto pedirchave
set /p GROQ_API_KEY=<chave.txt
goto temchave
:pedirchave
echo Cole sua chave do Groq e aperte Enter. Ela sera salva so neste computador, no arquivo chave.txt.
set /p GROQ_API_KEY=Chave: 
>chave.txt echo(%GROQ_API_KEY%
:temchave

if not exist dbsenha.txt goto pedirsenha
set /p DB_PASSWORD=<dbsenha.txt
goto seguir
:pedirsenha
echo Digite a senha do seu MySQL (usuario root) e aperte Enter. Fica salva so neste computador, em dbsenha.txt.
set /p DB_PASSWORD=Senha: 
>dbsenha.txt echo(%DB_PASSWORD%
:seguir

echo.
echo Iniciando... a tela abre sozinha em cerca de 20 segundos. Para encerrar, feche esta janela.
start "" /b cmd /c "timeout /t 20 /nobreak >nul & start http://localhost:8080"
call run.cmd spring-boot:run -Dspring-boot.run.profiles=groq
pause