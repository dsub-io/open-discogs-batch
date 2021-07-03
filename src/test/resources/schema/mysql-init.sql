SET REFERENTIAL_INTEGRITY FALSE;

create table if not exists artist
(
    id               int auto_increment
        primary key,
    created_at       timestamp                  not null,
    last_modified_at timestamp                  not null,
    data_quality     varchar(255) charset utf8  null,
    name             varchar(1000) charset utf8 null,
    profile          longtext                   null,
    real_name        varchar(2000) charset utf8 null
);

create table if not exists artist_alias
(
    id               int auto_increment
        primary key,
    created_at       timestamp not null,
    last_modified_at timestamp not null,
    alias_id         int       not null,
    artist_id        int       not null,
    constraint uq_artist_alias_artist_id_alias_id
        unique (artist_id, alias_id),
    constraint fk_artist_alias_alias_id_artist
        foreign key (alias_id) references artist (id),
    constraint fk_artist_alias_artist_id_artist
        foreign key (artist_id) references artist (id)
);

create table if not exists artist_group
(
    id               int auto_increment
        primary key,
    created_at       timestamp not null,
    last_modified_at timestamp not null,
    artist_id        int       not null,
    group_id         int       not null,
    constraint uq_artist_group_artist_id_group_id
        unique (artist_id, group_id),
    constraint fk_artist_group_artist_id_artist
        foreign key (artist_id) references artist (id),
    constraint fk_artist_group_group_id_artist
        foreign key (group_id) references artist (id)
);

create table if not exists artist_member
(
    id               int auto_increment
        primary key,
    created_at       timestamp not null,
    last_modified_at timestamp not null,
    artist_id        int       not null,
    member_id        int       not null,
    constraint uq_artist_member_artist_id_member_id
        unique (artist_id, member_id),
    constraint fk_artist_member_artist_id_artist
        foreign key (artist_id) references artist (id),
    constraint fk_artist_member_member_id_artist
        foreign key (member_id) references artist (id)
);

create table if not exists artist_name_variation
(
    id               int auto_increment
        primary key,
    created_at       timestamp                  not null,
    hash             int                        not null,
    last_modified_at timestamp                  not null,
    name_variation   varchar(2000) charset utf8 not null,
    artist_id        int                        not null,
    constraint uq_artist_name_variation_artist_id_hash
        unique (artist_id, hash),
    constraint fk_artist_name_variation_artist_id_artist
        foreign key (artist_id) references artist (id)
);

create table if not exists artist_url
(
    id               int auto_increment
        primary key,
    created_at       timestamp                  not null,
    last_modified_at timestamp                  not null,
    hash             int                        not null,
    url              varchar(5000) charset utf8 not null,
    artist_id        int                        not null,
    constraint uq_artist_url_artist_id_hash
        unique (artist_id, hash),
    constraint fk_artist_url_artist_id_artist
        foreign key (artist_id) references artist (id)
);

create table if not exists genre
(
    name varchar(255) charset utf8 not null
        primary key
);

create table if not exists label
(
    id               int auto_increment
        primary key,
    created_at       timestamp                 not null,
    last_modified_at timestamp                 not null,
    contact_info     longtext                  null,
    data_quality     varchar(255) charset utf8 null,
    name             varchar(255) charset utf8 null,
    profile          longtext                  null
);

create table if not exists label_sub_label
(
    id               int auto_increment
        primary key,
    created_at       timestamp not null,
    last_modified_at timestamp not null,
    parent_label_id  int       not null,
    sub_label_id     int       not null,
    constraint uq_label_sub_label_parent_label_id_sub_label_id
        unique (parent_label_id, sub_label_id),
    constraint fk_label_sub_label_parent_label_id_label
        foreign key (parent_label_id) references label (id),
    constraint fk_label_sub_label_sub_label_id_label
        foreign key (sub_label_id) references label (id)
);

create table if not exists label_url
(
    id               int auto_increment
        primary key,
    created_at       timestamp     not null,
    last_modified_at timestamp     not null,
    hash             int           not null,
    url              varchar(5000) not null,
    label_id         int           not null,
    constraint uq_label_url_label_id_hash
        unique (label_id, hash),
    constraint fk_label_url_label_id_label
        foreign key (label_id) references label (id)
);

create table if not exists master
(
    id               int auto_increment
        primary key,
    created_at       timestamp                  not null,
    last_modified_at timestamp                  not null,
    data_quality     varchar(255) charset utf8  null,
    title            varchar(2000) charset utf8 null,
    year             smallint                   null,
    main_release_id  int                        null
);

create table if not exists master_artist
(
    id               int auto_increment
        primary key,
    created_at       timestamp not null,
    last_modified_at timestamp not null,
    artist_id        int       not null,
    master_id        int       not null,
    constraint uq_master_artist_master_id_artist_id
        unique (master_id, artist_id),
    constraint fk_master_artist_artist_id_artist
        foreign key (artist_id) references artist (id),
    constraint fk_master_artist_master_id_master
        foreign key (master_id) references master (id)
);

create table if not exists master_genre
(
    id               int auto_increment
        primary key,
    created_at       timestamp                 not null,
    last_modified_at timestamp                 not null,
    genre            varchar(255) charset utf8 not null,
    master_id        int                       not null,
    constraint uq_master_genre_master_id_genre
        unique (master_id, genre),
    constraint fk_master_genre_genre_genre
        foreign key (genre) references genre (name),
    constraint fk_master_genre_master_id_master
        foreign key (master_id) references master (id)
);

create table if not exists master_video
(
    id               int auto_increment
        primary key,
    created_at       timestamp                   not null,
    last_modified_at timestamp                   not null,
    hash             int                         not null,
    description      varchar(15000) charset utf8 null,
    title            varchar(1000) charset utf8  null,
    url              varchar(1000) charset utf8  null,
    master_id        int                         not null,
    constraint uq_master_video_master_id_hash
        unique (master_id, hash),
    constraint fk_master_video_master_id_master
        foreign key (master_id) references master (id)
);

create table if not exists release_item
(
    id                  int auto_increment
        primary key,
    created_at          timestamp                   not null,
    last_modified_at    timestamp                   not null,
    country             varchar(255) charset utf8   null,
    data_quality        varchar(255) charset utf8   null,
    has_valid_day       bit                         null,
    has_valid_month     bit                         null,
    has_valid_year      bit                         null,
    is_master           bit                         null,
    master_id           int                         null,
    listed_release_date varchar(255) charset utf8   null,
    notes               text                        null,
    release_date        date                        null,
    status              varchar(255) charset utf8   null,
    title               varchar(10000) charset utf8 null,
    constraint fk_release_item_master_id_master
        foreign key (master_id) references master (id)
);

create table if not exists label_release_item
(
    id                int auto_increment
        primary key,
    created_at        timestamp                  not null,
    last_modified_at  timestamp                  not null,
    category_notation varchar(1000) charset utf8 null,
    label_id          int                        not null,
    release_item_id   int                        not null,
    constraint uq_label_release_item_release_item_id_label_id
        unique (release_item_id, label_id),
    constraint fk_label_release_label_id_label
        foreign key (label_id) references label (id),
    constraint fk_label_release_release_item_id_release_item
        foreign key (release_item_id) references release_item (id)
);

alter table master
    add constraint fk_master_main_release_id_release_item
        foreign key (main_release_id) references release_item (id);

create table if not exists release_item_artist
(
    id               int auto_increment
        primary key,
    created_at       timestamp not null,
    last_modified_at timestamp not null,
    artist_id        int       not null,
    release_item_id  int       not null,
    constraint uq_release_item_artist_release_item_id_artist_id
        unique (release_item_id, artist_id),
    constraint fk_release_item_artist_artist_id_artist
        foreign key (artist_id) references artist (id),
    constraint fk_release_item_artist_release_item_id_release_item
        foreign key (release_item_id) references release_item (id)
);

create table if not exists release_item_credited_artist
(
    id               int auto_increment
        primary key,
    created_at       timestamp                   not null,
    last_modified_at timestamp                   not null,
    hash             int                         not null,
    role             varchar(20000) charset utf8 null,
    artist_id        int                         not null,
    release_item_id  int                         not null,
    constraint uq_release_item_credited_artist_release_item_id_artist_id_hash
        unique (release_item_id, artist_id, hash),
    constraint fk_release_item_credited_artist_artist_id_artist
        foreign key (artist_id) references artist (id),
    constraint fk_release_item_credited_artist_release_item_id_release_item
        foreign key (release_item_id) references release_item (id)
);

create table if not exists release_item_format
(
    id               int auto_increment
        primary key,
    created_at       timestamp                   not null,
    last_modified_at timestamp                   not null,
    hash             int                         not null,
    description      varchar(10000) charset utf8 null,
    name             varchar(255) charset utf8   null,
    quantity         int                         null,
    text             varchar(5000) charset utf8  null,
    release_item_id  int                         not null,
    constraint uq_release_item_format_release_item_id_hash
        unique (release_item_id, hash),
    constraint fk_release_item_format_release_item_id_release_item
        foreign key (release_item_id) references release_item (id)
);

create table if not exists release_item_genre
(
    id               int auto_increment
        primary key,
    created_at       timestamp                 not null,
    last_modified_at timestamp                 not null,
    genre            varchar(255) charset utf8 not null,
    release_item_id  int                       not null,
    constraint uq_release_item_genre_release_item_id_genre
        unique (release_item_id, genre),
    constraint fk_release_item_genre_genre_genre
        foreign key (genre) references genre (name),
    constraint fk_release_item_genre_release_item_id_release_item
        foreign key (release_item_id) references release_item (id)
);

create table if not exists release_item_identifier
(
    id               int auto_increment
        primary key,
    created_at       timestamp not null,
    last_modified_at timestamp not null,
    hash             int       not null,
    description      longtext  null,
    type             longtext  null,
    value            longtext  null,
    release_item_id  int       not null,
    constraint uq_release_item_identifier_release_item_id_hash
        unique (release_item_id, hash),
    constraint fk_release_item_identifier_release_item_id_release_item
        foreign key (release_item_id) references release_item (id)
);

create table if not exists release_item_image
(
    id               int auto_increment
        primary key,
    created_at       timestamp not null,
    last_modified_at timestamp not null,
    hash             int       not null,
    file_name        longtext  null,
    release_item_id  int       not null,
    constraint uq_release_item_image_release_item_id_hash
        unique (release_item_id, hash),
    constraint fk_release_item_image_release_item_id_release_item
        foreign key (release_item_id) references release_item (id)
);

create table if not exists release_item_track
(
    id               int auto_increment
        primary key,
    created_at       timestamp not null,
    last_modified_at timestamp not null,
    hash             int       not null,
    duration         longtext  null,
    position         longtext  null,
    title            longtext  null,
    release_item_id  int       not null,
    constraint uq_release_item_track_release_item_id_hash
        unique (release_item_id, hash),
    constraint fk_release_item_track_release_item_id_release_item
        foreign key (release_item_id) references release_item (id)
);

create table if not exists release_item_video
(
    id               int auto_increment
        primary key,
    created_at       timestamp not null,
    last_modified_at timestamp not null,
    hash             int       not null,
    description      longtext  null,
    title            longtext  null,
    url              longtext  null,
    release_item_id  int       not null,
    constraint uq_release_item_video_release_item_id_hash
        unique (release_item_id, hash),
    constraint fk_release_item_video_release_item_id_release_item
        foreign key (release_item_id) references release_item (id)
);

create table if not exists release_item_work
(
    id               int auto_increment
        primary key,
    created_at       timestamp                  not null,
    last_modified_at timestamp                  not null,
    hash             int                        not null,
    work             varchar(5000) charset utf8 null,
    label_id         int                        not null,
    release_item_id  int                        not null,
    constraint uq_release_item_work_release_item_id_label_id_hash
        unique (release_item_id, label_id, hash),
    constraint fk_release_item_work_label_id_label
        foreign key (label_id) references label (id),
    constraint fk_release_item_work_release_item_id_release_item
        foreign key (release_item_id) references release_item (id)
);

create table if not exists style
(
    name varchar(255) charset utf8 not null
        primary key
);

create table if not exists master_style
(
    id               int auto_increment
        primary key,
    created_at       timestamp                 not null,
    last_modified_at timestamp                 not null,
    master_id        int                       not null,
    style            varchar(255) charset utf8 not null,
    constraint uq_master_style_master_id_style
        unique (master_id, style),
    constraint fk_master_style_master_id_master
        foreign key (master_id) references master (id),
    constraint fk_master_style_style_style
        foreign key (style) references style (name)
);

create table if not exists release_item_style
(
    id               int auto_increment
        primary key,
    created_at       timestamp                 not null,
    last_modified_at timestamp                 not null,
    release_item_id  int                       not null,
    style            varchar(255) charset utf8 not null,
    constraint uq_release_item_style_release_item_id_style
        unique (release_item_id, style),
    constraint fk_release_item_style_release_item_id_release_item
        foreign key (release_item_id) references release_item (id),
    constraint fk_release_item_style_style_style
        foreign key (style) references style (name)
);

SET REFERENTIAL_INTEGRITY TRUE;

CREATE TABLE BATCH_JOB_INSTANCE
(
    JOB_INSTANCE_ID BIGINT IDENTITY NOT NULL PRIMARY KEY,
    VERSION         BIGINT,
    JOB_NAME        VARCHAR(100) NOT NULL,
    JOB_KEY         VARCHAR(32)  NOT NULL,
    constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
);

CREATE TABLE BATCH_JOB_EXECUTION
(
    JOB_EXECUTION_ID           BIGINT IDENTITY NOT NULL PRIMARY KEY,
    VERSION                    BIGINT,
    JOB_INSTANCE_ID            BIGINT    NOT NULL,
    CREATE_TIME                TIMESTAMP NOT NULL,
    START_TIME                 TIMESTAMP DEFAULT NULL,
    END_TIME                   TIMESTAMP DEFAULT NULL,
    STATUS                     VARCHAR(10),
    EXIT_CODE                  VARCHAR(2500),
    EXIT_MESSAGE               VARCHAR(2500),
    LAST_UPDATED               TIMESTAMP,
    JOB_CONFIGURATION_LOCATION VARCHAR(2500) NULL,
    constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
        references BATCH_JOB_INSTANCE (JOB_INSTANCE_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS
(
    JOB_EXECUTION_ID BIGINT       NOT NULL,
    TYPE_CD          VARCHAR(6)   NOT NULL,
    KEY_NAME         VARCHAR(100) NOT NULL,
    STRING_VAL       VARCHAR(250),
    DATE_VAL         TIMESTAMP DEFAULT NULL,
    LONG_VAL         BIGINT,
    DOUBLE_VAL       DOUBLE PRECISION,
    IDENTIFYING      CHAR(1)      NOT NULL,
    constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION
(
    STEP_EXECUTION_ID  BIGINT IDENTITY NOT NULL PRIMARY KEY,
    VERSION            BIGINT       NOT NULL,
    STEP_NAME          VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID   BIGINT       NOT NULL,
    START_TIME         TIMESTAMP    NOT NULL,
    END_TIME           TIMESTAMP DEFAULT NULL,
    STATUS             VARCHAR(10),
    COMMIT_COUNT       BIGINT,
    READ_COUNT         BIGINT,
    FILTER_COUNT       BIGINT,
    WRITE_COUNT        BIGINT,
    READ_SKIP_COUNT    BIGINT,
    WRITE_SKIP_COUNT   BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT     BIGINT,
    EXIT_CODE          VARCHAR(2500),
    EXIT_MESSAGE       VARCHAR(2500),
    LAST_UPDATED       TIMESTAMP,
    constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT
(
    STEP_EXECUTION_ID  BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT LONGVARCHAR,
    constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
        references BATCH_STEP_EXECUTION (STEP_EXECUTION_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT
(
    JOB_EXECUTION_ID   BIGINT        NOT NULL PRIMARY KEY,
    SHORT_CONTEXT      VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT LONGVARCHAR,
    constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
        references BATCH_JOB_EXECUTION (JOB_EXECUTION_ID)
);

CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ;
CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ;
CREATE SEQUENCE BATCH_JOB_SEQ;