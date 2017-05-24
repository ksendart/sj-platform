package com.bwsw.sj.crud.rest.model.module

import java.io.File
import java.util.jar.JarFile

import com.bwsw.sj.common.si.model.module.ModuleMetadata
import com.bwsw.sj.common.utils.MessageResourceUtils.createMessage
import com.bwsw.sj.common.utils.RestLiterals
import com.bwsw.sj.crud.rest.ModuleInfo
import com.bwsw.sj.crud.rest.model.FileMetadataApi

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

class ModuleMetadataApi(filename: String,
                        file: File,
                        description: String = RestLiterals.defaultDescription,
                        customFileParts: Map[String, Any] = Map())
  extends FileMetadataApi(
    Option(filename),
    Option(file),
    description,
    customFileParts) {

  override def to(): ModuleMetadata =
    new ModuleMetadata(filename, SpecificationApi.from(file).to, Option(file))

  def validate: ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]

    if (!filename.endsWith(".jar"))
      errors += createMessage("rest.modules.modules.extension.unknown", filename)

    if (Try(new JarFile(file)).isFailure)
      errors += createMessage("rest.modules.module.jar.incorrect", filename)

    errors
  }
}

object ModuleMetadataApi {
  def toModuleInfo(moduleMetadata: ModuleMetadata): ModuleInfo = {
    ModuleInfo(
      moduleMetadata.specification.moduleType,
      moduleMetadata.specification.name,
      moduleMetadata.specification.version,
      moduleMetadata.length.get)
  }
}
