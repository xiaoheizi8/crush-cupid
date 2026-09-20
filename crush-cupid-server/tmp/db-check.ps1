$env:PGPASSWORD = '123456'
$ErrorActionPreference = 'Continue'
# 测试连接
$out = & psql -h localhost -p 5432 -U postgres -d crushCupid -t -A -c "SELECT 'CONN_OK';" 2>&1
if ($LASTEXITCODE -ne 0) { Write-Output ('CONN_FAIL: ' + ($out -join ' ')) } else {
    Write-Output ($out -join ' ')
    Write-Output '--- COLUMNS sys_user ---'
    $cols = & psql -h localhost -p 5432 -U postgres -d crushCupid -t -A -c "SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name='sys_user' ORDER BY ordinal_position;" 2>&1
    $cols
    Write-Output '--- COLUMNS crush ---'
    $cr = & psql -h localhost -p 5432 -U postgres -d crushCupid -t -A -c "SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name='crush' ORDER BY ordinal_position;" 2>&1
    $cr
    Write-Output '--- COLUMNS conversation ---'
    $cv = & psql -h localhost -p 5432 -U postgres -d crushCupid -t -A -c "SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name='conversation' ORDER BY ordinal_position;" 2>&1
    $cv
    Write-Output '--- COLUMNS ai_provider ---'
    $ap = & psql -h localhost -p 5432 -U postgres -d crushCupid -t -A -c "SELECT column_name FROM information_schema.columns WHERE table_schema='public' AND table_name='ai_provider' ORDER BY ordinal_position;" 2>&1
    $ap
}
