# HikariCP Timeout Demo

A demonstration project showing how connection timeouts work in HikariCP with Scala.


## Overview
This project demonstrates various scenarios with HikariCP connection pooling:
- Connection timeout when non-DB work blocks a connection
- Connection timeout when a long-running query blocks other queries

## Requirements
PostgreSQL database running on localhost:5432
```
docker compose up -d
```

## Blog Posts
For more detailed explanations of the concepts demonstrated in this project,
check out my blog posts:
- [Optimize HikariCP Pool to Prevent Timeouts](https://medium.com/@anasanjaria/optimize-hikaricp-pool-to-prevent-timeouts-4bdc1120a273)
- [How to Prevent HikariCP Timeout Failures](https://medium.com/@anasanjaria/how-to-prevent-hikaricp-timeout-failures-9486f398e15c)

## Running the Project
To run the project, use the following command:

```bash
sbt test
```
