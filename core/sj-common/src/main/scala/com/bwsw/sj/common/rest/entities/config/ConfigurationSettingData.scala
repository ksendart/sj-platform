package com.bwsw.sj.common.rest.entities.config

import com.bwsw.sj.common.DAL.model.ConfigurationSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.tstreams.env.TSF_Dictionary
import com.fasterxml.jackson.annotation.JsonIgnore

import scala.collection.mutable.ArrayBuffer

case class ConfigurationSettingData(name: String, value: String) extends ValidationUtils {
  @JsonIgnore
  def asModelConfigurationSetting(domain: String) = {
    val configurationSetting = new ConfigurationSetting(
      createConfigurationSettingName(domain, this.name),
      this.value,
      domain
    )

    configurationSetting
  }

  @JsonIgnore
  def validate(domain: String) = {
    val configService = ConnectionRepository.getConfigService
    val errors = new ArrayBuffer[String]()

    // 'name' field
    Option(this.name) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Name")
        }
        else {
          if (configService.get(x).isDefined) {
            errors += createMessage("entity.error.already.exists", "Config setting", x)
          }

          if (!validateConfigSettingName(x)) {
            errors += createMessage("entity.error.incorrect.name", "Config setting", x, "config setting")
          }

          if (domain == ConfigLiterals.tstreamsDomain && !validateTstreamProperty()) {
            errors += createMessage("entity.error.incorrect.name.tstreams.domain", "Config setting", x)
          }
        }
    }

    // 'value' field
    Option(this.value) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Value")
      case Some(x) =>
        if (x.isEmpty)
          errors += createMessage("entity.error.attribute.required", "Value")
    }

    errors
  }

  @JsonIgnore
  private def validateTstreamProperty(): Boolean = {
    this.name.contains("producer") || this.name.contains("consumer") || this.name == TSF_Dictionary.Producer.Transaction.DISTRIBUTION_POLICY
  }
}
