$ErrorActionPreference = 'Continue'
try {
    $body = '{"username":"admin","password":"admin123"}'
    $response = Invoke-WebRequest -Uri 'http://localhost:8081/auth/login' -Method 'POST' -Body $body -ContentType 'application/json' -ErrorAction Stop
    Write-Host "Status:" $response.StatusCode
    Write-Host "Content:" $response.Content
} catch {
    Write-Host "Error:" $_.Exception.Message
    if ($_.Exception.Response) {
        Write-Host "Status Code:" $_.Exception.Response.StatusCode
    }
}