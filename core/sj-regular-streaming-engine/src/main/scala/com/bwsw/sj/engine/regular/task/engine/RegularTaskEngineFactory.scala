package com.bwsw.sj.engine.regular.task.engine

import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.engine.core.engine.{PersistentBlockingQueue, NumericalCheckpointTaskEngine, TimeCheckpointTaskEngine}
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.sj.engine.regular.task.reporting.RegularStreamingPerformanceMetrics
import org.slf4j.LoggerFactory

/**
 * Factory is in charge of creating of a task engine of regular module
 *
 *
 * @param manager Manager of environment of task of regular module
 * @param performanceMetrics Set of metrics that characterize performance of a regular streaming module
 * @param blockingQueue Blocking queue for keeping incoming envelopes that are serialized into a string,
 *                      which will be retrieved into a module

 * @author Kseniya Mikhaleva
 */

class RegularTaskEngineFactory(manager: RegularTaskManager,
                               performanceMetrics: RegularStreamingPerformanceMetrics,
                               blockingQueue: PersistentBlockingQueue) {

  protected val logger = LoggerFactory.getLogger(this.getClass)

  /**
   * Creates RegularTaskEngine is in charge of a basic execution logic of task of regular module
   * @return Engine of regular task
   */
  def createRegularTaskEngine(): RegularTaskEngine = {
    manager.instance.checkpointMode match {
      case EngineLiterals.timeIntervalCheckpointMode =>
        logger.info(s"Task: ${manager.taskName}. Regular module has a '${EngineLiterals.timeIntervalCheckpointMode}' checkpoint mode, create an appropriate task engine\n")
        new RegularTaskEngine(manager, performanceMetrics, blockingQueue) with TimeCheckpointTaskEngine
      case EngineLiterals.everyNthCheckpointMode =>
        logger.info(s"Task: ${manager.taskName}. Regular module has an '${EngineLiterals.everyNthCheckpointMode}' checkpoint mode, create an appropriate task engine\n")
        new RegularTaskEngine(manager, performanceMetrics, blockingQueue) with NumericalCheckpointTaskEngine

    }
  }
}
