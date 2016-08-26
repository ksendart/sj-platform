package com.bwsw.sj.crud.rest.entities.module

import com.fasterxml.jackson.annotation.JsonProperty

class RegularInstanceMetadata extends InstanceMetadata {
  @JsonProperty("state-management") var stateManagement: String = null
  @JsonProperty("state-full-checkpoint") var stateFullCheckpoint: Int = 0
  @JsonProperty("event-wait-time") var eventWaitTime: Long = 0
}
