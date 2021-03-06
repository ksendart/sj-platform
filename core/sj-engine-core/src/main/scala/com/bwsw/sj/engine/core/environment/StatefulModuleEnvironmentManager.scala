package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.DAL.model.SjStream

import com.bwsw.sj.common.utils.SjTimer
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.core.state.StateStorage
import com.bwsw.tstreams.agents.producer.Producer

import scala.collection.mutable
import scala.collection._
/**
 * Class allowing to manage environment of module that has state
 *
 *
 * @author Kseniya Mikhaleva
 * @param stateStorage Storage of state of module
 * @param options User defined options from instance parameters
 * @param producers T-streams producers for each output stream of instance parameters
 * @param outputs Set of output streams of instance parameters that have tags
 * @param outputTags Keeps a tag (partitioned or round-robin output) corresponding to the output for each output stream
 * @param moduleTimer Provides a possibility to set a timer inside a module
 */

class StatefulModuleEnvironmentManager(stateStorage: StateStorage,
                                       options: Map[String, Any],
                                       producers: Map[String, Producer[Array[Byte]]],
                                       outputs: Array[SjStream],
                                       outputTags: mutable.Map[String, (String, RegularModuleOutput)],
                                       moduleTimer: SjTimer,
                                       performanceMetrics: PerformanceMetrics)
  extends RegularEnvironmentManager(options, producers, outputs, outputTags, moduleTimer, performanceMetrics) {
  /**
   * Returns specific state of module
   *
   * @return Module state
   */
  override def getState: StateStorage = {
    logger.info(s"Get a storage where a state is\n")
    stateStorage
  }
}
