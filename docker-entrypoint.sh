#!/bin/sh
set -eu

# Genera db.properties en el classpath compartido de Tomcat (/usr/local/tomcat/lib)
# para que Configuracion.java lo cargue sin incrustar credenciales en el .war
cat <<EOF > /usr/local/tomcat/lib/db.properties
db.url=${DB_URL:-jdbc:postgresql://db:5432/ecommerce_bd}
db.usuario=${DB_USER:-postgres}
db.contrasena=${DB_PASSWORD:-postgres}
db.pool.maximo=${DB_POOL_MAX:-10}
db.pool.minimo=${DB_POOL_MIN:-2}
cifrado.clave=${CIFRADO_CLAVE:-dnz8VsLjUv/E45JcEbflYg3hZKa0e0CXC2+R5LmoCso=}
cors.origen=${CORS_ORIGEN:-http://localhost:4200}
EOF

chmod 600 /usr/local/tomcat/lib/db.properties

exec "$@"
