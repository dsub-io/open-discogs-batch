package io.dsub.discogs.batch.job.progress;

final class ImportProgressQueries {

  static final String FIND_CHUNK =
      """
      select first_item_index, item_count
      from discogs_import_run_chunk
      where import_run_id = ?
        and entity_type = ?
        and chunk_index = ?
      """;

  static final String FIND_COMPLETED_CHUNKS =
      """
      select chunk_index, first_item_index, item_count
      from discogs_import_run_chunk
      where import_run_id = ?
        and entity_type = ?
      order by chunk_index
      """;

  static final String FIND_COMPLETED_ENTITY =
      """
      select total_items
      from discogs_import_run_dump
      where import_run_id = ?
        and entity_type = ?
        and chunk_size = ?
        and completed_at is not null
        and total_items is not null
        and total_chunks is not null
        and processed_items = total_items
      """;

  static final String RECORD_CHUNK =
      """
      with active_run as (
        select id
        from discogs_import_run
        where id = ?
          and status = 'running'
        for update
      ),
      inserted as (
        insert into discogs_import_run_chunk
            (import_run_id, entity_type, chunk_index, first_item_index, item_count)
        select active_run.id, ?, ?, ?, ?
        from active_run
        on conflict do nothing
        returning item_count
      )
      update discogs_import_run_dump run_dump
      set processed_items = run_dump.processed_items + inserted.item_count,
          last_progress_at = now()
      from inserted
      where run_dump.import_run_id = ?
        and run_dump.entity_type = ?
        and run_dump.chunk_size = ?
        and run_dump.completed_at is null
      """;

  static final String FENCE_ACTIVE_RUN =
      """
      select id
      from discogs_import_run
      where id = ?
        and status = 'running'
      for update
      """;

  static final String READ_PROGRESS =
      """
      select processed_items, total_items, last_progress_at
      from discogs_import_run_dump
      where import_run_id = ?
        and entity_type = ?
      """;

  static final String SUMMARIZE_PROGRESS =
      """
      select count(*) as total_chunks,
             coalesce(sum(item_count), 0) as total_items
      from discogs_import_run_chunk
      where import_run_id = ?
        and entity_type = ?
      """;

  static final String COMPLETE_ENTITY =
      """
      with active_run as (
        select id
        from discogs_import_run
        where id = ?
          and status = 'running'
        for update
      ),
      coverage as (
        select count(*) as completed_chunks,
               coalesce(sum(item_count), 0) as completed_items,
               count(*) filter (
                 where chunk_index >= ?
                    or first_item_index <> chunk_index * ?
                    or item_count <> case
                      when chunk_index = ? - 1 then ? - first_item_index
                      else ?
                    end
               ) as invalid_chunks
        from discogs_import_run_chunk
        where import_run_id = ?
          and entity_type = ?
      )
      update discogs_import_run_dump
      set total_items = ?,
          total_chunks = ?,
          completed_at = now(),
          last_progress_at = now()
      from coverage, active_run
      where import_run_id = active_run.id
        and entity_type = ?
        and chunk_size = ?
        and processed_items = ?
        and coverage.completed_chunks = ?
        and coverage.completed_items = ?
        and coverage.invalid_chunks = 0
      """;

  private ImportProgressQueries() {
  }
}
