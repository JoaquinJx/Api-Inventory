param(
    [int]$Count = 100,
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "admin",
    [string]$Password = "Admin1234!"
)

$ErrorActionPreference = "Stop"

function Get-RandomPrice {
    param(
        [double]$Min,
        [double]$Max
    )

    $value = Get-Random -Minimum $Min -Maximum $Max
    return [math]::Round($value, 2)
}

Write-Host "[1/5] Login..."
$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body $loginBody
$token = $login.token
$headers = @{ Authorization = "Bearer $token" }

Write-Host "[2/5] Cargando categorias..."
$categories = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/categories"
if (-not $categories -or $categories.Count -eq 0) {
    throw "No hay categorias en la base de datos. Crea al menos una categoria antes de ejecutar el seeder."
}

$categoryMap = @{}
foreach ($cat in $categories) {
    $categoryMap[$cat.name] = $cat.id
}

Write-Host "[3/5] Cargando productos existentes..."
$existingPage = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/products?size=2000"
$existingNames = @{}
foreach ($item in $existingPage.content) {
    $existingNames[$item.name] = $true
}

$catalogByCategory = @{
    "Portatiles" = @(
        @{ name = "ThinkBook"; desc = "Portatil para productividad"; min = 700; max = 1800 },
        @{ name = "ZenBook"; desc = "Portatil ligero para trabajo remoto"; min = 850; max = 2000 },
        @{ name = "ProBook"; desc = "Equipo profesional para oficina"; min = 650; max = 1600 }
    )
    "Monitores" = @(
        @{ name = "UltraView"; desc = "Monitor IPS para diseno"; min = 180; max = 900 },
        @{ name = "GameVision"; desc = "Monitor gaming alta frecuencia"; min = 220; max = 950 },
        @{ name = "OfficeDisplay"; desc = "Monitor eficiente para oficina"; min = 120; max = 400 }
    )
    "Teclados" = @(
        @{ name = "TypeMaster"; desc = "Teclado mecanico de alto rendimiento"; min = 45; max = 210 },
        @{ name = "SilentKeys"; desc = "Teclado silencioso para productividad"; min = 30; max = 130 },
        @{ name = "RGBBoard"; desc = "Teclado gaming con iluminacion"; min = 50; max = 190 }
    )
    "Ratones" = @(
        @{ name = "SwiftMouse"; desc = "Raton preciso para trabajo diario"; min = 20; max = 120 },
        @{ name = "HyperClick"; desc = "Raton gaming ergonomico"; min = 35; max = 180 },
        @{ name = "ProPointer"; desc = "Raton profesional inalambrico"; min = 40; max = 160 }
    )
    "Almacenamiento" = @(
        @{ name = "FlashDrive"; desc = "SSD de alto rendimiento"; min = 55; max = 420 },
        @{ name = "DataVault"; desc = "Disco para respaldo y archivos"; min = 60; max = 350 },
        @{ name = "TurboSSD"; desc = "Unidad NVMe para velocidad maxima"; min = 75; max = 500 }
    )
    "Componentes" = @(
        @{ name = "CoreBoost"; desc = "Componente para alto rendimiento"; min = 90; max = 900 },
        @{ name = "PowerChip"; desc = "Hardware para estaciones de trabajo"; min = 110; max = 1200 },
        @{ name = "UltraRAM"; desc = "Memoria para multitarea intensiva"; min = 70; max = 450 }
    )
    "Redes" = @(
        @{ name = "NetRouter"; desc = "Router de alto alcance"; min = 55; max = 300 },
        @{ name = "LinkSwitch"; desc = "Switch para red local"; min = 45; max = 260 },
        @{ name = "AirBridge"; desc = "Punto de acceso WiFi 6"; min = 70; max = 360 }
    )
    "Perifericos" = @(
        @{ name = "DeskGear"; desc = "Periferico para setup de escritorio"; min = 25; max = 220 },
        @{ name = "OfficeKit"; desc = "Accesorio para entorno de trabajo"; min = 15; max = 140 },
        @{ name = "ProAccessory"; desc = "Periferico premium para oficina"; min = 30; max = 250 }
    )
}

$availableCategories = @($categoryMap.Keys)
$created = 0
$attempts = 0
$maxAttempts = $Count * 15

Write-Host "[4/5] Insertando productos aleatorios..."
while ($created -lt $Count -and $attempts -lt $maxAttempts) {
    $attempts++

    $categoryName = Get-Random -InputObject $availableCategories
    $categoryId = $categoryMap[$categoryName]

    if ($catalogByCategory.ContainsKey($categoryName)) {
        $template = Get-Random -InputObject $catalogByCategory[$categoryName]
    } else {
        $template = @{ name = "GenericItem"; desc = "Producto generico"; min = 20; max = 300 }
    }

    $sku = Get-Random -Minimum 1000 -Maximum 9999
    $name = "$($template.name) $sku"

    if ($existingNames.ContainsKey($name)) {
        continue
    }

    $price = Get-RandomPrice -Min $template.min -Max $template.max
    $stock = Get-Random -Minimum 1 -Maximum 150

    $payload = @{
        name = $name
        description = "$($template.desc) | Categoria: $categoryName"
        price = $price
        stock = $stock
        categoryId = $categoryId
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/products" -Headers $headers -ContentType "application/json" -Body $payload | Out-Null
        $existingNames[$name] = $true
        $created++

        if ($created % 10 -eq 0) {
            Write-Host "  - Creados: $created/$Count"
        }
    } catch {
        # Ignora errores puntuales para continuar el lote
    }
}

Write-Host "[5/5] Resumen final..."
$final = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/products?size=2000"
Write-Host "Productos creados en esta ejecucion: $created"
Write-Host "Productos totales en BD: $($final.totalElements)"

if ($created -lt $Count) {
    Write-Warning "No se alcanzaron los $Count productos. Creados: $created."
}
