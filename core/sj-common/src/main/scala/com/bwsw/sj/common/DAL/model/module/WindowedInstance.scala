package com.bwsw.sj.common.DAL.model.module

import org.mongodb.morphia.annotations.Property

/**
 * Entity for windowed instance-json
  *
  * @author Kseniya Tomskikh
 */
class WindowedInstance() extends Instance {
  @Property("start-from") var startFrom: String = null
  @Property("state-management") var stateManagement: String = null
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 0
  @Property("event-wait-time") var eventWaitTime: Long = 0
  @Property("time-windowed") var timeWindowed: Int = 0
  @Property("window-full-max") var windowFullMax: Int = 0
}
