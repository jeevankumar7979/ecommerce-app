#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT_DIR"

for service in config-server discovery-server api-gateway order-service inventory-service payment-service notification-service; do
  echo "==> Testing $service"
  gradle --no-daemon -p "$service" clean test bootJar
done
