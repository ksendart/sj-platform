package com.bwsw.sj.common.utils

import java.util.UUID

import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}

object EngineLiterals {
  val persistentQueuePath = UUID.randomUUID().toString

  final val queueSize = 1000
  final val persistentBlockingQueue = "persistentBlockingQueue"
  final val batchInstanceBarrierPrefix = "/instance/barriers/"
  final val batchInstanceLeaderPrefix = "/instance/leaders/"
  final val eventWaitTimeout = 1000

  final val inputStreamingType = "input-streaming"
  final val outputStreamingType = "output-streaming"
  final val batchStreamingType = "batch-streaming"
  final val regularStreamingType = "regular-streaming"
  val moduleTypes = Seq(batchStreamingType, regularStreamingType, outputStreamingType, inputStreamingType)

  final val everyNthMode = "every-nth"
  final val timeIntervalMode = "time-interval"
  final val transactionIntervalMode = "transaction-interval"
  val checkpointModes = Seq(everyNthMode, timeIntervalMode)
  val batchFillTypes = Seq(everyNthMode, timeIntervalMode, transactionIntervalMode)

  final val noneStateMode = "none"
  final val ramStateMode = "ram"
  final val rocksStateMode = "rocks"
  val stateManagementModes = Seq(noneStateMode, ramStateMode, rocksStateMode)

  final val oldestStartMode = "oldest"
  final val newestStartMode = "newest"
  val startFromModes = Seq(oldestStartMode, newestStartMode)

  final val noneDefaultEvictionPolicy = "NONE"
  final val lruDefaultEvictionPolicy = "LRU"
  final val lfuDefaultEvictionPolicy = "LFU"
  val defaultEvictionPolicies = Seq(lruDefaultEvictionPolicy, lfuDefaultEvictionPolicy, noneDefaultEvictionPolicy)

  final val fixTimeEvictionPolicy = "fix-time"
  final val expandedTimeEvictionPolicy = "expanded-time"
  val evictionPolicies = Seq(fixTimeEvictionPolicy, expandedTimeEvictionPolicy)

  final val ready = "ready"
  final val starting = "starting"
  final val started = "started"
  final val stopping = "stopping"
  final val stopped = "stopped"
  final val deleting = "deleting"
  final val deleted = "deleted"
  final val failed = "failed"
  final val error = "error"
  val instanceStatusModes = Seq(starting,
    started,
    stopping,
    stopped,
    ready,
    deleting,
    deleted,
    failed,
    error
  )

  val toHandle = "to-handle"

  final val splitStreamMode = "split"
  final val fullStreamMode = "full"
  val streamModes = Array(splitStreamMode, fullStreamMode)
}

object StreamLiterals {
  final val inputDummy = "input"
  final val tstreamType = "stream.t-stream"
  final val kafkaStreamType = "stream.kafka"
  final val esOutputType = "elasticsearch-output"
  final val jdbcOutputType = "jdbc-output"
  val types = Seq(tstreamType, kafkaStreamType, jdbcOutputType, esOutputType)

  private val tstreamFactory = new TStreamsFactory()
  final val ttl = tstreamFactory.getProperty(ConfigurationOptions.Stream.ttlSec).asInstanceOf[Int]
}

object ServiceLiterals {
  final val cassandraType = "CassDB"
  final val elasticsearchType = "ESInd"
  final val kafkaType = "KfkQ"
  final val tstreamsType = "TstrQ"
  final val zookeeperType = "ZKCoord"
  final val aerospikeType = "ArspkDB"
  final val jdbcType = "JDBC"

  val types = Seq(
    cassandraType,
    elasticsearchType,
    kafkaType,
    tstreamsType,
    zookeeperType,
    aerospikeType,
    jdbcType
  )

  val typeToProviderType = Map(
    cassandraType -> ProviderLiterals.cassandraType,
    elasticsearchType -> ProviderLiterals.elasticsearchType,
    kafkaType -> ProviderLiterals.kafkaType,
    zookeeperType -> ProviderLiterals.zookeeperType,
    aerospikeType -> ProviderLiterals.aerospikeType,
    jdbcType -> ProviderLiterals.jdbcType,
    tstreamsType -> ProviderLiterals.zookeeperType
  )
}

object ProviderLiterals {
  final val cassandraType = "cassandra"
  final val aerospikeType = "aerospike"
  final val zookeeperType = "zookeeper"
  final val kafkaType = "kafka"
  final val elasticsearchType = "ES"
  final val jdbcType = "JDBC"
  val types = Seq(cassandraType, aerospikeType, zookeeperType, kafkaType, elasticsearchType, jdbcType)
}

object FrameworkLiterals {
  val instanceIdLabel = "INSTANCE_ID"
  val frameworkIdLabel = "FRAMEWORK_ID"
  val mesosMasterLabel = "MESOS_MASTER"
}