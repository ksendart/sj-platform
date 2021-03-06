package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.{KafkaService, KafkaSjStream, TStreamService}
import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, RegularInstanceMetadata, SpecificationData}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.bwsw.sj.common.utils.StreamLiterals._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer

/**
 * Validator for Stream-processing regular module type
 *
 * @author Kseniya Tomskikh
 */
class RegularStreamingValidator extends StreamingModuleValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)

  override def validate(parameters: InstanceMetadata, specification: SpecificationData) = {
    logger.debug(s"Instance: ${parameters.name}. Start regular-streaming validation.")
    val errors = new ArrayBuffer[String]()
    errors ++= super.validateGeneralOptions(parameters)
    val regularInstanceMetadata = parameters.asInstanceOf[RegularInstanceMetadata]

    // 'checkpoint-mode' field
    Option(regularInstanceMetadata.checkpointMode) match {
      case None =>
        errors += s"'Checkpoint-mode' is required"
      case Some(x) =>
        if (x.isEmpty) {
          errors += s"'Checkpoint-mode' is required"
        }
        else {
          if (!checkpointModes.contains(x)) {
            errors += s"Unknown value of 'checkpoint-mode' attribute: '$x'. " +
              s"'Checkpoint-mode' must be one of: ${checkpointModes.mkString("[", ", ", "]")}"
          }
        }
    }

    // 'event-wait-time' field
    if (regularInstanceMetadata.eventWaitTime <= 0) {
      errors += s"'Event-wait-time' attribute must be greater than zero"
    }

    // 'state-management' field
    if (!stateManagementModes.contains(regularInstanceMetadata.stateManagement)) {
      errors += s"Unknown value of 'state-management' attribute: '${regularInstanceMetadata.stateManagement}'. " +
        s"'State-management' must be one of: ${stateManagementModes.mkString("[", ", ", "]")}"
    } else {
      if (regularInstanceMetadata.stateManagement != EngineLiterals.noneStateMode) {
        // 'state-full-checkpoint' field
        if (regularInstanceMetadata.stateFullCheckpoint <= 0) {
          errors += s"'State-full-checkpoint' attribute must be greater than zero"
        }
      }
    }

    errors ++= validateStreamOptions(regularInstanceMetadata, specification)
  }

  private def validateStreamOptions(instance: RegularInstanceMetadata,
                                    specification: SpecificationData) = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    val errors = new ArrayBuffer[String]()

    // 'inputs' field
    val inputModes = instance.inputs.map(i => getStreamMode(i))
    if (inputModes.exists(m => !streamModes.contains(m))) {
      errors += s"Unknown stream mode. Input streams must have one of mode: ${streamModes.mkString("[", ", ", "]")}"
    }
    val inputsCardinality = specification.inputs("cardinality").asInstanceOf[Array[Int]]
    if (instance.inputs.length < inputsCardinality(0)) {
      errors += s"Count of inputs cannot be less than ${inputsCardinality(0)}"
    }
    if (instance.inputs.length > inputsCardinality(1)) {
      errors += s"Count of inputs cannot be more than ${inputsCardinality(1)}"
    }
    if (doesContainDoubles(instance.inputs.toList)) {
      errors += s"Inputs is not unique"
    }
    val inputStreams = getStreams(instance.inputs.toList.map(clearStreamFromMode))
    instance.inputs.toList.map(clearStreamFromMode).foreach { streamName =>
      if (!inputStreams.exists(s => s.name == streamName)) {
        errors += s"Input stream '$streamName' does not exist"
      }
    }
    val inputTypes = specification.inputs("types").asInstanceOf[Array[String]]
    if (inputStreams.exists(s => !inputTypes.contains(s.streamType))) {
      errors += s"Input streams must be one of: ${inputTypes.mkString("[", ", ", "]")}"
    }

    val kafkaStreams = inputStreams.filter(s => s.streamType.equals(kafkaStreamType)).map(_.asInstanceOf[KafkaSjStream])
    if (kafkaStreams.nonEmpty) {
      if (kafkaStreams.exists(s => !s.service.isInstanceOf[KafkaService])) {
        errors += s"Service for kafka streams must be 'KfkQ'"
      }
    }

    // 'start-from' field
    val startFrom = instance.startFrom
    if (inputStreams.exists(s => s.streamType.equals(kafkaStreamType))) {
      if (!startFromModes.contains(startFrom)) {
        errors += s"'Start-from' attribute must be one of: ${startFromModes.mkString("[", ", ", "]")}, if instance inputs have the kafka-streams"
      }
    } else {
      if (!startFromModes.contains(startFrom)) {
        try {
          startFrom.toLong
        } catch {
          case ex: NumberFormatException =>
            errors += s"'Start-from' attribute is not one of: ${startFromModes.mkString("[", ", ", "]")} or timestamp"
        }
      }
    }

    // 'outputs' field
    val outputsCardinality = specification.outputs("cardinality").asInstanceOf[Array[Int]]
    if (instance.outputs.length < outputsCardinality(0)) {
      errors += s"Count of outputs cannot be less than ${outputsCardinality(0)}."
    }
    if (instance.outputs.length > outputsCardinality(1)) {
      errors += s"Count of outputs cannot be more than ${outputsCardinality(1)}."
    }
    if (doesContainDoubles(instance.outputs.toList)) {
      errors += s"Outputs is not unique"
    }
    val outputStreams = getStreams(instance.outputs.toList)
    instance.outputs.toList.foreach { streamName =>
      if (!outputStreams.exists(s => s.name == streamName)) {
        errors += s"Output stream '$streamName' does not exist"
      }
    }
    val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
    if (outputStreams.exists(s => !outputTypes.contains(s.streamType))) {
      errors += s"Output streams must be one of: ${outputTypes.mkString("[", ", ", "]")}"
    }

    // 'parallelism' field
    val partitions = getStreamsPartitions(inputStreams)
    val minPartitionCount = if (partitions.nonEmpty) partitions.values.min else 0
    errors ++= checkParallelism(instance.parallelism, minPartitionCount)

    val allStreams = inputStreams.union(outputStreams)
    val tStreamsServices = getStreamServices(allStreams.filter { s =>
      s.streamType.equals(tStreamType)
    })
    if (tStreamsServices.size != 1) {
      errors += s"All t-streams should have the same service"
    } else {
      val service = serviceDAO.get(tStreamsServices.head)
      if (!service.get.isInstanceOf[TStreamService]) {
        errors += s"Service for t-streams must be 'TstrQ'"
      }
    }

    errors
  }
}
