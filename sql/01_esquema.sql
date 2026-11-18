-- =====================================================================
--  PROYECTO: Tienda en línea (E-commerce)  |  VERSIÓN 3
--  Materia:  Programación Web
--  Motor:    PostgreSQL 14+
--
--  CAMBIOS DE LA VERSIÓN 2 (revisión del profesor):
--   1. pedidos ya no guarda id_usuario: el cliente se obtiene a través
--      de id_direccion (se elimina el dato redundante).
--   2. categorias se divide en categorias y subcategorias
--      (se elimina la llave recursiva id_padre).
--   3. productos usa el sku como llave primaria.
--   4. detalle_pedido ya no usa llave primaria compuesta: ahora tiene
--      un identificador propio (id_detalle).
--
--  CAMBIOS DE LA VERSIÓN 3 (para soportar el wireframe):
--   5. Nueva tabla variantes_producto: talla de ropa, número de calzado,
--      color, capacidad o presentación, cada combinación con su stock.
--   6. productos agrega tipo_opciones (qué elige el cliente),
--      precio_oferta, requiere_envio (productos digitales) y
--      solo_mayores (bebidas alcohólicas, +18).
--   7. detalle_pedido guarda la variante comprada (id_variante).
--   8. Solo dos métodos de pago: tarjeta de crédito/débito y PayPal.
--   9. pedidos agrega paqueteria y numero_guia para el rastreo.
--  10. usuarios agrega fecha_nacimiento (validar mayoría de edad).
--  11. Nuevas tablas favoritos y tokens_recuperacion.
--  12. Categorías y productos de ejemplo de cada tipo de producto.
--
--  CÓMO EJECUTAR EN DBEAVER:
--   1. Selecciona tu conexión PostgreSQL.
--   2. Abre un editor SQL y pega todo este script.
--   3. Ejecútalo completo con Option + X (Mac) / Alt + X.
--   4. Refresca con F5: las tablas están en Schemas > public > Tables.
-- =====================================================================

-- Limpieza (para poder ejecutar el script varias veces)
DROP VIEW  IF EXISTS vw_ventas_por_producto, vw_pedidos_clientes, vw_calificacion_productos,
                    vw_stock_productos;
DROP TABLE IF EXISTS tokens_recuperacion, favoritos, resenas, pagos, detalle_pedido, pedidos,
                     metodos_pago, variantes_producto, producto_subcategoria, productos,
                     proveedores, subcategorias, categorias, direcciones, usuarios, roles CASCADE;
DROP FUNCTION IF EXISTS fn_descontar_stock() CASCADE;
DROP FUNCTION IF EXISTS fn_validar_mayor_edad() CASCADE;
DROP TYPE  IF EXISTS estado_pedido, estado_pago, tipo_opciones;

-- Tipos enumerados
CREATE TYPE estado_pedido AS ENUM ('pendiente','pagado','enviado','entregado','cancelado');
CREATE TYPE estado_pago   AS ENUM ('aprobado','rechazado','reembolsado');
-- Qué opción principal elige el cliente antes de agregar al carrito
--   ninguna      = producto simple (se agrega directo)
--   talla        = ropa (CH, M, G, XG)
--   numero       = calzado (25, 26, 27…)
--   capacidad    = electrónica con versiones (128 GB, 256 GB…)
--   presentacion = alimentos (250 g, 500 g, 1 kg)
-- El color es una opción adicional que puede acompañar a cualquiera.
CREATE TYPE tipo_opciones AS ENUM ('ninguna','talla','numero','capacidad','presentacion');

-- =====================================================================
-- 1. ROLES
-- =====================================================================
CREATE TABLE roles (
    id_rol        INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre        VARCHAR(30)  NOT NULL UNIQUE,
    descripcion   VARCHAR(150)
);

-- =====================================================================
-- 2. USUARIOS
-- =====================================================================
CREATE TABLE usuarios (
    id_usuario       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_rol           INT          NOT NULL,
    nombre           VARCHAR(60)  NOT NULL,
    apellidos        VARCHAR(80)  NOT NULL,
    email            VARCHAR(120) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    telefono         VARCHAR(15),
    fecha_nacimiento DATE,                         -- para validar compras +18
    fecha_registro   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activo           BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_nacimiento CHECK (fecha_nacimiento IS NULL OR fecha_nacimiento < CURRENT_DATE),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol)
        REFERENCES roles(id_rol) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- =====================================================================
-- 3. DIRECCIONES
-- =====================================================================
CREATE TABLE direcciones (
    id_direccion    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario      INT          NOT NULL,
    calle           VARCHAR(100) NOT NULL,
    numero          VARCHAR(10)  NOT NULL,
    colonia         VARCHAR(80)  NOT NULL,
    ciudad          VARCHAR(80)  NOT NULL,
    estado          VARCHAR(50)  NOT NULL,
    codigo_postal   CHAR(5)      NOT NULL,
    referencias     VARCHAR(200),
    es_principal    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_cp CHECK (codigo_postal ~ '^[0-9]{5}$'),
    CONSTRAINT fk_direccion_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id_usuario) ON UPDATE CASCADE ON DELETE CASCADE
);

-- =====================================================================
-- 4. CATEGORIAS
-- =====================================================================
CREATE TABLE categorias (
    id_categoria    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(60)  NOT NULL UNIQUE,
    descripcion     VARCHAR(200)
);

-- =====================================================================
-- 5. SUBCATEGORIAS
-- =====================================================================
CREATE TABLE subcategorias (
    id_subcategoria INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_categoria    INT          NOT NULL,
    nombre          VARCHAR(60)  NOT NULL UNIQUE,
    descripcion     VARCHAR(200),
    CONSTRAINT fk_subcategoria_categoria FOREIGN KEY (id_categoria)
        REFERENCES categorias(id_categoria) ON UPDATE CASCADE ON DELETE CASCADE
);

-- =====================================================================
-- 6. PROVEEDORES
-- =====================================================================
CREATE TABLE proveedores (
    id_proveedor    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    razon_social    VARCHAR(120) NOT NULL,
    rfc             VARCHAR(13)  NOT NULL UNIQUE,
    contacto        VARCHAR(80),
    telefono        VARCHAR(15),
    email           VARCHAR(120)
);

-- =====================================================================
-- 7. PRODUCTOS   (el SKU es la llave primaria)
-- =====================================================================
CREATE TABLE productos (
    sku             VARCHAR(30)   PRIMARY KEY,
    id_proveedor    INT           NOT NULL,
    nombre          VARCHAR(120)  NOT NULL,
    descripcion     TEXT,
    precio          NUMERIC(10,2) NOT NULL,
    precio_oferta   NUMERIC(10,2),                      -- NULL = sin oferta
    tipo_opciones   tipo_opciones NOT NULL DEFAULT 'ninguna',
    stock           INT           NOT NULL DEFAULT 0,   -- solo productos sin variantes
    requiere_envio  BOOLEAN       NOT NULL DEFAULT TRUE,   -- FALSE = producto digital
    solo_mayores    BOOLEAN       NOT NULL DEFAULT FALSE,  -- TRUE = venta solo a mayores de 18
    imagen_url      VARCHAR(255),
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    fecha_alta      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_precio CHECK (precio > 0),
    CONSTRAINT chk_oferta CHECK (precio_oferta IS NULL OR (precio_oferta > 0 AND precio_oferta < precio)),
    CONSTRAINT chk_stock  CHECK (stock >= 0),
    CONSTRAINT fk_producto_proveedor FOREIGN KEY (id_proveedor)
        REFERENCES proveedores(id_proveedor) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- =====================================================================
-- 8. PRODUCTO_SUBCATEGORIA  (N:M)
-- =====================================================================
CREATE TABLE producto_subcategoria (
    sku             VARCHAR(30) NOT NULL,
    id_subcategoria INT         NOT NULL,
    PRIMARY KEY (sku, id_subcategoria),
    CONSTRAINT fk_ps_producto FOREIGN KEY (sku)
        REFERENCES productos(sku) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_ps_subcategoria FOREIGN KEY (id_subcategoria)
        REFERENCES subcategorias(id_subcategoria) ON UPDATE CASCADE ON DELETE CASCADE
);

-- =====================================================================
-- 9. VARIANTES_PRODUCTO  (talla, número, color, capacidad o presentación)
-- =====================================================================
CREATE TABLE variantes_producto (
    id_variante     INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sku             VARCHAR(30)   NOT NULL,
    talla           VARCHAR(10),                 -- talla de ropa o número de calzado
    color           VARCHAR(30),
    capacidad       VARCHAR(20),                 -- ej. 128 GB
    presentacion    VARCHAR(20),                 -- ej. 500 g
    precio_extra    NUMERIC(10,2) NOT NULL DEFAULT 0,
    stock           INT           NOT NULL DEFAULT 0,
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_variante_opcion CHECK (talla IS NOT NULL OR color IS NOT NULL
                                          OR capacidad IS NOT NULL OR presentacion IS NOT NULL),
    CONSTRAINT chk_variante_extra  CHECK (precio_extra >= 0),
    CONSTRAINT chk_variante_stock  CHECK (stock >= 0),
    CONSTRAINT uq_variante_sku UNIQUE (id_variante, sku),   -- permite validar la variante en detalle_pedido
    CONSTRAINT fk_variante_producto FOREIGN KEY (sku)
        REFERENCES productos(sku) ON UPDATE CASCADE ON DELETE CASCADE
);
-- No puede repetirse la misma combinación de opciones en un producto
CREATE UNIQUE INDEX uq_variante_combinacion
    ON variantes_producto (sku, COALESCE(talla,''), COALESCE(color,''),
                           COALESCE(capacidad,''), COALESCE(presentacion,''));

-- =====================================================================
-- 10. METODOS_PAGO
-- =====================================================================
CREATE TABLE metodos_pago (
    id_metodo       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre          VARCHAR(40) NOT NULL UNIQUE,
    activo          BOOLEAN     NOT NULL DEFAULT TRUE
);

-- =====================================================================
-- 11. PEDIDOS   (el cliente se obtiene a través de la dirección)
-- =====================================================================
CREATE TABLE pedidos (
    id_pedido       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_direccion    INT           NOT NULL,
    fecha_pedido    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado          estado_pedido NOT NULL DEFAULT 'pendiente',
    subtotal        NUMERIC(10,2) NOT NULL DEFAULT 0,
    costo_envio     NUMERIC(10,2) NOT NULL DEFAULT 0,
    total           NUMERIC(10,2) NOT NULL DEFAULT 0,
    paqueteria      VARCHAR(40),                  -- se captura al marcarlo como enviado
    numero_guia     VARCHAR(40),
    CONSTRAINT chk_envio CHECK (costo_envio >= 0),
    CONSTRAINT fk_pedido_direccion FOREIGN KEY (id_direccion)
        REFERENCES direcciones(id_direccion) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- =====================================================================
-- 12. DETALLE_PEDIDO   (identificador propio, sin llave compuesta)
-- =====================================================================
CREATE TABLE detalle_pedido (
    id_detalle       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido        INT           NOT NULL,
    sku              VARCHAR(30)   NOT NULL,
    id_variante      INT,                        -- NULL si el producto no tiene opciones
    cantidad         INT           NOT NULL,
    precio_unitario  NUMERIC(10,2) NOT NULL,
    importe          NUMERIC(10,2) GENERATED ALWAYS AS (cantidad * precio_unitario) STORED,
    CONSTRAINT chk_cantidad CHECK (cantidad > 0),
    CONSTRAINT fk_detalle_pedido FOREIGN KEY (id_pedido)
        REFERENCES pedidos(id_pedido) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (sku)
        REFERENCES productos(sku) ON UPDATE CASCADE ON DELETE RESTRICT,
    -- la variante debe pertenecer al mismo producto (sku)
    CONSTRAINT fk_detalle_variante FOREIGN KEY (id_variante, sku)
        REFERENCES variantes_producto(id_variante, sku) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- =====================================================================
-- 13. PAGOS (1:1 con pedidos)
-- =====================================================================
CREATE TABLE pagos (
    id_pago         INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_pedido       INT           NOT NULL UNIQUE,
    id_metodo       INT           NOT NULL,
    monto           NUMERIC(10,2) NOT NULL,
    fecha_pago      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    referencia      VARCHAR(60),                  -- folio; de la tarjeta solo los últimos 4 dígitos
    estado          estado_pago   NOT NULL DEFAULT 'aprobado',
    CONSTRAINT chk_monto CHECK (monto > 0),
    CONSTRAINT fk_pago_pedido FOREIGN KEY (id_pedido)
        REFERENCES pedidos(id_pedido) ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_pago_metodo FOREIGN KEY (id_metodo)
        REFERENCES metodos_pago(id_metodo) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- =====================================================================
-- 14. RESENAS
-- =====================================================================
CREATE TABLE resenas (
    id_resena       INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario      INT         NOT NULL,
    sku             VARCHAR(30) NOT NULL,
    calificacion    SMALLINT    NOT NULL,
    comentario      VARCHAR(500),
    fecha           TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_calificacion CHECK (calificacion BETWEEN 1 AND 5),
    CONSTRAINT uq_resena UNIQUE (id_usuario, sku),
    CONSTRAINT fk_resena_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id_usuario) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_resena_producto FOREIGN KEY (sku)
        REFERENCES productos(sku) ON UPDATE CASCADE ON DELETE CASCADE
);

-- =====================================================================
-- 15. FAVORITOS  (lista de deseos: N:M usuarios-productos)
-- =====================================================================
CREATE TABLE favoritos (
    id_usuario      INT         NOT NULL,
    sku             VARCHAR(30) NOT NULL,
    fecha_agregado  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_usuario, sku),
    CONSTRAINT fk_favorito_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id_usuario) ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_favorito_producto FOREIGN KEY (sku)
        REFERENCES productos(sku) ON UPDATE CASCADE ON DELETE CASCADE
);

-- =====================================================================
-- 16. TOKENS_RECUPERACION  ("¿Olvidaste tu contraseña?")
-- =====================================================================
CREATE TABLE tokens_recuperacion (
    id_token        INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_usuario      INT         NOT NULL,
    token           VARCHAR(64) NOT NULL UNIQUE,   -- se envía por correo en un enlace
    expira          TIMESTAMP   NOT NULL,
    usado           BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_token_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuarios(id_usuario) ON UPDATE CASCADE ON DELETE CASCADE
);

-- Índices
CREATE INDEX idx_productos_nombre ON productos(nombre);
CREATE INDEX idx_pedidos_estado   ON pedidos(estado);
CREATE INDEX idx_pedidos_fecha    ON pedidos(fecha_pedido);
CREATE INDEX idx_variantes_sku    ON variantes_producto(sku);

-- =====================================================================
-- TRIGGER 1: descontar stock al vender
-- =====================================================================
CREATE OR REPLACE FUNCTION fn_descontar_stock()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.id_variante IS NOT NULL THEN
        UPDATE variantes_producto          -- producto con talla, número, capacidad o presentación
           SET stock = stock - NEW.cantidad
         WHERE id_variante = NEW.id_variante;
    ELSE
        UPDATE productos                   -- producto simple
           SET stock = stock - NEW.cantidad
         WHERE sku = NEW.sku;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_descontar_stock
AFTER INSERT ON detalle_pedido
FOR EACH ROW
EXECUTE FUNCTION fn_descontar_stock();

-- =====================================================================
-- TRIGGER 2: productos +18 solo para clientes mayores de edad
-- =====================================================================
CREATE OR REPLACE FUNCTION fn_validar_mayor_edad()
RETURNS TRIGGER AS $$
DECLARE
    v_solo_mayores BOOLEAN;
    v_nacimiento   DATE;
BEGIN
    SELECT solo_mayores INTO v_solo_mayores FROM productos WHERE sku = NEW.sku;
    IF v_solo_mayores THEN
        SELECT u.fecha_nacimiento INTO v_nacimiento
          FROM pedidos p
          JOIN direcciones d ON d.id_direccion = p.id_direccion
          JOIN usuarios u    ON u.id_usuario   = d.id_usuario
         WHERE p.id_pedido = NEW.id_pedido;
        IF v_nacimiento IS NULL OR v_nacimiento > CURRENT_DATE - INTERVAL '18 years' THEN
            RAISE EXCEPTION 'El producto % solo se vende a mayores de 18 años', NEW.sku;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_mayor_edad
BEFORE INSERT ON detalle_pedido
FOR EACH ROW
EXECUTE FUNCTION fn_validar_mayor_edad();

-- =====================================================================
-- DATOS DE PRUEBA
-- =====================================================================
INSERT INTO roles (nombre, descripcion) VALUES
('administrador', 'Gestiona productos, pedidos y usuarios'),
('empleado',      'Atiende pedidos y envíos'),
('cliente',       'Compra productos en la tienda');

INSERT INTO usuarios (id_rol, nombre, apellidos, email, password_hash, telefono, fecha_nacimiento) VALUES
(1, 'Admin',   'Principal',        'admin@tienda.com',    '$2y$10$hashdeejemplo1', '9511000001', NULL),
(2, 'Laura',   'Méndez Cruz',      'laura@tienda.com',    '$2y$10$hashdeejemplo2', '9511000002', '1995-03-14'),
(3, 'Carlos',  'Ramírez López',    'carlos@correo.com',   '$2y$10$hashdeejemplo3', '9511000003', '1990-07-22'),
(3, 'María',   'Hernández Ruiz',   'maria@correo.com',    '$2y$10$hashdeejemplo4', '9511000004', '1998-11-02'),
(3, 'José',    'Santiago Pérez',   'jose@correo.com',     '$2y$10$hashdeejemplo5', '9511000005', '2001-05-30'),
(3, 'Ana',     'García Martínez',  'ana@correo.com',      '$2y$10$hashdeejemplo6', '9511000006', '1987-01-19');

INSERT INTO direcciones (id_usuario, calle, numero, colonia, ciudad, estado, codigo_postal, referencias, es_principal) VALUES
(3, 'Av. Independencia', '120', 'Centro',                'Oaxaca de Juárez',       'Oaxaca', '68000', 'Frente al parque', TRUE),
(4, 'Calle Macedonio Alcalá', '45', 'Centro',             'Oaxaca de Juárez',       'Oaxaca', '68000', NULL,               TRUE),
(5, 'Av. Universidad', '300', 'Ex Hacienda 5 Señores',   'Oaxaca de Juárez',       'Oaxaca', '68120', 'Casa azul',        TRUE),
(6, 'Calle Reforma', '18', 'Reforma',                    'Oaxaca de Juárez',       'Oaxaca', '68050', NULL,               TRUE),
(3, 'Calle Pino Suárez', '8', 'Centro',                  'Santa Lucía del Camino', 'Oaxaca', '71228', 'Oficina',          FALSE);

INSERT INTO categorias (nombre, descripcion) VALUES
('Electrónica',          'Dispositivos electrónicos'),
('Ropa',                 'Prendas de vestir'),
('Hogar',                'Artículos para el hogar'),
('Calzado',              'Tenis, zapatos y sandalias'),
('Alimentos y bebidas',  'Productos regionales de Oaxaca'),
('Tarjetas de regalo',   'Saldo digital para la tienda');

INSERT INTO subcategorias (id_categoria, nombre, descripcion) VALUES
(1, 'Computación', 'Laptops y accesorios'),        -- 1
(1, 'Audio',       'Audífonos y bocinas'),         -- 2
(2, 'Playeras',    'Playeras y camisetas'),        -- 3
(3, 'Cocina',      'Utensilios de cocina'),        -- 4
(3, 'Iluminación', 'Lámparas y focos'),            -- 5
(4, 'Tenis',       'Tenis deportivos y urbanos'),  -- 6
(1, 'Celulares',   'Teléfonos inteligentes'),      -- 7
(5, 'Café',        'Café de altura'),              -- 8
(5, 'Mezcal',      'Bebidas destiladas (+18)'),    -- 9
(6, 'Digitales',   'Se entregan por correo');      -- 10

INSERT INTO proveedores (razon_social, rfc, contacto, telefono, email) VALUES
('TecnoDistribuidora SA de CV',   'TDI010101AB1', 'Roberto Luna',   '5550000001', 'ventas@tecnodis.com'),
('Textiles Oaxaca SA de CV',      'TOA020202CD2', 'Sofía Vásquez',  '9510000010', 'contacto@textoax.com'),
('Casa Hogar Mayoreo SA de CV',   'CHM030303EF3', 'Luis Ortega',    '3330000020', 'pedidos@casahogar.com'),
('Calzado del Bajío SA de CV',    'CDB040404GH4', 'Elena Robles',   '4770000030', 'ventas@calzadobajio.com'),
('Productores de Oaxaca SC de RL','POA050505IJ5', 'Tomás Jiménez',  '9510000040', 'ventas@prodoax.com'),
('Regalos Digitales MX SA de CV', 'RDM060606KL6', 'Paola Núñez',    '5550000050', 'soporte@regalosdig.com');

-- tipo_opciones indica qué elige el cliente; los productos con variantes llevan stock 0 aquí
-- porque su stock vive en variantes_producto
INSERT INTO productos (sku, id_proveedor, nombre, descripcion, precio, precio_oferta, tipo_opciones,
                       stock, requiere_envio, solo_mayores) VALUES
('ELEC-001', 1, 'Laptop 14 pulgadas Ryzen 5', '16 GB RAM, 512 GB SSD',       12999.00, NULL,  'ninguna',      16, TRUE,  FALSE),
('ELEC-002', 1, 'Mouse inalámbrico',          'Receptor USB, 1600 DPI',        249.00, NULL,  'ninguna',      82, TRUE,  FALSE),
('ELEC-003', 1, 'Audífonos Bluetooth',        'Cancelación de ruido',         1299.00, 999.00,'ninguna',      40, TRUE,  FALSE),
('ELEC-004', 1, 'Bocina portátil',            'Resistente al agua IPX7',       899.00, NULL,  'ninguna',      25, TRUE,  FALSE),
('ELEC-005', 1, 'Celular 6.5 pulgadas 5G',    'Cámara de 50 MP, 8 GB RAM',    5999.00, NULL,  'capacidad',     0, TRUE,  FALSE),
('ROPA-001', 2, 'Playera algodón negra',      '100% algodón',                  199.00, NULL,  'talla',         0, TRUE,  FALSE),
('ROPA-002', 2, 'Playera bordada Oaxaca',     'Bordado artesanal',             450.00, NULL,  'talla',         0, TRUE,  FALSE),
('CALZ-001', 4, 'Tenis urbanos',              'Suela de hule, piel sintética', 899.00, NULL,  'numero',        0, TRUE,  FALSE),
('HOG-001',  3, 'Juego de sartenes',          'Antiadherentes, 3 piezas',      799.00, NULL,  'ninguna',      20, TRUE,  FALSE),
('HOG-002',  3, 'Lámpara de escritorio LED',  'Luz cálida y fría',             349.00, NULL,  'ninguna',       1, TRUE,  FALSE),  -- queda agotada tras el pedido 3
('ALIM-001', 5, 'Café de Oaxaca molido',      'Café de altura, tueste medio',  180.00, NULL,  'presentacion',  0, TRUE,  FALSE),
('BEB-001',  5, 'Mezcal espadín 750 ml',      'Joven, 40% Alc. Vol.',          650.00, NULL,  'ninguna',      30, TRUE,  TRUE),
('DIG-500',  6, 'Tarjeta de regalo $500',     'Código que se envía por correo',500.00, NULL,  'ninguna',     999, FALSE, FALSE),
('DIG-1000', 6, 'Tarjeta de regalo $1,000',   'Código que se envía por correo',1000.00,NULL,  'ninguna',     999, FALSE, FALSE);

-- Variantes: tallas, números, colores, capacidades y presentaciones con su propio stock
INSERT INTO variantes_producto (sku, talla, color, capacidad, presentacion, precio_extra, stock) VALUES
('ROPA-001', 'CH', NULL, NULL, NULL, 0, 20),          -- id 1
('ROPA-001', 'M',  NULL, NULL, NULL, 0, 40),          -- id 2
('ROPA-001', 'G',  NULL, NULL, NULL, 0, 30),          -- id 3
('ROPA-001', 'XG', NULL, NULL, NULL, 0, 10),          -- id 4
('ROPA-002', 'CH', NULL, NULL, NULL, 0,  5),          -- id 5
('ROPA-002', 'M',  NULL, NULL, NULL, 0, 15),          -- id 6
('ROPA-002', 'G',  NULL, NULL, NULL, 0,  8),          -- id 7
('ROPA-002', 'XG', NULL, NULL, NULL, 0,  0),          -- id 8 (agotada)
('CALZ-001', '25', 'Blanco', NULL, NULL, 0, 5),       -- id 9
('CALZ-001', '26', 'Blanco', NULL, NULL, 0, 8),       -- id 10
('CALZ-001', '27', 'Blanco', NULL, NULL, 0, 6),       -- id 11
('CALZ-001', '28', 'Blanco', NULL, NULL, 0, 0),       -- id 12 (agotado)
('CALZ-001', '26', 'Negro',  NULL, NULL, 0, 4),       -- id 13
('CALZ-001', '27', 'Negro',  NULL, NULL, 0, 3),       -- id 14
('ELEC-005', NULL, 'Negro', '128 GB', NULL,    0, 10),-- id 15
('ELEC-005', NULL, 'Azul',  '128 GB', NULL,    0,  6),-- id 16
('ELEC-005', NULL, 'Negro', '256 GB', NULL, 1200,  4),-- id 17
('ALIM-001', NULL, NULL, NULL, '250 g',   0, 50),     -- id 18
('ALIM-001', NULL, NULL, NULL, '500 g', 150, 30),     -- id 19
('ALIM-001', NULL, NULL, NULL, '1 kg',  380, 12);     -- id 20

INSERT INTO producto_subcategoria (sku, id_subcategoria) VALUES
('ELEC-001',1), ('ELEC-002',1), ('ELEC-003',2), ('ELEC-004',2), ('ELEC-005',7),
('ROPA-001',3), ('ROPA-002',3), ('CALZ-001',6), ('HOG-001',4),  ('HOG-002',5),
('ALIM-001',8), ('BEB-001',9),  ('DIG-500',10), ('DIG-1000',10);

INSERT INTO metodos_pago (nombre) VALUES
('Tarjeta de crédito/débito'), ('PayPal');

INSERT INTO pedidos (id_direccion, estado, costo_envio, paqueteria, numero_guia) VALUES
(1, 'entregado', 99.00, 'Estafeta', 'EST4401928371'),
(2, 'enviado',    0.00, 'DHL',      'DHL7730016542'),
(3, 'pagado',    99.00, NULL, NULL),
(4, 'pendiente', 99.00, NULL, NULL),
(5, 'pagado',     0.00, NULL, NULL),
(1, 'pagado',    99.00, NULL, NULL);      -- pedido del wireframe (laptop, mouse y playera talla M)

INSERT INTO detalle_pedido (id_pedido, sku, id_variante, cantidad, precio_unitario) VALUES
(1, 'ELEC-001', NULL, 1, 12999.00), (1, 'ELEC-002', NULL, 1, 249.00),
(2, 'ELEC-003', NULL, 1,  1299.00), (2, 'ROPA-001', 2,    2, 199.00),   -- playera talla M
(3, 'ROPA-002', 6,    2,   450.00), (3, 'HOG-002',  NULL, 1, 349.00),   -- playera bordada talla M
(4, 'HOG-001',  NULL, 1,   799.00),
(5, 'ELEC-004', NULL, 1,   899.00), (5, 'ELEC-002', NULL, 2, 249.00),
(6, 'ELEC-001', NULL, 1, 12999.00), (6, 'ELEC-002', NULL, 2, 249.00), (6, 'ROPA-002', 6, 1, 450.00);

-- Calcular subtotal y total de cada pedido
UPDATE pedidos p
   SET subtotal = d.suma,
       total    = d.suma + p.costo_envio
  FROM (SELECT id_pedido, SUM(importe) AS suma
          FROM detalle_pedido GROUP BY id_pedido) d
 WHERE d.id_pedido = p.id_pedido;

-- id_metodo: 1 = tarjeta, 2 = PayPal. La referencia de tarjeta guarda solo los últimos 4 dígitos.
INSERT INTO pagos (id_pedido, id_metodo, monto, referencia) VALUES
(1, 1, 13347.00, 'TXN-0001 ****4821'),
(2, 2,  1697.00, 'PP-0002'),
(3, 1,  1348.00, 'TXN-0003 ****1093'),
(5, 2,  1397.00, 'PP-0005'),
(6, 1, 14046.00, 'TXN-0006 ****4821');

INSERT INTO resenas (id_usuario, sku, calificacion, comentario) VALUES
(3, 'ELEC-001', 5, 'Excelente laptop, muy rápida'),
(3, 'ELEC-002', 4, 'Buen mouse por el precio'),
(4, 'ELEC-003', 5, 'Se escuchan increíble'),
(4, 'ROPA-001', 3, 'La tela es algo delgada');

INSERT INTO favoritos (id_usuario, sku) VALUES
(3, 'ELEC-005'), (3, 'CALZ-001'), (3, 'ALIM-001'), (4, 'ROPA-002');

-- =====================================================================
-- VISTAS
-- =====================================================================
CREATE VIEW vw_ventas_por_producto AS
SELECT pr.sku, pr.nombre,
       SUM(d.cantidad) AS unidades_vendidas,
       SUM(d.importe)  AS total_vendido
  FROM detalle_pedido d
  JOIN productos pr ON pr.sku      = d.sku
  JOIN pedidos   p  ON p.id_pedido = d.id_pedido
 WHERE p.estado <> 'cancelado'
 GROUP BY pr.sku, pr.nombre;

-- El cliente se obtiene a través de la dirección de envío
CREATE VIEW vw_pedidos_clientes AS
SELECT p.id_pedido, u.nombre || ' ' || u.apellidos AS cliente,
       p.fecha_pedido, p.estado, p.total,
       COALESCE(mp.nombre, 'Sin pago') AS metodo_pago,
       p.paqueteria, p.numero_guia
  FROM pedidos p
  JOIN direcciones dir      ON dir.id_direccion = p.id_direccion
  JOIN usuarios u           ON u.id_usuario     = dir.id_usuario
  LEFT JOIN pagos pg        ON pg.id_pedido     = p.id_pedido
  LEFT JOIN metodos_pago mp ON mp.id_metodo     = pg.id_metodo;

-- Stock real de cada producto: el propio si es simple, o la suma de sus variantes
CREATE VIEW vw_stock_productos AS
SELECT p.sku, p.nombre, p.tipo_opciones,
       CASE WHEN p.tipo_opciones = 'ninguna' THEN p.stock
            ELSE COALESCE(SUM(v.stock), 0) END AS stock_total,
       COALESCE(p.precio_oferta, p.precio) AS precio_venta
  FROM productos p
  LEFT JOIN variantes_producto v ON v.sku = p.sku AND v.activo
 GROUP BY p.sku, p.nombre, p.tipo_opciones, p.stock, p.precio, p.precio_oferta;

CREATE VIEW vw_calificacion_productos AS
SELECT pr.sku, pr.nombre,
       ROUND(AVG(r.calificacion), 1) AS promedio,
       COUNT(r.id_resena)            AS num_resenas
  FROM productos pr
  LEFT JOIN resenas r ON r.sku = pr.sku
 GROUP BY pr.sku, pr.nombre;

-- =====================================================================
-- CONSULTAS DE COMPROBACIÓN
-- =====================================================================
-- SELECT * FROM vw_pedidos_clientes;
-- SELECT * FROM vw_ventas_por_producto ORDER BY total_vendido DESC;
-- SELECT * FROM vw_calificacion_productos;
-- SELECT * FROM vw_stock_productos ORDER BY stock_total;          -- stock bajo / agotados
-- SELECT * FROM variantes_producto WHERE sku = 'ROPA-002';
-- Prueba del trigger +18: si el cliente es menor de edad o no tiene fecha de nacimiento,
-- el INSERT del mezcal marca error y el pedido no se completa.
--   UPDATE usuarios SET fecha_nacimiento = '2010-01-01' WHERE id_usuario = 5;
--   INSERT INTO pedidos (id_direccion) VALUES (3);
--   INSERT INTO detalle_pedido (id_pedido, sku, cantidad, precio_unitario) VALUES (7, 'BEB-001', 1, 650);

-- Confirmar cambios (necesario si DBeaver está en modo de commit manual)
COMMIT;
