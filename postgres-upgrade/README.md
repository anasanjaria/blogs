# PostgreSQL Cluster Upgrade Guide

This repository contains scripts and configurations for setting up and upgrading a PostgreSQL cluster using Zalando's spilo repository. 
It provides guides for both minor and major version upgrades.

## Getting Started

To set up a local cluster, follow these steps:

1. Ensure you have sudo privileges.
2. Run the setup script:
```
./setup.sh
```
3. Start the first node:
```
docker-compose up -d node-1
```
4. Wait until the cluster is healthy.
5. Start the second node
```
docker-compose up -d node-2
```
6. To tear down completely.
```
./teardown.sh
```
## Upgrade Process

I have covered both upgrades in detail in my following blog posts.

- [Minor version upgrade guide](https://medium.com/@anasanjaria/optimize-postgresql-minor-version-upgrade-guide-7101a94236de)
- [Major version upgrade guide](https://medium.com/@anasanjaria/7af55e2c80a5)