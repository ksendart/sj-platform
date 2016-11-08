package com.bwsw.sj.engine.windowed.task.reporting

import java.util.Calendar

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.engine.core.entities._
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * Class represents a set of metrics that characterize performance of a windowed streaming module
 *
 * @author Kseniya Mikhaleva
 */

class WindowedStreamingPerformanceMetrics(manager: CommonTaskManager)
  extends PerformanceMetrics(manager) {

  currentThread.setName(s"windowed-task-${manager.taskName}-performance-metrics")
  private var totalIdleTime = 0L
  private val windowedInstance = manager.instance.asInstanceOf[WindowedInstance]
  private val inputStreamNames = manager.inputs.map(_._1.name).toArray
  private val outputStreamNames = windowedInstance.outputs

  override protected val inputEnvelopesPerStream = createStorageForInputEnvelopes(inputStreamNames)
  override protected val outputEnvelopesPerStream = createStorageForOutputEnvelopes(outputStreamNames)

  private val batchesPerStream = createStorageForBatches(inputStreamNames)
  private val windowsPerStream = createStorageForWindows(inputStreamNames)

  /**
   * Increases time when there are no messages (envelopes)
   * @param idle How long waiting a new envelope was
   */
  def increaseTotalIdleTime(idle: Long) = {
    mutex.lock()
    totalIdleTime += idle
    mutex.unlock()
  }

  /**
   * Constructs a report of performance metrics of task's work
   * @return Constructed performance report
   */
  override def getReport(): String = {
    logger.info(s"Start preparing a report of performance for task: ${manager.taskName} of windowed module\n")
    mutex.lock()
    val numberOfInputEnvelopesPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.size))
    val numberOfOutputEnvelopesPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.size))
    val numberOfInputElementsPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.size).sum))
    val numberOfOutputElementsPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.size).sum))
    val bytesOfInputEnvelopesPerStream = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.sum).sum))
    val bytesOfOutputEnvelopesPerStream = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.sum).sum))
    val inputEnvelopesTotalNumber = numberOfInputEnvelopesPerStream.values.sum
    val inputElementsTotalNumber = numberOfInputElementsPerStream.values.sum
    val outputEnvelopesTotalNumber = numberOfOutputEnvelopesPerStream.values.sum
    val outputElementsTotalNumber = numberOfOutputElementsPerStream.values.sum
    val inputEnvelopesSize = inputEnvelopesPerStream.flatMap(x => x._2.map(_.size))
    val outputEnvelopesSize = outputEnvelopesPerStream.flatMap(x => x._2.map(_._2.size))
    val numberOfBatchesPerStream = batchesPerStream.map(x => (x._1, x._2.size))
    val batchesElementsTotalNumber = batchesPerStream.map(x => x._2.sum).sum
    val batchesTotalNumber = numberOfBatchesPerStream.values.sum

    report.pmDatetime = Calendar.getInstance().getTime
    report.totalIdleTime = totalIdleTime
    report.totalInputEnvelopes = inputEnvelopesTotalNumber
    report.inputEnvelopesPerStream = numberOfInputEnvelopesPerStream.toMap
    report.totalInputElements = inputElementsTotalNumber
    report.inputElementsPerStream = numberOfInputElementsPerStream.toMap
    report.totalInputBytes = bytesOfInputEnvelopesPerStream.values.sum
    report.inputBytesPerStream = bytesOfInputEnvelopesPerStream.toMap
    report.averageSizeInputEnvelope = if (inputElementsTotalNumber != 0) inputElementsTotalNumber / inputEnvelopesTotalNumber else 0
    report.maxSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0
    report.minSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0
    report.averageSizeInputElement = if (inputElementsTotalNumber != 0) bytesOfInputEnvelopesPerStream.values.sum / inputElementsTotalNumber else 0
    report.totalOutputEnvelopes = outputEnvelopesTotalNumber
    report.totalOutputElements = outputElementsTotalNumber
    report.outputEnvelopesPerStream = numberOfOutputEnvelopesPerStream.toMap
    report.outputElementsPerStream = numberOfOutputElementsPerStream.toMap
    report.outputBytesPerStream = bytesOfOutputEnvelopesPerStream.toMap
    report.totalOutputBytes = bytesOfOutputEnvelopesPerStream.values.sum
    report.averageSizeOutputEnvelope = if (outputEnvelopesTotalNumber != 0) outputElementsTotalNumber / outputEnvelopesTotalNumber else 0
    report.maxSizeOutputEnvelope = if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.max else 0
    report.minSizeOutputEnvelope = if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.min else 0
    report.averageSizeOutputElement = if (outputEnvelopesTotalNumber != 0) bytesOfOutputEnvelopesPerStream.values.sum / outputElementsTotalNumber else 0
    report.window = windowedInstance.window
    report.batchFillType = windowedInstance.batchFillType
    report.batchesPerStream = numberOfBatchesPerStream.toMap
    report.averageSizeBatchPerStream = batchesPerStream.map(x => (x._1, average(x._2))).toMap
    report.totalBatches = batchesTotalNumber
    report.averageSizeBatch = if (batchesTotalNumber != 0) batchesElementsTotalNumber / batchesTotalNumber else 0
    report.maxSizeBatch = if (batchesTotalNumber != 0) batchesPerStream.map(x => x._2.max).max else 0
    report.minSizeBatch = if (batchesTotalNumber != 0) batchesPerStream.map(x => x._2.min).min else 0
    report.windowsPerStream = windowsPerStream.toMap
    report.uptime = (System.currentTimeMillis() - startTime) / 1000

    clear()

    mutex.unlock()
    reportSerializer.serialize(report)
  }

  override def clear() = {
    logger.debug(s"Reset variables for performance report for next reporting\n")
    inputEnvelopesPerStream.clear()
    outputEnvelopesPerStream.clear()
    batchesPerStream.clear()
    windowsPerStream.clear()
    totalIdleTime = 0L
  }

  def addBatch(batch: Batch) = {
    batchesPerStream(batch.stream) += batch.envelopes.size
  }

  def addWindow(window: Window) = {
    windowsPerStream(window.stream) += 1
  }

  private def createStorageForBatches(inputStreamNames: Array[String]) = {
    mutable.Map(inputStreamNames.map(x => (x, mutable.ListBuffer[Int]())): _*)
  }

  private def createStorageForWindows(inputStreamNames: Array[String]) = {
    mutable.Map(inputStreamNames.map(x => (x, 0)): _*)
  }

  private def average(elements: ListBuffer[Int]) = {
    val totalBatches = elements.size
    val sum = elements.sum
    if (totalBatches != 0) sum / totalBatches else 0
  }

}