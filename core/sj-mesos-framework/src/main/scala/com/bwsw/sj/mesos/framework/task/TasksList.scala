package com.bwsw.sj.mesos.framework.task

import com.bwsw.sj.common.DAL.ConnectionConstants
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.FrameworkRestEntity
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.mesos.framework.schedule.{FrameworkUtil, OffersHandler}
import org.apache.log4j.Logger
import org.apache.mesos.Protos.{TaskID, TaskInfo, _}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object TasksList {
  private val logger = Logger.getLogger(this.getClass)
  private val tasksToLaunch = mutable.ListBuffer[String]()
  private val listTasks = mutable.Map[String, Task]()
  private var message: String = "Initialization"
  private val availablePorts = collection.mutable.ListBuffer[Long]()
  private var launchedOffers = Map[OfferID, ArrayBuffer[TaskInfo]]()

  private var launchedTasks = mutable.ListBuffer[String]()

  var perTaskCores: Double = 0.0
  var perTaskMem: Double = 0.0
  var perTaskPortsCount: Int = 0

  def newTask(taskId: String) = {
    val task = new Task(taskId)
    listTasks += taskId -> task
//    tasksToLaunch += taskId
  }

  def getList = {
    listTasks.values
  }

  def getTask(taskId: String): Task = {
    listTasks(taskId)
  }

  def addToLaunch(taskId: String) = {
    tasksToLaunch += taskId
  }

  def toLaunch: mutable.ListBuffer[String] = {
    tasksToLaunch
  }

  def launched(taskId: String) = {
    tasksToLaunch -= taskId
    launchedTasks += taskId
  }

  def stopped(taskId: String) = {
    launchedTasks -= taskId
  }

  def killTask(taskId: String) = {
    FrameworkUtil.driver.get.killTask(TaskID.newBuilder().setValue(taskId).build)
    stopped(taskId)
  }

  def clearLaunchedTasks() = {
    launchedTasks = mutable.ListBuffer[String]()
  }

  def toFrameworkTask = {
    FrameworkRestEntity(listTasks.values.map(_.toFrameworkTask).toSeq)
  }

  def apply(taskId: String): Option[Task] = {
    listTasks.get(taskId)
  }

  def clearAvailablePorts() = {
    availablePorts.remove(0, availablePorts.length)
  }

  def getAvailablePorts = {
    availablePorts
  }

  def count = {
    toLaunch.size
  }

  def getLaunchedOffers = {
    launchedOffers
  }

  def getLaunchedTasks = {
    launchedTasks
  }

  def clearLaunchedOffers() = {
    launchedOffers = Map[OfferID, ArrayBuffer[TaskInfo]]()
  }

  def setMessage(message: String) = {
    this.message = message
  }

  def prepare(instance: Instance) = {
    perTaskCores = FrameworkUtil.instance.get.perTaskCores
    perTaskMem = FrameworkUtil.instance.get.perTaskRam
    perTaskPortsCount = FrameworkUtil.getCountPorts(FrameworkUtil.instance.get)

    val tasks = FrameworkUtil.instance.get.moduleType match {
      case EngineLiterals.inputStreamingType =>
        (0 until FrameworkUtil.instance.get.parallelism).map(tn => FrameworkUtil.instance.get.name + "-task" + tn)
      case _ =>
        val executionPlan = FrameworkUtil.instance.get match {
          case regularInstance: RegularInstance => regularInstance.executionPlan
          case outputInstance: OutputInstance => outputInstance.executionPlan
          case batchInstance: BatchInstance => batchInstance.executionPlan
        }
        executionPlan.tasks.asScala.keys
    }
    tasks.foreach(task => newTask(task))
  }


  def createTaskToLaunch(task: String, offer: Offer): TaskInfo = {
    // Task Resources
    val cpus = Resource.newBuilder
      .setType(Value.Type.SCALAR)
      .setName("cpus")
      .setScalar(Value.Scalar.newBuilder.setValue(TasksList.perTaskCores))
      .build
    val mem = Resource.newBuilder
      .setType(org.apache.mesos.Protos.Value.Type.SCALAR)
      .setName("mem")
      .setScalar(org.apache.mesos.Protos.Value.
        Scalar.newBuilder.setValue(TasksList.perTaskMem)
      ).build
    val ports = OffersHandler.getPorts(offer, task)

    var agentPorts: String = ""
    var taskPort: String = ""

    var availablePorts = ports.getRanges.getRangeList.asScala.map(_.getBegin.toString)

    val host = OffersHandler.getOfferIp(offer)
    TasksList(task).foreach(task => task.update(host=host))
    if (FrameworkUtil.instance.get.moduleType.equals(EngineLiterals.inputStreamingType)) {
      taskPort = availablePorts.head
      availablePorts = availablePorts.tail
      val inputInstance = FrameworkUtil.instance.get.asInstanceOf[InputInstance]
      inputInstance.tasks.put(task, new InputTask(host, taskPort.toInt))
      ConnectionRepository.getInstanceService.save(FrameworkUtil.instance.get)
    }

    agentPorts = availablePorts.mkString(",")
    agentPorts.dropRight(1)

    logger.debug(s"Task: $task. Ports for task: ${availablePorts.mkString(",")}.")

    val cmd = CommandInfo.newBuilder()
    val hosts: collection.mutable.ListBuffer[String] = collection.mutable.ListBuffer()
    TasksList.toLaunch.foreach(task =>
      if (TasksList.getTask(task).host.nonEmpty) hosts.append(TasksList.getTask(task).host.get)
    )

    var environmentVariables = List(
//      Environment.Variable.newBuilder.setName("MONGO_HOSTS").setValue(FrameworkUtil.params {"mongodbHosts"}),
      Environment.Variable.newBuilder.setName("INSTANCE_NAME").setValue(FrameworkUtil.params {"instanceId"}),
      Environment.Variable.newBuilder.setName("TASK_NAME").setValue(task),
      Environment.Variable.newBuilder.setName("AGENTS_HOST").setValue(OffersHandler.getOfferIp(offer)),
      Environment.Variable.newBuilder.setName("AGENTS_PORTS").setValue(agentPorts),
      Environment.Variable.newBuilder.setName("INSTANCE_HOSTS").setValue(hosts.mkString(","))
    )
    ConnectionConstants.mongoEnvironment.foreach(variable =>
      environmentVariables = environmentVariables :+ Environment.Variable.newBuilder.setName(variable._1).setValue(variable._2)
    )

    try {
      val environments = Environment.newBuilder
      environmentVariables.foreach(variable => environments.addVariables(variable))

      val jvmOptions = FrameworkUtil.instance.get.jvmOptions.asScala
        .foldLeft("")((acc, option) => s"$acc ${option._1}${option._2}")
      cmd
        .addUris(CommandInfo.URI.newBuilder.setValue(FrameworkUtil.getModuleUrl(FrameworkUtil.instance.get)))
        .setValue("java " + jvmOptions + " -jar " + FrameworkUtil.jarName.get)
        .setEnvironment(environments)
    } catch {
      case e: Exception => FrameworkUtil.handleSchedulerException(e, logger)
    }
    logger.info(s"Task: $task => Slave: ${offer.getSlaveId.getValue}")

    TaskInfo.newBuilder
      .setCommand(cmd)
      .setName(task)
      .setTaskId(TaskID.newBuilder.setValue(task))
      .addResources(cpus)
      .addResources(mem)
      .addResources(ports)
      .setSlaveId(offer.getSlaveId)
      .build()
  }





  def addTaskToSlave(task: TaskInfo, offer: (Offer, Int)) = {
    if (launchedOffers.contains(offer._1.getId)) {
      launchedOffers(offer._1.getId) += task
    } else {
      launchedOffers += offer._1.getId -> ArrayBuffer(task)
    }
  }

}
