#!/bin/bash
set -e
JWT_DIR="/deployments/jwt"
PRIV_KEY="$JWT_DIR/privateKey.pem"
PUB_KEY="$JWT_DIR/publicKey.pem"
if [ ! -f "$PRIV_KEY" ]; then
    echo "[entrypoint] Generando par de llaves RSA 2048 en $JWT_DIR"
    mkdir -p "$JWT_DIR"
    openssl genrsa -out "$PRIV_KEY" 2048
    openssl rsa -in "$PRIV_KEY" -pubout -out "$PUB_KEY"
    echo "[entrypoint] Llaves generadas: $PRIV_KEY $PUB_KEY"
else
    echo "[entrypoint] $PRIV_KEY ya existe, saltando generacion"
fi
exec java -jar /deployments/quarkus-run.jar
