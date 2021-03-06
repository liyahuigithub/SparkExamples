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

package org.apache.spark.examples.mllib

import org.apache.spark.SparkConf
import org.apache.spark.mllib.stat.test.{BinarySample, StreamingTest}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.util.Utils

/**
 * Perform streaming testing using Welch's 2-sample t-test on a stream of data, where the data
 * stream arrives as text files in a directory. Stops when the two groups are statistically
 * significant (p-value < 0.05) or after a user-specified timeout in number of batches is exceeded.
 *
 * The rows of the text files must be in the form `Boolean, Double`. For example:
 * false, -3.92
 * true, 99.32
 *
 * Usage:
 * StreamingTestExample <dataDir> <batchDuration> <numBatchesTimeout>
 *
 * To run on your local machine using the directory `dataDir` with 5 seconds between each batch and
 * a timeout after 100 insignificant batches, call:
 * $ bin/run-example mllib.StreamingTestExample dataDir 5 100
 *
 * As you add text files to `dataDir` the significance test wil continually update every
 * `batchDuration` seconds until the test becomes significant (p-value < 0.05) or the number of
 * batches processed exceeds `numBatchesTimeout`.
 *
 * 使用 Welch 的 2-sample t-test 对数据流执行流式测试，其中数据流作为目录中的文本文件到达。当两组具有统计显着性（p 值 < 0.05）或超过用户指定的批次数超时后停止。
 * 文本文件的行必须采用Boolean, Double形式。例如：假，-3.92 真，99.32
 * 用法：StreamingTestExample      要在本地计算机上使用目录 dataDir运行，每个批次之间间隔 5 秒，并且在 100 个无关紧要的批次之后超时，
 * 请调用： $ bin/run-example mllib.StreamingTestExample dataDir 5 100 当您将文本文件添加到 dataDir时，
 * 重要性测试将持续每 batchDuration秒更新一次，直到测试变得重要（p 值 < 0.05）或处理的批次数超过 numBatchesTimeout 。
 */

/**
 * 流式测试示例
 */
object StreamingTestExample {

  def main(args: Array[String]) {
    if (args.length != 3) {
      // scalastyle:off println
      System.err.println(
        "Usage: StreamingTestExample " +
          "<dataDir> <batchDuration> <numBatchesTimeout>")
      // scalastyle:on println
      System.exit(1)
    }
    val dataDir = args(0)
    val batchDuration = Seconds(args(1).toLong)
    val numBatchesTimeout = args(2).toInt

    val conf = new SparkConf().setMaster("local").setAppName("StreamingTestExample")
    val ssc = new StreamingContext(conf, batchDuration)
    ssc.checkpoint {
      val dir = Utils.createTempDir()
      dir.toString
    }

    // $example on$
    val data = ssc.textFileStream(dataDir).map(line => line.split(",") match {
      case Array(label, value) => BinarySample(label.toBoolean, value.toDouble)
    })

    val streamingTest = new StreamingTest()
      .setPeacePeriod(0)
      .setWindowSize(0)
      .setTestMethod("welch")

    val out = streamingTest.registerStream(data)
    out.print()
    // $example off$

    // Stop processing if test becomes significant or we time out
    var timeoutCounter = numBatchesTimeout
    out.foreachRDD { rdd =>
      timeoutCounter -= 1
      val anySignificant = rdd.map(_.pValue < 0.05).fold(false)(_ || _)
      if (timeoutCounter == 0 || anySignificant) rdd.context.stop()
    }

    ssc.start()
    ssc.awaitTermination()
  }
}
