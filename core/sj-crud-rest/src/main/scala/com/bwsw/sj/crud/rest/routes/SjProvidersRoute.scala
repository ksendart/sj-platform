package com.bwsw.sj.crud.rest.routes

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.utils.ProviderLiterals
import com.bwsw.sj.crud.rest.controller.ProviderController
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

trait SjProvidersRoute extends Directives with SjCrudValidator {
  private val providerController = new ProviderController()

  val providersApi = {
    pathPrefix("providers") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val entity = getEntityFromContext(ctx)
          val response = providerController.post(entity)

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            complete(restResponseToHttpResponse(providerController.getAll()))
          }
      } ~
        pathPrefix("_types") {
          pathEndOrSingleSlash {
            get {
              val response = OkRestResponse(TypesResponseEntity(ProviderLiterals.types))

              complete(restResponseToHttpResponse(response))
            }
          }
        } ~
        pathPrefix(Segment) { (providerName: String) =>
          pathEndOrSingleSlash {
            get {
              complete(restResponseToHttpResponse(providerController.get(providerName)))
            } ~
              delete {
                complete(restResponseToHttpResponse(providerController.delete(providerName)))
              }
          } ~
            pathPrefix("connection") {
              pathEndOrSingleSlash {
                get {
                  complete(restResponseToHttpResponse(providerController.checkConnection(providerName)))
                }
              }
            } ~
            pathPrefix("related") {
              pathEndOrSingleSlash {
                get {
                  complete(restResponseToHttpResponse(providerController.getRelated(providerName)))
                }
              }
            }
        }
    }
  }
}