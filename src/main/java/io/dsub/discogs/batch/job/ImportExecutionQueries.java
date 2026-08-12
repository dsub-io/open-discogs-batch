package io.dsub.discogs.batch.job;

final class ImportExecutionQueries {

  static final String MARK_ABANDONED =
      """
      update discogs_import_run import_run
      set status = 'failed',
          completed_at = now(),
          failure_message = 'recovered after entity advisory locks were released'
      where import_run.status = 'running'
        and exists (
          select 1
          from discogs_import_run_dump run_dump
          where run_dump.import_run_id = import_run.id
            and run_dump.entity_type = any (?)
        )
      """;

  static final String FIND_CURRENT_CHECKPOINT_DATE =
      """
      select dump_date
      from discogs_import_checkpoint
      where entity_type = ?
      """;

  static final String FIND_DEPENDENCY_CHECKPOINT =
      """
      select checkpoint.dump_date as checkpoint_date,
             checkpoint.checksum_sha256 as checkpoint_checksum,
             expected.dump_date as expected_date,
             expected.checksum_sha256 as expected_checksum
      from discogs_import_checkpoint checkpoint
      left join lateral (
        select dump.dump_date,
               dump.checksum_sha256
        from discogs_dump dump
        where dump.entity_type = ?
          and dump.dump_date < ?
        order by dump.dump_date desc, dump.id desc
        limit 1
      ) expected on true
      where checkpoint.entity_type = ?
      """;

  static final String FIND_CURRENT_SUCCESS =
      """
      select candidate_run.id
      from discogs_import_run candidate_run
      where candidate_run.manifest_sha256 = ?
        and candidate_run.status = 'success'
        and not exists (
          select 1
          from discogs_import_run_dump revision_dump
          where revision_dump.import_run_id = candidate_run.id
            and revision_dump.import_contract_revision is distinct from
                case revision_dump.entity_type
                  when 'artist' then ?
                  when 'label' then ?
                  when 'master' then ?
                  when 'release' then ?
                end
        )
        and not exists (
          select 1
          from discogs_import_run_dump candidate_dump
          left join discogs_import_checkpoint checkpoint
            on checkpoint.entity_type = candidate_dump.entity_type
          left join discogs_import_run_dump current_dump
            on current_dump.import_run_id = checkpoint.import_run_id
           and current_dump.entity_type = checkpoint.entity_type
          where candidate_dump.import_run_id = candidate_run.id
            and current_dump.dump_id is distinct from candidate_dump.dump_id
        )
        and not exists (
          select 1
          from discogs_import_run_dump candidate_dump
          join discogs_import_checkpoint checkpoint
            on checkpoint.entity_type = candidate_dump.entity_type
          join discogs_import_run_dump failed_dump
            on failed_dump.entity_type = candidate_dump.entity_type
          join discogs_import_run failed_run
            on failed_run.id = failed_dump.import_run_id
          where candidate_dump.import_run_id = candidate_run.id
            and failed_run.status = 'failed'
            and (failed_run.completed_at > checkpoint.applied_at
                 or (failed_run.completed_at = checkpoint.applied_at
                     and failed_run.id > checkpoint.import_run_id))
        )
      order by candidate_run.completed_at desc, candidate_run.id desc
      limit 1
      """;

  static final String FIND_RESUMABLE_RUN =
      """
      select import_run.id
      from discogs_import_run import_run
      where import_run.manifest_sha256 = ?
        and import_run.status = 'failed'
        and import_run.processor = ?
        and import_run.processor_version = ?
        and not import_run.force_requested
        and not exists (
          select 1
          from discogs_import_run_dump revision_dump
          where revision_dump.import_run_id = import_run.id
            and revision_dump.import_contract_revision is distinct from
                case revision_dump.entity_type
                  when 'artist' then ?
                  when 'label' then ?
                  when 'master' then ?
                  when 'release' then ?
                end
        )
        and (select count(*)
             from discogs_import_run_dump run_dump
             where run_dump.import_run_id = import_run.id) = ?
        and not exists (
          select 1
          from discogs_import_run_dump run_dump
          where run_dump.import_run_id = import_run.id
            and run_dump.chunk_size is distinct from ?
        )
        and not exists (
          select 1
          from discogs_import_run_dump run_dump
          where run_dump.import_run_id = import_run.id
            and run_dump.processed_items <>
                (select coalesce(sum(run_chunk.item_count), 0)
                 from discogs_import_run_chunk run_chunk
                 where run_chunk.import_run_id = run_dump.import_run_id
                   and run_chunk.entity_type = run_dump.entity_type)
        )
        and not exists (
          select 1
          from discogs_import_run_chunk run_chunk
          join discogs_import_run_dump run_dump
            on run_dump.import_run_id = run_chunk.import_run_id
           and run_dump.entity_type = run_chunk.entity_type
          where run_chunk.import_run_id = import_run.id
            and (run_chunk.first_item_index <> run_chunk.chunk_index * run_dump.chunk_size
                 or run_chunk.item_count > run_dump.chunk_size)
        )
        and not exists (
          select 1
          from discogs_import_run_dump failed_dump
          join discogs_import_checkpoint checkpoint
            on checkpoint.entity_type = failed_dump.entity_type
          join discogs_import_run_dump current_dump
            on current_dump.import_run_id = checkpoint.import_run_id
           and current_dump.entity_type = checkpoint.entity_type
          join discogs_import_run current_run
            on current_run.id = checkpoint.import_run_id
          where failed_dump.import_run_id = import_run.id
            and (current_dump.dump_id <> failed_dump.dump_id
                 or current_run.processor <> import_run.processor
                 or current_run.processor_version <> import_run.processor_version
                 or current_dump.import_contract_revision
                    <> failed_dump.import_contract_revision)
            and (checkpoint.applied_at > import_run.completed_at
                 or (checkpoint.applied_at = import_run.completed_at
                     and checkpoint.import_run_id > import_run.id))
        )
      order by import_run.completed_at desc, import_run.id desc
      limit 1
      """;

  static final String INSERT_DUMP =
      """
      insert into discogs_dump
          (etag, dump_date, entity_type, checksum_sha256, size_bytes, uri)
      values (?, ?, ?, ?, ?, ?)
      on conflict (dump_date, entity_type, checksum_sha256) do nothing
      returning id
      """;

  static final String FIND_DUMP =
      """
      select id
      from discogs_dump
      where dump_date = ?
        and entity_type = ?
        and checksum_sha256 = ?
      """;

  static final String INSERT_RUN =
      """
      insert into discogs_import_run
          (manifest_sha256, status, force_requested, allow_downgrade_requested,
           processor, processor_version, resumed_from_run_id)
      values (?, 'running', ?, ?, ?, ?, ?)
      """;

  static final String INSERT_RUN_DUMP =
      """
      insert into discogs_import_run_dump
          (import_run_id, entity_type, dump_id, chunk_size, import_contract_revision)
      values (?, ?, ?, ?, ?)
      """;

  static final String COPY_RESUME_SUMMARIES =
      """
      update discogs_import_run_dump target
      set processed_items = source.processed_items,
          total_items = source.total_items,
          total_chunks = source.total_chunks,
          last_progress_at = source.last_progress_at,
          completed_at = source.completed_at
      from discogs_import_run_dump source
      where target.import_run_id = ?
        and source.import_run_id = ?
        and target.entity_type = source.entity_type
        and target.dump_id = source.dump_id
        and target.chunk_size = source.chunk_size
      """;

  static final String COPY_RESUME_CHUNKS =
      """
      insert into discogs_import_run_chunk
          (import_run_id, entity_type, chunk_index, first_item_index, item_count, completed_at)
      select ?, entity_type, chunk_index, first_item_index, item_count, completed_at
      from discogs_import_run_chunk
      where import_run_id = ?
      """;

  static final String DELETE_RUN_CHUNKS =
      "delete from discogs_import_run_chunk where import_run_id = ?";

  static final String COUNT_INCOMPLETE_ENTITIES =
      """
      select count(*)
      from discogs_import_run_dump
      where import_run_id = ?
        and (completed_at is null
             or total_items is null
             or total_chunks is null
             or processed_items <> total_items)
      """;

  static final String COMPLETE_RUN =
      """
      update discogs_import_run
      set status = ?,
          completed_at = now(),
          failure_message = ?
      where id = ?
        and status = 'running'
      """;

  static final String PRUNE_SUPERSEDED_FAILED_PROGRESS =
      """
      delete from discogs_import_run_chunk run_chunk
      where run_chunk.import_run_id in (
        select failed_run.id
        from discogs_import_run failed_run
        where failed_run.status = 'failed'
          and not exists (
            select 1
            from discogs_import_run_dump failed_dump
            left join discogs_import_checkpoint checkpoint
              on checkpoint.entity_type = failed_dump.entity_type
            left join discogs_import_run current_run
              on current_run.id = checkpoint.import_run_id
            left join discogs_import_run_dump current_dump
              on current_dump.import_run_id = checkpoint.import_run_id
             and current_dump.entity_type = checkpoint.entity_type
            where failed_dump.import_run_id = failed_run.id
              and (current_dump.dump_id is distinct from failed_dump.dump_id
                   or current_run.processor is distinct from failed_run.processor
                   or current_run.processor_version
                      is distinct from failed_run.processor_version
                   or current_dump.import_contract_revision
                      is distinct from failed_dump.import_contract_revision)
          )
      )
      """;

  private ImportExecutionQueries() {
  }
}
