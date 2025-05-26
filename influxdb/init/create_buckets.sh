#!/bin/sh
influx bucket create \
	-n metrics_pipeline \
	-o "$DOCKER_INFLUXDB_INIT_ORG" \
	-r 30d \
	-t "$DOCKER_INFLUXDB_INIT_ADMIN_TOKEN"