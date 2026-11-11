# Flash LineageOS 21 (Android 14, Google Play) sur Crosscall Core-X4 (L762) - bootloader DEVERROUILLE requis
# Usage : clic droit > Executer avec PowerShell, ou :  powershell -ExecutionPolicy Bypass -File FLASH_X4.ps1
# Le telephone doit etre en mode fastboot (eteint, puis Volume bas + Power) et branche en USB.
param([string]$PlatformTools = "C:\pt\platform-tools-r34\platform-tools")

$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$fb   = Join-Path $PlatformTools 'fastboot.exe'
$mke2 = Join-Path $PlatformTools 'mke2fs.exe'
$env:MKE2FS_CONFIG = Join-Path $PlatformTools 'mke2fs.conf'
$tmp  = Join-Path $env:TEMP 'x4flash'; New-Item -ItemType Directory -Force $tmp | Out-Null

function Stop-Err($m) { Write-Host "ERREUR : $m" -ForegroundColor Red; Read-Host 'Entree pour quitter'; exit 1 }
function FB { param([string[]]$a)
  Write-Host ">>> fastboot $($a -join ' ')" -ForegroundColor Cyan
  & $fb @a 2>&1 | ForEach-Object { $_.ToString() } | Where-Object { $_ -notmatch 'Did you mean' } | Write-Host
  if ($LASTEXITCODE -ne 0) { Stop-Err "fastboot $($a[0]) $($a[1]) a echoue" } }
function PartBlocks($p) { $o = (& $fb getvar "partition-size:$p" 2>&1 | Out-String)
  $h = [regex]::Match($o,'0x[0-9a-fA-F]+').Value; if (-not $h) { Stop-Err "taille de $p introuvable" }
  [math]::Floor([Convert]::ToInt64($h,16) / 4096) }
function MkExt4($name, $blocks, $extra) { $img = Join-Path $tmp "$name.img"; Remove-Item $img -ErrorAction SilentlyContinue
  & $mke2 -F -t ext4 -b 4096 @extra -E android_sparse $img $blocks 2>&1 | Out-Null
  if ($LASTEXITCODE -ne 0) { Stop-Err "mke2fs $name" }; $img }

if (-not (Test-Path $fb))   { Stop-Err "fastboot introuvable : $fb (platform-tools r34.0.5 obligatoire, voir LISEZMOI)" }
if (-not (Test-Path $mke2)) { Stop-Err "mke2fs introuvable dans $PlatformTools" }
$v = (& $fb --version 2>&1 | Out-String); if ($v -notmatch 'version 34\.') { Write-Host "ATTENTION : fastboot n'est pas en 34.x ($($v.Split("`n")[0])). Les versions 36/37 plantent (bad_alloc)." -ForegroundColor Yellow }

$dev = (& $fb devices 2>&1 | Out-String); if ($dev -notmatch 'fastboot') { Stop-Err "aucun telephone en mode fastboot" }
$prod = (& $fb getvar product 2>&1 | Out-String); if ($prod -notmatch 'L762') { Stop-Err "ce n'est pas un Core-X4 (L762) : $prod" }
$unl  = (& $fb getvar unlocked 2>&1 | Out-String); if ($unl -notmatch 'unlocked: yes') { Stop-Err "bootloader verrouille : faire d'abord 'fastboot flashing unlock' (efface tout)" }

Write-Host "`nCore-X4 detecte, bootloader deverrouille." -ForegroundColor Green
Write-Host "TOUTES LES DONNEES DU TELEPHONE VONT ETRE EFFACEES." -ForegroundColor Yellow
if ((Read-Host "Taper OUI pour continuer") -ne 'OUI') { exit 0 }

FB @('--disable-verity','--disable-verification','flash','vbmeta',(Join-Path $here 'vbmeta_disabled.img'))
FB @('flash','system',(Join-Path $here 'x4_lineage21_gapps_system.img'))
FB @('flash','userdata',(MkExt4 'userdata' (PartBlocks 'userdata') @('-O','encrypt,quota','-L','data')))
FB @('flash','metadata',(MkExt4 'metadata' (PartBlocks 'metadata') @()))
FB @('flash','cache',(MkExt4 'cache' (PartBlocks 'cache') @('-L','cache')))
FB @('reboot')
Write-Host "`nTermine. Premier demarrage : 3 a 5 minutes (logo Crosscall puis animation LineageOS)." -ForegroundColor Green
Write-Host "NE JAMAIS faire 'Factory data reset' depuis le recovery Crosscall." -ForegroundColor Yellow
Read-Host 'Entree pour quitter'
