package com.bwsw.sj.common.dal.model

import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.IdField
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.rest.model.config.ConfigurationSettingData
import org.mongodb.morphia.annotations.Entity

/**
  * Entity for one element from configuration settings.
  * Configuration settings is a whole collection in mongo,
  * collection element is one configuration setting.
  */
@Entity("config")
class ConfigurationSetting(@IdField val name: String, val value: String, val domain: String) {

  def asProtocolConfigurationSetting() = {
    val configurationSettingData = new ConfigurationSettingData(
      clearConfigurationSettingName(domain, name),
      this.value,
      this.domain
    )

    configurationSettingData
  }
}