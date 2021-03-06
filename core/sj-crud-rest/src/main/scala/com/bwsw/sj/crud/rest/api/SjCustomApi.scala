package com.bwsw.sj.crud.rest.api

import java.io.{File, FileOutputStream}
import java.nio.file.Paths

import akka.http.scaladsl.model.Multipart.FormData.BodyPart
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.server.directives.FileInfo
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.bwsw.sj.common.DAL.model.ConfigurationSetting
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.utils.{RestLiterals, ConfigLiterals}
import com.bwsw.sj.common.utils.ConfigurationSettingsUtils._
import com.bwsw.sj.crud.rest.utils.CompletionUtils
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Rest-api for sj-platform executive units and custom files
 *
 *
 *
 * @author Kseniya Tomskikh
 */
trait SjCustomApi extends Directives with SjCrudValidator with CompletionUtils {
  private var previousFileName: Option[String] = None

  private def fileUpload(filename: String, part: BodyPart) = {
    val fileOutput = new FileOutputStream(filename)
    val bytes = part.entity.dataBytes

    def writeFileOnLocal(array: Array[Byte], byteString: ByteString): Array[Byte] = {
      val byteArray: Array[Byte] = byteString.toArray
      fileOutput.write(byteArray)
      array ++ byteArray
    }

    val future = bytes.runFold(Array[Byte]())(writeFileOnLocal)
    Await.result(future, 30.seconds)
    fileOutput.close()
  }

  private def deletePreviousFile() = {
    if (previousFileName.isDefined) {
      val file = new File(previousFileName.get)
      if (file.exists()) file.delete()
    }
  }

  val customApi = {
    pathPrefix("custom") {
      pathPrefix("jars") {
        pathPrefix(Segment) { (name: String) =>
          pathEndOrSingleSlash {
            get {
              if (storage.exists(name)) {
                deletePreviousFile()
                val jarFile = storage.get(name, RestLiterals.tmpDirectory + name)
                previousFileName = Some(jarFile.getAbsolutePath)
                val source = FileIO.fromPath(Paths.get(jarFile.getAbsolutePath))
                complete(HttpResponse(
                  headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> name))),
                  entity = HttpEntity.Chunked.fromData(MediaTypes.`application/java-archive`, source)
                ))
              } else {
                val response = NotFoundRestResponse(Map("message" -> createMessage("rest.custom.jars.file.notfound", name)))
                complete(restResponseToHttpResponse(response))
              }
            }
          } ~
            pathSuffix(Segment) { (version: String) =>
              pathEndOrSingleSlash {
                val fileMetadatas = fileMetadataDAO.getByParameters(Map("specification.name" -> name, "specification.version" -> version))
                if (fileMetadatas.isEmpty) {
                  val response: RestResponse = NotFoundRestResponse(Map("message" ->
                    createMessage("rest.custom.jars.file.notfound", name)))
                  complete(restResponseToHttpResponse(response))
                }
                val filename = fileMetadatas.head.filename
                get {
                  if (storage.exists(filename)) {
                    deletePreviousFile()
                    val jarFile = storage.get(filename, RestLiterals.tmpDirectory + filename)
                    previousFileName = Some(jarFile.getAbsolutePath)
                    val source = FileIO.fromPath(Paths.get(jarFile.getAbsolutePath))
                    complete(HttpResponse(
                      headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> filename))),
                      entity = HttpEntity.Chunked.fromData(MediaTypes.`application/java-archive`, source)
                    ))
                  } else {
                    val response = NotFoundRestResponse(Map("message" ->
                      createMessage("rest.custom.jars.file.notfound", name)))
                    complete(restResponseToHttpResponse(response))
                  }
                } ~
                  delete {
                    var response: RestResponse = NotFoundRestResponse(Map("message" ->
                      createMessage("rest.custom.jars.file.notfound", name)))
                    if (storage.delete(filename)) {
                      configService.delete(createConfigurationSettingName(ConfigLiterals.systemDomain, name + "-" + version))
                      response = OkRestResponse(
                        Map("message" -> createMessage("rest.custom.jars.file.deleted", name, version)))
                    }

                    complete(restResponseToHttpResponse(response))
                  }
              }
            }
        } ~
          pathEndOrSingleSlash {
            post {
              uploadedFile("jar") {
                case (metadata: FileInfo, file: File) =>
                  try {
                    var response: RestResponse = ConflictRestResponse(Map("message" ->
                      createMessage("rest.custom.jars.file.exists", metadata.fileName)))

                    if (!storage.exists(metadata.fileName)) {
                      if (checkSpecification(file)) {
                        val specification = getSpecification(file)
                        val uploadingFile = new File(metadata.fileName)
                        FileUtils.copyFile(file, uploadingFile)
                        storage.put(uploadingFile, metadata.fileName, specification, "custom")
                        val name = specification("name").toString + "-" + specification("version").toString
                        val customJarConfigElement = new ConfigurationSetting(
                          createConfigurationSettingName(ConfigLiterals.systemDomain, name),
                          metadata.fileName,
                          ConfigLiterals.systemDomain
                        )
                        configService.save(customJarConfigElement)
                        response = OkRestResponse(Map("message" ->
                          createMessage("rest.custom.jars.file.uploaded", metadata.fileName)))
                      } else {
                        response = BadRequestRestResponse(Map("message" -> getMessage("rest.errors.invalid.specification")))
                      }
                    }

                    complete(restResponseToHttpResponse(response))
                  } finally {
                    file.delete()
                  }
              }
            } ~
              get {
                val files = fileMetadataDAO.getByParameters(Map("filetype" -> "custom"))
                val response = OkRestResponse(Map("custom-jars" -> mutable.Buffer()))
                if (files.nonEmpty) {
                  response.entity = Map("custom-jars" -> files.map(metadata =>
                    Map("name" -> metadata.specification.name,
                      "version" -> metadata.specification.version))
                  )
                }

                complete(restResponseToHttpResponse(response))
              }
          }
      } ~
        pathPrefix("files") {
          pathEndOrSingleSlash {
            post {
              entity(as[Multipart.FormData]) { (entity: Multipart.FormData) =>
                  val parts: Source[BodyPart, Any] = entity.parts
                  var filename = ""
                  var description = ""
                  val partsResult = parts.runForeach { (part: BodyPart) =>
                    if (part.name.equals("file")) {
                      filename = part.filename.get
                      fileUpload(filename, part)
                      logger.debug(s"File $filename is uploaded")
                    } else if (part.name.equals("description")) {
                      val bytes = part.entity.dataBytes

                      def writeContent(array: Array[Byte], byteString: ByteString): Array[Byte] = {
                        val byteArray: Array[Byte] = byteString.toArray
                        description = new String(byteArray)
                        array ++ byteArray
                      }

                      val future = bytes.runFold(Array[Byte]())(writeContent)
                      Await.result(future, 30.seconds)
                    }
                  }
                  Await.result(partsResult, 30.seconds)

                  var response: RestResponse = ConflictRestResponse(Map("message" ->
                    createMessage("rest.custom.files.file.exists", filename)))

                  if (!storage.exists(filename)) {
                    val uploadingFile = new File(filename)
                    val spec = Map("description" -> description)
                    storage.put(uploadingFile, filename, spec, "custom-file")
                    uploadingFile.delete()

                    response = OkRestResponse(Map("message" -> createMessage("rest.custom.files.file.uploaded", filename)))
                  }

                  complete(restResponseToHttpResponse(response))
              }
            } ~
              get {
                val files = fileMetadataDAO.getByParameters(Map("filetype" -> "custom-file"))
                val response = OkRestResponse(Map("custom-files" -> mutable.Buffer()))
                if (files.nonEmpty) {
                  response.entity = Map("custom-files" -> files.map(metadata =>
                    Map("name" -> metadata.filename,
                      "description" -> metadata.specification.description,
                      "upload-date" -> metadata.uploadDate.toString))
                  )
                }

                complete(restResponseToHttpResponse(response))
              }
          } ~
            pathPrefix(Segment) { (filename: String) =>
              pathEndOrSingleSlash {
                get {
                  if (storage.exists(filename)) {
                    deletePreviousFile()
                    val file = storage.get(filename, RestLiterals.tmpDirectory + filename)
                    previousFileName = Some(file.getAbsolutePath)
                    val source = FileIO.fromPath(Paths.get(file.getAbsolutePath))
                    complete(HttpResponse(
                      headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> filename))),
                      entity = HttpEntity.Chunked.fromData(ContentTypes.`application/octet-stream`, source)
                    ))
                  } else {
                    val response: RestResponse = NotFoundRestResponse(Map("message" ->
                      createMessage("rest.custom.files.file.notfound", filename)))

                    complete(restResponseToHttpResponse(response))
                  }
                } ~
                  delete {
                    var response: RestResponse = NotFoundRestResponse(Map("message" ->
                      createMessage("rest.custom.files.file.notfound", filename)))

                    if (storage.delete(filename)) {
                      response = OkRestResponse(Map("message" -> createMessage("rest.custom.files.file.deleted", filename)))
                    }

                    complete(restResponseToHttpResponse(response))
                  }
              }
            }
        }
    }
  }
}
