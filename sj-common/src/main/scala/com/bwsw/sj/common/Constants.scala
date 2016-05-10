package com.bwsw.sj.common

/**
  * Object with constants
  */
object ModuleConstants {
//  TODO: use this constants everywhere in code instead of sj.common.module.ModuleConstants
//  val timeWindowedType = "time-windowed-streaming"
//  val regularStreamingType = "regular-streaming"
//
//  val moduleTypes = Set(timeWindowedType, regularStreamingType)
//  val checkpointModes = Set("every-nth", "time-interval")
//  val stateManagementModes = Set("none", "ram", "rocks")
//  val startFromModes = Set("oldest", "newest")
//
//  val ready = "ready"
//  val started = "started"
//  val stopped = "stopped"
//  val instanceStatusModes = Set(started, stopped, ready)

}

object StreamConstants {
  val streamTypes = Set("Tstream", "kafka")
}

object GeneratorConstants {
  val generatorTypes = Set("global", "local", "per-stream")
  val generatorTypesWithService = Set("global", "per-stream")
}