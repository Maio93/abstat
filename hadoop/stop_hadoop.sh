#!/bin/bash

# Stop the namenode daemon
$HADOOP_PREFIX/sbin/hadoop-daemon.sh stop namenode
# Stop the datanode daemon
$HADOOP_PREFIX/sbin/hadoop-daemon.sh stop datanode

# Stop the resourcemanager daemon
$HADOOP_PREFIX/sbin/yarn-daemon.sh stop resourcemanager
# Stop the nodemanager daemon
$HADOOP_PREFIX/sbin/yarn-daemon.sh stop nodemanager
