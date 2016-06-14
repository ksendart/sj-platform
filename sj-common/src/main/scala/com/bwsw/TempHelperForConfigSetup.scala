package com.bwsw

import com.bwsw.sj.common.ConfigConstants
import com.bwsw.sj.common.DAL.model.ConfigSetting
import com.bwsw.sj.common.DAL.repository.ConnectionRepository

object TempHelperForConfigSetup extends App{

  val configService = ConnectionRepository.getConfigService

  configService.save(new ConfigSetting("system" + "." + "com.bwsw.tg-0.1", "sj-transaction-generator-assembly-1.0.jar", "system"))
  configService.save(new ConfigSetting(ConfigConstants.transactionGeneratorTag, "com.bwsw.tg-0.1", "system"))

  configService.save(new ConfigSetting("system" + "." + "com.bwsw.mf-0.1", "ScalaMesos-assembly-1.0.jar", "system"))
  configService.save(new ConfigSetting(ConfigConstants.frameworkTag, "com.bwsw.mf-0.1", "system"))

  configService.save(new ConfigSetting("system" + "." + "com.bwsw.regular.streaming.engine-0.1", "sj-regular-streaming-engine-assembly-1.0.jar", "system"))
  configService.save(new ConfigSetting(ConfigConstants.regularEngineTag, "com.bwsw.regular.streaming.engine-0.1", "system"))

  configService.save(new ConfigSetting("system" + "." + "regular-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.RegularStreamingValidator", "system"))
  configService.save(new ConfigSetting("system" + "." + "windowed-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.WindowedStreamingValidator", "system"))
  configService.save(new ConfigSetting("system" + "." + "output-streaming-validator-class", "com.bwsw.sj.crud.rest.validator.module.OutputStreamingValidator", "system"))

  configService.save(new ConfigSetting(ConfigConstants.marathonTag, "http://stream-juggler.z1.netpoint-dc.com:8080", "system"))

  configService.save(new ConfigSetting(ConfigConstants.zkSessionTimeoutTag, "7000", "zk"))

  configService.save(new ConfigSetting(ConfigConstants.txnPreloadTag, "10", "t-streams"))
  configService.save(new ConfigSetting(ConfigConstants.dataPreloadTag, "7", "t-streams"))
  configService.save(new ConfigSetting(ConfigConstants.consumerKeepAliveInternalTag, "5", "t-streams"))
  configService.save(new ConfigSetting(ConfigConstants.txnTTLTag, "6", "t-streams"))
  configService.save(new ConfigSetting(ConfigConstants.txnKeepAliveIntervalTag, "2", "t-streams"))
  configService.save(new ConfigSetting(ConfigConstants.transportTimeoutTag, "5", "t-streams"))
  configService.save(new ConfigSetting(ConfigConstants.producerKeepAliveIntervalTag, "1", "t-streams"))
  configService.save(new ConfigSetting(ConfigConstants.streamTTLTag, "60000", "t-streams"))

  //configService.save(new ConfigSetting("session.timeout.ms", "30000", "kafka")) e.g. for kafka domain
  configService.save(new ConfigSetting(ConfigConstants.esTimeoutTag, "6000", "es"))
  configService.save(new ConfigSetting(ConfigConstants.jdbcTimeoutTag, "6000", "jdbc"))
}
