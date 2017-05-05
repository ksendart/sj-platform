package com.bwsw.sj.common.rest.model.service

import com.bwsw.sj.common.dal.model.service.CassandraService
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.utils.MessageResourceUtils._

class CassDBServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.cassandraType
  var provider: String = null
  var keyspace: String = null

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new CassandraService(
      this.name,
      this.description,
      providerDAO.get(this.provider).get,
      this.keyspace
    )

    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

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