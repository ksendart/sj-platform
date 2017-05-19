package com.bwsw.sj.common.si.model.instance

import com.bwsw.sj.common.dal.model.instance.{FrameworkStage, InputInstanceDomain, InputTask}
import com.bwsw.sj.common.dal.model.service.ZKServiceDomain
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.utils.{EngineLiterals, RestLiterals}

import scala.collection.JavaConverters._
import scala.collection.mutable

class InputInstance(name: String,
                    description: String = RestLiterals.defaultDescription,
                    parallelism: Any = 1,
                    options: String = "{}",
                    perTaskCores: Double = 1,
                    perTaskRam: Int = 1024,
                    jvmOptions: Map[String, String] = Map(),
                    nodeAttributes: Map[String, String] = Map(),
                    coordinationService: String,
                    environmentVariables: Map[String, String] = Map(),
                    performanceReportingInterval: Long = 60000,
                    moduleName: String,
                    moduleVersion: String,
                    moduleType: String,
                    engine: String,
                    val checkpointMode: String,
                    val checkpointInterval: Long,
                    outputs: Array[String],
                    val lookupHistory: Int,
                    val queueMaxSize: Int,
                    val duplicateCheck: Boolean = false,
                    val defaultEvictionPolicy: String = EngineLiterals.noneDefaultEvictionPolicy,
                    val evictionPolicy: String = EngineLiterals.fixTimeEvictionPolicy,
                    val backupCount: Int = 0,
                    val asyncBackupCount: Int = 0,
                    var tasks: mutable.Map[String, InputTask] = mutable.Map(),
                    restAddress: Option[String] = None,
                    stage: FrameworkStage = FrameworkStage(),
                    status: String = EngineLiterals.ready,
                    frameworkId: String = System.currentTimeMillis().toString)
  extends Instance(
    name,
    description,
    parallelism,
    options,
    perTaskCores,
    perTaskRam,
    jvmOptions,
    nodeAttributes,
    coordinationService,
    environmentVariables,
    performanceReportingInterval,
    moduleName,
    moduleVersion,
    moduleType,
    engine,
    restAddress,
    stage,
    status,
    frameworkId,
    outputs) {

  override def to: InputInstanceDomain = {
    val serviceRepository = ConnectionRepository.getServiceRepository

    new InputInstanceDomain(
      name,
      moduleType,
      moduleName,
      moduleVersion,
      engine,
      serviceRepository.get(coordinationService).asInstanceOf[ZKServiceDomain],
      status,
      restAddress.getOrElse(""),
      description,
      countParallelism,
      options,
      perTaskCores,
      perTaskRam,
      jvmOptions.asJava,
      nodeAttributes.asJava,
      environmentVariables.asJava,
      stage,
      performanceReportingInterval,
      frameworkId,

      outputs,
      checkpointMode,
      checkpointInterval,
      duplicateCheck,
      lookupHistory,
      queueMaxSize,
      defaultEvictionPolicy,
      evictionPolicy,
      backupCount,
      asyncBackupCount,
      tasks.asJava)
  }

  override def prepareInstance(): Unit = {
    Range(0, countParallelism).foreach { i =>
      val task = new InputTask()
      tasks += (s"$name-task$i" -> task)
    }
  }

  override def createStreams(): Unit =
    getStreams(outputs).foreach(_.create())

  override def countParallelism: Int =
    castParallelismToNumber(getStreamsPartitions(outputs))

  override val streams: Array[String] = outputs
}
