<#
.SYNOPSIS
    Demo script for the E2E Pipeline - sends test alerts and displays generated checklists.

.DESCRIPTION
    This script sends a test alert to the running Runbook Synthesizer server
    and displays the generated troubleshooting checklist.

.PARAMETER AlertType
    Type of alert to send: 'memory', 'cpu', or 'disk'. Default: 'memory'

.PARAMETER Severity
    Alert severity: 'info', 'warning', or 'critical'. Default: 'warning'

.PARAMETER ServerUrl
    Base URL of the server. Default: 'http://localhost:8080'

.EXAMPLE
    .\demo-e2e-pipeline.ps1 -AlertType memory -Severity warning

.EXAMPLE
    .\demo-e2e-pipeline.ps1 -AlertType cpu -Severity critical -ServerUrl http://localhost:9080
#>

param(
    [ValidateSet('memory', 'cpu', 'disk')]
    [string]$AlertType = 'memory',

    [ValidateSet('info', 'warning', 'critical')]
    [string]$Severity = 'warning',

    [string]$ServerUrl = 'http://localhost:8080'
)

# Alert templates
$alerts = @{
    memory = @{
        title      = "High Memory Utilization"
        message    = "Memory utilization exceeded 90% threshold on production server"
        dimensions = @{
            instanceId = "i-01ab33d5aaf85b544"
            metricName = "MemoryUtilization"
            region     = "us-west-2"
        }
    }
    cpu    = @{
        title      = "High CPU Utilization"
        message    = "CPU usage exceeded 85% threshold - potential runaway process"
        dimensions = @{
            instanceId = "i-prod-cpu-001"
            metricName = "CPUUtilization"
            region     = "us-west-2"
        }
    }
    disk   = @{
        title      = "Disk Space Critical"
        message    = "Disk space on /var/log exceeded 95% - immediate action required"
        dimensions = @{
            instanceId = "i-prod-disk-001"
            metricName = "DiskSpaceUtilization"
            mountPoint = "/var/log"
        }
    }
}

# Build the alert payload
$alertData = $alerts[$AlertType]
$payload = @{
    title         = $alertData.title
    message       = $alertData.message
    severity      = $Severity.ToUpper()
    sourceService = "demo-script"
    dimensions    = $alertData.dimensions
} | ConvertTo-Json -Depth 3

Write-Host ""
Write-Host "===== E2E Pipeline Demo =====" -ForegroundColor Cyan
Write-Host ""
Write-Host "Sending alert to: $ServerUrl/api/v1/alerts" -ForegroundColor Yellow
Write-Host ""
Write-Host "Alert Payload:" -ForegroundColor Gray
Write-Host $payload
Write-Host ""

try {
    # Send the request
    $response = Invoke-RestMethod -Uri "$ServerUrl/api/v1/alerts" `
        -Method POST `
        -ContentType "application/json" `
        -Body $payload `
        -ErrorAction Stop

    Write-Host "===== Generated Checklist =====" -ForegroundColor Green
    Write-Host ""
    Write-Host "Alert ID: $($response.alertId)" -ForegroundColor White
    Write-Host "Summary: $($response.summary)" -ForegroundColor White
    Write-Host "LLM Provider: $($response.llmProviderUsed)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "Steps:" -ForegroundColor Cyan

    foreach ($step in $response.steps) {
        Write-Host ""
        Write-Host "  [$($step.order)] $($step.instruction)" -ForegroundColor White
        if ($step.rationale) {
            Write-Host "      Rationale: $($step.rationale)" -ForegroundColor Gray
        }
        if ($step.commands -and $step.commands.Count -gt 0) {
            Write-Host "      Commands:" -ForegroundColor Yellow
            foreach ($cmd in $step.commands) {
                Write-Host "        > $cmd" -ForegroundColor DarkYellow
            }
        }
    }

    Write-Host ""
    Write-Host "Source Runbooks:" -ForegroundColor Cyan
    foreach ($runbook in $response.sourceRunbooks) {
        Write-Host "  - $runbook" -ForegroundColor Gray
    }

    Write-Host ""
    Write-Host "Generated at: $($response.generatedAt)" -ForegroundColor Gray
    Write-Host ""
    Write-Host "===== Demo Complete =====" -ForegroundColor Green

}
catch {
    Write-Host "Error calling API:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response body: $responseBody" -ForegroundColor DarkRed
    }
    
    Write-Host ""
    Write-Host "Make sure the server is running:" -ForegroundColor Yellow
    Write-Host "  cd c:\Users\bwend\repos\ops-scribe" -ForegroundColor Gray
    Write-Host "  mvn exec:java -Dexec.mainClass=com.oracle.runbook.RunbookSynthesizerApp" -ForegroundColor Gray
    
    exit 1
}
