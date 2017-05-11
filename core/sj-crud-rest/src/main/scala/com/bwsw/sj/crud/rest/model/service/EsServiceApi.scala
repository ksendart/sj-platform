package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.ESService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty

class EsServiceApi(name: String,
                   val index: String,
                   val provider: String,
                   description: String = RestLiterals.defaultDescription,
                   @JsonProperty("type") serviceType: String = ServiceLiterals.elasticsearchType)
  extends ServiceApi(serviceType, name, description) {

  override def to(): ESService = {
    val modelService =
      new ESService(
        name = this.name,
        description = this.description,
        provider = this.provider,
        index = this.index,
        serviceType = this.serviceType
      )

    modelService
  }
}