package com.bwsw.sj.engine.output.benchmark

import java.io.File

import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.engine.output.benchmark.BenchmarkDataFactory._

/**
  * Created: 17/06/2016
  *
  * @author Kseniya Tomskikh
  */
object BenchmarkDataPrepare extends App {

  val instanceName: String = "test-bench-instance"
  val checkpointInterval = 2
  val checkpointMode = "every-nth"
  val partitions = 4

  val module = new File(getClass.getClassLoader.getResource("sj-stub-output-bench-test.jar").getPath)
  println("module upload")
  uploadModule(module)

  println("cassandra prepare")
  prepareCassandra("bench")
  Thread.sleep(30000)
  println("create providers")
  createProviders()
  println("create services")
  createServices()
  println("create streams")
  createStreams(partitions)
  println("create instance")
  createInstance(instanceName, checkpointMode, checkpointInterval)

  println("create test data")
  Thread.sleep(30000)
  createData(6, 5)

  println("close connections")
  close()
  ConnectionRepository.close()

  println("DONE")

}