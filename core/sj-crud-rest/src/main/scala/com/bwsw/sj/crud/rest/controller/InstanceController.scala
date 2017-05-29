package com.bwsw.sj.crud.rest.controller

import java.net.URI

import com.bwsw.common.JsonSerializer
import com.bwsw.common.exceptions.JsonDeserializationException
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.model.instance.Instance
import com.bwsw.sj.common.si.model.module.{ModuleMetadata, Specification}
import com.bwsw.sj.common.si._
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.MessageResourceUtils.{createMessage, createMessageWithErrors, getMessage}
import com.bwsw.sj.crud.rest.exceptions.ConfigSettingNotFound
import com.bwsw.sj.crud.rest.instance.{HttpClient, InstanceDestroyer, InstanceStarter, InstanceStopper}
import com.bwsw.sj.crud.rest.model.instance._
import com.bwsw.sj.crud.rest.model.instance.response.InstanceApiResponse
import com.bwsw.sj.crud.rest.utils.JsonDeserializationErrorMessageCreator
import com.bwsw.sj.crud.rest.validator.instance.InstanceValidator
import com.bwsw.sj.crud.rest.{InstanceResponseEntity, InstancesResponseEntity, ShortInstance, ShortInstancesResponseEntity}
import org.apache.http.client.methods.HttpGet
import org.apache.http.util.EntityUtils
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

class InstanceController {

  private val logger = LoggerFactory.getLogger(getClass)
  private val serializer = new JsonSerializer(true, true)
  private val serviceInterface = new InstanceSI
  private val moduleSI = new ModuleSI
  private val configService = ConnectionRepository.getConfigRepository

  def create(serializedEntity: String, moduleType: String, moduleName: String, moduleVersion: String): RestResponse = {
    ifModuleExists(moduleType, moduleName, moduleVersion) { module =>
      Try(deserializeInstanceApi(serializedEntity, moduleType))
        .map(_.to(moduleType, moduleName, moduleVersion)) match {
        case Success(instance) =>
          val errors = new ArrayBuffer[String]
          errors ++= validateInstance(instance, module.specification)
          if (errors.isEmpty) {
            serviceInterface.create(instance, module) match {
              case Created =>
                OkRestResponse(
                  MessageResponseEntity(
                    createMessage("rest.modules.instances.instance.created", instance.name, module.signature)))

              case NotCreated(validationErrors) =>
                BadRequestRestResponse(
                  MessageResponseEntity(
                    createMessageWithErrors(
                      "rest.modules.instances.instance.cannot.create.incorrect.parameters",
                      validationErrors)))
            }
          } else {
            BadRequestRestResponse(
              MessageResponseEntity(
                createMessageWithErrors("rest.modules.instances.instance.cannot.create", errors)))
          }

        case Failure(exception: JsonDeserializationException) =>
          val error = JsonDeserializationErrorMessageCreator(exception)
          BadRequestRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.instances.instance.cannot.create", error)))

        case Failure(exception) => throw exception
      }
    }
  }

  def get(moduleType: String, moduleName: String, moduleVersion: String, name: String): RestResponse = {
    processInstance(moduleType, moduleName, moduleVersion, name) { instance =>
      OkRestResponse(InstanceResponseEntity(InstanceApiResponse.from(instance)))
    }
  }

  def getAll: RestResponse = {
    val instances = serviceInterface.getAll.map { instance =>
      ShortInstance(
        instance.name,
        instance.moduleType,
        instance.moduleName,
        instance.moduleVersion,
        instance.description,
        instance.status,
        instance.restAddress.getOrElse(""))
    }

    OkRestResponse(ShortInstancesResponseEntity(instances))
  }

  def getByModule(moduleType: String, moduleName: String, moduleVersion: String): RestResponse = {
    ifModuleExists(moduleType, moduleName, moduleVersion) { _ =>
      val instances = serviceInterface.getByModule(moduleType, moduleName, moduleVersion)
        .map(InstanceApiResponse.from)
      OkRestResponse(InstancesResponseEntity(instances))
    }
  }

  def delete(moduleType: String, moduleName: String, moduleVersion: String, name: String): RestResponse = {
    ifModuleExists(moduleType, moduleName, moduleVersion) { _ =>
      serviceInterface.delete(name) match {
        case Deleted =>
          OkRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.instances.instance.deleted", name)))

        case WillBeDeleted(instance) =>
          destroyInstance(instance)
          OkRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.instances.instance.deleting", name)))

        case DeletionError(error) =>
          UnprocessableEntityRestResponse(MessageResponseEntity(error))

        case EntityNotFound =>
          NotFoundRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.module.instances.instance.notfound", name)))
      }
    }
  }

  def start(moduleType: String, moduleName: String, moduleVersion: String, name: String): RestResponse = {
    processInstance(moduleType, moduleName, moduleVersion, name) { instance =>
      if (serviceInterface.canStart(instance)) {
        startInstance(instance)
        OkRestResponse(
          MessageResponseEntity(
            createMessage("rest.modules.instances.instance.starting", name)))
      } else {
        UnprocessableEntityRestResponse(
          MessageResponseEntity(
            createMessage("rest.modules.instances.instance.cannot.start", name)))
      }
    }
  }

  def stop(moduleType: String, moduleName: String, moduleVersion: String, name: String): RestResponse = {
    processInstance(moduleType, moduleName, moduleVersion, name) { instance =>
      if (serviceInterface.canStop(instance)) {
        stopInstance(instance)
        OkRestResponse(
          MessageResponseEntity(
            createMessage("rest.modules.instances.instance.stopping", name)))
      } else {
        UnprocessableEntityRestResponse(
          MessageResponseEntity(
            createMessage("rest.modules.instances.instance.cannot.stop", name)))
      }
    }
  }

  def tasks(moduleType: String, moduleName: String, moduleVersion: String, name: String): RestResponse = {
    processInstance(moduleType, moduleName, moduleVersion, name) { instance =>
      var response: RestResponse = UnprocessableEntityRestResponse(MessageResponseEntity(
        getMessage("rest.modules.instances.instance.cannot.get.tasks")))

      if (instance.restAddress.isDefined) {
        val client = new HttpClient(3000).client
        val url = new URI(instance.restAddress.get)
        val httpGet = new HttpGet(url.toString)
        val httpResponse = client.execute(httpGet)
        response = OkRestResponse(
          serializer.deserialize[FrameworkRestEntity](EntityUtils.toString(httpResponse.getEntity, "UTF-8")))
        client.close()
      }

      response
    }
  }

  private def ifModuleExists(moduleType: String, moduleName: String, moduleVersion: String)
                            (f: ModuleMetadata => RestResponse): RestResponse = {
    moduleSI.exists(moduleType, moduleName, moduleVersion) match {
      case Right(moduleMetadata) =>
        f(ModuleMetadata.from(moduleMetadata))
      case Left(error) =>
        NotFoundRestResponse(MessageResponseEntity(error))
    }
  }

  private def processInstance(moduleType: String, moduleName: String, moduleVersion: String, name: String)
                             (f: Instance => RestResponse): RestResponse = {
    ifModuleExists(moduleType, moduleName, moduleVersion) { _ =>
      serviceInterface.get(name) match {
        case Some(instance) => f(instance)
        case None =>
          NotFoundRestResponse(
            MessageResponseEntity(
              createMessage("rest.modules.module.instances.instance.notfound", name)))
      }
    }
  }

  private def startInstance(instance: Instance) = {
    logger.debug(s"Starting application of instance ${instance.name}.")
    new Thread(new InstanceStarter(instance)).start()
  }

  private def stopInstance(instance: Instance) = {
    logger.debug(s"Stopping application of instance ${instance.name}.")
    new Thread(new InstanceStopper(instance)).start()
  }

  private def destroyInstance(instance: Instance) = {
    logger.debug(s"Destroying application of instance ${instance.name}.")
    new Thread(new InstanceDestroyer(instance)).start()
  }

  private def deserializeInstanceApi(serialized: String, moduleType: String): InstanceApi = moduleType match {
    case EngineLiterals.inputStreamingType =>
      serializer.deserialize[InputInstanceApi](serialized)
    case EngineLiterals.regularStreamingType =>
      serializer.deserialize[RegularInstanceApi](serialized)
    case EngineLiterals.batchStreamingType =>
      serializer.deserialize[BatchInstanceApi](serialized)
    case EngineLiterals.outputStreamingType =>
      serializer.deserialize[OutputInstanceApi](serialized)
    case _ =>
      serializer.deserialize[InstanceApi](serialized)
  }

  private def validateInstance(instance: Instance, specification: Specification) = {
    val validatorClassConfig = s"${instance.moduleType}-validator-class"
    val validatorClassName = configService.get(s"${ConfigLiterals.systemDomain}.$validatorClassConfig") match {
      case Some(configurationSetting) => configurationSetting.value
      case None => throw ConfigSettingNotFound(
        createMessage("rest.config.setting.notfound", s"${ConfigLiterals.systemDomain}.$validatorClassConfig"))
    }
    val validatorClass = Class.forName(validatorClassName)
    val validator = validatorClass.newInstance().asInstanceOf[InstanceValidator]
    validator.validate(instance, specification)
  }
}
