package com.bwsw.sj.engine.output.task.reporting

import java.util.Calendar

import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.output.task.OutputTaskManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Class represents a set of metrics that characterize performance of an output streaming module
  *
  * @param manager allows to manage an environment of output streaming task
  * @author Kseniya Mikhaleva
  */

class OutputStreamingPerformanceMetrics(manager: OutputTaskManager)
  extends PerformanceMetrics(manager) {

  currentThread.setName(s"output-task-${manager.taskName}-performance-metrics")
  private val inputStreamNames: Array[String] = instance.getInputsWithoutStreamMode()
  private val outputStreamNames: Array[String] = instance.outputs

  override protected var inputEnvelopesPerStream: mutable.Map[String, ListBuffer[List[Int]]] = createStorageForInputEnvelopes(inputStreamNames)
  override protected var outputEnvelopesPerStream: mutable.Map[String, mutable.Map[String, ListBuffer[Int]]] = createStorageForOutputEnvelopes(outputStreamNames)

  /**
    * Constructs a report of performance metrics of task work (one module could have multiple tasks)
    */
  override def getReport(): String = {
    logger.info(s"Start preparing a report of performance for task: $taskName of output module.")
    mutex.lock()
    val bytesOfInputEnvelopes = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.sum).sum)).head._2
    val bytesOfOutputEnvelopes = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.sum).sum)).head._2
    val inputEnvelopesTotalNumber = inputEnvelopesPerStream.map(x => (x._1, x._2.size)).head._2
    val inputElementsTotalNumber = inputEnvelopesPerStream.map(x => (x._1, x._2.map(_.size).sum)).head._2
    val outputEnvelopesTotalNumber = outputEnvelopesPerStream.map(x => (x._1, x._2.size)).head._2
    val outputElementsTotalNumber = outputEnvelopesPerStream.map(x => (x._1, x._2.map(_._2.size).sum)).head._2
    val inputEnvelopesSize = inputEnvelopesPerStream.flatMap(x => x._2.map(_.size))
    val outputEnvelopesSize = outputEnvelopesPerStream.flatMap(x => x._2.map(_._2.size))

    report.pmDatetime = Calendar.getInstance().getTime
    report.inputStreamName = inputStreamNames.head
    report.totalInputEnvelopes = inputEnvelopesTotalNumber
    report.totalInputElements = inputElementsTotalNumber
    report.totalInputBytes = bytesOfInputEnvelopes
    report.averageSizeInputEnvelope = if (inputElementsTotalNumber != 0) inputElementsTotalNumber / inputEnvelopesTotalNumber else 0
    report.maxSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.max else 0
    report.minSizeInputEnvelope = if (inputEnvelopesSize.nonEmpty) inputEnvelopesSize.min else 0
    report.averageSizeInputElement = if (inputElementsTotalNumber != 0) bytesOfInputEnvelopes / inputElementsTotalNumber else 0
    report.outputStreamName = outputStreamNames.head
    report.totalOutputEnvelopes = outputEnvelopesTotalNumber
    report.totalOutputElements = outputElementsTotalNumber
    report.totalOutputBytes = bytesOfOutputEnvelopes
    report.averageSizeOutputEnvelope = if (outputEnvelopesTotalNumber != 0) outputElementsTotalNumber / outputEnvelopesTotalNumber else 0
    report.maxSizeOutputEnvelope = if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.max else 0
    report.minSizeOutputEnvelope = if (outputEnvelopesSize.nonEmpty) outputEnvelopesSize.min else 0
    report.averageSizeOutputElement = if (outputEnvelopesTotalNumber != 0) bytesOfOutputEnvelopes / outputElementsTotalNumber else 0
    report.uptime = (System.currentTimeMillis() - startTime) / 1000

    clear()

    mutex.unlock()

    reportSerializer.serialize(report)
  }

  override def clear(): Unit = {
    logger.debug(s"Reset variables for performance report for next reporting.")
    inputEnvelopesPerStream = mutable.Map(inputStreamNames.head -> mutable.ListBuffer[List[Int]]())
    outputEnvelopesPerStream = mutable.Map(outputStreamNames.head -> mutable.Map[String, mutable.ListBuffer[Int]]())
  }
}