#!/bin/bash
echo "Initializing PostgreSQL custom scripts execution..."

# Enable nullglob so *.sql expands to nothing if no files are found
shopt -s nullglob

# --- 01_Tables ---
if [ -d /docker-entrypoint-initdb.d/01_Tables ]; then
  files=(/docker-entrypoint-initdb.d/01_Tables/*.sql)
  if [ ${#files[@]} -gt 0 ]; then
    for sql_file in "${files[@]}"; do
      echo "Running $sql_file..."
      psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f "$sql_file"
    done
  else
    echo "No SQL files found in 01_Tables, skipping."
  fi
else
  echo "Directory 01_Tables not found, skipping."
fi

# --- 02_Constraints ---
if [ -d /docker-entrypoint-initdb.d/02_Constraints ]; then
  files=(/docker-entrypoint-initdb.d/02_Constraints/*.sql)
  if [ ${#files[@]} -gt 0 ]; then
    for sql_file in "${files[@]}"; do
      echo "Running $sql_file..."
      psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f "$sql_file"
    done
  else
    echo "No SQL files found in 02_Constraints, skipping."
  fi
else
  echo "Directory 02_Constraints not found, skipping."
fi

# Disable nullglob back
shopt -u nullglob

echo "PostgreSQL custom scripts execution completed."

