# Migrating from the Java importer to Go

The Java importer is deprecated. Use
[Go OpenDiscogs Batch](https://github.com/dsub-io/go-open-discogs-batch) for imports
and [Go OpenDiscogs API](https://github.com/dsub-io/go-open-discogs-api) to serve
the imported data.

## Prepare the switch

1. Back up the database and record the Java version, selected schema, dump
   month, entities, chunk size, and current import state.
2. Stop the Java importer and any other importers before changing runtimes or
   applying canonical migrations.
3. Select a Go release from [GitHub Releases](https://github.com/dsub-io/go-open-discogs-batch/releases).
   Compare its bundled model with the database's migration ledger. Every
   importer that will run afterward must support the resulting ledger and
   entity contracts. Older Java releases may use a legacy schema; follow the
   model's [compatibility rules](https://github.com/dsub-io/open-discogs-model#schema-ownership).
4. Keep the same database and selected schema. Configure
   `OPEN_DISCOGS_BATCH_DATABASE_URL` through your existing secret mechanism,
   and review the Go [configuration reference](https://github.com/dsub-io/go-open-discogs-batch#configuration).
   Current Java and Go importers share option names, but historical Java
   releases can have different options.

With the database URL supplied through the environment, run the Go executable:

```sh
go-open-discogs-batch \
  --database-schema open_discogs \
  --entities artist,label,master,release
```

Replace `open_discogs` with your existing schema and select the same entities
and dump month intended for the Java run. See the Go
[quick start](https://github.com/dsub-io/go-open-discogs-batch#quick-start)
for dump selection.

## Interrupted imports

Java and Go can transfer a failed import ledger only when the manifest,
per-entity contract revision, entity and dump identity, and chunk size match.
Check the Go [resume contract](https://github.com/dsub-io/go-open-discogs-batch/blob/main/docs/import-safety.md#interruption-and-resume)
before relying on an existing Java checkpoint. Do not use `--force` just to
change runtimes; it restarts the manifest from zero.

Keep the database backup and the previous deployment configuration for
rollback. A previous Java artifact may reject a schema upgraded by the Go
importer, so reverting the executable alone may not restore the previous setup.
