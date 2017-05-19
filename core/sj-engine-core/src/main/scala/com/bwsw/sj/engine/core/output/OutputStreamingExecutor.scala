package com.bwsw.sj.engine.core.output

import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.environment.OutputEnvironmentManager

/**
  *
  * It is responsible for output module execution logic.
  * Module uses a specific instance to configure its work.
  * Executor provides the following methods, which don't do anything by default so you should define their implementation by yourself.
  *
  * @author Kseniya Tomskikh
  */
abstract class OutputStreamingExecutor[T <: AnyRef](manager: OutputEnvironmentManager) extends StreamingExecutor {
  /**
    * it is invoked for every received message from one of the inputs that are defined within the instance.
    * Inside the method you have an access to the message that has the TStreamEnvelope type.
    * By extension a t-stream envelope should be transformed to output envelopes.
    *
    */
  def onMessage(envelope: TStreamEnvelope[T]): Seq[OutputEnvelope] = {
    List()
  }

  /** *
    * This method return current working entity.
    * Must be implemented.
    * For example:
    * {{{
    *   val entityBuilder = new ConcreteEntityBuilder()
    *   val entity = entityBuilder
    *     .field(new IntegerField("id"))
    *     .field(new JavaStringField("name"))
    *     .build()
    *   return entity
    * }}}
    *
    * @return Current working Entity.
    */
  def getOutputEntity: Entity[_]
}
