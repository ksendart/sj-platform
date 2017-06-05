package com.bwsw.common

import java.lang.reflect.{ParameterizedType, Type}

import com.bwsw.common.exceptions._
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonMappingException, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * Class based on jackson for json serialization.
  * Most commonly used in REST to serialize/deserialize api entities
  */
class JsonSerializer {

  def this(ignore: Boolean, disableNullForPrimitives: Boolean = false) = {
    this()
    this.setIgnoreUnknown(ignore)
    this.disableNullForPrimitives(disableNullForPrimitives)
  }

  private val logger = LoggerFactory.getLogger(this.getClass)

  val mapper = new ObjectMapper()
  mapper.registerModule(DefaultScalaModule)
  mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)

  def serialize(value: Any): String = {
    import java.io.StringWriter

    logger.debug(s"Serialize a value of class: '${value.getClass}' to string.")
    val writer = new StringWriter()
    mapper.writeValue(writer, value)
    writer.toString
  }

  def deserialize[T: Manifest](value: String): T = {
    logger.debug(s"Deserialize a value: '$value' to object.")
    Try {
      mapper.readValue[T](value, typeReference[T])
    } match {
      case Success(entity) => entity
      case Failure(e: UnrecognizedPropertyException) =>
        throw new JsonUnrecognizedPropertyException(getProblemProperty(e))
      case Failure(e: JsonMappingException) =>
        if (e.getMessage.startsWith("No content"))
          throw new JsonDeserializationException("Empty JSON")
        else if (e.getMessage.startsWith("Missing required creator property"))
          throw new JsonMissedPropertyException(getMissedProperty(e))
        else
          throw new JsonIncorrectValueException(getProblemProperty(e))
      case Failure(e: JsonParseException) =>
        val position = e.getProcessor.getTokenLocation.getCharOffset.toInt
        val leftBound = Math.max(0, position - 16)
        val rightBound = Math.min(position + 16, value.length)
        throw new JsonNotParsedException(value.substring(leftBound, rightBound))
      case Failure(_: NullPointerException) =>
        throw new JsonDeserializationException("JSON is null")
      case Failure(e) => throw e
    }
  }

  private def getProblemProperty(exception: JsonMappingException) = {
    exception.getPath.asScala.foldLeft("") { (s, ref) =>
      val fieldName = Option(ref.getFieldName)
      s + {
        if (fieldName.isDefined) {
          if (s.isEmpty) ""
          else "."
        } + fieldName.get
        else "(" + ref.getIndex + ")"
      }
    }
  }

  private def getMissedProperty(exception: JsonMappingException): String =
    exception.getOriginalMessage.replaceFirst("Missing required creator property\\s*'(.*?)'.*", "$1")

  private def typeReference[T: Manifest]: TypeReference[T] = new TypeReference[T] {
    override def getType: Type = typeFromManifest(manifest[T])
  }

  private def typeFromManifest(m: Manifest[_]): Type = {
    if (m.typeArguments.isEmpty) {
      m.runtimeClass
    }
    else new ParameterizedType {
      def getRawType: Class[_] = m.runtimeClass

      def getActualTypeArguments: Array[Type] = m.typeArguments.map(typeFromManifest).toArray

      def getOwnerType: Null = null
    }
  }

  def setIgnoreUnknown(ignore: Boolean): Unit = {
    logger.debug(s"Set a value of flag: FAIL_ON_UNKNOWN_PROPERTIES to '$ignore'.")
    if (ignore) {
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    } else {
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
    }
  }

  def getIgnoreUnknown(): Boolean = {
    logger.debug(s"Retrieve a value of flag: FAIL_ON_UNKNOWN_PROPERTIES.")
    !((mapper.getDeserializationConfig.getDeserializationFeatures & DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.getMask) == DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES.getMask)
  }

  def disableNullForPrimitives(disable: Boolean): Unit = {
    logger.debug(s"Set a value of flag: FAIL_ON_NULL_FOR_PRIMITIVES to '$disable'.")
    mapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, disable)
  }

  def nullForPrimitivesIsDisabled(): Boolean = {
    logger.debug(s"Retrieve a value of flag: FAIL_ON_NULL_FOR_PRIMITIVES.")
    mapper.isEnabled(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
  }
}
