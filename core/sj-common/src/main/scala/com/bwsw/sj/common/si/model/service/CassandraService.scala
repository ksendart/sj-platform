package com.bwsw.sj.common.si.model.service

import com.bwsw.sj.common.dal.model.service.CassandraServiceDomain
import com.bwsw.sj.common.rest.utils.ValidationUtils.validateNamespace
import scaldi.Injector

import scala.collection.mutable.ArrayBuffer

class CassandraService(name: String,
                       val keyspace: String,
                       provider: String,
                       description: String,
                       serviceType: String)
                      (implicit injector: Injector)
  extends Service(serviceType, name, provider, description) {

  import messageResourceUtils.createMessage

  override def to(): CassandraServiceDomain = {
    val providerRepository = connectionRepository.getProviderRepository

    val modelService =
      new CassandraServiceDomain(
        name = this.name,
        description = this.description,
        provider = providerRepository.get(this.provider).get,
        keyspace = this.keyspace
      )

    modelService
  }

  override def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider()

    // 'keyspace' field
    Option(this.keyspace) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Keyspace")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Keyspace")
        }
        else {
          if (!validateNamespace(x)) {
            errors += createMessage("entity.error.incorrect.service.namespace", "keyspace", x)
          }
        }
    }

    errors
  }
}
