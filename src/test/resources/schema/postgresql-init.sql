CREATE TABLE public.label
(
    id               serial
        CONSTRAINT pk_label
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    contact_info     text,
    data_quality     varchar(255),
    name             varchar(255),
    profile          text
);

CREATE TABLE public.style
(
    name varchar(255) NOT NULL
        CONSTRAINT pk_style
            PRIMARY KEY
);

CREATE TABLE public.genre
(
    name varchar(255) NOT NULL
        CONSTRAINT pk_genre
            PRIMARY KEY
);

CREATE TABLE public.release_item
(
    id                  serial
        CONSTRAINT pk_release_item
            PRIMARY KEY,
    created_at          timestamp NOT NULL,
    last_modified_at    timestamp NOT NULL,
    country             varchar(255),
    data_quality        varchar(255),
    has_valid_day       boolean,
    has_valid_month     boolean,
    has_valid_year      boolean,
    is_master           boolean,
    master_id           integer,
    listed_release_date varchar(255),
    notes               text,
    release_date        date,
    status              varchar(255),
    title               varchar(10000)
);

CREATE TABLE public.release_item_genre
(
    id               serial
        CONSTRAINT pk_release_item_genre
            PRIMARY KEY,
    created_at       timestamp    NOT NULL,
    last_modified_at timestamp    NOT NULL,
    genre            varchar(255) NOT NULL
        CONSTRAINT fk_release_item_genre_genre_genre
            REFERENCES public.genre,
    release_item_id  integer      NOT NULL
        CONSTRAINT fk_release_item_genre_release_item_id_release_item
            REFERENCES public.release_item,
    CONSTRAINT uq_release_item_genre_release_item_id_genre
        UNIQUE (release_item_id, genre)
);

CREATE TABLE public.release_item_track
(
    id               serial
        CONSTRAINT pk_release_item_track
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    hash             integer   NOT NULL,
    duration         text,
    position         text,
    title            text,
    release_item_id  integer   NOT NULL
        CONSTRAINT fk_release_item_track_release_item_id_release_item
            REFERENCES public.release_item,
    CONSTRAINT uq_release_item_track_release_item_id_hash
        UNIQUE (release_item_id, hash)
);

CREATE TABLE public.label_release_item
(
    id                serial
        CONSTRAINT pk_label_release_item
            PRIMARY KEY,
    created_at        timestamp NOT NULL,
    last_modified_at  timestamp NOT NULL,
    category_notation varchar(1000),
    label_id          integer   NOT NULL
        CONSTRAINT fk_label_release_label_id_label
            REFERENCES public.label,
    release_item_id   integer   NOT NULL
        CONSTRAINT fk_label_release_release_item_id_release_item
            REFERENCES public.release_item,
    CONSTRAINT uq_label_release_item_release_item_id_label_id
        UNIQUE (release_item_id, label_id)
);

CREATE TABLE public.release_item_image
(
    id               serial
        CONSTRAINT pk_release_item_image
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    hash             integer   NOT NULL,
    file_name        text,
    release_item_id  integer   NOT NULL
        CONSTRAINT fk_release_item_image_release_item_id_release_item
            REFERENCES public.release_item,
    CONSTRAINT uq_release_item_image_release_item_id_hash
        UNIQUE (release_item_id, hash)
);

CREATE TABLE public.release_item_work
(
    id               serial
        CONSTRAINT pk_release_item_work
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    hash             integer   NOT NULL,
    work             varchar(5000),
    label_id         integer   NOT NULL
        CONSTRAINT fk_release_item_work_label_id_label
            REFERENCES public.label,
    release_item_id  integer   NOT NULL
        CONSTRAINT fk_release_item_work_release_item_id_release_item
            REFERENCES public.release_item,
    CONSTRAINT uq_release_item_work_release_item_id_label_id_hash
        UNIQUE (release_item_id, label_id, hash)
);

CREATE TABLE public.release_item_identifier
(
    id               serial
        CONSTRAINT pk_release_item_identifier
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    hash             integer   NOT NULL,
    description      text,
    type             text,
    value            text,
    release_item_id  integer   NOT NULL
        CONSTRAINT fk_release_item_identifier_release_item_id_release_item
            REFERENCES public.release_item,
    CONSTRAINT uq_release_item_identifier_release_item_id_hash
        UNIQUE (release_item_id, hash)
);

CREATE TABLE public.master
(
    id               serial
        CONSTRAINT pk_master
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    data_quality     varchar(255),
    title            varchar(2000),
    year             smallint,
    main_release_id  integer
        CONSTRAINT fk_master_main_release_id_release_item
            REFERENCES public.release_item
);

CREATE TABLE public.master_video
(
    id               serial
        CONSTRAINT pk_master_video
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    hash             integer   NOT NULL,
    description      varchar(15000),
    title            varchar(1000),
    url              varchar(1000),
    master_id        integer   NOT NULL
        CONSTRAINT fk_master_video_master_id_master
            REFERENCES public.master,
    CONSTRAINT uq_master_video_master_id_hash
        UNIQUE (master_id, hash)
);

ALTER TABLE public.release_item
    ADD CONSTRAINT fk_release_item_master_id_master
        FOREIGN KEY (master_id)
            REFERENCES public.master;

CREATE TABLE public.master_genre
(
    id               serial
        CONSTRAINT pk_master_genre
            PRIMARY KEY,
    created_at       timestamp    NOT NULL,
    last_modified_at timestamp    NOT NULL,
    genre            varchar(255) NOT NULL
        CONSTRAINT fk_master_genre_genre_genre
            REFERENCES public.genre,
    master_id        integer      NOT NULL
        CONSTRAINT fk_master_genre_master_id_master
            REFERENCES public.master,
    CONSTRAINT uq_master_genre_master_id_genre
        UNIQUE (master_id, genre)
);

CREATE TABLE public.master_style
(
    id               serial
        CONSTRAINT pk_master_style
            PRIMARY KEY,
    created_at       timestamp    NOT NULL,
    last_modified_at timestamp    NOT NULL,
    master_id        integer      NOT NULL
        CONSTRAINT fk_master_style_master_id_master
            REFERENCES public.master,
    style            varchar(255) NOT NULL
        CONSTRAINT fk_master_style_style_style
            REFERENCES public.style,
    CONSTRAINT uq_master_style_master_id_style
        UNIQUE (master_id, style)
);

CREATE TABLE public.release_item_style
(
    id               serial
        CONSTRAINT pk_release_item_style
            PRIMARY KEY,
    created_at       timestamp    NOT NULL,
    last_modified_at timestamp    NOT NULL,
    release_item_id  integer      NOT NULL
        CONSTRAINT fk_release_item_style_release_item_id_release_item
            REFERENCES public.release_item,
    style            varchar(255) NOT NULL
        CONSTRAINT fk_release_item_style_style_style
            REFERENCES public.style,
    CONSTRAINT uq_release_item_style_release_item_id_style
        UNIQUE (release_item_id, style)
);

CREATE TABLE public.label_sub_label
(
    id               serial
        CONSTRAINT pk_label_sub_label
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    parent_label_id  integer   NOT NULL
        CONSTRAINT fk_label_sub_label_parent_label_id_label
            REFERENCES public.label,
    sub_label_id     integer   NOT NULL
        CONSTRAINT fk_label_sub_label_sub_label_id_label
            REFERENCES public.label,
    CONSTRAINT uq_label_sub_label_parent_label_id_sub_label_id
        UNIQUE (parent_label_id, sub_label_id)
);

CREATE TABLE public.release_item_video
(
    id               serial
        CONSTRAINT pk_release_item_video
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    hash             integer   NOT NULL,
    description      text,
    title            text,
    url              text,
    release_item_id  integer   NOT NULL
        CONSTRAINT fk_release_item_video_release_item_id_release_item
            REFERENCES public.release_item,
    CONSTRAINT uq_release_item_video_release_item_id_hash
        UNIQUE (release_item_id, hash)
);

CREATE TABLE public.label_url
(
    id               serial
        CONSTRAINT pk_label_url
            PRIMARY KEY,
    created_at       timestamp     NOT NULL,
    last_modified_at timestamp     NOT NULL,
    hash             integer       NOT NULL,
    url              varchar(5000) NOT NULL,
    label_id         integer       NOT NULL
        CONSTRAINT fk_label_url_label_id_label
            REFERENCES public.label,
    CONSTRAINT uq_label_url_label_id_hash
        UNIQUE (label_id, hash)
);

CREATE TABLE public.release_item_format
(
    id               serial
        CONSTRAINT pk_release_item_format
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    hash             integer   NOT NULL,
    description      varchar(10000),
    name             varchar(255),
    quantity         integer,
    text             varchar(5000),
    release_item_id  integer   NOT NULL
        CONSTRAINT fk_release_item_format_release_item_id_release_item
            REFERENCES public.release_item,
    CONSTRAINT uq_release_item_format_release_item_id_hash
        UNIQUE (release_item_id, hash)
);

CREATE TABLE public.artist
(
    id               serial
        CONSTRAINT pk_artist
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    data_quality     varchar(255),
    name             varchar(1000),
    profile          text,
    real_name        varchar(2000)
);

CREATE TABLE public.artist_alias
(
    id               serial
        CONSTRAINT pk_artist_alias
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    alias_id         integer   NOT NULL
        CONSTRAINT fk_artist_alias_alias_id_artist
            REFERENCES public.artist,
    artist_id        integer   NOT NULL
        CONSTRAINT fk_artist_alias_artist_id_artist
            REFERENCES public.artist,
    CONSTRAINT uq_artist_alias_artist_id_alias_id
        UNIQUE (artist_id, alias_id)
);

CREATE TABLE public.artist_name_variation
(
    id               serial
        CONSTRAINT pk_artist_name_variation
            PRIMARY KEY,
    created_at       timestamp     NOT NULL,
    hash             integer       NOT NULL,
    last_modified_at timestamp     NOT NULL,
    name_variation   varchar(2000) NOT NULL,
    artist_id        integer       NOT NULL
        CONSTRAINT fk_artist_name_variation_artist_id_artist
            REFERENCES public.artist,
    CONSTRAINT uq_artist_name_variation_artist_id_hash
        UNIQUE (artist_id, hash)
);

CREATE TABLE public.master_artist
(
    id               serial
        CONSTRAINT pk_master_artist
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    artist_id        integer   NOT NULL
        CONSTRAINT fk_master_artist_artist_id_artist
            REFERENCES public.artist,
    master_id        integer   NOT NULL
        CONSTRAINT fk_master_artist_master_id_master
            REFERENCES public.master,
    CONSTRAINT uq_master_artist_master_id_artist_id
        UNIQUE (master_id, artist_id)
);

CREATE TABLE public.release_item_artist
(
    id               serial
        CONSTRAINT pk_release_item_artist
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    artist_id        integer   NOT NULL
        CONSTRAINT fk_release_item_artist_artist_id_artist
            REFERENCES public.artist,
    release_item_id  integer   NOT NULL
        CONSTRAINT fk_release_item_artist_release_item_id_release_item
            REFERENCES public.release_item,
    CONSTRAINT uq_release_item_artist_release_item_id_artist_id
        UNIQUE (release_item_id, artist_id)
);

CREATE TABLE public.release_item_credited_artist
(
    id               serial
        CONSTRAINT pk_release_item_credited_artist
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    hash             integer   NOT NULL,
    role             varchar(20000),
    artist_id        integer   NOT NULL
        CONSTRAINT fk_release_item_credited_artist_artist_id_artist
            REFERENCES public.artist,
    release_item_id  integer   NOT NULL
        CONSTRAINT fk_release_item_credited_artist_release_item_id_release_item
            REFERENCES public.release_item,
    CONSTRAINT uq_release_item_credited_artist_release_item_id_artist_id_hash
        UNIQUE (release_item_id, artist_id, hash)
);

CREATE TABLE public.artist_url
(
    id               serial
        CONSTRAINT pk_artist_url
            PRIMARY KEY,
    created_at       timestamp     NOT NULL,
    last_modified_at timestamp     NOT NULL,
    hash             integer       NOT NULL,
    url              varchar(5000) NOT NULL,
    artist_id        integer       NOT NULL
        CONSTRAINT fk_artist_url_artist_id_artist
            REFERENCES public.artist,
    CONSTRAINT uq_artist_url_artist_id_hash
        UNIQUE (artist_id, hash)
);

CREATE TABLE public.artist_group
(
    id               serial
        CONSTRAINT pk_artist_group
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    artist_id        integer   NOT NULL
        CONSTRAINT fk_artist_group_artist_id_artist
            REFERENCES public.artist,
    group_id         integer   NOT NULL
        CONSTRAINT fk_artist_group_group_id_artist
            REFERENCES public.artist,
    CONSTRAINT uq_artist_group_artist_id_group_id
        UNIQUE (artist_id, group_id)
);

CREATE TABLE public.artist_member
(
    id               serial
        CONSTRAINT pk_artist_member
            PRIMARY KEY,
    created_at       timestamp NOT NULL,
    last_modified_at timestamp NOT NULL,
    artist_id        integer   NOT NULL
        CONSTRAINT fk_artist_member_artist_id_artist
            REFERENCES public.artist,
    member_id        integer   NOT NULL
        CONSTRAINT fk_artist_member_member_id_artist
            REFERENCES public.artist,
    CONSTRAINT uq_artist_member_artist_id_member_id
        UNIQUE (artist_id, member_id)
);

CREATE TABLE batch_job_instance
(
    job_instance_id BIGINT       NOT NULL
        PRIMARY KEY,
    version         BIGINT,
    job_name        VARCHAR(100) NOT NULL,
    job_key         VARCHAR(32)  NOT NULL,
    CONSTRAINT job_inst_un
        UNIQUE (job_name, job_key)
);

CREATE TABLE batch_job_execution
(
    job_execution_id           BIGINT        NOT NULL
        PRIMARY KEY,
    version                    BIGINT,
    job_instance_id            BIGINT        NOT NULL,
    create_time                TIMESTAMP     NOT NULL,
    start_time                 TIMESTAMP DEFAULT NULL,
    end_time                   TIMESTAMP DEFAULT NULL,
    status                     VARCHAR(10),
    exit_code                  VARCHAR(2500),
    exit_message               VARCHAR(2500),
    last_updated               TIMESTAMP,
    job_configuration_location VARCHAR(2500) NULL,
    CONSTRAINT job_inst_exec_fk
        FOREIGN KEY (job_instance_id)
            REFERENCES batch_job_instance (job_instance_id)
);

CREATE TABLE batch_job_execution_params
(
    job_execution_id BIGINT       NOT NULL,
    type_cd          VARCHAR(6)   NOT NULL,
    key_name         VARCHAR(100) NOT NULL,
    string_val       VARCHAR(250),
    date_val         TIMESTAMP DEFAULT NULL,
    long_val         BIGINT,
    double_val       DOUBLE PRECISION,
    identifying      CHAR(1)      NOT NULL,
    CONSTRAINT job_exec_params_fk
        FOREIGN KEY (job_execution_id)
            REFERENCES batch_job_execution (job_execution_id)
);

CREATE TABLE batch_step_execution
(
    step_execution_id  BIGINT       NOT NULL
        PRIMARY KEY,
    version            BIGINT       NOT NULL,
    step_name          VARCHAR(100) NOT NULL,
    job_execution_id   BIGINT       NOT NULL,
    start_time         TIMESTAMP    NOT NULL,
    end_time           TIMESTAMP DEFAULT NULL,
    status             VARCHAR(10),
    commit_count       BIGINT,
    read_count         BIGINT,
    filter_count       BIGINT,
    write_count        BIGINT,
    read_skip_count    BIGINT,
    write_skip_count   BIGINT,
    process_skip_count BIGINT,
    rollback_count     BIGINT,
    exit_code          VARCHAR(2500),
    exit_message       VARCHAR(2500),
    last_updated       TIMESTAMP,
    CONSTRAINT job_exec_step_fk
        FOREIGN KEY (job_execution_id)
            REFERENCES batch_job_execution (job_execution_id)
);

CREATE TABLE batch_step_execution_context
(
    step_execution_id  BIGINT        NOT NULL
        PRIMARY KEY,
    short_context      VARCHAR(2500) NOT NULL,
    serialized_context TEXT,
    CONSTRAINT step_exec_ctx_fk
        FOREIGN KEY (step_execution_id)
            REFERENCES batch_step_execution (step_execution_id)
);

CREATE TABLE batch_job_execution_context
(
    job_execution_id   BIGINT        NOT NULL
        PRIMARY KEY,
    short_context      VARCHAR(2500) NOT NULL,
    serialized_context TEXT,
    CONSTRAINT job_exec_ctx_fk
        FOREIGN KEY (job_execution_id)
            REFERENCES batch_job_execution (job_execution_id)
);

CREATE SEQUENCE batch_step_execution_seq MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE batch_job_execution_seq MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE batch_job_seq MAXVALUE 9223372036854775807 NO CYCLE;