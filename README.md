![Java CI with Maven](https://github.com/ther3tyle/dump-db-mgmt-mongodb/workflows/Java%20CI%20with%20Maven/badge.svg?branch=master)

> NOTE: Currently under heavy modification. Rework ETA will be around MAY 2021

# Batch uploader for MongoDB and Discogs Dump.
A simplified, one-shot based batch uploader for all artists, releases, labels and master releases and their relationships.

If you have any suggestions, critics or equivalent, don't hesitate.
I am currently wide open to accept any contributors!

## About this project...
Inspired by dump xml monthly exports from [discogs.com](https://www.discogs.com), I personally made this to be used as rest service with hateoas as a mirror DB for my other projects. 
Estimates for total procedure may vary depends on operating system.

### Built with
*[Spring-Boot starter](https://spring.io/projects/spring-boot) - Base framework.

*[Spring-Batch](https://spring.io/projects/spring-batch) - With separated DB connection (MySQL, MongoDB)

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
Just as any boilerplate spring project, run main.
Note: be sure to set properties before proceed.

### Notes
By default, Spring-Batch relies on one datasource (a database) to upload its status and progress reports.

This project utilizes MySQL to record such data, while batch processing xml data itself to be persisted in separate Mongo DB. Note that traditional relational databases will require extra steps to ensure the ACID transaction.

One way to tackle the transaction issue (and possibly the best) would be to stick with single threaded job execution, however, will take very long time (approx 3 to 4 days)

I highly recommend to upload to local MongoDB first; then migrate.

Also, do note that I will not develop features in terms of command-line interface as the nature of project requires too many environment variables to be generalized. It would be wiser for you to fork or clone the project; then bring cli features that meets your req.
  
### Parallel Processing
Set core size to define parellel operation.
The project contains two distinct operations

1. XML -> Process -> Persist

2. XML -> find from MongoDB -> Process -> Persist

Both procedures would benefit from parellel operation, however, it may be changed over time.

Also, keep in mind that each operation could be blocked time to time as the nature of batch works with DB locks engaged normally does.

### Minimal Parallel Processing
If you wish to run the steps with minimal core utilization, you may do so by commenting out the ".taskexecutor()" from each step builders in JobConfig.

Remember, DO NOT set the core pool size to 1, which will effectively NOT running the batch operation.

### REST Service
Since the project is built upon spring-boot, it is simple to implement rest service.
Simply add Spring-Boot Web Starter as a dependency, then use services in your controller classes.
For further instructions, please read official documentations from spring boot reference.

### Future Plans
1. Refactoring codes for duplicated, boilerplate codes.
2. Reconsider project structure (v.0.1.7 handled first major reconstruction)
3. Implement slow, but secure MySQL variant (meaning two separate schema; JobRepository DataSource, actual DataSource)
