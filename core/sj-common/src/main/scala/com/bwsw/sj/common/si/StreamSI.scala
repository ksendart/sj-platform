package com.bwsw.sj.common.si

import com.bwsw.sj.common.dal.model.instance.{InputInstanceDomain, InstanceDomain}
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.stream.{SjStream, CreateStream}
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.MessageResourceUtils
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable

/**
  * Provides methods to access [[SjStream]]s in [[GenericMongoRepository]]
  */
class StreamSI(implicit injector: Injector) extends ServiceInterface[SjStream, StreamDomain] {
  private val messageResourceUtils = inject[MessageResourceUtils]

  import messageResourceUtils.createMessage

  private val connectionRepository = inject[ConnectionRepository]

  override protected val entityRepository: GenericMongoRepository[StreamDomain] = connectionRepository.getStreamRepository

  private val instanceRepository = connectionRepository.getInstanceRepository
  private val createStream = inject[CreateStream]

  override def create(entity: SjStream): CreationResult = {
    val errors = entity.validate()

    if (errors.isEmpty) {
      entity.create()
      entityRepository.save(entity.to())

      Created
    } else {
      NotCreated(errors)
    }
  }

  override def get(name: String): Option[SjStream] =
    entityRepository.get(name).map(createStream.from)

  override def getAll(): mutable.Buffer[SjStream] =
    entityRepository.getAll.map(createStream.from)

  override def delete(name: String): DeletionResult = {
    if (hasRelatedInstances(name))
      DeletionError(createMessage("rest.streams.stream.cannot.delete", name))
    else entityRepository.get(name) match {
      case Some(entity) =>
        createStream.from(entity).delete()
        entityRepository.delete(name)

        Deleted
      case None =>
        EntityNotFound
    }
  }

  /**
    * Returns [[com.bwsw.sj.common.dal.model.instance.InstanceDomain InstanceDomain]]s related with [[SjStream]]
    *
    * @param name name of stream
    * @return Some(instances) if stream exists, None otherwise
    */
  def getRelated(name: String): Option[mutable.Buffer[String]] =
    entityRepository.get(name).map(_ => getRelatedInstances(name))

  private def getRelatedInstances(streamName: String): mutable.Buffer[String] =
    instanceRepository.getAll.filter(related(streamName)).map(_.name)

  private def related(streamName: String)(instance: InstanceDomain): Boolean = {
    instance match {
      case (_: InputInstanceDomain) =>
        instance.outputs.contains(streamName)
      case _ =>
        instance.outputs.contains(streamName) || instance.getInputsWithoutStreamMode.contains(streamName)
    }
  }

  private def hasRelatedInstances(streamName: String): Boolean =
    instanceRepository.getAll.exists(related(streamName))
}
