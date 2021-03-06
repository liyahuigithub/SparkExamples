/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// scalastyle:off println
package org.apache.spark.examples

import org.apache.log4j.{Level, Logger}
import org.apache.spark.rdd.RDD

import java.util.Random
import org.apache.spark.sql.SparkSession

/**
 * Usage: SimpleSkewedGroupByTest [numMappers] [numKVPairs] [valSize] [numReducers] [ratio]
 *
 * 倾斜分组测试
 */
object SimpleSkewedGroupByTest {
  def main(args: Array[String]) {
    Logger.getLogger("org").setLevel(Level.WARN)
    val spark = SparkSession
      .builder
      .master("local[*]")
      .appName("SimpleSkewedGroupByTest")
      .getOrCreate()

    val numMappers: Int = if (args.length > 0) args(0).toInt else 2
    val numKVPairs: Int = if (args.length > 1) args(1).toInt else 1000
    val valSize: Int = if (args.length > 2) args(2).toInt else 1000
    val numReducers: Int = if (args.length > 3) args(3).toInt else numMappers
    val ratio: Double = if (args.length > 4) args(4).toInt else 5.0

    val pairs1: RDD[(Int, Array[Byte])] = spark.sparkContext.parallelize(0 until numMappers, numMappers).flatMap { p =>
      val ranGen = new Random
      val result = new Array[(Int, Array[Byte])](numKVPairs)
      for (i <- 0 until numKVPairs) {
        val byteArr = new Array[Byte](valSize)
        ranGen.nextBytes(byteArr)
        val offset: Int = ranGen.nextInt(1000) * numReducers
        if (ranGen.nextDouble < ratio / (numReducers + ratio - 1)) {
          // give ratio times higher chance of generating key 0 (for reducer 0)
          result(i) = (offset, byteArr)
        } else {
          // generate a key for one of the other reducers
          val key: Int = 1 + ranGen.nextInt(numReducers-1) + offset
          result(i) = (key, byteArr)
        }
      }
      result
    }.cache
    // Enforce that everything has been calculated and in cache
    pairs1.count

    println("RESULT: " + pairs1.groupByKey(numReducers).count)
    // Print how many keys each reducer got (for debugging)
    // println("RESULT: " + pairs1.groupByKey(numReducers)
    //                           .map{case (k,v) => (k, v.size)}
    //                           .collectAsMap)

    spark.stop()
  }
}
// scalastyle:on println
