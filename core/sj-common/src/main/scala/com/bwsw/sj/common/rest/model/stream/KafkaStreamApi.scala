package com.bwsw.sj.common.rest.model.stream

import java.util.Properties

import com.bwsw.sj.common.dal.model.service.KafkaServiceDomain
import com.bwsw.sj.common.dal.model.stream.KafkaStreamDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigurationSettingsUtils
import com.bwsw.sj.common.si.model.stream.KafkaStream
import com.bwsw.sj.common.utils.MessageResourceUtils._
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals, StreamLiterals}
import kafka.admin.AdminUtils
import kafka.common.TopicAlreadyMarkedForDeletionException
import kafka.utils.ZkUtils
import org.I0Itec.zkclient.ZkConnection
import org.apache.kafka.common.errors.TopicExistsException

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class KafkaStreamApi(override val name: String,
                     override val service: String,
                     override val tags: Array[String] = Array(),
                     override val force: Boolean = false,
                     override val description: String = RestLiterals.defaultDescription,
                     val partitions: Int = Int.MinValue,
                     val replicationFactor: Int = Int.MinValue)
  extends StreamApi(StreamLiterals.kafkaStreamType, name, service, tags, force, description) {

  override def to: KafkaStream = new KafkaStream(
    name,
    service,
    partitions,
    replicationFactor,
    tags,
    force,
    streamType,
    description)

  override def validate(): ArrayBuffer[String] = {
    val serviceDAO = ConnectionRepository.getServiceRepository
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    //partitions
    if (this.partitions <= 0)
      errors += createMessage("entity.error.attribute.required", "Partitions") + ". " +
        createMessage("entity.error.attribute.must.be.positive.integer", "Partitions")


    Option(this.service) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Service")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Service")
        }
        else {
          val serviceObj = serviceDAO.get(x)
          serviceObj match {
            case None =>
              errors += createMessage("entity.error.doesnot.exist", "Service", x)
            case Some(someService) =>
              if (someService.serviceType != ServiceLiterals.kafkaType) {
                errors += createMessage("entity.error.must.one.type.other.given",
                  s"Service for '${StreamLiterals.kafkaStreamType}' stream",
                  ServiceLiterals.kafkaType,
                  someService.serviceType)
              } else {
                if (errors.isEmpty) errors ++= checkStreamPartitionsOnConsistency(someService.asInstanceOf[KafkaServiceDomain])
              }
          }
        }
    }

    //replicationFactor
    if (this.replicationFactor <= 0) {
      errors += createMessage("entity.error.attribute.required", "replicationFactor") + ". " +
        createMessage("entity.error.attribute.must.be.positive.integer", "replicationFactor")
    }


    errors
  }

  override def asModelStream(): KafkaStreamDomain = {
    val serviceDAO = ConnectionRepository.getServiceRepository
    val modelStream = new KafkaStreamDomain(
      this.name,
      serviceDAO.get(this.service).get.asInstanceOf[KafkaServiceDomain],
      this.partitions,
      this.replicationFactor,
      this.description,
      this.force,
      this.tags
    )

    modelStream
  }

  private def checkStreamPartitionsOnConsistency(service: KafkaServiceDomain): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val zkUtils = createZkUtils()
    val topicMetadata = AdminUtils.fetchTopicMetadataFromZk(this.name, zkUtils)
    if (!topicMetadata.partitionMetadata().isEmpty && topicMetadata.partitionMetadata().size != this.partitions) {
      errors += createMessage("entity.error.mismatch.partitions", this.name, s"${this.partitions}", s"${topicMetadata.partitionMetadata().size}")
    }

    errors
  }

  override def create(): Unit = {
    Try {
      val zkUtils = createZkUtils()
      if (doesStreamHaveForcedCreation(zkUtils)) {
        deleteTopic(zkUtils)
        createTopic(zkUtils)
      } else {
        if (!doesTopicExist(zkUtils)) createTopic(zkUtils)
      }
    } match {
      case Success(_) =>
      case Failure(_: TopicAlreadyMarkedForDeletionException) =>
        throw new Exception(s"Cannot delete a kafka topic '${this.name}'. Topic is already marked for deletion. It means that kafka doesn't support deletion")
      case Failure(_: TopicExistsException) =>
        throw new Exception(s"Cannot create a kafka topic '${this.name}'. Topic is marked for deletion. It means that kafka doesn't support deletion")
      case Failure(e) => throw e
    }
  }

  private def createZkUtils(): ZkUtils = {
    val serviceDAO = ConnectionRepository.getServiceRepository
    val service = serviceDAO.get(this.service).get.asInstanceOf[KafkaServiceDomain]
    val zkHost = service.zkProvider.hosts
    val zkConnect = new ZkConnection(zkHost.mkString(";"))
    val zkTimeout = ConfigurationSettingsUtils.getZkSessionTimeout()
    val zkClient = ZkUtils.createZkClient(zkHost.mkString(";"), zkTimeout, zkTimeout)
    val zkUtils = new ZkUtils(zkClient, zkConnect, false)

    zkUtils
  }

  private def doesStreamHaveForcedCreation(zkUtils: ZkUtils): Boolean = {
    doesTopicExist(zkUtils) && this.force
  }

  private def doesTopicExist(zkUtils: ZkUtils): Boolean = {
    AdminUtils.topicExists(zkUtils, this.name)
  }

  private def deleteTopic(zkUtils: ZkUtils): Unit = AdminUtils.deleteTopic(zkUtils, this.name)


  private def createTopic(zkUtils: ZkUtils): Unit = {
    AdminUtils.createTopic(zkUtils, this.name, this.partitions, this.replicationFactor, new Properties())
  }
}
