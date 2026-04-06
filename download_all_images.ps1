$nodpi = "app\src\main\res\drawable-nodpi"

if (-not (Test-Path $nodpi)) {
    New-Item -ItemType Directory -Path $nodpi | Out-Null
}

$images = @(
    "img_audio.jpg|https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=600`&h=400`&fit=crop`&q=85",
    "img_visual.jpg|https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=600`&h=400`&fit=crop`&q=85",
    "img_misc.jpg|https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=600`&h=400`&fit=crop`&q=85",
    "bg_campus.jpg|https://images.unsplash.com/photo-1562774053-701939374585?w=1200`&h=800`&fit=crop`&q=80",
    "img_header_banner.jpg|https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=1200`&h=300`&fit=crop`&q=85",
    "img_empty_state.jpg|https://images.unsplash.com/photo-1586281380349-632531db7ed4?w=600`&h=400`&fit=crop`&q=80",
    "img_booking_success.jpg|https://images.unsplash.com/photo-1540553016722-983e48a2cd10?w=600`&h=400`&fit=crop`&q=80",
    "img_dialog_bg.jpg|https://images.unsplash.com/photo-1497366216548-37526070297c?w=800`&h=600`&fit=crop`&q=75",
    "img_splash_bg.jpg|https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=1080`&h=1920`&fit=crop`&q=85",
    "img_app_logo_bg.jpg|https://images.unsplash.com/photo-1497366811353-6870744d04b2?w=400`&h=400`&fit=crop`&q=85"
)

$client = New-Object System.Net.WebClient
$client.Headers.Add("User-Agent", "Mozilla/5.0")

$ok = 0
$fail = 0

Write-Host "Downloading $($images.Count) images to $nodpi ..."
Write-Host "------------------------------------------------------------"

foreach ($entry in $images) {
    $parts = $entry -split "\|"
    $name  = $parts[0]
    $url   = $parts[1]
    $dest  = Join-Path $nodpi $name

    Write-Host "  $name" -NoNewline
    try {
        $client.DownloadFile($url, $dest)
        $kb = [math]::Round((Get-Item $dest).Length / 1024)
        if ($kb -lt 5) {
            Write-Host " WARN: only ${kb}KB" -ForegroundColor Yellow
            $fail++
        } else {
            Write-Host " OK (${kb}KB)" -ForegroundColor Green
            $ok++
        }
    } catch {
        Write-Host " FAILED: $($_.Exception.Message)" -ForegroundColor Red
        $fail++
    }
}

$client.Dispose()

Write-Host "------------------------------------------------------------"
Write-Host "Done: $ok OK, $fail failed"
Write-Host "Location: $nodpi"
