package com.bwsw.sj.crud.rest.validator.module

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.model.{SjStream, TStreamService, TStreamSjStream}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.module.{InstanceMetadata, SpecificationData, OutputInstanceMetadata}
import com.bwsw.sj.common.utils.EngineConstants._
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer

/**
 *
 *
 * @author Kseniya Tomskikh
 */
class OutputStreamingValidator extends StreamingModuleValidator {

  private val logger: Logger = LoggerFactory.getLogger(getClass.getName)
  private val streamsDAO = ConnectionRepository.getStreamService

  private def getStream(streamName: String) = {
    streamsDAO.get(streamName)
  }

  /**
   * Validating input parameters for 'output-streaming' module
   *
   * @param parameters - input parameters for running module
   * @param specification - specification of module
   * @return - List of errors
   */
  override def validate(parameters: InstanceMetadata, specification: SpecificationData) = {
    logger.debug(s"Instance: ${parameters.name}. Start output-streaming validation.")
    val errors = super.validateGeneralOptions(parameters)
    val outputInstanceMetadata = parameters.asInstanceOf[OutputInstanceMetadata]

    Option(parameters.checkpointMode) match {
      case None =>
        errors += s"'Checkpoint-mode' is required"
      case Some(x) =>
        if (!x.equals("every-nth")) {
          errors += s"Unknown value of 'checkpoint-mode' attribute: '$x'. " +
            s"'Checkpoint-mode' attribute for output-streaming module must be only 'every-nth'"
        }
    }

    validateStreamOptions(outputInstanceMetadata, specification, errors)
  }

  /**
   * Validating options of streams of instance for module
   *
   * @param instance - Input instance parameters
   * @param specification - Specification of module
   * @param errors - List of validating errors
   * @return - List of errors and validating instance (null, if errors non empty)
   */
  def validateStreamOptions(instance: OutputInstanceMetadata,
                            specification: SpecificationData,
                            errors: ArrayBuffer[String]) = {
    logger.debug(s"Instance: ${instance.name}. Stream options validation.")
    
    // 'inputs' field
    var inputStream: Option[SjStream] = None
    if (instance.input != null) {
      val inputMode: String = getStreamMode(instance.input)
      if (!inputMode.equals("split")) {
        errors += s"Unknown value of 'stream-mode' attribute. Input stream must have the mode 'split'"
      }

      val inputStreamName = instance.input.replaceAll("/split", "")
      inputStream = getStream(inputStreamName)
      inputStream match {
        case None =>
          errors += s"Input stream '$inputStreamName' does not exist"
        case Some(stream) =>
          val inputTypes = specification.inputs("types").asInstanceOf[Array[String]]
          if (!inputTypes.contains(stream.streamType)) {
            errors += s"Input stream must be one of: ${inputTypes.mkString("[", ", ", "]")}"
          }
      }
    } else {
      errors += s"'Input' attribute is required"
    }

    if (instance.output != null) {
      val outputStream = getStream(instance.output)
      outputStream match {
        case None =>
          errors += s"Output stream '${instance.output}' does not exist"
        case Some(stream) =>
          val outputTypes = specification.outputs("types").asInstanceOf[Array[String]]
          if (!outputTypes.contains(stream.streamType)) {
            errors += s"Output streams must be one of: ${outputTypes.mkString("[", ", ", "]")}"
          }
      }
    } else {
      errors += s"'Output' attribute is required"
    }

    // 'start-from' field
    val startFrom = instance.startFrom
    if (!startFromModes.contains(startFrom)) {
      try {
        startFrom.toLong
      } catch {
        case ex: NumberFormatException =>
          errors += s"'Start-from' attribute is not one of: ${startFromModes.mkString("[", ", ", "]")} or timestamp"
      }
    }

    var validatedInstance: Option[Instance] = None
    if (errors.isEmpty) {
      val input = inputStream.get.asInstanceOf[TStreamSjStream]

      val service = input.service
      if (!service.isInstanceOf[TStreamService]) {
        errors += s"Service for t-streams must be 'TstrQ'"
      } else {
        checkTStreams(errors, ArrayBuffer(input))
      }

      instance.parallelism = checkParallelism(instance.parallelism, input.partitions, errors)

      val partitions = getStreamsPartitions(Array(input))
      validatedInstance = createInstance(instance, partitions, Set(input))
    }

    (errors, validatedInstance)
  }
}
