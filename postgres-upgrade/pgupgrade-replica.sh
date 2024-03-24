#!/bin/bash

# As per official postgres guideline (checkout step # 11) [1]
# i.e "11. Upgrade streaming replication and log-shipping standby servers",
# typical upgrade process for replica is as follows:
#
# 1. Install the new PostgreSQL binaries on standby servers
# > spilo has new binaries.
# 2. Make sure the new standby data directories do not exist (or are empty)
# > done
# 3. Install extension shared object files
# > Spilo takes care of it for instance same binaries for timescaledb and/or pg_cron in our case.
# 4. Stop standby servers
# 5. Save configuration files
# > I am skipping this step as we can easily add a new node. Furthermore, this upgrade step is tested thoroughly.
# 6. Run rsync
# 7. Configure streaming replication and log-shipping standby servers
# > Patroni takes care of it. We need to sync "pg_replslot" directory at the end. This allows us to have smooth streaming
# replication.
# [1] https://www.postgresql.org/docs/current/pgupgrade.html

# This script will be executed directly on instance.
# Reason: To avoid complexity of rsync i.e replica & leader communication.

DATA_DIR_PATH_IN_CONTAINER='/home/postgres/pgdata/pgroot/data'
SPILO_CONTAINER_NAME="node-2"
LEADER_IP="localhost"
CLUSTER_NAME="test-cluster"

SOURCE_PG_VERSION="13"
TARGET_PG_VERSION="15"

output() { echo -e "$(date): $*"; }

info() { output "$*"; }

show_prompt() {
  local message=$1
  while true; do
      read -p "$message (No will abort script) " yn
      case $yn in
          [Yy]* ) echo "Awesome, we can proceed now.";break;;
          [Nn]* ) exit;;
          * ) echo "Please answer yes or no.";;
      esac
  done
}

stop_postgres_replica_current_cluster() {
  local container_name=$1
  # since spilo allow specifying different postgres versions in a same bundle, hence multiple
  # binary exist for various versions.
  # I am explicitly using version specific binary.
  docker exec -it "$container_name" su - postgres -c "/usr/lib/postgresql/$SOURCE_PG_VERSION/bin/pg_ctl stop -D $DATA_DIR_PATH_IN_CONTAINER/"

  # https://patroni.readthedocs.io/en/latest/existing_data.html#major-upgrade-of-postgresql-version
  # As per official guideline, we should stop patroni as well.
  docker exec -it "$container_name" sv stop patroni
  list_processes "$container_name" "Logically, you should not see any patroni and/or postgres processes. Are they stopped?"
}

sync_data_directory() {
  info "Syncing DATA directory"
  local old_data_dir=$1
  local new_data_dir=$2
  local destination_data_dir=$3

  rsync --rsync-path="sudo rsync" --archive --delete --hard-links --size-only --no-inc-recursive "$old_data_dir" "$new_data_dir" "$destination_data_dir"

  info "Sync DATA directory completed successfully"

}

sync_directory() {
  info "Syncing directory"
  local source=$1
  local destination=$2

  rsync --rsync-path="sudo rsync" --archive --delete --hard-links --size-only --no-inc-recursive "$source/" "$destination"

  info "Sync WAL directory completed successfully"

}

adjust_patroni_config_for_pg15() {
  local container_name=$1
  info "Adjusting patroni configuration"
  docker exec -it "$container_name" cp /home/postgres/postgres.yml /home/postgres/postgres.yml_BAK
  docker exec -it "$container_name" sed -r --in-place 's/\/wal$/\/wal-new/g;' /home/postgres/postgres.yml
  docker exec -it "$container_name" sed -r --in-place 's/\/data$/\/data-new/g;' /home/postgres/postgres.yml
  docker exec -it "$container_name" sed -r --in-place "s/\/postgresql\/$SOURCE_PG_VERSION\//\/postgresql\/$TARGET_PG_VERSION\//g;" /home/postgres/postgres.yml
  docker exec -it "$container_name" diff /home/postgres/postgres.yml_BAK /home/postgres/postgres.yml

  docker exec -it "$container_name" rm /home/postgres/postgres.yml_BAK
}

list_processes() {
  local container_name=$1
  local message=$2

  while true; do
        docker exec -it "$container_name" bash -c "ps aux"
        read -p "$message (No will recheck it). " yn
        case $yn in
            [Yy]* ) echo "Awesome, we can proceed now.";break;;
            [Nn]* ) echo "Let's check it again.";;
            * ) echo "Please answer yes or no.";;
        esac
    done
}

start_patroni() {
  local container_name=$1

  docker exec -it "$container_name" sv start patroni
  check_patroni_state "$container_name"
}

restart_postgres_service() {
  local leader_ip=$1
  info "Restarting postgres service on leade node: $leader_ip"
  curl --request POST --url "http://$leader_ip:8008/restart"
}

resume_patroni() {
  info "Check whether to resume patroni or not"
  sleep 2

  local cluster_name=$1
  local container_name=$2

  is_cluster_resumeable "$cluster_name" "$container_name"
}

is_cluster_resumeable() {
  local cluster_name=$1
  local container_name=$2
  while true; do
        docker exec -it "$container_name" patronictl list
        read -p "Is our cluster in maintenance mode? " yn
        case $yn in
            [Yy]* ) echo "Resuming patroni."; docker exec -it "$container_name" patronictl resume "$cluster_name" --wait; break;;
            [Nn]* ) echo "Cool. Nothing needs to be done."; break;;
            * ) echo "Please answer yes or no.";;
        esac
    done
}

check_patroni_state() {
  local container_name=$1
  while true; do
        docker exec -it "$container_name" sv status patroni
        read -p "Is Patroni running? (write 'no' to recheck)" yn
        case $yn in
            [Yy]* ) echo "Awesome, we can proceed now.";break;;
            [Nn]* ) echo "Let's check it again.";;
            * ) echo "Please answer yes or no.";;
        esac
    done
}

successful_notification() {
  local container_name=$1
  info "Replica upgraded successfully"

  docker exec -it "$container_name" patronictl list
}
failure_notification() {
  info "Encountered failure. Exiting ..."
}

# NOTE: We dont need to adjust recovery config for replica as we are copying data directory from leader node.
# Leader node does not have this configuration at all.
# Furthermore, we dont have to start postgres service explicitly on a replica node. Patroni takes care of it.
{ show_prompt "Could you successfully SSH leader node?"; } &&
  { stop_postgres_replica_current_cluster "$SPILO_CONTAINER_NAME"; } &&
  { sync_data_directory "data-1/pgroot/data" "data-1/pgroot//data-new" "data-1/pgroot/pgroot"; } &&
  { adjust_patroni_config_for_pg15 "$SPILO_CONTAINER_NAME"; } &&
  { start_patroni "$SPILO_CONTAINER_NAME"; } &&
  { restart_postgres_service "$LEADER_IP"; } &&
  { resume_patroni "$CLUSTER_NAME" "$SPILO_CONTAINER_NAME"; } &&
  { sync_directory "data-1/pgroot/data-new/pg_replslot" "data-1/pgroot/data-new/pg_replslot"; } &&
  { successful_notification "$SPILO_CONTAINER_NAME"; } ||
  { failure_notification; }