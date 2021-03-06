package com.bwsw.sj.crud.rest.instance

import com.bwsw.sj.common.DAL.model.module.{InputInstance, Instance}
import com.bwsw.sj.common.utils.EngineLiterals
import org.apache.http.client.methods.CloseableHttpResponse
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._

/**
 * One-thread stopper object for instance
 * using synchronous apache http client
 *
 *
 * @author Kseniya Tomskikh
 */
class InstanceStopper(instance: Instance, delay: Long = 1000) extends Runnable with InstanceManager {
  private val logger = LoggerFactory.getLogger(getClass.getName)
  import EngineLiterals._

  def run() = {
    try {
      updateInstanceStatus(instance, stopping)
      stopFramework()
      markInstanceAsStopped()
    } catch {
      case e: Exception => //todo что тут подразумевалось? зачем try catch, если непонятен результат при падении
    }
  }

  private def stopFramework() = {
    updateFrameworkState(instance, stopping)
    val response = stopMarathonApplication(instance.name)
    if (isStatusOK(response)) {
      waitForFrameworkToStop()
    }
  }

  private def waitForFrameworkToStop() = {
    var hasStopped = false
    while (!hasStopped) {
      val frameworkApplicationInfo = getApplicationInfo(instance.name)
      if (isStatusOK(frameworkApplicationInfo)) {
        if (hasFrameworkStopped(frameworkApplicationInfo)) {
          updateFrameworkState(instance, stopped)
          hasStopped = true
        } else {
          updateFrameworkState(instance, stopping)
          Thread.sleep(delay)
        }
      } else {
        //todo error?
      }
    }
  }

  private def hasFrameworkStopped(response: CloseableHttpResponse) = {
    val tasksRunning = getNumberOfRunningTasks(response)

    tasksRunning == 0
  }

  private def markInstanceAsStopped() = {
    if (isInputInstance()) {
      clearTasks()
    }
    updateInstanceStatus(instance, stopped)
  }

  private def isInputInstance() = {
    instance.moduleType.equals(inputStreamingType)
  }

  private def clearTasks() = {
    instance.asInstanceOf[InputInstance].tasks.asScala.foreach(x => x._2.clear())
  }
}
