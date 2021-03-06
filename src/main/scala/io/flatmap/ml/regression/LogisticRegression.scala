package io.flatmap.ml.regression

import breeze.linalg._
import breeze.numerics.log

object LogisticRegression extends RegressionModel with Optimization with LogisticRegressionSupport {

  override def h(theta: Theta): (Features) => Prediction = (X: Features) => (X * theta) map { sigmoid }

  override def computeCost(X: Features, y: Labels, theta: Theta): Double =
    (1.0/y.length) * sum((-y :* log(h(theta)(X))) :- ((1.0 :- y) :* log(1.0 :- h(theta)(X))))

  def sigmoid(z: Double): Double = 1.0 / (1.0 + Math.pow(scala.math.E, -1 * z))

  def costFunction(X: Features, y: Labels, theta: Theta): (Cost, Gradients) = {
    val cost = computeCost(X, y, theta)
    val gradients = Array.tabulate[Double](theta.length)(i => (1.0/y.length) * sum((h(theta)(X) :- y) :* X(::, i)))
    (cost, DenseVector[Double](gradients))
  }

  def linearDecisionBoundaryEval: Unit = {
    val (x, y, theta, m, figure, plotConfig) = initializeLinearModel
    println("Cost at initial theta (zeros): " + computeCost(x, y, theta))
    val newTheta = fminunc((theta: Theta) => costFunction(x, y, theta), theta)
    println(s"New theta found by fminunc (LBFGS): $newTheta")
    Plot.decisionBoundary(x, y, newTheta)(figure, plotConfig)
    val probability = h(newTheta)(DenseVector[Double](1.0, 45.0, 85.0).toDenseMatrix)
    println(s"A student with an Exam 1 score of 45 and an Exam 2 score of 85 has admission probability: $probability")
  }

  def computeCostReg(X: Features, y: Labels, theta: Theta, lambda: Double): Double =
    (1.0/y.length) * sum((-y :* log(h(theta)(X))) :- ((1.0 :- y) :* log(1.0 :- h(theta)(X)))) + (lambda/(2*y.length) * sum(theta(1 to theta.length-1) :^ 2.0))

  def costFunctionReg(X: Features, y: Labels, theta: Theta, lambda: Double): (Cost, Gradients) = {
    val cost = computeCostReg(X, y, theta, lambda)
    val gradient_0 = Array((1.0/y.length) * sum((h(theta)(X) :- y) :* X(::, 0)))
    val gradient_rest = Array.tabulate[Double](theta.length-1) { i => (1.0/y.length) * sum((h(theta)(X) :- y) :* X(::, i.toInt+1)) }
    (cost, DenseVector[Double](gradient_0 ++ gradient_rest))
  }

  def polynomialDecisionBoudaryEval: Unit = {
    val (x, y, theta, m, figure, plotConfig) = initializePolynomialModel
    val lambda = 1
    val xPoly = mapFeatures(x(::, 1), x(::, 2))
    val thetaPoly = DenseVector.zeros[Double](xPoly.cols)
    println("Regularized cost at initial theta (zeros) for higher polynomials: " + computeCostReg(xPoly, y, thetaPoly, lambda))
    val (cost, gradients) = costFunctionReg(xPoly, y, thetaPoly, lambda)
    println("Regularized cost at initial theta (zeros): " + cost)
    println("Gradients at initial theta (zeros): " + gradients)
    val newTheta = fminunc((theta: Theta) => costFunctionReg(xPoly, y, theta, lambda), thetaPoly)
    Plot.decisionBoundary(xPoly, y, newTheta)(figure, plotConfig)
  }

  def main(args: Array[String]): Unit = {
    linearDecisionBoundaryEval
    polynomialDecisionBoudaryEval
  }

}
