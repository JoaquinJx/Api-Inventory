param(
    [string]$BaseUrl = "https://inventory-api-d3r8.onrender.com",
    [string]$Username = "admin",
    [string]$Password = "Admin1234!"
)

$ErrorActionPreference = "Stop"

Write-Host "[1/3] Login..."
$loginBody = @{ username = $Username; password = $Password } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/auth/login" -ContentType "application/json" -Body $loginBody
$token = $login.token
$headers = @{ Authorization = "Bearer $token" }

# ─── Catalog definition ───────────────────────────────────────────────────────
$catalog = @{
    "Tecnologia" = @(
        @{n="Smartphone Ultra";d="Telefono de ultima generacion";min=300;max=1200},
        @{n="Tablet Pro";d="Tableta de alto rendimiento";min=200;max=900},
        @{n="Smartwatch Series";d="Reloj inteligente con GPS";min=80;max=500},
        @{n="Auriculares BT";d="Auriculares inalambricos premium";min=30;max=350},
        @{n="Camara Digital";d="Camara con sensor CMOS";min=150;max=1500},
        @{n="Drone Vision";d="Drone con camara 4K";min=120;max=800},
        @{n="Bocina Portatil";d="Altavoz Bluetooth resistente al agua";min=25;max=200},
        @{n="Power Bank";d="Bateria externa de alta capacidad";min=15;max=90},
        @{n="Cargador Rapido";d="Cargador USB-C 65W";min=12;max=60},
        @{n="Soporte Movil";d="Soporte de escritorio ajustable";min=8;max=45}
    )
    "Belleza" = @(
        @{n="Crema Hidratante";d="Crema facial con acido hialuronico";min=10;max=80},
        @{n="Serum Vitamina C";d="Serum antioxidante para el rostro";min=15;max=100},
        @{n="Mascarilla Facial";d="Mascarilla purificante de arcilla";min=8;max=50},
        @{n="Labial Matte";d="Labial de larga duracion";min=5;max=35},
        @{n="Base de Maquillaje";d="Base liquida cobertura total";min=12;max=70},
        @{n="Rimel Volumen";d="Mascara de pestanas efecto volumen";min=8;max=40},
        @{n="Perfume Floral";d="Eau de parfum 100ml";min=25;max=150},
        @{n="Aceite Capilar";d="Aceite nutritivo para cabello seco";min=10;max=55},
        @{n="Protector Solar";d="SPF 50+ resistente al agua";min=8;max=45},
        @{n="Paleta Sombras";d="Paleta de 18 colores";min=12;max=65}
    )
    "Ropa" = @(
        @{n="Camiseta Basica";d="Camiseta de algodon premium";min=10;max=50},
        @{n="Pantalon Vaquero";d="Jeans slim fit";min=25;max=120},
        @{n="Vestido Casual";d="Vestido de verano estampado";min=20;max=90},
        @{n="Chaqueta Denim";d="Chaqueta de mezclilla clasica";min=35;max=130},
        @{n="Sudadera Hoodie";d="Sudadera con capucha de felpa";min=22;max=80},
        @{n="Camisa Formal";d="Camisa de vestir slim fit";min=20;max=85},
        @{n="Falda Midi";d="Falda de longitud media";min=18;max=70},
        @{n="Abrigo Invierno";d="Abrigo de lana para el frio";min=50;max=250},
        @{n="Leggings Sport";d="Leggings de compresion";min=15;max=60},
        @{n="Polo Rugby";d="Polo de punto con cuello";min=18;max=75}
    )
    "Hogar" = @(
        @{n="Lampara LED";d="Lampara de escritorio con dimmer";min=15;max=120},
        @{n="Cojin Decorativo";d="Cojin de terciopelo 45x45";min=8;max=40},
        @{n="Vela Aromatica";d="Vela de soja con esencias naturales";min=6;max=35},
        @{n="Organizador Closet";d="Sistema modular de almacenamiento";min=20;max=150},
        @{n="Cuadro Moderno";d="Cuadro abstracto para sala";min=18;max=200},
        @{n="Alfombra Sala";d="Alfombra tejida antideslizante";min=30;max=300},
        @{n="Espejo Decorativo";d="Espejo con marco dorado";min=25;max=180},
        @{n="Maceta Ceramica";d="Maceta artesanal con plato";min=8;max=55},
        @{n="Jarron Flores";d="Jarron de cristal soplado";min=12;max=80},
        @{n="Reloj Pared";d="Reloj analogico silencioso";min=15;max=90}
    )
    "Deportes" = @(
        @{n="Zapatilla Running";d="Zapatilla con amortiguacion extra";min=40;max=200},
        @{n="Mancuerna Par";d="Par de mancuernas ajustables";min=20;max=120},
        @{n="Yoga Mat";d="Colchoneta antideslizante 6mm";min=15;max=70},
        @{n="Botella Gym";d="Botella de acero inoxidable 1L";min=10;max=45},
        @{n="Banda Resistencia";d="Set de bandas elasticas";min=8;max=35},
        @{n="Cuerda Saltar";d="Cuerda de salto con contador";min=6;max=30},
        @{n="Guantes Boxeo";d="Guantes de entrenamiento 12oz";min=18;max=80},
        @{n="Casco Ciclismo";d="Casco aerodinamico certificado";min=25;max=150},
        @{n="Rodillera Sport";d="Rodillera de compresion";min=8;max=40},
        @{n="Bolsa Gym";d="Bolsa deportiva con compartimentos";min=15;max=75}
    )
    "Alimentacion" = @(
        @{n="Proteina Whey";d="Proteina de suero de leche 1kg";min=20;max=80},
        @{n="Granola Premium";d="Granola con frutos secos 500g";min=5;max=25},
        @{n="Aceite de Coco";d="Aceite de coco virgen extra 500ml";min=8;max=30},
        @{n="Cafe Especial";d="Cafe de origen unico molido 250g";min=7;max=35},
        @{n="Te Verde Organico";d="Te verde japones 100 bolsas";min=5;max=22},
        @{n="Miel Pura";d="Miel de abeja artesanal 500g";min=8;max=28},
        @{n="Avena Sin Gluten";d="Avena organica certificada 1kg";min=4;max=18},
        @{n="Snack Frutos Secos";d="Mix de nueces y almendras 300g";min=6;max=24},
        @{n="Pasta Integral";d="Pasta de trigo integral 500g";min=2;max=10},
        @{n="Salsa Artesanal";d="Salsa picante casera 250ml";min=4;max=18}
    )
    "Juguetes" = @(
        @{n="Set Lego Classic";d="Bloques de construccion 500 piezas";min=20;max=120},
        @{n="Muneca Interactiva";d="Muneca con voz y accesorios";min=15;max=60},
        @{n="Auto Control Remoto";d="Coche RC 4x4 todo terreno";min=18;max=90},
        @{n="Puzzle 1000 Piezas";d="Puzzle de paisaje montanas";min=8;max=35},
        @{n="Pizarron Magnetico";d="Pizarron doble cara con letras";min=12;max=50},
        @{n="Kit Ciencias";d="Laboratorio de experimentos infantil";min=15;max=65},
        @{n="Peluche XL";d="Peluche de oso 60cm";min=10;max=50},
        @{n="Juego Mesa Familia";d="Juego de estrategia para toda la familia";min=15;max=55},
        @{n="Patines Ajustables";d="Patines en linea con protecciones";min=20;max=80},
        @{n="Cometa Acrobatica";d="Cometa de dos hilos para niños";min=8;max=30}
    )
    "Libros" = @(
        @{n="Novela Historica";d="Relato historico bestseller";min=8;max=25},
        @{n="Autoayuda Liderazgo";d="Guia para el exito personal";min=7;max=22},
        @{n="Cocina Internacional";d="Recetas del mundo paso a paso";min=10;max=30},
        @{n="Ciencia Ficcion";d="Aventura intergalactica";min=8;max=24},
        @{n="Thriller Policiaco";d="Novela de suspense y misterio";min=7;max=20},
        @{n="Fantasia Epica";d="Saga de mundos magicos";min=9;max=28},
        @{n="Biograf Inspiradora";d="Vida de personaje historico";min=8;max=25},
        @{n="Desarrollo Personal";d="Habitos para la productividad";min=7;max=22},
        @{n="Fotografia Arte";d="Manual de composicion y luz";min=12;max=40},
        @{n="Viajes Aventura";d="Guia de destinos exoticos";min=10;max=32}
    )
    "Automovil" = @(
        @{n="Funda Asiento";d="Funda universal de cuero sintetico";min=15;max=80},
        @{n="Aspiradora Coche";d="Aspiradora portatil 12V";min=18;max=70},
        @{n="Camara Trasera";d="Camara de vision nocturna";min=20;max=100},
        @{n="Cargador Solar";d="Panel solar para bateria de coche";min=12;max=55},
        @{n="Ambientador Auto";d="Difusor de aromas para habitaculo";min=5;max=25},
        @{n="Soporte GPS";d="Soporte universal para parabrisas";min=8;max=35},
        @{n="Llave Emergencia";d="Herramienta multiusos para auto";min=10;max=40},
        @{n="Kit Limpieza Auto";d="Set de productos para detailing";min=15;max=60},
        @{n="Almohadilla Lumbar";d="Soporte lumbar para conduccion";min=12;max=50},
        @{n="Cerradura Volante";d="Antirrobo para volante";min=18;max=65}
    )
    "Salud" = @(
        @{n="Termometro Digital";d="Termometro de infrarrojos";min=12;max=60},
        @{n="Tensionometro";d="Monitor de presion arterial";min=20;max=90},
        @{n="Oximetro Pulso";d="Oximetro de dedo LED";min=10;max=45},
        @{n="Masajeador Electrico";d="Masajeador de cuello y espalda";min=15;max=80},
        @{n="Collagen Suplemento";d="Colageno hidrolizado en polvo";min=12;max=50},
        @{n="Vitamina C 1000mg";d="Vitamina C efervescente 30 comp";min=5;max=20},
        @{n="Omega 3 Capsulas";d="Aceite de pescado 60 capsulas";min=8;max=35},
        @{n="Probiotico Plus";d="Probiotico con 10 cepas bacterianas";min=10;max=45},
        @{n="Melatonina Sleep";d="Melatonina 1mg para el sueno";min=6;max=25},
        @{n="Crema Articulaciones";d="Gel de arnica y glucosamina";min=8;max=35}
    )
}

Write-Host "[2/3] Creando categorias y productos..."

$totalCreated = 0

foreach ($categoryName in $catalog.Keys) {
    # Create category
    try {
        $catBody = @{ name = $categoryName } | ConvertTo-Json
        $cat = Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/categories" -ContentType "application/json" -Headers $headers -Body $catBody
        $categoryId = $cat.id
        Write-Host "  Categoria creada: $categoryName (id=$categoryId)"
    } catch {
        # Category may already exist, fetch it
        $cats = Invoke-RestMethod -Method Get -Uri "$BaseUrl/api/categories" -Headers $headers
        $existing = $cats | Where-Object { $_.name -eq $categoryName }
        if ($existing) {
            $categoryId = $existing.id
            Write-Host "  Categoria ya existe: $categoryName (id=$categoryId)"
        } else {
            Write-Host "  ERROR creando categoria $categoryName`: $_"
            continue
        }
    }

    $templates = $catalog[$categoryName]
    $created = 0

    while ($created -lt 100) {
        $template = $templates[$created % $templates.Count]
        $num = Get-Random -Minimum 1000 -Maximum 9999
        $price = [math]::Round((Get-Random -Minimum ($template.min * 100) -Maximum ($template.max * 100)) / 100.0, 2)
        $stock = Get-Random -Minimum 1 -Maximum 200

        $payload = @{
            name        = "$($template.n) $num"
            description = $template.d
            price       = $price
            stock       = $stock
            categoryId  = $categoryId
        } | ConvertTo-Json

        try {
            Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/products" -ContentType "application/json" -Headers $headers -Body $payload | Out-Null
            $created++
            $totalCreated++
        } catch {
            # skip duplicates or errors
        }
    }

    Write-Host "    -> $created productos creados en '$categoryName'"
}

Write-Host ""
Write-Host "[3/3] Listo. Total productos creados: $totalCreated"
