#!/bin/bash

# This script will be run inside a container because
# - It makes upgrade process easier.
# - Some binaries such pg_init/pg_upgrade for PG 15 are only available inside a container.

# Typical upgrade process is as follows:
# 1. Init DB
# 2. Copy config
# 3. Perform upgrade.
# Since, we are managing our cluster with patroni, hence there are some additional steps we need to consider.

output() { echo -e "$(date): $*"; }

info() { output "$*"; }

# New data directory for PG 15
NEW_DATA_DIR_PATH='/home/postgres/pgdata/pgroot/data-new'
# Current data directory used by our system
DATA_DIR_PATH='/home/postgres/pgdata/pgroot/data'

SOURCE_PG_VERSION="13"
TARGET_PG_VERSION="15"

create_new_folders() {
  info "Creating new folders for upgrade"
  su - postgres -c "mkdir -p $NEW_DATA_DIR_PATH"
}

enable_write_lock_on_current_cluster() {
  { curl --request PATCH \
    --url http://localhost:8008/config \
    --header 'Content-Type: application/json' \
    --data '{
  	"postgresql": {
  		"parameters": {
  			"default_transaction_read_only": "on"
  		}
  	}
  }'; } &&
    { info "Write lock applied successfully. Now, confirming it from DB."; } &&
    { ensure_write_lock_activated; } &&
    { show_running_transactions; }
}

show_running_transactions() {
  # Logically, we should wait for running INSERT/UPDATE/DELETE queries to ensure data integrity.
  info "Showing currently running transaction. Take actions accordingly."
  su - postgres -c "psql -c \"SELECT pid, now() - pg_stat_activity.query_start AS duration, query, state FROM pg_stat_activity where not (query like '%select%' or query like '%SELECT%' or query = '');\""
  show_prompt "Have you taken necessary actions?"
}

ensure_write_lock_activated() {
  while true; do
        su - postgres -c "psql -c 'SHOW default_transaction_read_only;' "
        read -p "Logically, it should be 'on' now. Is write-lock already activated? (No will recheck it). " yn
        case $yn in
            [Yy]* ) echo "Awesome, we can proceed now.";break;;
            [Nn]* ) echo "Let's check it again.";;
            * ) echo "Please answer yes or no.";;
        esac
    done
}

ensure_replica_lag_zero() {
  check_patroni_cluster_state "Check that the replication lag is 0. Is it zero?"
}

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

stop_postgres_server_current_cluster() {
  # since spilo allow specifying different postgres versions in a same bundle, hence multiple
  # binary exist for various versions.
  # I am explicitly using version specific binary.
  su - postgres -c "/usr/lib/postgresql/$SOURCE_PG_VERSION/bin/pg_ctl stop -D $DATA_DIR_PATH/"
  # https://patroni.readthedocs.io/en/latest/existing_data.html#major-upgrade-of-postgresql-version
  # As per official guideline, we should stop patroni as well.
  sv stop patroni;
  ps aux
  show_prompt "Postgres processed should be stopped now. Are they already stopped?"

}

pause_patroni() {
  local cluster_name=$1
  patronictl pause "$cluster_name" --wait
}

initdb_pg15() {
  su - postgres -c "/usr/lib/postgresql/$TARGET_PG_VERSION/bin/initdb -D $NEW_DATA_DIR_PATH \
    --locale=en_US.UTF-8 \
    --encoding=UTF8 \
    --data-checksums"
}

copy_configs_to_pg_15() {
  # Copy pg 15 postgres.conf as base so that spilo can use this.
  # Background: Spilo uses default postgres as .base.conf and includes it at the very top in postgres.conf
  { su - postgres -c "cp $NEW_DATA_DIR_PATH/postgresql.conf $NEW_DATA_DIR_PATH/postgresql.base.conf"; } &&
    {
      # Copying old config to new cluster
      su - postgres -c "cp $DATA_DIR_PATH/postgresql.conf $NEW_DATA_DIR_PATH/postgresql.conf";
    } &&
    { su - postgres -c "cp $DATA_DIR_PATH/pg_hba.conf $NEW_DATA_DIR_PATH/pg_hba.conf"; } &&
    { su - postgres -c "cp $DATA_DIR_PATH/patroni.dynamic.json $NEW_DATA_DIR_PATH/patroni.dynamic.json"; }

}

perform_upgrade() {
  # --jobs -> number of simultaneous processes or threads to use
  local jobs
  jobs=$(grep -c -E '^processor' /proc/cpuinfo)
  su - postgres -c "/usr/lib/postgresql/$TARGET_PG_VERSION/bin/pg_upgrade \
      --jobs=$jobs \
      -b /usr/lib/postgresql/$SOURCE_PG_VERSION/bin/ \
      -B /usr/lib/postgresql/$TARGET_PG_VERSION/bin/ \
      -d $DATA_DIR_PATH/ \
      -D $NEW_DATA_DIR_PATH \
      --link"
}

adjust_patroni_config_for_pg15() {
  sed -r --in-place 's/\/wal$/\/wal-new/g;' /home/postgres/postgres.yml
  sed -r --in-place 's/\/data$/\/data-new/g;' /home/postgres/postgres.yml
  sed -r --in-place "s/\/postgresql\/$SOURCE_PG_VERSION\//\/postgresql\/$TARGET_PG_VERSION\//g;" /home/postgres/postgres.yml
}

check_patroni_cluster_state() {
  local message=$1
  while true; do
        patronictl list
        read -p "$message (No will recheck it). " yn
        case $yn in
            [Yy]* ) echo "Awesome, we can proceed now.";break;;
            [Nn]* ) echo "Let's check it again.";;
            * ) echo "Please answer yes or no.";;
        esac
    done
}
wipe_complete_cluster_state_from_DCS() {
  info "Wiping cluster state from DCS"
  local cluster_name=$1

  { check_patroni_cluster_state "Ideally, cluster should be unhealthy & leader node should not be there. Is it unhealthy?"; } &&
    { patronictl remove "$cluster_name"; }

}

# Starting postgres explicitly and not relying on patroni as patroni sometimes don't start it.
# I don't see any problem starting it explicitly.
# So, postgres service will be started followed by patroni.
start_postgres() {
  info "Starting postgres service"

  su - postgres -c "/usr/lib/postgresql/$TARGET_PG_VERSION/bin/pg_ctl -D $NEW_DATA_DIR_PATH -l logfile start"
  ensure_postgres_service_started
}

ensure_postgres_service_started() {
  while true; do
        ps aux
        read -p "Now, you see postgres processes. Do you see them? (No will recheck it). " yn
        case $yn in
            [Yy]* ) echo "Awesome, we can proceed now.";break;;
            [Nn]* ) echo "Let's check it again.";;
            * ) echo "Please answer yes or no.";;
        esac
    done
}

start_patroni() {
  info "Starting patroni"

  sv start patroni
}

successful_notification() {
  info "Cluster upgraded successfully"

  patronictl list
}
failure_notification() {
  info "Encountered failure. Exiting ..."
}

info "******************************"
info "**** UPGRADING STARTED ******"
info "******************************"

CLUSTER_NAME="test-cluster"

{ create_new_folders; } &&
  { enable_write_lock_on_current_cluster; } &&
  { ensure_replica_lag_zero; } &&
  { pause_patroni "$CLUSTER_NAME"; } &&
  { stop_postgres_server_current_cluster; } &&
  { initdb_pg15; } &&
  { copy_configs_to_pg_15; } &&
  { perform_upgrade; } &&
  { adjust_patroni_config_for_pg15; } &&
  { wipe_complete_cluster_state_from_DCS "$CLUSTER_NAME"; } &&
  { start_postgres; } &&
  { start_patroni; } &&
  { successful_notification; } ||
  { failure_notification; }
