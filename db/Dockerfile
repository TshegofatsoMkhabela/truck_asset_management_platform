# Bakes db/migrations and db/seed into Postgres's own docker-entrypoint-initdb.d at BUILD
# time, rather than bind-mounting the live host directories at run time.
#
# An earlier version bind-mounted db/migrations directly as /docker-entrypoint-initdb.d
# (read-write, since Docker cannot create a second mount's mountpoint inside a directory
# mounted read-only). That worked, but the read-write bind meant Docker created the seed
# file's mountpoint stub INSIDE the actual host db/migrations directory: a real file left
# behind in a git-tracked source folder after every `docker compose up`. Copying at build
# time avoids touching the host filesystem at all: the image gets its own layer, the named
# volume only ever holds Postgres's data directory, and nothing needs read-write access to
# source that should stay read-only.
FROM postgres:18-alpine

# Flat COPY, not a directory copy: docker-entrypoint-initdb.d only runs files it finds
# directly inside itself, not in subdirectories, so migrations and seed both land as
# siblings here. Ordering relies on the same filename convention ADR-2 already established
# (V01__..V11__ before Z01__), which COPY preserves exactly as it does for a bind mount.
COPY migrations/*.sql /docker-entrypoint-initdb.d/
COPY seed/dev-seed.sql /docker-entrypoint-initdb.d/Z01__dev_seed.sql
