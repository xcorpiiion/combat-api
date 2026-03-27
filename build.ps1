Write-Host "Buildando combat-api..." -ForegroundColor Cyan

mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0)
{
    Write-Host "ERRO: Maven falhou!" -ForegroundColor Red
    exit 1
}

docker build -t xcorpiiion/combat-api:latest .

if ($LASTEXITCODE -eq 0)
{
    Write-Host "combat-api buildada com sucesso!" -ForegroundColor Green
}
else
{
    Write-Host "ERRO: Docker build falhou!" -ForegroundColor Red
}