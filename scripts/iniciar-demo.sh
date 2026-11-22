#!/bin/sh
set -eu
cd "$(dirname "$0")/.."

if [ -L .env ] || { [ -e .env ] && [ ! -f .env ]; }; then
    printf '%s\n' 'Usa un archivo .env local y regular.' >&2
    exit 1
fi
umask 077
touch .env
chmod 600 .env

agregar_clave() {
    if ! grep -Eq "^[[:space:]]*(export[[:space:]]+)?$1[[:space:]]*=" .env; then
        clave=$(openssl rand -hex "$2")
        printf '\n%s=%s\n' "$1" "$clave" >> .env
    fi
}

agregar_clave DB_PASSWORD 24
agregar_clave CMS_MANAGER_CLAVE 20
agregar_clave CMS_DEMO_REVISOR_CLAVE 20
agregar_clave CMS_DEMO_CREADOR_CLAVE 20

CMS_DEMO_ACTIVA=true docker compose up -d --build
printf '%s\n' 'Cuentas locales: Manager configurado, revisor-demo y creador-demo.' 'Consulta las contraseñas en .env.'
