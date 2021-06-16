CREATE TABLE IF NOT EXISTS artist
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_artist_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    data_quality
    VARCHAR
(
    255
),
    name VARCHAR
(
    1000
),
    profile VARCHAR
(
    40000
),
    real_name VARCHAR
(
    2000
)
    );
CREATE TABLE IF NOT EXISTS artist_alias
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_artist_alias_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    alias_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_alias_alias_id_artist
    REFERENCES
    artist,
    artist_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_alias_artist_id_artist
    REFERENCES
    artist,
    CONSTRAINT
    uq_artist_alias_artist_id_alias_id
    UNIQUE
(
    artist_id,
    alias_id
)
    );
CREATE TABLE IF NOT EXISTS artist_group
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_artist_group_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    artist_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_group_artist_id_artist
    REFERENCES
    artist,
    group_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_group_group_id_artist
    REFERENCES
    artist,
    CONSTRAINT
    uq_artist_group_artist_id_group_id
    UNIQUE
(
    artist_id,
    group_id
)
    );
CREATE TABLE IF NOT EXISTS artist_member
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_artist_member_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    artist_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_member_artist_id_artist
    REFERENCES
    artist,
    member_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_member_member_id_artist
    REFERENCES
    artist,
    CONSTRAINT
    uq_artist_member_artist_id_member_id
    UNIQUE
(
    artist_id,
    member_id
)
    );
CREATE TABLE IF NOT EXISTS artist_name_variation
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_artist_name_variation_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    name_variation
    VARCHAR
(
    2000
),
    artist_id SERIAL NOT NULL
    CONSTRAINT fk_artist_name_variation_artist_id_artist
    REFERENCES artist,
    CONSTRAINT uq_artist_name_variation_artist_id_name_variation
    UNIQUE
(
    artist_id,
    name_variation
)
    );
CREATE TABLE IF NOT EXISTS artist_url
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_artist_url_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    url
    VARCHAR
(
    5000
),
    artist_id SERIAL NOT NULL
    CONSTRAINT fk_artist_url_artist_id_artist
    REFERENCES artist,
    CONSTRAINT uq_artist_url_artist_id_url
    UNIQUE
(
    artist_id,
    url
)
    );
CREATE TABLE IF NOT EXISTS discogs_dump
(
    etag VARCHAR
(
    255
) NOT NULL
    CONSTRAINT pk_discogs_dump_id
    PRIMARY KEY,
    created_at date,
    registered_at timestamp,
    size bigint,
    type VARCHAR
(
    255
),
    uri_string varchar
(
    255
),
    url VARCHAR
(
    255
)
    );
CREATE TABLE IF NOT EXISTS genre
(
    name VARCHAR
(
    255
) NOT NULL
    CONSTRAINT pk_genre_name
    PRIMARY KEY
    );
CREATE TABLE IF NOT EXISTS label
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_label_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    contact_info
    VARCHAR
(
    40000
),
    data_quality VARCHAR
(
    255
),
    name VARCHAR
(
    255
),
    profile VARCHAR
(
    40000
)
    );
CREATE TABLE IF NOT EXISTS label_sub_label
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_label_sub_label_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    parent_label_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_label_sub_label_parent_label_id_label
    REFERENCES
    label,
    sub_label_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_label_sub_label_sub_label_id_label
    REFERENCES
    label,
    CONSTRAINT
    uq_label_sub_label_parent_label_id_sub_label_id
    UNIQUE
(
    parent_label_id,
    sub_label_id
)
    );
CREATE TABLE IF NOT EXISTS label_url
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_label_url_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    url
    VARCHAR
(
    5000
),
    label_id SERIAL NOT NULL
    CONSTRAINT fk_label_url_label_id_label
    REFERENCES label,
    CONSTRAINT uq_label_url_label_id_url
    UNIQUE
(
    label_id,
    url
)
    );
CREATE TABLE IF NOT EXISTS master
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_master_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    data_quality
    VARCHAR
(
    255
),
    title VARCHAR
(
    2000
),
    year smallint
    );
CREATE TABLE IF NOT EXISTS master_artist
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_master_artist_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    artist_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_master_artist_artist_id_artist
    REFERENCES
    artist,
    master_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_master_artist_master_id_master
    REFERENCES
    master,
    CONSTRAINT
    uq_master_artist_master_id_artist_id
    UNIQUE
(
    master_id,
    artist_id
)
    );
CREATE TABLE IF NOT EXISTS master_genre
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_master_genre_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    genre
    VARCHAR
(
    255
)
    CONSTRAINT fk_master_genre_genre_genre
    REFERENCES genre,
    master_id SERIAL NOT NULL
    CONSTRAINT fk_master_genre_master_id_master
    REFERENCES master,
    CONSTRAINT uq_master_genre_master_id_genre
    UNIQUE
(
    master_id,
    genre
)
    );
CREATE TABLE IF NOT EXISTS master_video
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_master_video_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    description
    VARCHAR
(
    40000
),
    title VARCHAR
(
    2000
),
    url VARCHAR
(
    5000
),
    master_id SERIAL NOT NULL
    CONSTRAINT fk_master_video_master_id_master
    REFERENCES master,
    CONSTRAINT uq_master_video_master_id_url
    UNIQUE
(
    master_id,
    url
)
    );
CREATE TABLE IF NOT EXISTS release_item
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    country
    VARCHAR
(
    255
),
    data_quality varchar
(
    255
),
    has_valid_day BOOLEAN,
    has_valid_month BOOLEAN,
    has_valid_year BOOLEAN,
    is_master BOOLEAN,
    listed_release_date VARCHAR
(
    255
),
    notes TEXT,
    release_date date,
    status VARCHAR
(
    255
),
    title VARCHAR
(
    10000
),
    master_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_master_id_master
    REFERENCES master
    );
CREATE TABLE IF NOT EXISTS label_item_release
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_label_item_release_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    label_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_label_item_release_label_id_label
    REFERENCES
    label,
    release_item_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_label_item_release_release_item_id_release_item
    REFERENCES
    release_item,
    category_notation
    VARCHAR
(
    5000
),
    CONSTRAINT uq_label_item_release_release_item_id_label_id
    UNIQUE
(
    release_item_id,
    label_id
)
    );
CREATE TABLE IF NOT EXISTS release_item_artist
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_artist_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    artist_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_release_item_artist_artist_id_artist
    REFERENCES
    artist,
    release_item_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_release_item_artist_release_item_id_release_item
    REFERENCES
    release_item,
    CONSTRAINT
    uq_release_item_artist_release_item_id_artist_id
    UNIQUE
(
    release_item_id,
    artist_id
)
    );
CREATE TABLE IF NOT EXISTS release_item_credited_artist
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_credited_artist_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    role
    VARCHAR
(
    20000
),
    artist_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_credited_artist_artist_id_artist
    REFERENCES artist,
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_credited_artist_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_credited_artist_release_item_id_artist_id_role
    UNIQUE
(
    release_item_id,
    artist_id,
    role
)
    );
CREATE TABLE IF NOT EXISTS release_item_genre
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_genre_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    genre
    VARCHAR
(
    255
)
    CONSTRAINT fk_release_item_genre_genre_genre
    REFERENCES genre,
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_genre_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_genre_release_item_id_genre
    UNIQUE
(
    release_item_id,
    genre
)
    );
CREATE TABLE IF NOT EXISTS release_item_video
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_video_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    description
    VARCHAR
(
    10000
),
    title VARCHAR
(
    10000
),
    url VARCHAR
(
    10000
),
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_video_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_video_release_item_id_url
    UNIQUE
(
    release_item_id,
    url
)
    );
CREATE TABLE IF NOT EXISTS release_item_work
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_work_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    work
    VARCHAR
(
    5000
),
    label_id serial NOT NULL
    CONSTRAINT fk_release_item_work_label_id_label
    REFERENCES label,
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_work_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_work_release_item_id_label_id_work
    UNIQUE
(
    release_item_id,
    label_id,
    work
)
    );
CREATE TABLE IF NOT EXISTS style
(
    name VARCHAR
(
    255
) NOT NULL
    CONSTRAINT pk_style_name
    PRIMARY KEY
    );
CREATE TABLE IF NOT EXISTS master_style
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_master_style_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    master_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_master_style_master_id_master
    REFERENCES
    master,
    style
    VARCHAR
(
    255
)
    CONSTRAINT fk_master_style_style_style
    REFERENCES style,
    CONSTRAINT uq_master_style_master_id_style
    UNIQUE
(
    master_id,
    style
)
    );
CREATE TABLE IF NOT EXISTS release_item_style
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_style_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    release_item_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_release_item_style_release_item_id_release_item
    REFERENCES
    release_item,
    style
    VARCHAR
(
    255
)
    CONSTRAINT fk_release_item_style_style_style
    REFERENCES style,
    CONSTRAINT uq_release_item_style_release_item_id_style
    UNIQUE
(
    release_item_id,
    style
)
    );
CREATE TABLE IF NOT EXISTS release_item_track
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_track_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    position
    VARCHAR
(
    15000
),
    title VARCHAR
(
    15000
),
    duration varchar
(
    5000
),

    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_track_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_track_position_title_duration_release_item_id
    UNIQUE
(
    position,
    title,
    duration,
    release_item_id
)
    );
CREATE TABLE IF NOT EXISTS release_item_format
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_format_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    name
    VARCHAR
(
    2000
),
    quantity integer,
    text varchar
(
    5000
),
    description VARCHAR
(
    10000
),
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_format_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_format_name_quantity_text_release_item_id
    UNIQUE
(
    name,
    quantity,
    text,
    release_item_id
)
    );
CREATE TABLE IF NOT EXISTS release_item_identifier
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_identifier_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    description
    VARCHAR
(
    20000
),
    type varchar
(
    10000
),
    value VARCHAR
(
    10000
),
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_identifier_release_item_id_release_item
    REFERENCES release_item,
    =======
    id SERIAL NOT NULL
    CONSTRAINT pk_artist_id
    PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    last_modified_at TIMESTAMP NOT NULL,
    data_quality VARCHAR
(
    255
),
    name VARCHAR
(
    1000
),
    profile VARCHAR
(
    40000
),
    real_name VARCHAR
(
    2000
)
    );
CREATE TABLE IF NOT EXISTS artist_alias
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_artist_alias_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    alias_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_alias_alias_id_artist
    REFERENCES
    artist,
    artist_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_alias_artist_id_artist
    REFERENCES
    artist,
    CONSTRAINT
    uq_artist_alias_artist_id_alias_id
    UNIQUE
(
    artist_id,
    alias_id
)
    );
CREATE TABLE IF NOT EXISTS artist_group
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_artist_group_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    artist_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_group_artist_id_artist
    REFERENCES
    artist,
    group_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_group_group_id_artist
    REFERENCES
    artist,
    CONSTRAINT
    uq_artist_group_artist_id_group_id
    UNIQUE
(
    artist_id,
    group_id
)
    );
CREATE TABLE IF NOT EXISTS artist_member
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_artist_member_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    artist_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_member_artist_id_artist
    REFERENCES
    artist,
    member_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_artist_member_member_id_artist
    REFERENCES
    artist,
    CONSTRAINT
    uq_artist_member_artist_id_member_id
    UNIQUE
(
    artist_id,
    member_id
)
    );
CREATE TABLE IF NOT EXISTS artist_name_variation
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_artist_name_variation_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    name_variation
    VARCHAR
(
    2000
),
    artist_id SERIAL NOT NULL
    CONSTRAINT fk_artist_name_variation_artist_id_artist
    REFERENCES artist,
    CONSTRAINT uq_artist_name_variation_artist_id_name_variation
    UNIQUE
(
    artist_id,
    name_variation
)
    );
CREATE TABLE IF NOT EXISTS artist_url
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_artist_url_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    url
    VARCHAR
(
    5000
),
    artist_id SERIAL NOT NULL
    CONSTRAINT fk_artist_url_artist_id_artist
    REFERENCES artist,
    CONSTRAINT uq_artist_url_artist_id_url
    UNIQUE
(
    artist_id,
    url
)
    );
CREATE TABLE IF NOT EXISTS discogs_dump
(
    etag VARCHAR
(
    255
) NOT NULL
    CONSTRAINT pk_discogs_dump_id
    PRIMARY KEY,
    created_at date,
    registered_at timestamp,
    size bigint,
    type VARCHAR
(
    255
),
    uri_string varchar
(
    255
),
    url VARCHAR
(
    255
)
    );
CREATE TABLE IF NOT EXISTS genre
(
    name VARCHAR
(
    255
) NOT NULL
    CONSTRAINT pk_genre_name
    PRIMARY KEY
    );
CREATE TABLE IF NOT EXISTS label
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_label_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    contact_info
    VARCHAR
(
    40000
),
    data_quality VARCHAR
(
    255
),
    name VARCHAR
(
    255
),
    profile VARCHAR
(
    40000
)
    );
CREATE TABLE IF NOT EXISTS label_sub_label
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_label_sub_label_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    parent_label_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_label_sub_label_parent_label_id_label
    REFERENCES
    label,
    sub_label_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_label_sub_label_sub_label_id_label
    REFERENCES
    label,
    CONSTRAINT
    uq_label_sub_label_parent_label_id_sub_label_id
    UNIQUE
(
    parent_label_id,
    sub_label_id
)
    );
CREATE TABLE IF NOT EXISTS label_url
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_label_url_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    url
    VARCHAR
(
    5000
),
    label_id SERIAL NOT NULL
    CONSTRAINT fk_label_url_label_id_label
    REFERENCES label,
    CONSTRAINT uq_label_url_label_id_url
    UNIQUE
(
    label_id,
    url
)
    );
CREATE TABLE IF NOT EXISTS master
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_master_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    data_quality
    VARCHAR
(
    255
),
    title VARCHAR
(
    2000
),
    year smallint
    );
CREATE TABLE IF NOT EXISTS master_artist
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_master_artist_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    artist_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_master_artist_artist_id_artist
    REFERENCES
    artist,
    master_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_master_artist_master_id_master
    REFERENCES
    master,
    CONSTRAINT
    uq_master_artist_master_id_artist_id
    UNIQUE
(
    master_id,
    artist_id
)
    );
CREATE TABLE IF NOT EXISTS master_genre
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_master_genre_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    genre
    VARCHAR
(
    255
)
    CONSTRAINT fk_master_genre_genre_genre
    REFERENCES genre,
    master_id SERIAL NOT NULL
    CONSTRAINT fk_master_genre_master_id_master
    REFERENCES master,
    CONSTRAINT uq_master_genre_master_id_genre
    UNIQUE
(
    master_id,
    genre
)
    );
CREATE TABLE IF NOT EXISTS master_video
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_master_video_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    description
    VARCHAR
(
    40000
),
    title VARCHAR
(
    2000
),
    url VARCHAR
(
    5000
),
    master_id SERIAL NOT NULL
    CONSTRAINT fk_master_video_master_id_master
    REFERENCES master,
    CONSTRAINT uq_master_video_master_id_url
    UNIQUE
(
    master_id,
    url
)
    );
CREATE TABLE IF NOT EXISTS release_item
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    country
    VARCHAR
(
    255
),
    data_quality varchar
(
    255
),
    has_valid_day BOOLEAN,
    has_valid_month BOOLEAN,
    has_valid_year BOOLEAN,
    is_master BOOLEAN,
    listed_release_date VARCHAR
(
    255
),
    notes TEXT,
    release_date date,
    status VARCHAR
(
    255
),
    title VARCHAR
(
    10000
),
    master_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_master_id_master
    REFERENCES master
    );
CREATE TABLE IF NOT EXISTS label_item_release
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_label_item_release_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    label_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_label_item_release_label_id_label
    REFERENCES
    label,
    release_item_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_label_item_release_release_item_id_release_item
    REFERENCES
    release_item,
    category_notation
    VARCHAR
(
    5000
),
    CONSTRAINT uq_label_item_release_release_item_id_label_id
    UNIQUE
(
    release_item_id,
    label_id
)
    );
CREATE TABLE IF NOT EXISTS release_item_artist
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_artist_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    artist_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_release_item_artist_artist_id_artist
    REFERENCES
    artist,
    release_item_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_release_item_artist_release_item_id_release_item
    REFERENCES
    release_item,
    CONSTRAINT
    uq_release_item_artist_release_item_id_artist_id
    UNIQUE
(
    release_item_id,
    artist_id
)
    );
CREATE TABLE IF NOT EXISTS release_item_credited_artist
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_credited_artist_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    role
    VARCHAR
(
    20000
),
    artist_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_credited_artist_artist_id_artist
    REFERENCES artist,
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_credited_artist_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_credited_artist_release_item_id_artist_id_role
    UNIQUE
(
    release_item_id,
    artist_id,
    role
)
    );
CREATE TABLE IF NOT EXISTS release_item_genre
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_genre_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    genre
    VARCHAR
(
    255
)
    CONSTRAINT fk_release_item_genre_genre_genre
    REFERENCES genre,
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_genre_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_genre_release_item_id_genre
    UNIQUE
(
    release_item_id,
    genre
)
    );
CREATE TABLE IF NOT EXISTS release_item_video
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_video_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    description
    VARCHAR
(
    10000
),
    title VARCHAR
(
    10000
),
    url VARCHAR
(
    10000
),
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_video_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_video_release_item_id_url
    UNIQUE
(
    release_item_id,
    url
)
    );
CREATE TABLE IF NOT EXISTS release_item_work
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_work_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    work
    VARCHAR
(
    5000
),
    label_id serial NOT NULL
    CONSTRAINT fk_release_item_work_label_id_label
    REFERENCES label,
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_work_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_work_release_item_id_label_id_work
    UNIQUE
(
    release_item_id,
    label_id,
    work
)
    );
CREATE TABLE IF NOT EXISTS style
(
    name VARCHAR
(
    255
) NOT NULL
    CONSTRAINT pk_style_name
    PRIMARY KEY
    );
CREATE TABLE IF NOT EXISTS master_style
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_master_style_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    master_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_master_style_master_id_master
    REFERENCES
    master,
    style
    VARCHAR
(
    255
)
    CONSTRAINT fk_master_style_style_style
    REFERENCES style,
    CONSTRAINT uq_master_style_master_id_style
    UNIQUE
(
    master_id,
    style
)
    );
CREATE TABLE IF NOT EXISTS release_item_style
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_style_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    release_item_id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    fk_release_item_style_release_item_id_release_item
    REFERENCES
    release_item,
    style
    VARCHAR
(
    255
)
    CONSTRAINT fk_release_item_style_style_style
    REFERENCES style,
    CONSTRAINT uq_release_item_style_release_item_id_style
    UNIQUE
(
    release_item_id,
    style
)
    );
CREATE TABLE IF NOT EXISTS release_item_track
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_track_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    position
    VARCHAR
(
    15000
),
    title VARCHAR
(
    15000
),
    duration varchar
(
    5000
),

    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_track_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_track_position_title_duration_release_item_id
    UNIQUE
(
    position,
    title,
    duration,
    release_item_id
)
    );
CREATE TABLE IF NOT EXISTS release_item_format
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_format_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    name
    VARCHAR
(
    2000
),
    quantity integer,
    text varchar
(
    5000
),
    description VARCHAR
(
    10000
),
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_format_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_release_item_format_name_quantity_text_release_item_id
    UNIQUE
(
    name,
    quantity,
    text,
    release_item_id
)
    );
CREATE TABLE IF NOT EXISTS release_item_identifier
(
    id
    SERIAL
    NOT
    NULL
    CONSTRAINT
    pk_release_item_identifier_id
    PRIMARY
    KEY,
    created_at
    TIMESTAMP
    NOT
    NULL,
    last_modified_at
    TIMESTAMP
    NOT
    NULL,
    description
    VARCHAR
(
    20000
),
    type varchar
(
    10000
),
    value VARCHAR
(
    10000
),
    release_item_id SERIAL NOT NULL
    CONSTRAINT fk_release_item_identifier_release_item_id_release_item
    REFERENCES release_item,
    CONSTRAINT uq_identifier_type_description_value_release_item_id
    UNIQUE
(
    type,
    description,
    value,
    release_item_id
)
    );