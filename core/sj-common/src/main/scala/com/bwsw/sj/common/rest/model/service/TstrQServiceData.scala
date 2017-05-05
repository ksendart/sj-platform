package com.bwsw.sj.common.rest.model.service

import com.bwsw.sj.common.dal.model.service.TStreamService
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.ServiceLiterals

import scala.collection.mutable.ArrayBuffer
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.utils.MessageResourceUtils._

class TstrQServiceData() extends ServiceData() {
  serviceType = ServiceLiterals.tstreamsType
  var provider: String = null
  var prefix: String = null
  var token: String = null

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new TStreamService(
      this.name,
      this.description,
      providerDAO.get(this.provider).get,
      this.prefix,
      this.token
    )

    modelService
  }

  override def validate() = {
    val errors = new ArrayBuffer[String]()
    val providerDAO = ConnectionRepository.getProviderService

    errors ++= super.validateGeneralFields()

    // 'provider' field
    errors ++= validateProvider(this.provider, this.serviceType)

    // 'prefix' field
    Option(this.prefix) match {
      case None =>

        errors += createMessage("entity.error.attribute.required", "Prefix")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Prefix")
        }
        else {
          if (!validatePrefix(x)) {
            errors += createMessage("entity.error.incorrect.service.prefix", x)
          }
        }
    }

    // 'token' field
    Option(this.token) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Token")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Token")
        }
        else {
          if (!validateToken(x)) {
            errors += createMessage("entity.error.incorrect.service.token", x)
          }
        }
    }

    errors
  }
}