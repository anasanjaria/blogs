# HikariCP Timeout Demo

A small demonstration project that explains **how and why connection timeouts happen in HikariCP**, using Scala and PostgreSQL.

This repo focuses on *real-world failure patterns* you’re likely to see in production systems.

## What this project demonstrates

The examples cover common scenarios that lead to HikariCP timeouts:

- ⏳ Connection timeouts caused by **non-database work holding a connection**
- 🐢 Connection starvation caused by **slow or long-running queries**
- 🧵 How pool size, execution model, and workload interact under pressure

The goal is not to tune HikariCP blindly, but to understand **what’s actually blocking connections**.

## Prerequisites

- PostgreSQL running on `localhost:5432`

You can start a local database using Docker:

```bash
docker compose up -d
```

## Related content

If you want a deeper explanation of the concepts shown here, check out:

[Understanding HikariCP Connection Timeout | 3 Real-World Examples Explained](https://youtu.be/YzVdaoJEnRk)

✍️ Blog posts

- [Optimize HikariCP Pool to Prevent Timeouts](https://medium.com/illumination/optimize-hikaricp-pool-to-prevent-timeouts-4bdc1120a273)
- [How to Prevent HikariCP Timeout Failures](https://medium.com/illumination/how-to-prevent-hikaricp-timeout-failures-9486f398e15c)

Running the project

Run the test suite to reproduce the scenarios: