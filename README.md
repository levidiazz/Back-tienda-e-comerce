# Tienda en línea · Backend

API REST con **Java Servlets**, **JDBC**, **HikariCP** y **PostgreSQL**. 

- Java 17 o superior
- Apache Tomcat **10.1** (usa `jakarta.servlet`)
- PostgreSQL 14 o superior
- Maven (viene integrado en NetBeans)

## Estructura

```
src/main/java/com/tienda/
  conexion/       Configuracion, ConexionDB (pool HikariCP) e InicioAplicacion
  modelos/        POJO: una clase por tabla y las clases que llegan en las peticiones
  dao/            Consultas SQL (siempre con PreparedStatement)
  servlets/       Endpoints de la tienda y del cliente
  servlets/admin/ Endpoints del panel de administración
  filtros/        ApiFiltro: CORS, encabezados de seguridad y protección de /api/admin
  util/           JSON, validaciones, cifrado, contraseñas, sesión y bloqueo de intentos
sql/
  01_esquema.sql       Base de datos completa (16 tablas, vistas, triggers y datos)
  02_datos_prueba.sql  Contraseña de prueba e imágenes de los productos
  03_cifrado.sql       Amplía las columnas que se guardan cifradas
```

Flujo de cada petición: **filtro → servlet (valida) → DAO (SQL) → modelo → JSON**.

## Seguridad

| Qué | Cómo |
|---|---|
| Contraseñas | Hash **BCrypt** (costo 12). Nunca se guardan ni se regresan en texto. |
| Datos personales | Cifrado **AES-256-GCM**: teléfono del usuario, calle, número y referencias de la dirección y referencia del pago. La clave está en `db.properties`. |
| Tokens de recuperación | Se guarda su hash **SHA-256**, duran 30 minutos y son de un solo uso. |
| Tarjetas | Se valida número (algoritmo de Luhn), titular, vencimiento y CVV. Solo se guardan los últimos 4 dígitos, cifrados. El CVV nunca se guarda. |
| Inyección SQL | Todas las consultas usan `PreparedStatement` con `?`. El orden y los filtros salen de listas fijas. La búsqueda escapa `%` y `_`. Probado con sqlmap. |
| Validación | Cada campo se valida en tipo, longitud, formato y rango antes de llegar a la base. Se rechazan `<`, `>` y caracteres de control. |
| Fuerza bruta | 5 intentos fallidos de login bloquean ese correo 15 minutos. |
| Sesión | Cookie `HttpOnly`, se renueva al iniciar sesión, expira a los 60 minutos. |
| Permisos | `/api/admin` solo para administrador y empleado. Cada cliente solo ve sus propios datos. |
| Peticiones | Solo JSON (`415` si no), máximo 64 KB (`413`). |

## Cómo correrlo en NetBeans

1. **Base de datos.** Crea la base `ecommerce_bd` y ejecuta en orden `sql/01_esquema.sql`, `sql/02_datos_prueba.sql` y `sql/03_cifrado.sql`.
2. **Configuración.** Copia `src/main/resources/db.properties.ejemplo` como `db.properties` y pon tu usuario, contraseña y el nombre exacto de tu base. Este archivo no se sube a GitHub.
3. **Abrir.** *File › Open Project* y elige la carpeta del proyecto.
4. **Servidor.** Clic derecho › *Properties › Run*: Server *Apache Tomcat 10.1* y Context Path `/tienda-ecommerce-back`.
5. **Correr.** *Clean and Build* y luego *Run*. Al arrancar, los datos personales de prueba se cifran solos.
6. Abre `http://localhost:8080/tienda-ecommerce-back/api/productos`.

Usuarios de prueba (contraseña `Tienda2026`): `admin@tienda.com` (administrador), `laura@tienda.com` (empleado), `carlos@correo.com` y `maria@correo.com` (clientes).

## Endpoints

Base: `http://localhost:8080/tienda-ecommerce-back/api`. Errores: `{"error": "mensaje"}` con 400, 401, 403, 404, 409, 413, 415, 429 o 500.

### Públicos
| Método | Ruta | Uso |
|---|---|---|
| GET | `/categorias` | Categorías con subcategorías |
| GET | `/productos` | Catálogo. Filtros: `categoria`, `subcategoria`, `q`, `ofertas=true`, `orden` (`precio_asc`, `precio_desc`, `recientes`, `calificacion`, `nombre`) |
| GET | `/productos/{sku}` | Detalle con variantes |
| GET | `/resenas?sku={sku}` | Reseñas de un producto |
| GET | `/metodos-pago` | Métodos de pago activos |

### Cuenta
| Método | Ruta | Cuerpo |
|---|---|---|
| POST | `/auth/registro` | `nombre, apellidos, email, contrasena, telefono?, fechaNacimiento?` |
| POST | `/auth/login` | `email, contrasena` |
| POST | `/auth/logout` | — |
| GET | `/auth/sesion` | — |
| POST | `/auth/recuperar` | `email` (el enlace sale en la consola de Tomcat) |
| POST | `/auth/restablecer` | `token, contrasena` |

### Cliente (requiere sesión)
| Método | Ruta | Uso |
|---|---|---|
| GET / PUT | `/perfil` | Ver y editar mis datos |
| PUT | `/perfil/contrasena` | `actual, nueva` |
| GET / POST | `/direcciones` | Listar / agregar (máximo 10) |
| GET / PUT / DELETE | `/direcciones/{id}` | Ver / editar / eliminar |
| PATCH | `/direcciones/{id}/principal` | Marcar como principal |
| GET / POST | `/favoritos` | Listar / agregar `{sku}` |
| DELETE | `/favoritos/{sku}` | Quitar |
| GET / POST | `/pedidos` | Mis pedidos / crear `{idDireccion, articulos:[{sku, idVariante?, cantidad}]}` |
| GET | `/pedidos/{id}` | Detalle |
| POST | `/pedidos/{id}/pago` | Tarjeta: `{idMetodo, numeroTarjeta, titular, vencimiento:"MM/AA", cvv}` · PayPal: `{idMetodo, referenciaPaypal}` |
| PATCH | `/pedidos/{id}/cancelar` | Cancelar un pedido pendiente |
| GET | `/resenas/mias` | Mis reseñas |
| POST | `/resenas` | `{sku, calificacion 1-5, comentario?}` (solo productos entregados) |
| PUT / DELETE | `/resenas/{id}` | Editar / borrar mi reseña |

### Panel (administrador o empleado)
| Método | Ruta | Uso |
|---|---|---|
| GET | `/admin/dashboard` | Indicadores, ventas por día, más vendidos, stock bajo, pedidos recientes |
| GET / POST | `/admin/productos` | Listar (incluye inactivos) / crear con variantes |
| GET / PUT / DELETE | `/admin/productos/{sku}` | Ver / editar / desactivar |
| POST | `/admin/productos/{sku}/variantes` | Agregar variante |
| PUT / DELETE | `/admin/productos/{sku}/variantes/{id}` | Editar / desactivar variante |
| GET / POST | `/admin/categorias` | Listar / crear |
| PUT / DELETE | `/admin/categorias/{id}` | Editar / eliminar (si no tiene productos) |
| POST | `/admin/subcategorias` | Crear |
| PUT / DELETE | `/admin/subcategorias/{id}` | Editar / eliminar (si no tiene productos) |
| GET / POST | `/admin/proveedores` | Listar / crear |
| GET / PUT / DELETE | `/admin/proveedores/{id}` | Ver / editar / eliminar (si no tiene productos) |
| GET | `/admin/pedidos?estado=` | Todos los pedidos |
| GET / PATCH | `/admin/pedidos/{id}` | Ver / cambiar estado (`pagado → enviado → entregado`; enviado pide paquetería y guía) |
| GET | `/admin/pagos` | Pagos |
| GET | `/admin/usuarios` | Usuarios |
| GET / PATCH | `/admin/usuarios/{id}` | Ver / cambiar `{idRol?, activo?}` (solo administrador) |
| GET | `/admin/resenas` | Reseñas |
| DELETE | `/admin/resenas/{id}` | Borrar reseña |

## Equipo

| Integrante | Parte |
|---|---|
| Levi Raúl Pérez Díaz | Base del proyecto, seguridad y catálogo |
| Josué Saúl López Trujillo | Usuarios: registro, login, perfil, direcciones, favoritos y reseñas |
| Ángel Raymundo Pablo Zenón | Compras: pedidos y pagos |
| Jonathan Eleazar Díaz Reyes | Panel: dashboard, productos, categorías y proveedores |
