package com.bwsw.sj.engine.input.connection.tcp.server

import java.io.Closeable
import java.util.concurrent.{ArrayBlockingQueue, Callable}

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.{ByteBuf, PooledByteBufAllocator}
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import org.slf4j.LoggerFactory

import scala.collection.concurrent
import scala.util.{Failure, Success, Try}


/**
  * Input streaming server that sets up a server listening the specific host and port.
  * Bind and start to accept incoming connections.
  * Than wait until the server socket is closed gracefully shut down the server.
  *
  * @param host                 host of server
  * @param port                 port of server
  * @param channelContextQueue  queue for keeping a channel context [[ChannelHandlerContext]] to process messages ([[ByteBuf]]) in their turn
  * @param bufferForEachContext map for keeping a buffer containing incoming bytes [[ByteBuf]] with the appropriate channel context [[ChannelHandlerContext]]
  */
class InputStreamingServer(host: String,
                           port: Int,
                           channelContextQueue: ArrayBlockingQueue[ChannelHandlerContext],
                           bufferForEachContext: concurrent.Map[ChannelHandlerContext, ByteBuf]) extends Callable[Unit] with Closeable {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val bossGroup: EventLoopGroup = new NioEventLoopGroup(1)
  private val workerGroup = new NioEventLoopGroup()

  override def call(): Unit = {
    logger.info(s"Launch an input-streaming server on: '$host:$port'.")

    val result = Try {
      val bootstrapServer = new ServerBootstrap()
      bootstrapServer.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      bootstrapServer.group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(new InputStreamingChannelInitializer(channelContextQueue, bufferForEachContext))

      bootstrapServer.bind(host, port).sync().channel().closeFuture().sync()
    }

    result match {
      case Success(_) =>
      case Failure(e) =>
        close()
        throw e
    }
  }

  override def close(): Unit = {
    logger.info(s"Shutdown an input-streaming server (address: '$host:$port').")
    workerGroup.shutdownGracefully()
    bossGroup.shutdownGracefully()
  }
}