package com.bwsw.sj.crud.rest

import com.bwsw.sj.common.rest.ResponseEntity
import com.bwsw.sj.common.rest.model.module.{InstanceApi, SpecificationApi}
import com.bwsw.sj.crud.rest.model.provider.ProviderApi
import com.bwsw.sj.crud.rest.model.service.ServiceApi
import com.bwsw.sj.crud.rest.model.stream.StreamApi

import scala.collection.mutable

case class ConnectionResponseEntity(connection: Boolean = true) extends ResponseEntity

case class TestConnectionResponseEntity(connection: Boolean, errors: String) extends ResponseEntity

case class ProviderResponseEntity(provider: ProviderApi) extends ResponseEntity

case class ProvidersResponseEntity(providers: mutable.Buffer[ProviderApi] = mutable.Buffer()) extends ResponseEntity

case class RelatedToProviderResponseEntity(services: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity


case class ServiceResponseEntity(service: ServiceApi) extends ResponseEntity

case class ServicesResponseEntity(services: mutable.Buffer[ServiceApi] = mutable.Buffer()) extends ResponseEntity

case class RelatedToServiceResponseEntity(streams: mutable.Buffer[String] = mutable.Buffer(),
                                          instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity


case class StreamResponseEntity(stream: StreamApi) extends ResponseEntity

case class StreamsResponseEntity(streams: mutable.Buffer[StreamApi] = mutable.Buffer()) extends ResponseEntity

case class RelatedToStreamResponseEntity(instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity


case class ModuleInfo(moduleType: String, moduleName: String, moduleVersion: String, size: Long)

case class ModulesResponseEntity(modules: mutable.Buffer[ModuleInfo] = mutable.Buffer()) extends ResponseEntity

case class RelatedToModuleResponseEntity(instances: mutable.Buffer[String] = mutable.Buffer()) extends ResponseEntity

case class SpecificationResponseEntity(specification: SpecificationApi) extends ResponseEntity

case class ShortInstancesResponseEntity(instances: mutable.Buffer[ShortInstance] = mutable.Buffer()) extends ResponseEntity

case class InstanceResponseEntity(instance: InstanceApi) extends ResponseEntity

case class InstancesResponseEntity(instances: mutable.Buffer[InstanceApi] = mutable.Buffer()) extends ResponseEntity

case class ShortInstance(name: String, moduleType: String, moduleName: String, moduleVersion: String,
                         description: String, status: String, restAddress: String)