package com.bwsw.sj.common.rest.model.service

import com.bwsw.sj.common.dal.model.service.RestService
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{RestLiterals, ServiceLiterals}

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConverters._
import com.bwsw.sj.common.rest.utils.ValidationUtils._
import com.bwsw.sj.common.utils.MessageResourceUtils._

/**
  * @author Pavel Tomskikh
  */
class RestServiceData extends ServiceData {
  serviceType = ServiceLiterals.restType
  var provider: String = _
  var basePath: String = "/"
  var httpVersion: String = RestLiterals.http_1_1
  var headers: Map[String, String] = Map()

  override def asModelService() = {
    val providerDAO = ConnectionRepository.getProviderService
    val modelService = new RestService(
      this.name,
      this.description,
      providerDAO.get(this.provider).get,
      this.basePath,
      RestLiterals.httpVersionFromString(this.httpVersion),
      this.headers.asJava
    )

    modelService
  }

  override def validate() = {
    val basePathAttributeName = "basePath"
    val httpVersionAttributeName = "httpVersion"
    val errors = new ArrayBuffer[String]()
    errors ++= super.validate()

    // 'provider' field
    errors ++= validateProvider(provider, serviceType)

    // 'basePath' field
    Option(basePath) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", basePathAttributeName)
      case Some(x) =>
        if (!x.startsWith("/"))
          errors += createMessage("entity.error.attribute.must", basePathAttributeName, "starts with '/'")
      case _ =>
    }

    // 'httpVersion' field
    Option(httpVersion) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", httpVersionAttributeName)
      case Some(x) =>
        if (!RestLiterals.httpVersions.contains(x))
          errors += createMessage(
            "entity.error.attribute.must.one_of",
            httpVersionAttributeName,
            RestLiterals.httpVersions.mkString("[", ", ", "]"))
    }

    errors
  }
}