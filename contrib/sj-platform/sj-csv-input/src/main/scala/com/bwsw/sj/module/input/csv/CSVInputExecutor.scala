package com.bwsw.sj.module.input.csv

import java.io.IOException

import com.bwsw.sj.common.DAL.model.{KafkaSjStream, SjStream, TStreamSjStream}
import com.bwsw.sj.common.utils.stream_distributor.{ByHash, SjStreamDistributor}
import com.bwsw.sj.common.utils.{AvroUtils, StreamLiterals}
import com.bwsw.sj.engine.core.entities.InputEnvelope
import com.bwsw.sj.engine.core.environment.InputEnvironmentManager
import com.bwsw.sj.engine.core.input.utils.SeparateTokenizer
import com.bwsw.sj.engine.core.input.{InputStreamingExecutor, Interval}
import com.opencsv.CSVParserBuilder
import io.netty.buffer.ByteBuf
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData.Record

import scala.io.Source

/**
  * Executor for work with csv.
  *
  * @author Pavel Tomskikh
  */
class CSVInputExecutor(manager: InputEnvironmentManager) extends InputStreamingExecutor[Record](manager) {

  val outputStream: String = manager.options(CSVInputOptionNames.outputStream).asInstanceOf[String]
  val fallbackStream: String = manager.options(CSVInputOptionNames.fallbackStream).asInstanceOf[String]
  val fieldSeparator: Option[Char] = manager.options.get(CSVInputOptionNames.fieldSeparator).asInstanceOf[Option[String]].map(_.head)
  val quoteSymbol: Option[Char] = manager.options.get(CSVInputOptionNames.quoteSymbol).asInstanceOf[Option[String]].map(_.head)
  val encoding: String = manager.options(CSVInputOptionNames.encoding).asInstanceOf[String]
  val lineSeparator: String = manager.options(CSVInputOptionNames.lineSeparator).asInstanceOf[String]
  val tokenizer = new SeparateTokenizer(lineSeparator, encoding)

  val fields: Seq[String] = manager.options(CSVInputOptionNames.fields).asInstanceOf[Seq[String]]
  val fieldsNumber = fields.length
  val schema = {
    var scheme = SchemaBuilder.record("csv").fields()
    fields.foreach { field =>
      scheme = scheme.name(field).`type`().stringType().noDefault()
    }
    scheme.endRecord()
  }

  val fallbackFieldName = "data"
  val fallbackSchema = SchemaBuilder.record("fallback").fields()
    .name(fallbackFieldName).`type`().stringType().noDefault().endRecord()

  val uniqueKey = manager.options.get(CSVInputOptionNames.uniqueKey) match {
    case Some(uniqueFields: Seq[Any]) => uniqueFields.map(_.asInstanceOf[String])
    case _ => fields
  }

  val partitionCount = getPartitionCount(manager.outputs.find(_.name == outputStream).get)
  val distribution = manager.options.get(CSVInputOptionNames.distribution).map(_.asInstanceOf[Seq[String]])
  val distributor = {
    if (distribution.isEmpty) new SjStreamDistributor(partitionCount)
    else new SjStreamDistributor(partitionCount, ByHash, distribution.get)
  }

  val fallbackPartitionCount = getPartitionCount(manager.outputs.find(_.name == fallbackStream).get)
  val fallbackDistributor = new SjStreamDistributor(fallbackPartitionCount)

  val csvParser = {
    val csvParserBuilder = new CSVParserBuilder
    fieldSeparator.foreach(csvParserBuilder.withSeparator)
    quoteSymbol.foreach(csvParserBuilder.withQuoteChar)
    csvParserBuilder.build()
  }

  override def tokenize(buffer: ByteBuf): Option[Interval] = tokenizer.tokenize(buffer)

  override def parse(buffer: ByteBuf, interval: Interval): Option[InputEnvelope[Record]] = {
    val length = interval.finalValue - interval.initialValue
    val dataBuffer = buffer.slice(interval.initialValue, length)
    val data = new Array[Byte](length)
    dataBuffer.getBytes(0, data)
    buffer.readerIndex(interval.finalValue + 1)
    val line = Source.fromBytes(data, encoding).mkString
    try {
      val values = csvParser.parseLine(line)

      if (values.length == fieldsNumber) {
        val record = new Record(schema)
        fields.zip(values).foreach { case (field, value) => record.put(field, value) }
        val key = AvroUtils.concatFields(uniqueKey, record)

        Some(new InputEnvelope(
          s"$outputStream$key",
          Array((outputStream, distributor.getNextPartition(record))),
          true,
          record))
      } else {
        buildFallbackEnvelope(line)
      }
    } catch {
      case _: IOException => buildFallbackEnvelope(line)
    }
  }

  private def buildFallbackEnvelope(data: String): Option[InputEnvelope[Record]] = {
    val record = new Record(fallbackSchema)
    record.put(fallbackFieldName, data)
    Some(new InputEnvelope(
      s"$fallbackStream,$data",
      Array((fallbackStream, fallbackDistributor.getNextPartition())),
      false,
      record))
  }

  private def getPartitionCount(sjStream: SjStream) = {
    sjStream match {
      case s: TStreamSjStream => s.partitions
      case s: KafkaSjStream => s.partitions
      case _ => throw new IllegalArgumentException(s"stream type must be ${StreamLiterals.tstreamType} or " +
        s"${StreamLiterals.kafkaStreamType}")
    }
  }
}