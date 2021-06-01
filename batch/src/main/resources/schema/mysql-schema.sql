SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS artist
(
    id               BIGINT        NOT NULL,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at DATETIME(6)   NOT NULL,
    data_quality     VARCHAR(255)  NULL,
    name             VARCHAR(1000) NULL,
    profile          LONGTEXT      NULL,
    real_name        VARCHAR(2000) NULL,
    CONSTRAINT pk_artist_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS artist_alias
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at DATETIME(6) NOT NULL,
    artist_id        BIGINT      NULL,
    alias_id         BIGINT      NULL,
    CONSTRAINT uq_artist_alias_artist_id_alias_id
        UNIQUE (artist_id, alias_id),
    CONSTRAINT fk_artist_alias_artist_id_artist
        FOREIGN KEY (artist_id) REFERENCES artist (id),
    CONSTRAINT fk_artist_alias_alias_id_artist
        FOREIGN KEY (alias_id) REFERENCES artist (id),
    CONSTRAINT pk_artist_alias_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS artist_group
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at DATETIME(6) NOT NULL,
    artist_id        BIGINT      NULL,
    group_id         BIGINT      NULL,
    CONSTRAINT uq_artist_group_artist_id_group_id
        UNIQUE (artist_id, group_id),
    CONSTRAINT fk_artist_group_artist_id_artist
        FOREIGN KEY (artist_id) REFERENCES artist (id),
    CONSTRAINT fk_artist_group_group_id_artist
        FOREIGN KEY (group_id) REFERENCES artist (id),
    CONSTRAINT pk_artist_group_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS artist_member
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at DATETIME(6) NOT NULL,
    artist_id        BIGINT      NULL,
    member_id        BIGINT      NULL,
    CONSTRAINT uq_artist_member_artist_id_member_id
        UNIQUE (artist_id, member_id),
    CONSTRAINT fk_artist_member_artist_id_artist
        FOREIGN KEY (artist_id) REFERENCES artist (id),
    CONSTRAINT fk_artist_member_member_id_artist
        FOREIGN KEY (member_id) REFERENCES artist (id),
    CONSTRAINT pk_artist_member_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS artist_name_variation
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at DATETIME(6)   NOT NULL,
    name_variation   VARCHAR(2000) NULL,
    artist_id        BIGINT        NOT NULL,
    CONSTRAINT fk_artist_name_variation_artist_id_artist
        FOREIGN KEY (artist_id) REFERENCES artist (id),
    CONSTRAINT pk_artist_name_variation_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS artist_url
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at DATETIME(6) NOT NULL,
    url              TEXT        NULL,
    artist_id        BIGINT      NOT NULL,
    CONSTRAINT fk_artist_url_artist_id_artist
        FOREIGN KEY (artist_id) REFERENCES artist (id),
    CONSTRAINT pk_artist_url_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS discogs_dump
(
    id            BIGINT AUTO_INCREMENT,
    registered_at DATETIME(6)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    type          VARCHAR(255) NULL,
    etag          VARCHAR(255) NULL,
    size          BIGINT       NULL,
    uri_string    VARCHAR(255) NULL,
    url           VARCHAR(255) NULL,
    CONSTRAINT pk_discogs_dump_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS genre
(
    name             VARCHAR(255) NOT NULL,
    CONSTRAINT pk_genre_name PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS label
(
    id               BIGINT       NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    contact_info     LONGTEXT     NULL,
    data_quality     VARCHAR(255) NULL,
    name             VARCHAR(255) NULL,
    profile          LONGTEXT     NULL,
    CONSTRAINT pk_label_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS label_sub_label
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at DATETIME(6) NOT NULL,
    parent_label_id  BIGINT      NULL,
    sub_label_id     BIGINT      NULL,
    CONSTRAINT uq_label_sub_label_parent_label_id_sub_label_id
        UNIQUE (parent_label_id, sub_label_id),
    CONSTRAINT fk_label_sub_label_parent_label_id_label
        FOREIGN KEY (parent_label_id) REFERENCES label (id),
    CONSTRAINT fk_label_sub_label_sub_label_id_label
        FOREIGN KEY (sub_label_id) REFERENCES label (id),
    CONSTRAINT pk_label_sub_label_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS label_url
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at DATETIME(6) NOT NULL,
    url              TEXT        NULL,
    label_id         BIGINT      NOT NULL,
    CONSTRAINT fk_label_url_label_id_label
        FOREIGN KEY (label_id) REFERENCES label (id),
    CONSTRAINT pk_label_url_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS master
(
    id                   BIGINT        NOT NULL,
    created_at           DATETIME(6)   NOT NULL,
    last_modified_at     DATETIME(6)   NOT NULL,
    data_quality         VARCHAR(255)  NULL,
    title                VARCHAR(2000) NULL,
    year                 smallint      NOT NULL,
    main_release_item_id BIGINT        NULL,
    CONSTRAINT fk_master_main_release_item_id_release_item
        FOREIGN KEY (main_release_item_id) REFERENCES release_item (id),
    CONSTRAINT pk_master_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS master_artist
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at DATETIME(6) NOT NULL,
    artist_id        BIGINT      NULL,
    master_id        BIGINT      NULL,
    CONSTRAINT uq_master_artist_master_id_artist_id
        UNIQUE (master_id, artist_id),
    CONSTRAINT fk_master_artist_artist_id_artist
        FOREIGN KEY (artist_id) REFERENCES artist (id),
    CONSTRAINT fk_master_artist_master_id_master
        FOREIGN KEY (master_id) REFERENCES master (id),
    CONSTRAINT pk_master_artist_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS master_genre
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    genre            VARCHAR(255) NULL,
    master_id        BIGINT       NULL,
    CONSTRAINT uq_master_genre_master_id_genre
        UNIQUE (master_id, genre),
    CONSTRAINT fk_master_genre_genre_genre
        FOREIGN KEY (genre) REFERENCES genre (name),
    CONSTRAINT fk_master_master_id_master
        FOREIGN KEY (master_id) REFERENCES master (id),
    CONSTRAINT master_genre_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS master_video
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at DATETIME(6)   NOT NULL,
    description      TEXT          NULL,
    title            VARCHAR(2000) NULL,
    url              TEXT          NULL,
    master_id        BIGINT        NULL,
    CONSTRAINT fk_master_video_master_id_master
        FOREIGN KEY (master_id) REFERENCES master (id),
    CONSTRAINT pk_master_video_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item
(
    id                  BIGINT         NOT NULL,
    created_at          DATETIME(6)    NOT NULL,
    last_modified_at    DATETIME(6)    NOT NULL,
    country             VARCHAR(255)   NULL,
    data_quality        VARCHAR(255)   NULL,
    has_valid_day       bit            NOT NULL,
    has_valid_month     bit            NOT NULL,
    has_valid_year      bit            NOT NULL,
    is_master           bit            NOT NULL,
    listed_release_date VARCHAR(255)   NULL,
    notes               LONGTEXT       NULL,
    release_date        date           NULL,
    status              VARCHAR(255)   NULL,
    title               VARCHAR(10000) NULL,
    master_id           BIGINT         NULL,
    CONSTRAINT fk_release_item_master_id_master
        FOREIGN KEY (master_id) REFERENCES master (id),
    CONSTRAINT pk_release_item_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_format
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at DATETIME(6)   NOT NULL,
    name             VARCHAR(255)  NULL,
    quantity         INT           NULL,
    text             VARCHAR(5000) NULL,
    release_item_id  BIGINT        NULL,
    CONSTRAINT fk_release_item_format_release_item_id_release_item
        FOREIGN KEY (release_item_id) REFERENCES release_item (id),
    CONSTRAINT pk_release_item_format_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_format_description
(
    format_id        BIGINT       NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    description      VARCHAR(255) NULL,
    CONSTRAINT fk_release_item_format_description_format_id
        FOREIGN KEY (format_id) REFERENCES release_item_format (id)
);

CREATE TABLE IF NOT EXISTS release_item_identifier
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    description      TEXT         NULL,
    type             VARCHAR(255) NULL,
    value            VARCHAR(255) NULL,
    release_item_id  BIGINT       NULL,
    CONSTRAINT fk_release_item_identifier_release_item_id
        FOREIGN KEY (release_item_id) REFERENCES release_item (id),
    CONSTRAINT pk_release_item_identifier_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS label_release
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    category_notation  VARCHAR(255) NULL,
    label_id         BIGINT       NULL,
    release_item_id  BIGINT       NULL,
    CONSTRAINT uq_label_release_release_item_id_label_id
        UNIQUE (release_item_id, label_id),
    CONSTRAINT fk_label_release_label_id_label
        FOREIGN KEY (label_id) REFERENCES label (id),
    CONSTRAINT fk_label_release_release_item_id_release_item
        FOREIGN KEY (release_item_id) REFERENCES release_item (id),
    CONSTRAINT pk_label_release_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_artist
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at DATETIME(6) NOT NULL,
    artist_id        BIGINT      NULL,
    release_item_id  BIGINT      NULL,
    CONSTRAINT uq_release_item_artist_release_item_id_artist_id
        UNIQUE (release_item_id, artist_id),
    CONSTRAINT fk_release_item_artist_artist_id_artist
        FOREIGN KEY (artist_id) REFERENCES artist (id),
    CONSTRAINT fk_release_item_artist_release_item_id_release_item
        FOREIGN KEY (release_item_id) REFERENCES release_item (id),
    CONSTRAINT pk_release_item_artist_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_credited_artist
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at DATETIME(6) NOT NULL,
    role             TEXT        NULL,
    artist_id        BIGINT      NULL,
    release_item_id  BIGINT      NULL,
    CONSTRAINT fk_release_item_credited_artist_artist_id_artist
        FOREIGN KEY (artist_id) REFERENCES artist (id),
    CONSTRAINT fk_release_item_credited_artist_release_item_id_release_item
        FOREIGN KEY (release_item_id) REFERENCES release_item (id),
    CONSTRAINT pk_release_item_credited_artist_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_genre
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    genre            VARCHAR(255) NULL,
    release_item_id  BIGINT       NULL,
    CONSTRAINT uq_release_item_genre_release_item_id_genre
        UNIQUE (release_item_id, genre),
    CONSTRAINT fk_release_item_genre_release_item_id_release_item
        FOREIGN KEY (release_item_id) REFERENCES release_item (id),
    CONSTRAINT fk_release_item_genre_genre_genre
        FOREIGN KEY (genre) REFERENCES genre (name),
    CONSTRAINT pk_release_item_genre_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_video
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)    NOT NULL,
    last_modified_at DATETIME(6)    NOT NULL,
    description      TEXT           NULL,
    title            TEXT           NULL,
    url              VARCHAR(10000) NULL,
    release_item_id  BIGINT         NULL,
    CONSTRAINT fk_release_item_video_release_item_id_release_item
        FOREIGN KEY (release_item_id) REFERENCES release_item (id),
    CONSTRAINT pk_release_item_video_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_work
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    work              VARCHAR(255) NULL,
    label_id         BIGINT       NULL,
    release_item_id  BIGINT       NULL,
    CONSTRAINT uq_release_item_work_release_item_id_label_id_work
        UNIQUE (release_item_id, label_id, work),
    CONSTRAINT fk_release_item_work_label_id_label
        FOREIGN KEY (label_id) REFERENCES label (id),
    CONSTRAINT fk_release_item_work_release_item_id_release_item
        FOREIGN KEY (release_item_id) REFERENCES release_item (id),
    CONSTRAINT pk_release_item_work_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS style
(
    name             VARCHAR(255) NOT NULL,
    CONSTRAINT pk_style_name PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS master_style
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    master_id        BIGINT       NULL,
    style            VARCHAR(255) NULL,
    CONSTRAINT uq_master_style_master_id_style
        UNIQUE (master_id, style),
    CONSTRAINT fk_master_style_master_id_master
        FOREIGN KEY (master_id) REFERENCES master (id),
    CONSTRAINT fk_master_style_style_style
        FOREIGN KEY (style) REFERENCES style (name),
    CONSTRAINT pk_master_style_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_style
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at DATETIME(6)  NOT NULL,
    release_item_id  BIGINT       NULL,
    style            VARCHAR(255) NULL,
    CONSTRAINT uq_release_item_style_release_item_id_style
        UNIQUE (release_item_id, style),
    CONSTRAINT fq_release_item_style_style_style
        FOREIGN KEY (style) REFERENCES style (name),
    CONSTRAINT fq_release_item_style_release_item_id_release_item
        FOREIGN KEY (release_item_id) REFERENCES release_item (id),
    CONSTRAINT pk_release_item_style_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_track
(
    id               BIGINT AUTO_INCREMENT,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at DATETIME(6)   NOT NULL,
    position         VARCHAR(255)  NULL,
    duration         VARCHAR(1000) NULL,
    title            VARCHAR(2000) NULL,
    release_item_id  BIGINT        NULL,
    CONSTRAINT uq_release_item_track_position_title_duration_release_item_id
        UNIQUE (position, title, duration, release_item_id),
    CONSTRAINT fk_release_item_track_release_item_id_release_item
        FOREIGN KEY (release_item_id) REFERENCES release_item (id),
    CONSTRAINT pk_release_item_track_id PRIMARY KEY (id)
);

SET FOREIGN_KEY_CHECKS = 1;