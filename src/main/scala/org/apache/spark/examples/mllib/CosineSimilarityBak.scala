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
package examples.mllib

import org.apache.log4j.{Level, Logger}
import org.apache.spark.examples.mllib.AbstractParams
import org.apache.spark.mllib.linalg
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.distributed.{CoordinateMatrix, MatrixEntry, RowMatrix}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}
import scopt.OptionParser

/**
 * Compute the similar columns of a matrix, using cosine similarity.
 *
 * The input matrix must be stored in row-oriented dense format, one line per row with its entries
 * separated by space. For example,
 * {{{
 * 0.5 1.0
 * 2.0 3.0
 * 4.0 5.0
 * }}}
 * represents a 3-by-2 matrix, whose first row is (0.5, 1.0).
 *
 * Example invocation:
 *
 * bin/run-example mllib.CosineSimilarity \
 * --threshold 0.1 data/mllib/sample_svm_data.txt
 *
 * 使用余弦相似度计算矩阵的相似列。
 * 输入矩阵必须以面向行的密集格式存储，每行一行，其条目由空格分隔。例如，
 * 0.5 1.0
 * 2.0 3.0
 * 4.0 5.0
 *
 * 表示一个 3×2 矩阵，其第一行是 (0.5, 1.0)。
 * 示例调用：
 * bin/run-example mllib.CosineSimilarity \ --threshold 0.1 data/mllib/sample_svm_data.txt
 */
object CosineSimilarityBak {
  case class Params(inputFile: String = null, threshold: Double = 0.1) extends AbstractParams[Params]

  def main(args: Array[String]): Unit = {
    val defaultParams: Params = Params()

    val parser: OptionParser[Params] = new OptionParser[Params]("CosineSimilarity") {
      head("CosineSimilarity: an example app.")
      opt[Double]("threshold")
        .required()
        .text(s"threshold similarity: to tradeoff computation vs quality estimate")
        .action((x, c) => c.copy(threshold = x))
      arg[String]("<inputFile>")
        .required()
        .text(s"input file, one row per line, space-separated")
        .action((x, c) => c.copy(inputFile = x))
      note(
        """
          |For example, the following command runs this app on a dataset:
          |
          | ./bin/spark-submit  --class org.apache.spark.examples.mllib.CosineSimilarity \
          | examplesjar.jar \
          | --threshold 0.1 data/mllib/sample_svm_data.txt
        """.stripMargin)
    }

    parser.parse(args, defaultParams) match {
      case Some(params) => run(params)
      case _ => sys.exit(1)
    }
  }

  def run(params: Params): Unit = {
    Logger.getLogger("org").setLevel(Level.WARN)
    val conf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("CosineSimilarity")
    val sc = new SparkContext(conf)

    // Load and parse the data file.
    val rows: RDD[linalg.Vector] = sc.textFile(params.inputFile).map { line =>
      val values: Array[Double] = line.split(' ').map(_.toDouble)
      Vectors.dense(values)
    }.cache()
    val mat = new RowMatrix(rows)

    // Compute similar columns perfectly, with brute force.
    val exact: CoordinateMatrix = mat.columnSimilarities()

    // Compute similar columns with estimation using DIMSUM

    val approx: CoordinateMatrix = mat.columnSimilarities(params.threshold)

    val exactEntries: RDD[((Long, Long), Double)] = exact.entries.map { case MatrixEntry(i, j, u) => ((i, j), u) }

    val approxEntries: RDD[((Long, Long), Double)] = approx.entries.map { case MatrixEntry(i, j, v) => ((i, j), v) }

    val MAE: Double = exactEntries.leftOuterJoin(approxEntries).values.map {
      case (u, Some(v)) =>
        math.abs(u - v)
      case (u, None) =>
        math.abs(u)
    }.mean()

    println(s"Average absolute error in estimate is: $MAE")

    sc.stop()
  }
}
// scalastyle:on println
