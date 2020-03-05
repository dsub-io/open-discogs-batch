# Batch uploader for MongoDB and Discogs Dump.
A simplified, one-shot based batch uploader for all artists, releases, labels and master releases and their relationships.

## About this project...
Inspired by dump xml monthly exports from [discogs.com](https://www.discogs.com), I personally made this to be used as rest service with hateoas as a mirror DB for my other projects. 
Currently, the entire steps to build a full collections of Mongo DB will take 6~7 hours with 3 concurrent TaskExecutors.

### Built with
*[Spring-Boot starter](https://spring.io/projects/spring-boot) - Base framework.

*[Spring-Batch](https://spring.io/projects/spring-batch) - With separated DB connection.

*[Spring-Data](https://spring.io/projects/spring-data) - Database connection with simplified repositories.

## Before start
The project is done with spring-boot framework, which shares most common senses from
general, typical spring-boot project defaults.
```
src/main/resources/application.properties
```
Almost all required properties reside in application.properties file, hence it is
highly recommended to look at the file before begin batch process. Especially, the directory, or a file name of xml source materials must be specified in [application.properties](https://raw.githubusercontent.com/sehy0121/dump-db-mgmt-mongodb/master/src/main/resources/application.properties).

## How to use
  1. Open the project in any IDE of your choice, import dependencies as usual.
  2. Put each extracted* xml files (artists, labels, masters, releases) in classpath (where your maven wrapper resides)
  3. Go to application.properties, set each file name accordingly, set db connections (for local, just set user id and password will be suffice)
  4. Run the sql (schema-mysql.sql) to create schema for the batch log in MySQL.
  5. Launch.....

### Notes
By default, Spring-Batch relies on one datasource (a database) to upload its status and progress reports.
This project utilizes MySQL to record such data, while batch processing xml data itself to be persisted in separate Mongo DB. Note that traditional relational databases will require extra steps to ensure the ACID transaction.
  One way to tackle the transaction issue (and possibly the best) would be to stick with single threaded job execution, however, will take very long time (approx 3 to 4 days)
I highly recommend to upload to local MongoDB first, then migrate.

Also, do note that I will not develop any possible command-line based configurations as of this project requires too many environment variables required, and its nature of one-shot process.
  
### Parallel Processing
As of the largest bottleneck is the parsing speed from XML, I highly recommend only manage core pool size from the application.properties. The efficiency is absolutely depending on read speed, hence you may try 16 cores, 24 cores even.

However, it is important that in spring batch, each processing and writing needs to be FILLED FIRST, THEN then fired up (The count is not guaranteed to exactly 1000, 2000, or so)

Personally, I think 2 to 4 cores will be suffice, but it is absolutely up to you, as a developer, which option would be best suited in your use case.

WARNING: setting core pool size to 1 will help your batch operation NOT working at all.

### Synchronized Processing
If you wish to run the steps with minimal core utilization, you may do so by commenting out the ".taskexecutor()" from each builders in JobConfig.
Remember, DO NOT set the core pool size to 1, which will effectively NOT running the batch operation.

### REST Service
Since the project is built upon spring-boot, it is simple to implement rest service.
Simply add Spring-Boot Web Starter as a dependency from pom.xml, then utilize service classes.
For further instructions, please read official documentations from spring boot reference.

### Future Plans
1. Refactoring codes for duplicated, boilerplate codes.
2. Reconsider project structure.
3. Implement slow, but secure MySQL variant (meaning two separate schema; JobRepository DataSource, actual DataSource)
