package com.bwsw.sj.engine.core.batch

import com.bwsw.sj.common.si.model.instance.BatchInstance
import com.bwsw.sj.engine.core.entities.Window
import org.slf4j.LoggerFactory

import scala.collection.mutable

class WindowRepository(instance: BatchInstance) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val windowPerStream: mutable.Map[String, Window] = createStorageOfWindows()
  val window = instance.window
  val slidingInterval = instance.slidingInterval

  private def createStorageOfWindows(): mutable.Map[String, Window] = {
    logger.debug("Create a storage to keep windows.")
    mutable.Map(instance.getInputsWithoutStreamMode.map(x => (x, new Window(x))): _*)
  }

  def get(stream: String): Window = {
    logger.debug(s"Get a window for stream: '$stream'.")
    windowPerStream(stream)
  }

  def put(stream: String, window: Window): Unit = {
    logger.debug(s"Put a window for stream: '$stream'.")
    windowPerStream(stream) = window
  }

  def getAll(): Map[String, Window] = {
    logger.debug(s"Get all windows.")
    Map(windowPerStream.toList: _*)
  }
}
