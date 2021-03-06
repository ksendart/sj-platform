package com.bwsw.sj.common.DAL.model

import java.util.Properties

import com.bwsw.sj.common.rest.entities.stream.KafkaSjStreamData
import com.bwsw.sj.common.utils.ConfigSettingsUtils
import kafka.admin.AdminUtils
import kafka.common.TopicAlreadyMarkedForDeletionException
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection

class KafkaSjStream() extends SjStream {

  var partitions: Int = 0
  var replicationFactor: Int = 0

  def this(name: String,
           description: String,
           partitions: Int,
           service: Service,
           streamType: String,
           tags: Array[String],
           replicationFactor: Int) = {
    this()
    this.name = name
    this.description = description
    this.partitions = partitions
    this.service = service
    this.streamType = streamType
    this.tags = tags
    this.replicationFactor = replicationFactor
  }

  override def asProtocolStream() = {
    val streamData = new KafkaSjStreamData
    super.fillProtocolStream(streamData)

    streamData.partitions = this.partitions
    streamData.replicationFactor = this.replicationFactor

    streamData
  }

  override def create() = {
    try {
      val zkUtils = createZkUtils()
      if (!AdminUtils.topicExists(zkUtils, this.name)) {
        AdminUtils.createTopic(zkUtils, this.name, this.partitions, this.replicationFactor, new Properties())
      }
    } catch {
      case ex: TopicAlreadyMarkedForDeletionException =>
        throw new Exception(s"Cannot create a kafka topic ${this.name}. Topic is marked for deletion. It means that kafka doesn't support deleti")
    }
  }

  override def delete() = {
    try {
      val zkUtils = createZkUtils()
      if (AdminUtils.topicExists(zkUtils, this.name)) {
        AdminUtils.deleteTopic(zkUtils, this.name)
      }
    } catch {
      case ex: TopicAlreadyMarkedForDeletionException =>
        throw new Exception(s"Cannot delete a kafka topic '${this.name}'. Topic is already marked for deletion. It means that kafka doesn't support deleti")
    }
  }

  private def createZkUtils() = {
    val service = this.service.asInstanceOf[KafkaService]
    val zkHost = service.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = ConfigSettingsUtils.getZkSessionTimeout()
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)

    zkUtils
  }
}
