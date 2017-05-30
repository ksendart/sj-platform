package com.bwsw.sj.common.si

import java.io.File

import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.si.model.module.ModuleMetadata
import com.bwsw.sj.common.si.result._
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import org.apache.commons.io.FileUtils
import scaldi.Injectable.inject
import scaldi.Injector

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class ModuleSI(implicit injector: Injector) extends JsonValidator {

  private val connectionRepository: ConnectionRepository = inject[ConnectionRepository]
  private val fileStorage = connectionRepository.getFileStorage
  private val fileMetadataRepository = connectionRepository.getFileMetadataRepository
  private val instanceRepository = connectionRepository.getInstanceRepository
  private val entityRepository: GenericMongoRepository[FileMetadataDomain] = connectionRepository.getFileMetadataRepository
  private val tmpDirectory = "/tmp/"
  private val previousFilesNames: ListBuffer[String] = ListBuffer[String]()

  def create(entity: ModuleMetadata): CreationResult = {
    val modules = entity.map(getFilesMetadata)

    if (modules.nonEmpty) {
      NotCreated(
        ArrayBuffer[String](
          createMessage("rest.modules.module.exists", entity.signature)))
    } else {
      val errors = entity.validate()

      if (errors.isEmpty) {
        val uploadingFile = new File(entity.filename)
        FileUtils.copyFile(entity.file.get, uploadingFile)
        fileStorage.put(uploadingFile, entity.filename, entity.specification.to, FileMetadata.moduleType)

        Created
      } else {
        NotCreated(errors)
      }
    }
  }

  def get(moduleType: String, moduleName: String, moduleVersion: String): Either[String, ModuleMetadata] = {
    exists(moduleType, moduleName, moduleVersion).map { metadata =>
      deletePreviousFiles()
      val file = fileStorage.get(metadata.filename, tmpDirectory + metadata.filename)
      previousFilesNames.append(file.getAbsolutePath)

      ModuleMetadata.from(metadata, Option(file))
    }
  }

  def getAll: mutable.Buffer[ModuleMetadata] = {
    entityRepository
      .getByParameters(Map("filetype" -> FileMetadata.moduleType))
      .map(ModuleMetadata.from(_))
  }

  def getMetadataWithoutFile(moduleType: String, moduleName: String, moduleVersion: String): Either[String, ModuleMetadata] =
    exists(moduleType, moduleName, moduleVersion).map(ModuleMetadata.from(_))

  def getFileName(moduleType: String, moduleName: String, moduleVersion: String) = {
    val filesMetadata = getFilesMetadata(moduleType, moduleName, moduleVersion)

    filesMetadata.head.filename
  }

  def getByType(moduleType: String): Either[String, mutable.Buffer[ModuleMetadata]] = {
    if (EngineLiterals.moduleTypes.contains(moduleType)) {
      val modules = fileMetadataRepository.getByParameters(
        Map("filetype" -> "module", "specification.module-type" -> moduleType))
        .map(ModuleMetadata.from(_))

      Right(modules)
    } else
      Left(createMessage("rest.modules.type.unknown", moduleType))
  }

  def getRelatedInstances(metadata: ModuleMetadata): mutable.Buffer[String] = {
    instanceRepository.getByParameters(Map(
      "module-name" -> metadata.specification.name,
      "module-type" -> metadata.specification.moduleType,
      "module-version" -> metadata.specification.version)
    ).map(_.name)
  }

  def delete(metadata: ModuleMetadata): DeletionResult = {
    if (getRelatedInstances(metadata).nonEmpty) {
      DeletionError(createMessage(
        "rest.modules.module.cannot.delete",
        metadata.signature))
    } else {
      if (fileStorage.delete(metadata.filename))
        Deleted
      else
        DeletionError(createMessage("rest.cannot.delete.file", metadata.filename))
    }
  }

  def exists(moduleType: String, moduleName: String, moduleVersion: String): Either[String, FileMetadataDomain] = {
    if (EngineLiterals.moduleTypes.contains(moduleType)) {
      val moduleSignature = ModuleMetadata.getModuleSignature(moduleType, moduleName, moduleVersion)
      val filesMetadata = getFilesMetadata(moduleType, moduleName, moduleVersion)
      if (filesMetadata.isEmpty)
        Left(createMessage("rest.modules.module.notfound", moduleSignature))
      else {
        if (!fileStorage.exists(filesMetadata.head.filename))
          Left(createMessage("rest.modules.module.jar.notfound", moduleSignature))
        else
          Right(filesMetadata.head)
      }
    } else {
      Left(createMessage("rest.modules.type.unknown", moduleType))
    }
  }

  private def getFilesMetadata(moduleType: String, moduleName: String, moduleVersion: String) = {
    fileMetadataRepository.getByParameters(Map("filetype" -> "module",
      "specification.name" -> moduleName,
      "specification.module-type" -> moduleType,
      "specification.version" -> moduleVersion)
    )
  }

  private def deletePreviousFiles() = {
    previousFilesNames.foreach(filename => {
      val file = new File(filename)
      if (file.exists()) file.delete()
    })
  }
}
