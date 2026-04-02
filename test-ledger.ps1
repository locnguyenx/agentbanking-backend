$body = @{
    agentId = "be581cb8-62b0-414f-9826-b539ee40dc4e"
    amount = 100.0
    customerFee = $null
    agentCommission = $null
    bankShare = $null
    idempotencyKey = "test-123"
    customerCardMasked = "411111******1111"
    geofenceLat = 3.139
    geofenceLng = 101.6869
} | ConvertTo-Json

Write-Host "Request Body:"
Write-Host $body
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri 'http://localhost:8082/internal/debit' -Method Post -Body $body -ContentType 'application/json'
    Write-Host "Status Code: $($response.StatusCode)"
    Write-Host "Response: $($response.Content)"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)"
    try {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $errorContent = $reader.ReadToEnd()
        Write-Host "Error Response: $errorContent"
    } catch {
        Write-Host "Could not read error response"
    }
}
