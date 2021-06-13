CREATE TABLE IF NOT EXISTS artist_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_artist_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    data_quality     VARCHAR(255),
    name             VARCHAR(1000),
    profile          VARCHAR(40000),
    real_name        VARCHAR(2000)
);
CREATE TABLE IF NOT EXISTS artist_alias_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_artist_alias_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    alias_id         SERIAL    NOT NULL,
    artist_id        SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS artist_group_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_artist_group_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    artist_id        SERIAL    NOT NULL,
    group_id         SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS artist_member_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_artist_member_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    artist_id        SERIAL    NOT NULL,
    member_id        SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS artist_name_variation_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_artist_name_variation_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    name_variation   VARCHAR(2000),
    artist_id        SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS artist_url_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_artist_url_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    url              VARCHAR(5000),
    artist_id        SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS discogs_dump_tmp
(
    etag          VARCHAR(255) NOT NULL
        CONSTRAINT pk_discogs_dump_id
            PRIMARY KEY,
    created_at    date,
    registered_at timestamp,
    size          bigint,
    type          VARCHAR(255),
    uri_string    varchar(255),
    url           VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS genre_tmp
(
    name VARCHAR(255) NOT NULL
        CONSTRAINT pk_genre_name
            PRIMARY KEY
);
CREATE TABLE IF NOT EXISTS label_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_label_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    contact_info     VARCHAR(40000),
    data_quality     VARCHAR(255),
    name             VARCHAR(255),
    profile          VARCHAR(40000)
);
CREATE TABLE IF NOT EXISTS label_sub_label_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_label_sub_label_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    parent_label_id  SERIAL    NOT NULL,
    sub_label_id     SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS label_url_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_label_url_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    url              VARCHAR(5000),
    label_id         SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS master_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_master_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    data_quality     VARCHAR(255),
    title            VARCHAR(2000),
    year             smallint
);
CREATE TABLE IF NOT EXISTS master_artist_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_master_artist_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    artist_id        SERIAL    NOT NULL,
    master_id        SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS master_genre_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_master_genre_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    genre            VARCHAR(255),
    master_id        SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS master_video_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_master_video_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    description      VARCHAR(40000),
    title            VARCHAR(2000),
    url              VARCHAR(5000),
    master_id        SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS release_item_tmp
(
    id                  SERIAL    NOT NULL
        CONSTRAINT pk_release_item_id
            PRIMARY KEY,
    created_at          TIMESTAMP NOT NULL,
    last_modified_at    TIMESTAMP NOT NULL,
    country             VARCHAR(255),
    data_quality        varchar(255),
    has_valid_day       BOOLEAN,
    has_valid_month     BOOLEAN,
    has_valid_year      BOOLEAN,
    is_master           BOOLEAN,
    listed_release_date VARCHAR(255),
    notes               TEXT,
    release_date        date,
    status              VARCHAR(255),
    title               VARCHAR(10000),
    master_id           SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS label_item_release_tmp
(
    id                SERIAL    NOT NULL
        CONSTRAINT pk_label_item_release_id
            PRIMARY KEY,
    created_at        TIMESTAMP NOT NULL,
    last_modified_at  TIMESTAMP NOT NULL,
    label_id          SERIAL    NOT NULL,
    release_item_id   SERIAL    NOT NULL,
    category_notation VARCHAR(5000)
);
CREATE TABLE IF NOT EXISTS release_item_artist_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_release_item_artist_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    artist_id        SERIAL    NOT NULL,
    release_item_id  SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS release_item_credited_artist_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_release_item_credited_artist_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    role             VARCHAR(20000),
    artist_id        SERIAL    NOT NULL,
    release_item_id  SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS release_item_genre_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_release_item_genre_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    genre            VARCHAR(255),
    release_item_id  SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS release_item_video_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_release_item_video_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    description      VARCHAR(10000),
    title            VARCHAR(10000),
    url              VARCHAR(10000),
    release_item_id  SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS release_item_work_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_release_item_work_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    work             VARCHAR(5000),
    label_id         serial    NOT NULL,
    release_item_id  SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS style_tmp
(
    name VARCHAR(255) NOT NULL
        CONSTRAINT pk_style_name
            PRIMARY KEY
);
CREATE TABLE IF NOT EXISTS master_style_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_master_style_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    master_id        SERIAL    NOT NULL,
    style            VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS release_item_style_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_release_item_style_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    release_item_id  SERIAL    NOT NULL,
    style            VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS release_item_track_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_release_item_track_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    position         VARCHAR(15000),
    title            VARCHAR(15000),
    duration         varchar(5000),
    release_item_id  SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS release_item_format_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_release_item_format_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    name             VARCHAR(2000),
    quantity         integer,
    text             varchar(5000),
    description      VARCHAR(10000),
    release_item_id  SERIAL    NOT NULL
);
CREATE TABLE IF NOT EXISTS release_item_identifier_tmp
(
    id               SERIAL    NOT NULL
        CONSTRAINT pk_release_item_identifier_id
            PRIMARY KEY,
    created_at       TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    description      VARCHAR(20000),
    type             varchar(10000),
    value            VARCHAR(10000),
    release_item_id  SERIAL    NOT NULL
);