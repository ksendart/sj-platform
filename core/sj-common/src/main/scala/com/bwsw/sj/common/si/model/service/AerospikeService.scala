package com.bwsw.sj.common.si.model.service

import com.bwsw.sj.common.dal.model.service.AerospikeServiceDomain
import com.bwsw.sj.common.rest.utils.ValidationUtils.validateNamespace
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class AerospikeService(name: String,
                       val namespace: String,
                       provider: String,
                       description: String,
                       serviceType: String)
                      (implicit injector: Injector)
  extends Service(serviceType, name, provider, description) {

  import messageResourceUtils.createMessage

  override def to(): AerospikeServiceDomain = {
    val providerRepository = connectionRepository.getProviderRepository

    val modelService =
      new AerospikeServiceDomain(
        name = this.name,
        description = this.description,
        provider = providerRepository.get(this.provider).get,
        namespace = this.namespace
      )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider()

    // 'namespace' field
    Option(this.namespace) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Namespace")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Namespace")
        }
        else {
          if (!validateNamespace(x)) {
            errors += createMessage("entity.error.incorrect.service.namespace", "namespace", x)
          }
        }
    }

    errors
  }
}
