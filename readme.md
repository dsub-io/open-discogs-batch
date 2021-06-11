# Discogs-Data for Batch and API

### ABOUT THIS PROJECT

This repository contains mainly two components:

1. Batch uploader for Database
    - Currently, supporting PostgresQL, MySQL
    - One shot process with many run options before firing jobs
    - Supports docker


2. Web API (WIP) - currently not implemented nor documented.
    - Will support data-flux, async REST API
    - Will support docker

### Built With

[Spring-Boot starter](https://spring.io/projects/spring-boot)

[Spring-Batch](https://spring.io/projects/spring-batch)

[Spring-Data](https://spring.io/projects/spring-data)

[ProgressBar](https://github.com/ctongfei/progressbar)

## Batch Commands

Commands will be accepted regardless of -- mark <b>ONLY IF</b> gets arguments directly from jar
file. Also, there is no impact from giving arguments in certain order. However, it will NOT accept
any duplicated arguments.

i.e. --m will work, as well as -m, m will.

Brief summary for the commands are given as below...

|    NAME    | SYNONYM  |      REQUIRED         | MIN | MAX | FORMAT    | DEFAULT |  NOTE |
|------------|----------|-----------------------|-----|-----|-----------|---------|-------|
| username   | user, u  | :heavy_check_mark:    | 1   | 1   | STRING    | NULL                                |
| password   | pass, p  | :heavy_check_mark:    | 1   | 1   | STRING    | NULL                                |
| url        |          | :heavy_check_mark:    | 1   | 1   | addr:port | mysql://localhost:3306/discogs_data |
| type       | t        | :black_square_button: | 1   | 4   | a,b,...   | EVERY TYPE                          |
| chunk_size | chunk, c | :black_square_button: | 1   | 1   | 0 < N     | 3000    |
| year       | y        | :black_square_button: | 1   | 1   | yyyy      | CURRENT | this or year_month.
| year_month | ym       | :black_square_button: | 1   | 1   | yyyy-mm   | CURRENT | this or year.
| etag       | e        | :black_square_button: | 1   | 4   | a,b,...   | MOST_RECENT | overrides type, date.
| mount      | m        | :black_square_button: | 0   | 0   | NONE      | -       | keep dump file
| strict     | s        | :black_square_button: | 0   | 0   | NONE      | -       | only perform specified type or ETag

### Required Arguments

It is important to note that there are three required arguments.

##### username

Username of the target database server. This will automatically be encoded to UTF-8.

##### password

Password for the username given. This will automatically be encoded to UTF-8.

##### url

URL for the target database. The expected format for the url would be...

```text
--url={db_type://}{server_address}:{port}/{target_database}
```

Default for db_type, server_address, port, target_database are as following:

- db_type=mysql
- server_address=localhost
- port=3306
- target_database=discogs_data

By defining any of above will be set as-is. Plus, it is OK to prepend Jdbc: to the url value (but
will work just the same.)

### Year, Year Month, Type and ETag.

First and foremost, by specifying the ETag, any arguments given for year, year-month, type will be
ignored. This is intended behavior as each dump relies on other dump types in specified year and
month.

Other than ETag, it is important to note that providing both year and year-month at the same time is
not supported.

Finally, types cannot be duplicated.

If you specify a year and a type for example, batch will automatically fetch and process the target
dump INCLUDING the dependant dump.

### Dependencies

Dump dependency for other type are described as following:

|    TYPE   |       REQUIRES        |
|-----------|-----------------------|
| ARTIST    | -                     |
| LABEL     | -                     |
| MASTER    | ARTIST, LABEL         |
| RELEASE   | ARTIST, LABEL, MASTER |

The job will always be executed by order as following:

```text
ARTIST > LABEL > MASTER > RELEASE
```

If you run the batch with following arguments:
> url=[?] user=[?] pass=[?] year-month=2021-3 type=release

Batch will be executed with artist, label, master, release dumps from 2021, March.

If you do not specify any options, but simply call the batch by username, password and url, then
batch will be executed with most recent artist, label, master, release dumps.

### Mount and Strict

##### Mount

If mount option is specified, the downloaded file from the discogs data will not be removed. This
maybe useful if you need to keep the downloaded dump.

##### Strict

This option will not resolve any dependency, but to simply execute with given etag or type.

## About Chunk-Size and Concurrency

The application will automatically resolve the current core size of running system (currently 80%).
I will implement the manual options if any issue or request regarding this happens.

The default chunk-size is 3000, however, in average environment, I would recommend to set to 300~
500. This is totally up to the I/O spec of the running client and database server. There are chances
I may dig into this issue a bit deeper, to find the sweet spot (if possible.)