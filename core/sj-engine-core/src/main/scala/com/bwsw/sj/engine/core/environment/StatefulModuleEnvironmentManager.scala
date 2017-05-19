package com.bwsw.sj.engine.core.environment

import com.bwsw.sj.common.dal.model.instance.InstanceDomain
import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.utils.{EngineLiterals, SjTimer}
import com.bwsw.sj.engine.core.entities.{KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.reporting.PerformanceMetrics
import com.bwsw.sj.engine.core.state.StateStorage
import com.bwsw.tstreams.agents.producer.Producer

import scala.collection.mutable
import scala.collection._

/**
  * Class allowing to manage environment of module that has got a state
  *
  * @param stateStorage           storage of state of module [[StateStorage]]
  * @param producers              t-streams producers for each output stream from instance [[InstanceDomain.outputs]]
  * @param options                user defined options from instance [[InstanceDomain.options]]
  * @param outputs                set of output streams [[StreamDomain]] from instance [[InstanceDomain.outputs]]
  * @param producerPolicyByOutput keeps a tag (partitioned or round-robin output) corresponding to the output for each output stream
  * @param moduleTimer            provides a possibility to set a timer inside a module
  * @param performanceMetrics     set of metrics that characterize performance of [[EngineLiterals.regularStreamingType]] or [[EngineLiterals.batchStreamingType]] module
  * @param classLoader            it is needed for loading some custom classes from module jar to serialize/deserialize envelope data
  *                               (ref. [[TStreamEnvelope.data]] or [[KafkaEnvelope.data]])
  * @author Kseniya Mikhaleva
  */

class StatefulModuleEnvironmentManager(stateStorage: StateStorage,
                                       options: String,
                                       producers: Map[String, Producer],
                                       outputs: Array[StreamDomain],
                                       producerPolicyByOutput: mutable.Map[String, (String, ModuleOutput)],
                                       moduleTimer: SjTimer,
                                       performanceMetrics: PerformanceMetrics,
                                       classLoader: ClassLoader)
  extends ModuleEnvironmentManager(options, producers, outputs, producerPolicyByOutput, moduleTimer, performanceMetrics, classLoader) {

  override def getState: StateStorage = {
    logger.info(s"Get a storage where states are kept.")
    stateStorage
  }
}
