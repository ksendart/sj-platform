package com.bwsw.sj.crud.rest.model.service

import com.bwsw.sj.common.si.model.service.ZKService
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}
import com.fasterxml.jackson.annotation.JsonProperty
import scaldi.Injector

class ZKCoordServiceApi(name: String,
                        provider: String,
                        val namespace: String,
                        description: Option[String] = Some(RestLiterals.defaultDescription),
                        @JsonProperty("type") serviceType: Option[String] = Some(ServiceLiterals.zookeeperType))
  extends ServiceApi(serviceType.getOrElse(ServiceLiterals.zookeeperType), name, provider, description) {

  override def to()(implicit injector: Injector): ZKService = {
    val modelService =
      new ZKService(
        name = this.name,
        provider = this.provider,
        namespace = this.namespace,
        description = this.description.getOrElse(RestLiterals.defaultDescription),
        serviceType = this.serviceType.getOrElse(ServiceLiterals.zookeeperType)
      )

    modelService
  }
}
