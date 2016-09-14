package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{SjStream, TStreamSjStream}
import com.bwsw.sj.common.utils.{GeneratorLiterals, StreamLiterals, EngineLiterals}
import org.slf4j.LoggerFactory

/**
  * One-thread deleting object for instance
  * using synchronous apache http client
  *
  *
  * @author Kseniya Tomskikh
  */
class InstanceDestroyer(instance: Instance, delay: Long) extends Runnable with InstanceMarathonManager {
  private val logger = LoggerFactory.getLogger(getClass.getName)

  import EngineLiterals._

  def run() = {
    logger.debug(s"Instance: ${instance.name}. Destroy instance.")
    try {
      updateInstanceStage(instance, instance.name, deleting)
      deleteGenerators(instance)
      deleteInstance(instance)
      instanceDAO.delete(instance.name)
      logger.debug(s"Instance: ${instance.name}. Instance is deleted.")
    } catch {
      case e: Exception =>

    }
  }

  /**
    * Stopping all running generators for streams of instance,
    * if generators is not using any streams of started instances
    *
    * @param instance - Instance for stopping
    * @return - Response from marathon
    */
  def deleteGenerators(instance: Instance) = {
    logger.debug(s"Instance: ${instance.name}. Deleting generators.")
    var instanceStreams = instance.outputs
    if (!instance.moduleType.equals(inputStreamingType)) {
      instanceStreams = instanceStreams.union(instance.inputs.map(_.replaceAll("/split|/full", "")))
    }
    val allStreams = instanceStreams.flatMap(streamDAO.get)
    val startedInstances = instanceDAO.getByParameters(Map("status" -> started))
    val startingInstance = startedInstances.union(instanceDAO.getByParameters(Map("status" -> starting)))
    val tStreamStreamsToStop = allStreams
      .filter { stream: SjStream =>
        if (stream.streamType.equals(StreamLiterals.tStreamType)) {
          val stage = instance.stages.get(stream.name)
          !stream.asInstanceOf[TStreamSjStream].generator.generatorType.equals(GeneratorLiterals.localType) &&
            (stage.state.equals(failed) ||
              !startingInstance.exists { instance =>
                val streamGeneratorName = getGeneratorAppName(stream.asInstanceOf[TStreamSjStream])
                val instanceStreamGenerators = instance.inputs.map(_.replaceAll("/split|/full", ""))
                  .union(instance.outputs)
                  .flatMap(name => streamDAO.get(name))
                  .filter(s => s.streamType.equals(StreamLiterals.tStreamType))
                  .map(sjStream => getGeneratorAppName(sjStream.asInstanceOf[TStreamSjStream]))
                instanceStreamGenerators.contains(streamGeneratorName)
              })
        } else false
    }
    tStreamStreamsToStop.foreach { stream =>
      var isTaskDeleted = false
      updateInstanceStage(instance, stream.name, deleting)
      val taskId = getGeneratorAppName(stream.asInstanceOf[TStreamSjStream])
      val taskInfoResponse = getApplicationInfo(taskId)
      if (taskInfoResponse.getStatusLine.getStatusCode != NotFound) {
        logger.debug(s"Instance: ${instance.name}. Delete generator: $taskId.")
        val stopResponse = destroyMarathonApplication(taskId)
        if (stopResponse.getStatusLine.getStatusCode == OK) {
          while (!isTaskDeleted) {
            val taskInfoResponse = getApplicationInfo(taskId)
            if (taskInfoResponse.getStatusLine.getStatusCode != NotFound) {
              updateInstanceStage(instance, stream.name, deleting)
              Thread.sleep(delay)
            } else {
              updateInstanceStage(instance, stream.name, deleted)
              isTaskDeleted = true
            }
          }
        }
      } else {
        updateInstanceStage(instance, stream.name, deleted)
        isTaskDeleted = true
      }
    }
  }

  /**
    * Destroying application of instance on mesos
    *
    * @param instance - Instance for deleting
    */
  def deleteInstance(instance: Instance) = {
    logger.debug(s"Instance: ${instance.name}. Delete instance application.")
    var isInstanceDeleted = false
    //todo maybe add timeout and retry count?
    while (!isInstanceDeleted) {
      val response = destroyMarathonApplication(instance.name)
      if (response.getStatusLine.getStatusCode == OK) {
        while (!isInstanceDeleted) {
          val taskInfoResponse = getApplicationInfo(instance.name)
          if (taskInfoResponse.getStatusLine.getStatusCode != NotFound) {
            updateInstanceStage(instance, instance.name, deleting)
            Thread.sleep(delay)
          } else {
            instance.status = deleted
            updateInstanceStage(instance, instance.name, deleted)
            isInstanceDeleted = true
          }
        }
      }
    }
  }

}