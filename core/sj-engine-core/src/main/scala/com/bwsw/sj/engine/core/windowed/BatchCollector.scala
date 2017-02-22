package com.bwsw.sj.engine.core.windowed

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.reporting.WindowedStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

import scala.collection.Map

/**
  * Provides methods are responsible for a basic execution logic of task of windowed module
  *
  * @param performanceMetrics Set of metrics that characterize performance of a windowed streaming module
  * @author Kseniya Mikhaleva
  */
abstract class BatchCollector(protected val instance: WindowedInstance,
                              performanceMetrics: WindowedStreamingPerformanceMetrics) {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val streamDAO = ConnectionRepository.getStreamService
  private val inputs = instance.getInputsWithoutStreamMode().map(x => streamDAO.get(x).get)
  private val currentBatchPerStream: Map[String, Batch] = createStorageOfBatches()

  private def createStorageOfBatches() = {
    inputs.map(x => (x.name, new Batch(x.name, x.tags))).toMap
  }

  def onReceive(envelope: Envelope): Unit = {
    logger.debug(s"Invoke onReceive() handler.")
    registerEnvelope(envelope)
    afterReceivingEnvelope(envelope)
  }

  private def registerEnvelope(envelope: Envelope) = {
    logger.debug(s"Register an envelope: ${envelope.toString}.")
    currentBatchPerStream(envelope.stream).envelopes += envelope
    performanceMetrics.addEnvelopeToInputStream(envelope)
  }

  def collectBatch(streamName: String) = {
    logger.info(s"It's time to collect batch (stream: $streamName)\n")
    val batch = currentBatchPerStream(streamName).copy()
    currentBatchPerStream(streamName).envelopes.clear()
    prepareForNextCollecting(streamName)

    batch
  }

  protected def afterReceivingEnvelope(envelope: Envelope)

  def getBatchesToCollect(): Seq[String]

  protected def prepareForNextCollecting(streamName: String)
}





