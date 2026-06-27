#!/bin/bash
set -e

awslocal secretsmanager create-secret \
  --name /mithril-vault/mongodb \
  --secret-string '{"uri":"mongodb://root:root@localhost:27017/mithril_vault?authSource=admin&replicaSet=rs0"}'

awslocal secretsmanager create-secret \
  --name /mithril-vault/jwt \
  --secret-string '{"secretKey":"local-dev-jwt-secret-at-least-32-chars!!"}'
