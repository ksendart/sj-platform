package com.bwsw.sj.common.si

import java.io._

import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.dal.model.ConfigurationSettingDomain
import com.bwsw.sj.common.dal.model.module.FileMetadataDomain
import com.bwsw.sj.common.dal.repository.{ConnectionRepository, GenericMongoRepository}
import com.bwsw.sj.common.si.model.FileMetadata
import com.bwsw.sj.common.si.model.config.ConfigurationSetting
import com.bwsw.sj.common.si.result._
import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Provides methods to access custom jar files represented by [[FileMetadata]] in [[GenericMongoRepository]]
  */
class CustomJarsSI extends ServiceInterface[FileMetadata, FileMetadataDomain] {
  override protected val entityRepository: GenericMongoRepository[FileMetadataDomain] = ConnectionRepository.getFileMetadataRepository

  private val fileStorage = ConnectionRepository.getFileStorage
  private val configRepository = ConnectionRepository.getConfigRepository
  private val tmpDirectory = "/tmp/"
  private val previousFilesNames: ListBuffer[String] = ListBuffer[String]()

  private def deletePreviousFiles() = {
    previousFilesNames.foreach(filename => {
      val file = new File(filename)
      if (file.exists()) file.delete()
    })
  }

  override def create(entity: FileMetadata): CreationResult = {
    val errors = entity.validate()

    if (errors.isEmpty) {
      val specification = FileMetadata.getSpecification(entity.file.get)
      val uploadingFile = new File(entity.filename)
      FileUtils.copyFile(entity.file.get, uploadingFile)
      val specificationMap = serializer.deserialize[Map[String, Any]](serializer.serialize(specification))
      fileStorage.put(uploadingFile, entity.filename, specificationMap, FileMetadata.customJarType)
      val name = specification.name + "-" + specification.version
      val customJarConfig = ConfigurationSettingDomain(
        ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, name),
        entity.filename,
        ConfigLiterals.systemDomain
      )
      configRepository.save(customJarConfig)

      Created
    } else NotCreated(errors)
  }

  override def getAll(): mutable.Buffer[FileMetadata] = {
    entityRepository.getByParameters(Map("filetype" -> FileMetadata.customJarType)).map(x => FileMetadata.from(x))
  }

  override def get(name: String): Option[FileMetadata] = {
    if (fileStorage.exists(name)) {
      deletePreviousFiles()
      val jarFile = fileStorage.get(name, tmpDirectory + name)
      previousFilesNames.append(jarFile.getAbsolutePath)

      Some(new FileMetadata(name, Some(jarFile)))
    } else {
      None
    }
  }

  override def delete(name: String): DeletionResult = {
    val fileMetadatas = entityRepository.getByParameters(Map("filetype" -> FileMetadata.customJarType, "filename" -> name))

    if (fileMetadatas.isEmpty)
      EntityNotFound
    else {
      if (fileStorage.delete(name)) {
        val fileMetadata = fileMetadatas.head
        configRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain,
          fileMetadata.specification.name + "-" + fileMetadata.specification.version))

        Deleted
      } else DeletionError(s"Can't delete jar '$name' for some reason. It needs to be debugged.")
    }
  }

  /**
    * Returns custom jar file with this name and version from [[entityRepository]]
    *
    * @param name    name of custom jar file from [[com.bwsw.sj.common.si.model.module.Specification]]
    * @param version version of custom jar file from [[com.bwsw.sj.common.si.model.module.Specification]]
    */
  def getBy(name: String, version: String): Option[FileMetadata] = {
    val fileMetadatas = entityRepository.getByParameters(Map("filetype" -> FileMetadata.customJarType,
      "specification.name" -> name,
      "specification.version" -> version)
    )

    if (fileMetadatas.nonEmpty) {
      val filename = fileMetadatas.head.filename
      deletePreviousFiles()
      val jarFile = fileStorage.get(filename, tmpDirectory + filename)
      previousFilesNames.append(jarFile.getAbsolutePath)

      Some(new FileMetadata(name, Some(jarFile)))
    } else {
      None
    }
  }

  /**
    * Deletes custom jar file with this name and version from [[entityRepository]]
    *
    * @param name    name of custom jar file from [[com.bwsw.sj.common.si.model.module.Specification]]
    * @param version version of custom jar file from [[com.bwsw.sj.common.si.model.module.Specification]]
    * @return Right(true) if custom jar file deleted, Right(false) if custom jar file not found in [[entityRepository]],
    *         Left(error) if some error happened
    */
  def deleteBy(name: String, version: String): DeletionResult = {
    val fileMetadatas = entityRepository.getByParameters(Map("filetype" -> FileMetadata.customJarType,
      "specification.name" -> name,
      "specification.version" -> version)
    )

    if (fileMetadatas.nonEmpty) {
      val filename = fileMetadatas.head.filename

      if (fileStorage.delete(filename)) {
        configRepository.delete(ConfigurationSetting.createConfigurationSettingName(ConfigLiterals.systemDomain, s"$name-$version"))
        Deleted
      } else DeletionError(s"Can't delete jar '$filename' for some reason. It needs to be debugged.")
    } else EntityNotFound
  }
}
