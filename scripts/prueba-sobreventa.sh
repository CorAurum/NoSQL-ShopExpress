#!/usr/bin/env bash
# -----------------------------------------------------------------------------
# Prueba de concurrencia: N pedidos simultáneos sobre un producto con STOCK unidades.
# Resultado esperado: exactamente STOCK pedidos confirmados y stock final 0.
#
# Uso:  ./scripts/prueba-sobreventa.sh [STOCK] [PEDIDOS]
#       (por defecto 5 unidades y 20 pedidos)
# Requiere bash y curl (en Windows: Git Bash o WSL).
# -----------------------------------------------------------------------------
set -euo pipefail

STOCK=${1:-5}
PEDIDOS=${2:-20}
CATALOGO=${CATALOGO_URL:-http://localhost:8082}
PEDIDOS_URL=${PEDIDOS_URL:-http://localhost:8083}
USUARIO=${USUARIO_ID:-usr_12345}
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

echo "==> Creando producto con stock $STOCK"
RESP=$(curl -s -X POST "$CATALOGO/api/productos" -H "Content-Type: application/json" -d "{
  \"sku\": \"CONC-$(date +%s%N)\",
  \"nombre\": \"Producto concurrencia\",
  \"categoria\": {\"codigo\": \"TEST\", \"nombre\": \"Pruebas\"},
  \"precio\": 10.00,
  \"stock_inicial\": $STOCK
}")
PRODUCTO=$(echo "$RESP" | sed -n 's/.*"id":"\([^"]*\)".*/\1/p')
if [ -z "$PRODUCTO" ]; then
  echo "No se pudo crear el producto: $RESP"; exit 1
fi
echo "    producto_id = $PRODUCTO"

echo "==> Lanzando $PEDIDOS pedidos en paralelo (1 unidad cada uno)"
for i in $(seq 1 "$PEDIDOS"); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST "$PEDIDOS_URL/api/pedidos" \
    -H "Content-Type: application/json" \
    -d "{\"usuario_id\":\"$USUARIO\",\"items\":[{\"producto_id\":\"$PRODUCTO\",\"cantidad\":1}],\"pago\":{\"metodo\":\"TRANSFERENCIA\"}}" \
    > "$TMP/$i" &
done
wait

OK=$(cat "$TMP"/* | grep -c '^201$' || true)
CONFLICTO=$(cat "$TMP"/* | grep -c '^409$' || true)
OTROS=$((PEDIDOS - OK - CONFLICTO))

# Lectura fuerte (sin caché) directamente desde MongoDB.
FINAL=$(curl -s "$CATALOGO/api/inventario/stock/$PRODUCTO")
DISPONIBLE=$(echo "$FINAL" | sed -n 's/.*"stock_disponible":\([0-9-]*\).*/\1/p')

echo
echo "  Pedidos confirmados (201): $OK"
echo "  Rechazados por stock (409): $CONFLICTO"
echo "  Otros códigos:              $OTROS"
echo "  Stock disponible final:     $DISPONIBLE"
echo
if [ "$OK" -eq "$STOCK" ] && [ "$DISPONIBLE" -eq 0 ]; then
  echo "OK: no hubo sobreventa."
else
  echo "ATENCIÓN: resultado inesperado."; exit 1
fi
