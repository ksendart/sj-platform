package com.bwsw.sj.common.DAL.model.module

import com.bwsw.sj.common.rest.entities.module.{ExecutionPlan, InstanceMetadata, OutputInstanceMetadata}
import org.mongodb.morphia.annotations.{Embedded, Property}

/**
 * Entity for output-streaming instance-json
 *
 *
 * @author Kseniya Tomskikh
 */
class OutputInstance() extends Instance {
  @Property("checkpoint-mode") var checkpointMode: String = null
  @Property("checkpoint-interval") var checkpointInterval: Long = 0
  @Embedded("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan()
  @Property("start-from") var startFrom: String = "newest"

  override def asProtocolInstance(): InstanceMetadata = {
    val protocolInstance = new OutputInstanceMetadata()
    super.fillProtocolInstance(protocolInstance)
    protocolInstance.checkpointMode = this.checkpointMode
    protocolInstance.checkpointInterval = this.checkpointInterval
    protocolInstance.executionPlan = this.executionPlan
    protocolInstance.input = this.inputs.head
    protocolInstance.output = this.outputs.head
    protocolInstance.startFrom = this.startFrom

    protocolInstance
  }
}
