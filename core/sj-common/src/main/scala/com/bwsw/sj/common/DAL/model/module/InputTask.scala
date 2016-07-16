package com.bwsw.sj.common.DAL.model.module

/**
  * Entity for task of tasks of input instance
  * Created: 16/07/2016
  *
  * @author Kseniya Tomskikh
  */
class InputTask() {
  var host: String = null
  var port: Int = null

  def this(host: String, port: Int) = {
    this()
    this.host = host
    this.port = port
  }

}
