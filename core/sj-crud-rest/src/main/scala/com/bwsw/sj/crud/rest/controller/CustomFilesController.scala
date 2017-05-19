package com.bwsw.sj.crud.rest.controller

import java.io.File
import java.nio.file.Paths

import akka.stream.ActorMaterializer
import akka.stream.scaladsl.FileIO
import com.bwsw.sj.common.rest._
import com.bwsw.sj.common.si.CustomFilesSI
import com.bwsw.sj.common.utils.MessageResourceUtils.{createMessage, getMessage}
import com.bwsw.sj.crud.rest.model.FileMetadataApi
import com.bwsw.sj.crud.rest.{CustomFile, CustomFilesResponseEntity}

import scala.concurrent.ExecutionContextExecutor

class CustomFilesController(implicit val materializer: ActorMaterializer, implicit val executor: ExecutionContextExecutor) extends Controller {
  override val serviceInterface = new CustomFilesSI()

  def create(entity: FileMetadataApi): RestResponse = {
    var response: RestResponse = BadRequestRestResponse(MessageResponseEntity(
      getMessage("rest.custom.files.file.missing")))

    if (entity.filename.isDefined) {
      val file = entity.customFileParts("file").asInstanceOf[File]
      entity.file = Some(file)
      if (entity.customFileParts.isDefinedAt("description")) entity.description = entity.customFileParts("description").asInstanceOf[String]

      val created = serviceInterface.create(entity.to())

      response = created match {
        case Right(_) =>
          OkRestResponse(MessageResponseEntity(
            createMessage("rest.custom.files.file.uploaded", entity.filename.get)))
        case Left(_) => ConflictRestResponse(MessageResponseEntity(
          createMessage("rest.custom.files.file.exists", entity.filename.get)))
      }
      file.delete()
    }
    entity.file.get.delete()

    response
  }

  override def getAll(): RestResponse = {
    val response = OkRestResponse(CustomFilesResponseEntity())
    val fileMetadata = serviceInterface.getAll()
    if (fileMetadata.nonEmpty) {
      response.entity = CustomFilesResponseEntity(fileMetadata.map(m => FileMetadataApi.toCustomFileInfo(m)))
    }

    response
  }

  override def get(name: String): RestResponse = {
    val service = serviceInterface.get(name)

    val response = service match {
      case Some(x) =>
        val source = FileIO.fromPath(Paths.get(x.file.get.getAbsolutePath))

        CustomFile(name, source)
      case None =>
        NotFoundRestResponse(MessageResponseEntity(createMessage("rest.custom.files.file.notfound", name)))
    }

    response
  }

  override def delete(name: String): RestResponse = {
    val deleteResponse = serviceInterface.delete(name)
    val response: RestResponse = deleteResponse match {
      case Right(isDeleted) =>
        if (isDeleted)
          OkRestResponse(MessageResponseEntity(createMessage("rest.custom.files.file.deleted", name)))
        else
          NotFoundRestResponse(MessageResponseEntity(createMessage("rest.custom.files.file.notfound", name)))
      case Left(message) =>
        UnprocessableEntityRestResponse(MessageResponseEntity(message))
    }

    response
  }

  override def create(serializedEntity: String): RestResponse = ???
}