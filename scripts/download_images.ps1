param(
    [string]$OutputDir = "app/src/main/res/drawable-nodpi"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$images = @(
    @{ Name = "bg_campus.jpg"; Url = "https://images.unsplash.com/photo-1488190211105-8b0e65b80b4e?auto=format&fit=crop&w=1400&q=80" },
    @{ Name = "img_audio.jpg"; Url = "https://images.unsplash.com/photo-1516280440614-37939bbacd81?auto=format&fit=crop&w=800&q=80" },
    @{ Name = "img_visual.jpg"; Url = "https://images.unsplash.com/photo-1498050108023-c5249f4df085?auto=format&fit=crop&w=800&q=80" },
    @{ Name = "img_misc.jpg"; Url = "https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=800&q=80" }
)

foreach ($item in $images) {
    $outFile = Join-Path $OutputDir $item.Name
    try {
        Invoke-WebRequest -Uri $item.Url -OutFile $outFile -UseBasicParsing
        Write-Host "Downloaded $($item.Name)"
    }
    catch {
        Write-Host "Failed $($item.Name): $($_.Exception.Message)"
    }
}

Write-Host "Image download script completed"
