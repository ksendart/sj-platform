package com.bwsw.sj.common.rest.model.stream

import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}

class RestStreamApi(override val name: String,
                    override val service: String,
                    override val tags: Array[String] = Array(),
                    override val force: Boolean = false,
                    override val description: String = RestLiterals.defaultDescription)
  extends StreamApi(StreamLiterals.restOutputType, name, service, tags, force, description)
