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
package org.apache.spark.examples.mllib

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.linalg.distributed.RowMatrix
import org.apache.spark.mllib.linalg.Vectors

/**
 * Compute the singular value decomposition (SVD) of a tall-and-skinny matrix.
 *
 * The input matrix must be stored in row-oriented dense format, one line per row with its entries
 * separated by space. For example,
 * {{{
 * 0.5 1.0
 * 2.0 3.0
 * 4.0 5.0
 * }}}
 * represents a 3-by-2 matrix, whose first row is (0.5, 1.0).
 * 计算高瘦矩阵的奇异值分解 (SVD)。
 * 输入矩阵必须以面向行的密集格式存储，每行一行，其条目由空格分隔。例如，
 * 0.5 1.0
 * 2.0 3.0
 * 4.0 5.0
 *
 * 表示一个 3×2 矩阵，其第一行是 (0.5, 1.0)。
 */
object TallSkinnySVD {
  def main(args: Array[String]) {
    if (args.length != 1) {
      System.err.println("Usage: TallSkinnySVD <input>")
      System.exit(1)
    }

    val conf = new SparkConf().setMaster("local[*]").setAppName("TallSkinnySVD")
    val sc = new SparkContext(conf)

    // Load and parse the data file.
    val rows = sc.textFile(args(0)).map { line =>
      val values = line.split(' ').map(_.toDouble)
      Vectors.dense(values)
    }
    val mat = new RowMatrix(rows)

    // Compute SVD.
    val svd = mat.computeSVD(mat.numCols().toInt)

    println("Singular values are " + svd.s)

    sc.stop()
  }
}
// scalastyle:on println
