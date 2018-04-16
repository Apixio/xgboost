/*
 Copyright (c) 2014 by Contributors

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package ml.dmlc.xgboost4j.scala.spark

class XGBoostTrainingSummary private(
    val trainObjectiveHistory: Array[Float],
    val testObjectiveHistory: Option[Array[Float]]
) extends Serializable {
  override def toString: String = {
    val train = trainObjectiveHistory.toList
    val test = testObjectiveHistory.map(_.toList)
    s"XGBoostTrainingSummary(trainObjectiveHistory=$train, testObjectiveHistory=$test)"
  }

  def bestIter: Int = {
    if (testObjectiveHistory.isDefined) {
      // we want to avoid picking the 0 values which are presumable a result of padding
      testObjectiveHistory.get.map(x => if (x < 0.0000001f) Float.MaxValue else x).
        zipWithIndex.minBy(_._1)._2
    } else {
      0
    }
  }
}

private[xgboost4j] object XGBoostTrainingSummary {
  def apply(metrics: Map[String, Array[Float]]): XGBoostTrainingSummary = {
    new XGBoostTrainingSummary(
      trainObjectiveHistory = metrics("train"),
      testObjectiveHistory = metrics.get("test"))
  }
}
