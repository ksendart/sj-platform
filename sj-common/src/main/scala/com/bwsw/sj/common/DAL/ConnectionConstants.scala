package com.bwsw.sj.common.DAL

import com.bwsw.sj.common.ConfigLoader

object ConnectionConstants {
   val conf = ConfigLoader.load()
   val host = System.getenv("MONGO_HOST")
   val port = System.getenv("MONGO_PORT").toInt

   val databaseName = "stream_juggler"
   lazy val fileMetadataCollection = "fs.files"
   lazy val instanceCollection = "instances"
   lazy val streamCollection = "streams"
   lazy val serviceCollection = "services"
   lazy val providerCollection = "providers"

 }