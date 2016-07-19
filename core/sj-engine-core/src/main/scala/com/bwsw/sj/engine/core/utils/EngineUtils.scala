package com.bwsw.sj.engine.core.utils

import java.util.Date

import com.bwsw.common.tstream.NetworkTimeUUIDGenerator
import com.bwsw.sj.common.ConfigConstants
import com.bwsw.sj.common.DAL.model.{TStreamSjStream, ZKService}
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.tstreams.agents.consumer.Offsets.{DateTime, IOffset, Newest, Oldest}
import com.bwsw.tstreams.generator.{IUUIDGenerator, LocalTimeUUIDGenerator}

/**
  * Created: 27/05/2016
  *
  * @author Kseniya Tomskikh
  */
object EngineUtils {

  private val configService = ConnectionRepository.getConfigService
  private val retryPeriod = configService.get(ConfigConstants.tgClientRetryPeriodTag).value.toInt
  private val retryCount = configService.get(ConfigConstants.tgRetryCountTag).value.toInt

  /**
    * Chooses offset policy for t-streams consumers
    *
    * @param startFrom Offset policy name or specific date
    * @return Offset
    */
  def chooseOffset(startFrom: String): IOffset = {
    startFrom match {
      case "oldest" => Oldest
      case "newest" => Newest
      case time => DateTime(new Date(time.toLong * 1000))
    }
  }

  /**
    * Creating UUID generator for t-stream
    *
    * @param stream T-stream object
    * @return UUID generator
    */
  def getUUIDGenerator(stream: TStreamSjStream) : IUUIDGenerator = {
    stream.generator.generatorType match {
      case "local" => new LocalTimeUUIDGenerator
      case generatorType =>
        val service = stream.generator.service.asInstanceOf[ZKService]
        val zkHosts = service.provider.hosts
        var prefix = service.namespace
        if (stream.generator.generatorType.equals("per-stream")) {
          prefix += s"/${stream.name}"
        } else {
          prefix += "/global"
        }
        new NetworkTimeUUIDGenerator(zkHosts, prefix, retryPeriod, retryCount)
    }
  }

  def portIsOpen(address:String, port:Int): Boolean = {
    var closed = true
    val socket = new java.net.Socket()
    try {
      socket.setReuseAddress(true)
      socket.connect(new java.net.InetSocketAddress(address, port), 5)
      socket.close()
      closed = false
    } catch {
      case e:Exception =>
    }
    !closed
  }

}