#!/bin/bash
# Database backup script for BookStore
# Usage: ./backup-db.sh
# Scheduled via cron: 0 2 * * * /path/to/backup-db.sh

BACKUP_DIR="./backup"
DB_NAME="${DB_NAME:-mydatabase2}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/bookstore_${TIMESTAMP}.sql"

mkdir -p "$BACKUP_DIR"

# If running inside docker-compose
if command -v docker &> /dev/null; then
    docker exec bookstore-db mysqldump -u root -p"${DB_ROOT_PASSWORD:-rootpassword}" "$DB_NAME" > "$BACKUP_FILE"
else
    # Direct mysqldump
    mysqldump -u "${DB_USERNAME:-root}" -p"${DB_PASSWORD}" "$DB_NAME" > "$BACKUP_FILE"
fi

# Compress
gzip "$BACKUP_FILE"

# Remove backups older than 30 days
find "$BACKUP_DIR" -name "bookstore_*.sql.gz" -mtime +30 -delete

echo "Backup completed: ${BACKUP_FILE}.gz"
