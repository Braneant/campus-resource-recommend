@echo off
rem Copy this file to local-env.cmd and fill in real values.
rem local-env.cmd is ignored by git, so real secrets never enter the repo.
rem Keep this file ASCII-only.

set "MYSQL_PASSWORD=PUT_MYSQL_PASSWORD_HERE"
set "NEO4J_PASSWORD=PUT_NEO4J_PASSWORD_HERE"
set "JWT_SECRET=PUT_A_RANDOM_LONG_STRING_HERE"
