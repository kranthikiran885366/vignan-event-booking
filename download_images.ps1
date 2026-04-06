# download_images.ps1
# Downloads all required drawable images for the College Event Equipment Booking app
# Run from the project root: powershell -ExecutionPolicy Bypass -File download_images.ps1

$dest = "app\src\main\res\drawable-nodpi"

# Ensure destination folder exists
if (-not (Test-Path $dest)) {
    New-Item -ItemType Directory -Path $dest | Out-Null
    Write-Host "Created folder: $dest"
}

# Image map: filename => Unsplash direct URL (fixed-size, no API key required)
$images = @{
    "bg_campus.jpg"  = "https://images.unsplash.com/photo-1562774053-701939374585?w=1200&q=80&fit=crop"
    "img_audio.jpg"  = "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=600&q=80&fit=crop"
    "img_visual.jpg" = "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=600&q=80&fit=crop"
    "img_misc.jpg"   = "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=600&q=80&fit=crop"
}

$client = New-Object System.Net.WebClient
$client.Headers.Add("User-Agent", "Mozilla/5.0")

foreach ($entry in $images.GetEnumerator()) {
    $filePath = Join-Path $dest $entry.Key
    Write-Host "Downloading $($entry.Key) ..." -NoNewline
    try {
        $client.DownloadFile($entry.Value, $filePath)
        $size = (Get-Item $filePath).Length
        if ($size -gt 1024) {
            Write-Host " OK ($([math]::Round($size/1024))KB)" -ForegroundColor Green
        } else {
            Write-Host " WARN: file too small ($size bytes), may be invalid" -ForegroundColor Yellow
        }
    } catch {
        Write-Host " FAILED: $_" -ForegroundColor Red
    }
}

$client.Dispose()

Write-Host ""
Write-Host "Done. Images placed in: $dest" -ForegroundColor Cyan
Write-Host "Verify in Android Studio: res/drawable-nodpi/" -ForegroundColor Cyan
