package com.bwsw.common.tcp.client

import java.util.concurrent.ArrayBlockingQueue

import com.bwsw.common.LeaderLatch
import com.bwsw.sj.common.utils.GeneratorLiterals
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.ResourceLeakDetector
import io.netty.util.ResourceLeakDetector.Level

/**
 * Simple tcp client for retrieving transaction ID
 *
 * @author Kseniya Tomskikh
 */

class TcpClient(options: TcpClientOptions) {
  private val out = new ArrayBlockingQueue[Long](1)
  private var channel: Channel = null
  private val workerGroup = new NioEventLoopGroup()
  private val bootstrap = new Bootstrap().option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
  private val (host, port) = getMasterAddress()

  createChannel()

  private def getMasterAddress() = {
    val leader = new LeaderLatch(Set(options.zkServers), options.prefix + GeneratorLiterals.masterDirectory)
    val leaderInfo = leader.getLeaderInfo()
    val address = leaderInfo.split(":")
    leader.close()

    (address(0), address(1).toInt)
  }

  private def createChannel() = {
    ResourceLeakDetector.setLevel(Level.ADVANCED)
    bootstrap.group(workerGroup)
      .channel(classOf[NioSocketChannel])
      .handler(new TcpClientChannelInitializer(out))

    channel = bootstrap.connect(host, port).sync().channel()
  }

  def get() = {
    channel.writeAndFlush(GeneratorLiterals.messageForServer)
    val id = out.take()

    id
  }

  def close() = {
    workerGroup.shutdownGracefully()
  }
}