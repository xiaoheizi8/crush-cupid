Write-Output ('PSQL_EXE: ' + (Get-Command psql -ErrorAction SilentlyContinue).Source)
Write-Output ('PGPASSWORD SET? ' + [bool]$env:PGPASSWORD)
$env:PGPASSWORD = '123456'
$o = & psql --version 2>&1
Write-Output ('VERSION_OUT: ' + ($o -join ' | '))
Write-Output ('EXIT: ' + $LASTEXITCODE)
$c = & psql -h localhost -p 5432 -U postgres -d crushCupid -t -A -c "SELECT 'HELLO';" 2>&1
Write-Output ('CONN: ' + ($c -join ' | '))
Write-Output ('CEXIT: ' + $LASTEXITCODE)
