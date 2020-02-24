# Batch uploader for MongoDB and Discogs Dump.
A simplified, one-shot based batch uploader for all artists, releases, labels and master releases and their relationships.

## About this project...
  Inspired by dump xml monthly exports from [discogs.com](https://www.discogs.com), I personally made this to be used as rest service with hateoas as a mirror DB for my other projects. Currently, the entire steps to build a full collections of Mongo DB will take 6~7 hours with 3 concurrent TaskExecutors.

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

### Notes
  By default, Spring-Batch relies on one datasource (a database) to upload its status and progress reports.
This project utilizes MySQL to record such data, while batch processing xml data itself to be persisted in separate Mongo DB. Note that traditional relational databases will require extra steps to ensure the transcation, due to the deadlocks caused by multithreadings.
  One way to solve the issue (and possibly the best) would be to stick with single threaded job execution, however, will take very long time (approx 3 to 4 days)
I highly recommend to upload to local MongoDB first, then migrate.

  Also, do note that I will not develop any possible command-line based configurations as of this project requires too many environment variables required, and its nature of one-shot process.
  
### Concurrency Issue
  By default, Spring Data Jpa utilizes OGM for NoSQL databases. Currently, there are no issues when running a step with concurrent setups. However, do note that there will be significant deadlocks and retries, plus exception handlings will be required if you wish to run a job against relational databases. Moreover, the xml itself contains many empty holes such as release dates are not exactly formatted to be naturally fit into any database date time format. Thus, one must assume that it omit many exceptions during parse then persist procedure during each release steps.
  
Note that there is a reason why I ditched the beloved rdbms for this project. However, I may provide updates upon any supports to MySQL or PostGRES if time allows... (or maybe with a Golang)

### REST Service
Since the project is built upon spring-boot, it is simple to implement rest service.
Simply add Spring-Boot Web Starter as a dependency from pom.xml, then utilize service classes.
For further instructions, please read official documentations from spring boot reference.

### Future Plans
1. Refactoring codes for duplicated, boilerplate codes.
2. Reconsider project structure.
3. Implement slow, but secure MySQL variant (meaning two separate schema; JobRepository DataSource, actual DataSource)
