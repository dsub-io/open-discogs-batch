CREATE TABLE IF NOT EXISTS style_tmp
(
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_style_tmp_name PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS genre_tmp
(
    name VARCHAR(255) NOT NULL,
    CONSTRAINT pk_genre_tmp_name PRIMARY KEY (name)
);

CREATE TABLE IF NOT EXISTS artist_tmp
(
    id               BIGINT        NOT NULL,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    data_quality     VARCHAR(255)  NULL,
    name             VARCHAR(1000) NULL,
    profile          VARCHAR(2000) NULL,
    real_name        VARCHAR(2000) NULL,
    CONSTRAINT pk_artist_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS artist_alias_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at datetime(6) NOT NULL,
    artist_id        BIGINT      NOT NULL,
    alias_id         BIGINT      NOT NULL,
    CONSTRAINT pk_artist_alias_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS artist_group_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at datetime(6) NOT NULL,
    artist_id        BIGINT      NOT NULL,
    group_id         BIGINT      NOT NULL,
    CONSTRAINT pk_artist_group_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS artist_member_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at datetime(6) NOT NULL,
    artist_id        BIGINT      NOT NULL,
    member_id        BIGINT      NOT NULL,
    CONSTRAINT pk_artist_member_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS artist_name_variation_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    name_variation   VARCHAR(2000) NULL,
    artist_id        BIGINT        NOT NULL,
    CONSTRAINT pk_artist_name_variation_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS artist_url_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    url              VARCHAR(2000) NULL,
    artist_id        BIGINT        NOT NULL,
    CONSTRAINT pk_artist_url_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS label_tmp
(
    id               BIGINT        NOT NULL,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    contact_info     VARCHAR(2000) NULL,
    data_quality     VARCHAR(255)  NULL,
    name             VARCHAR(255)  NULL,
    profile          VARCHAR(2000) NULL,
    CONSTRAINT pk_label_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS label_sub_label_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at datetime(6) NOT NULL,
    parent_label_id  BIGINT      NOT NULL,
    sub_label_id     BIGINT      NOT NULL,
    CONSTRAINT pk_label_sub_label_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS label_url_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    url              VARCHAR(2000) NULL,
    label_id         BIGINT        NOT NULL,
    CONSTRAINT pk_label_url_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS master_tmp
(
    id               BIGINT        NOT NULL,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    data_quality     VARCHAR(255)  NULL,
    title            VARCHAR(2000) NULL,
    year             SMALLINT      NOT NULL,
    CONSTRAINT pk_master_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS master_artist_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at datetime(6) NOT NULL,
    artist_id        BIGINT      NOT NULL,
    master_id        BIGINT      NOT NULL,
    CONSTRAINT pk_master_artist_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS master_genre_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at datetime(6)  NOT NULL,
    genre            VARCHAR(255) NOT NULL,
    master_id        BIGINT       NOT NULL,
    CONSTRAINT master_genre_tmp_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS master_style_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at datetime(6)  NOT NULL,
    master_id        BIGINT       NOT NULL,
    style            VARCHAR(255) NOT NULL,
    CONSTRAINT pk_master_style_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS master_video_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    description      VARCHAR(2000) NULL,
    title            VARCHAR(2000) NULL,
    url              VARCHAR(2000) NULL,
    master_id        BIGINT        NOT NULL,
    CONSTRAINT pk_master_video_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS release_item_tmp
(
    id                  BIGINT        NOT NULL,
    created_at          DATETIME(6)   NOT NULL,
    last_modified_at    datetime(6)   NOT NULL,
    country             VARCHAR(255)  NULL,
    data_quality        VARCHAR(255)  NULL,
    has_valid_day       bit           NOT NULL,
    has_valid_month     bit           NOT NULL,
    has_valid_year      bit           NOT NULL,
    is_master           bit           NOT NULL,
    listed_release_date VARCHAR(255)  NULL,
    notes               VARCHAR(2000) NULL,
    release_date        date          NULL,
    status              VARCHAR(255)  NULL,
    title               VARCHAR(2000) NULL,
    master_id           BIGINT        NULL,
    CONSTRAINT pk_release_item_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS release_item_format_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    name             VARCHAR(2000) NULL,
    quantity         INT           NULL,
    text             VARCHAR(5000) NULL,
    description      VARCHAR(2000) NULL,
    release_item_id  BIGINT        NOT NULL,
    CONSTRAINT pk_release_item_format_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS release_item_identifier_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    description      VARCHAR(2000) NULL,
    type             VARCHAR(2000) NULL,
    value            VARCHAR(2000) NULL,
    release_item_id  BIGINT        NOT NULL,
    CONSTRAINT pk_release_item_identifier_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS label_item_release_tmp
(
    id                BIGINT auto_increment,
    created_at        DATETIME(6)   NOT NULL,
    last_modified_at  datetime(6)   NOT NULL,
    category_notation VARCHAR(5000) NULL,
    label_id          BIGINT        NOT NULL,
    release_item_id   BIGINT        NOT NULL,
    CONSTRAINT pk_label_item_release_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS release_item_artist_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at datetime(6) NOT NULL,
    artist_id        BIGINT      NOT NULL,
    release_item_id  BIGINT      NOT NULL,
    CONSTRAINT pk_release_item_artist_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS release_item_credited_artist_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    role             VARCHAR(2000) NULL,
    artist_id        BIGINT        NOT NULL,
    release_item_id  BIGINT        NOT NULL,
    CONSTRAINT pk_release_item_credited_artist_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS release_item_genre_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at datetime(6)  NOT NULL,
    genre            VARCHAR(255) NOT NULL,
    release_item_id  BIGINT       NOT NULL,
    CONSTRAINT pk_release_item_genre_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS release_item_video_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    description      VARCHAR(2000) NULL,
    title            VARCHAR(2000) NULL,
    url              VARCHAR(2000) NOT NULL,
    release_item_id  BIGINT        NOT NULL,
    CONSTRAINT pk_release_item_video_tmp_id PRIMARY KEY (id)
);
CREATE TABLE IF NOT EXISTS release_item_work_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    work             VARCHAR(5000) NULL,
    label_id         BIGINT        NOT NULL,
    release_item_id  BIGINT        NOT NULL,
    CONSTRAINT pk_release_item_work_tmp_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_style_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)  NOT NULL,
    last_modified_at datetime(6)  NOT NULL,
    release_item_id  BIGINT       NOT NULL,
    style            VARCHAR(255) NOT NULL,
    CONSTRAINT pk_release_item_style_tmp_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_track_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6)   NOT NULL,
    last_modified_at datetime(6)   NOT NULL,
    position         VARCHAR(2000) NULL,
    title            VARCHAR(2000) NULL,
    duration         VARCHAR(5000) NULL,
    release_item_id  BIGINT        NOT NULL,
    CONSTRAINT pk_release_item_track_tmp_id PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS release_item_master_tmp
(
    id               BIGINT auto_increment,
    created_at       DATETIME(6) NOT NULL,
    last_modified_at datetime(6) NOT NULL,
    release_item_id  BIGINT      NOT NULL,
    master_id        BIGINT      NOT NULL,
    CONSTRAINT pk_release_item_master_tmp_id PRIMARY KEY (id)
);