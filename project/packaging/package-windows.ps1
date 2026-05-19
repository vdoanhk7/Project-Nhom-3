param(
    [string] $ServerHost = "34.126.166.158",
    [int] $ServerPort = 8080,
    [ValidateSet("app-image", "exe", "msi")]
    [string] $Type = "app-image"
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = Resolve-Path (Join-Path $ScriptDir "..")
$ReleaseDir = Join-Path $ProjectDir "release\windows"
$BuildDir = Join-Path $ProjectDir "target"
$DistDir = Join-Path $ProjectDir "dist"
$PackageInputDir = Join-Path $BuildDir "package-input"
$ClientInputDir = Join-Path $PackageInputDir "client"
$ServerInputDir = Join-Path $PackageInputDir "server"

function Resolve-JPackage {
    if ($env:JAVA_HOME) {
        $javaHomeJPackage = Join-Path $env:JAVA_HOME "bin\jpackage.exe"
        if (Test-Path $javaHomeJPackage) {
            return $javaHomeJPackage
        }
    }

    $pathJPackage = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if ($pathJPackage) {
        return $pathJPackage.Source
    }

    $jdkRoots = @(
        "C:\Program Files\Java",
        "C:\Program Files\Eclipse Adoptium",
        "C:\Program Files\Microsoft"
    )

    foreach ($root in $jdkRoots) {
        if (-not (Test-Path $root)) {
            continue
        }

        $candidate = Get-ChildItem $root -Directory -ErrorAction SilentlyContinue |
            Sort-Object Name -Descending |
            ForEach-Object { Join-Path $_.FullName "bin\jpackage.exe" } |
            Where-Object { Test-Path $_ } |
            Select-Object -First 1

        if ($candidate) {
            return $candidate
        }
    }

    throw "Cannot find jpackage.exe. Install JDK 21 and set JAVA_HOME to the JDK folder."
}

function Invoke-Checked {
    param(
        [string] $FilePath,
        [string[]] $Arguments
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FilePath failed with exit code $LASTEXITCODE."
    }
}

function Invoke-JPackage {
    param(
        [string] $Name,
        [string] $Destination,
        [string] $InputDir,
        [string] $MainJar,
        [string] $MainClass,
        [string[]] $JavaOptions
    )

    $args = @(
        "--type", $Type,
        "--name", $Name,
        "--dest", $Destination,
        "--input", $InputDir,
        "--main-jar", $MainJar,
        "--main-class", $MainClass,
        "--app-version", "1.0.0",
        "--vendor", "Nhom 3"
    )

    foreach ($option in $JavaOptions) {
        $args += @("--java-options", $option)
    }

    Invoke-Checked -FilePath $JPackage -Arguments $args
}

$JPackage = Resolve-JPackage

Set-Location $ProjectDir
Invoke-Checked -FilePath "mvn.cmd" -Arguments @("--batch-mode", "-DskipTests", "-Djavafx.platform=win", "package")

$ClientDir = Join-Path $ReleaseDir "client"
$ServerDir = Join-Path $ReleaseDir "server"
Remove-Item $ClientDir, $ServerDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $ClientDir, $ServerDir | Out-Null

Remove-Item $PackageInputDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $ClientInputDir, $ServerInputDir | Out-Null
Copy-Item (Join-Path $DistDir "client-app.jar") $ClientInputDir
Copy-Item (Join-Path $DistDir "server-app.jar") $ServerInputDir

Invoke-JPackage `
    -Name "AuctionClientNhom3" `
    -Destination $ClientDir `
    -InputDir $ClientInputDir `
    -MainJar "client-app.jar" `
    -MainClass "com.nhom3.client.Main" `
    -JavaOptions @(
        "-Dauction.server.host=$ServerHost",
        "-Dauction.server.port=$ServerPort"
    )

Invoke-JPackage `
    -Name "AuctionServerNhom3" `
    -Destination $ServerDir `
    -InputDir $ServerInputDir `
    -MainJar "server-app.jar" `
    -MainClass "com.nhom3.server.ServerMain" `
    -JavaOptions @(
        "-Dauction.server.port=$ServerPort"
    )

if ($Type -eq "app-image") {
    $clientZip = Join-Path $ReleaseDir "AuctionClientNhom3-windows.zip"
    $serverZip = Join-Path $ReleaseDir "AuctionServerNhom3-windows.zip"
    Remove-Item $clientZip, $serverZip -Force -ErrorAction SilentlyContinue

    Compress-Archive -Path (Join-Path $ClientDir "AuctionClientNhom3") -DestinationPath $clientZip
    Compress-Archive -Path (Join-Path $ServerDir "AuctionServerNhom3") -DestinationPath $serverZip

    Write-Host "Created:"
    Write-Host $clientZip
    Write-Host $serverZip
} else {
    Write-Host "Created Windows packages in:"
    Write-Host $ClientDir
    Write-Host $ServerDir
}
