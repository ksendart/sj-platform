package com.bwsw.sj.engine.windowed.module.checkers

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.core.entities.{KafkaEnvelope, TStreamEnvelope, Batch}
import com.bwsw.sj.engine.windowed.module.DataFactory._
import scala.collection.JavaConverters._

object ModuleStatelessChecker extends App {
  open()
  val streamService = ConnectionRepository.getStreamService
  val objectSerializer = new ObjectSerializer()

  val inputTstreamConsumers = (1 to inputCount).map(x => createInputTstreamConsumer(streamService, x.toString))
  val inputKafkaConsumer = createInputKafkaConsumer(streamService, partitions)
  val outputConsumers = (1 to outputCount).map(x => createOutputConsumer(streamService, x.toString))

  inputTstreamConsumers.foreach(x => x.start())
  outputConsumers.foreach(x => x.start())

  var totalInputElements = 0
  var totalOutputElements = 0

  var inputElements = scala.collection.mutable.ArrayBuffer[Int]()
  var outputElements = scala.collection.mutable.ArrayBuffer[Int]()

  inputTstreamConsumers.foreach(inputTstreamConsumer => {
    val partitions = inputTstreamConsumer.getPartitions().toIterator

    while (partitions.hasNext) {
      val currentPartition = partitions.next()
      var maybeTxn = inputTstreamConsumer.getTransaction(currentPartition)
      while (maybeTxn.isDefined) {
        val transaction = maybeTxn.get
        while (transaction.hasNext()) {
          val element = objectSerializer.deserialize(transaction.next()).asInstanceOf[Int]
          inputElements.+=(element)
          totalInputElements += 1
        }
        maybeTxn = inputTstreamConsumer.getTransaction(currentPartition)
      }
    }
  })

  var records = inputKafkaConsumer.poll(100 * 60)
  records.asScala.foreach(x => {
    val bytes = x.value()
    val element = objectSerializer.deserialize(bytes).asInstanceOf[Int]
    inputElements.+=(element)
    totalInputElements += 1
  })

  outputConsumers.foreach(outputConsumer => {
    val partitions = outputConsumer.getPartitions().toIterator

    while (partitions.hasNext) {
      val currentPartition = partitions.next()
      var maybeTxn = outputConsumer.getTransaction(currentPartition)

      while (maybeTxn.isDefined) {
        val transaction = maybeTxn.get
        while (transaction.hasNext()) {
          val batch = objectSerializer.deserialize(transaction.next()).asInstanceOf[Batch]
          batch.envelopes.foreach {
            case tstreamEnvelope: TStreamEnvelope[Array[Byte] @unchecked] => tstreamEnvelope.data.foreach(x => {
              outputElements.+=(objectSerializer.deserialize(x).asInstanceOf[Int])
              totalOutputElements += 1
            })
            case kafkaEnvelope: KafkaEnvelope[Array[Byte] @unchecked] =>
              outputElements.+=(objectSerializer.deserialize(kafkaEnvelope.data).asInstanceOf[Int])
              totalOutputElements += 1
          }
        }
        maybeTxn = outputConsumer.getTransaction(currentPartition)
      }
    }
  })

  assert(totalInputElements == totalOutputElements,
    "Count of all txns elements that are consumed from output stream should equals count of all txns elements that are consumed from input stream")

  assert(inputElements.forall(x => outputElements.contains(x)) && outputElements.forall(x => inputElements.contains(x)),
    "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

  inputTstreamConsumers.foreach(x => x.stop())
  outputConsumers.foreach(x => x.stop())
  close()
  ConnectionRepository.close()

  println("DONE")
}
