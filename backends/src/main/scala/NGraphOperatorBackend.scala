package org.emergentorder.onnx.backends

import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file._

import scala.{specialized => sp}
import scala.collection.mutable.{Map => MMap};
import scala.reflect.ClassTag
import spire.implicits._
import spire.math.Numeric
import spire.math.UByte
import spire.math.UShort
import spire.math.UInt
import spire.math.ULong
import spire.math.Complex

import org.emergentorder.onnx._
import org.emergentorder.union._
import org.bytedeco.javacpp._;
import org.bytedeco.onnx.ModelProto;
import org.bytedeco.onnx.global.onnx.ParseProtoFromBytes;
import org.bytedeco.onnx.MessageLite;
import org.bytedeco.onnx.NodeProto;
import org.bytedeco.onnx.GraphProto
import org.bytedeco.ngraph.global._
import ngraph.import_onnx_model
import org.bytedeco.ngraph.Backend

// TODO: check import org.bytedeco.onnx.global.onnx.check_model

//TODEFER: ONNX-JS backend for both JS and JVM
//TODEFER: ONNX Runtime backend for JVM (and Native?)
trait NGraphOperatorBackend extends Operator with NGraphBackendUtils with AutoCloseable {

  val scope = new PointerScope()

  val ngraphBackend = Backend.create("CPU")

  def opToONNXBytes[
      T: ClassTag,
      T1: ClassTag,
      T2: ClassTag,
      T3: ClassTag,
      T4: ClassTag,
      T5: ClassTag,
      T6: ClassTag,
      T7: ClassTag,
      T8: ClassTag
  ](
      name: String,
      opName: String,
      inputs: Tuple9[T, T1, T2, T3, T4, T5, T6, T7, T8],
      outName: String,
      attrs: Map[String, Any]
  ): Array[Byte] = {

    val model = (new ModelProto).New()
    val graph = new org.bytedeco.onnx.GraphProto
    model.set_producer_name("ONNX-Scala")
    graph.set_name(name)

    //TODO: pass real names
    val origNode = opToNode(name, opName, inputs, outName, attrs)

    val node = graph.add_node
    node.MergeFrom(origNode)

    origNode.close
    model.set_allocated_graph(graph)
    model.set_ir_version(3)

    model.add_opset_import
    model.opset_import(0).set_version(8)

    val outputValueInfo = graph.add_output

    outputValueInfo.set_name(outName)

    outputValueInfo.mutable_type
    outputValueInfo.`type`.mutable_tensor_type
    outputValueInfo.`type`.tensor_type.set_elem_type(1)

    addInputToGraph(inputs._1, "A", graph)
    addInputToGraph(inputs._2, "B", graph)
    addInputToGraph(inputs._3, "C", graph)
    addInputToGraph(inputs._4, "D", graph)
    addInputToGraph(inputs._5, "E", graph)
    addInputToGraph(inputs._6, "F", graph)
    addInputToGraph(inputs._7, "G", graph)
    addInputToGraph(inputs._8, "H", graph)
    addInputToGraph(inputs._9, "I", graph)

    val modelString = model.SerializeAsString
    model.close
    val modelStringBytes = modelString.getStringBytes
    modelString.close

    (modelStringBytes)
  }

  def callByteArrayOp[
      T: ClassTag,
      T1: ClassTag,
      T2: ClassTag,
      T3: ClassTag,
      T4: ClassTag,
      T5: ClassTag,
      T6: ClassTag,
      T7: ClassTag,
      T8: ClassTag,
      T9: ClassTag,
      T10: ClassTag,
      T11: ClassTag,
      T12: ClassTag,
      T13: ClassTag,
      T14: ClassTag,
      T15: ClassTag,
      T16: ClassTag,
      T17: ClassTag
  ](
      opModel: Array[Byte],
      inputs: Tuple9[T, T1, T2, T3, T4, T5, T6, T7, T8]
  ): (T9) = {
    val modelString = new BytePointer(opModel: _*)

    val ngraphFunc = import_onnx_model(modelString)
    modelString.close

    callNGraphFuncOp[T, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](
      ngraphFunc,
      inputs
    )
  }

  def callNGraphFuncOp[
      T: ClassTag,
      T1: ClassTag,
      T2: ClassTag,
      T3: ClassTag,
      T4: ClassTag,
      T5: ClassTag,
      T6: ClassTag,
      T7: ClassTag,
      T8: ClassTag,
      T9: ClassTag,
      T10: ClassTag,
      T11: ClassTag,
      T12: ClassTag,
      T13: ClassTag,
      T14: ClassTag,
      T15: ClassTag,
      T16: ClassTag,
      T17: ClassTag
  ](
      ngraphFunc: org.bytedeco.ngraph.Function,
      inputs: Tuple9[T, T1, T2, T3, T4, T5, T6, T7, T8]
  ): (T9) = {
    val scope = new PointerScope()

    val inputShapes = Seq(
      getTensorShape(inputs._1),
      getTensorShape(inputs._2),
      getTensorShape(inputs._3),
      getTensorShape(inputs._4),
      getTensorShape(inputs._5),
      getTensorShape(inputs._6),
      getTensorShape(inputs._7),
      getTensorShape(inputs._8),
      getTensorShape(inputs._9)
    ).flatten

    val outputShape = ngraphFunc.get_output_shape(0)
    val outputType  = ngraphFunc.get_output_element_type(0)

    val inputTensors = Seq(
      getTensorPointerAndType(inputs._1),
      getTensorPointerAndType(inputs._2),
      getTensorPointerAndType(inputs._3),
      getTensorPointerAndType(inputs._4),
      getTensorPointerAndType(inputs._5),
      getTensorPointerAndType(inputs._6),
      getTensorPointerAndType(inputs._7),
      getTensorPointerAndType(inputs._8),
      getTensorPointerAndType(inputs._9)
    ).flatten

    val ngraphInputs =
      (inputShapes zip inputTensors).map(x => ngraphBackend.create_tensor(x._2._2, x._1, x._2._1))

    val output = ngraphBackend.create_tensor(outputType, outputShape)

    val inputVector = new org.bytedeco.ngraph.TensorVector(ngraphInputs: _*)

    val outputVector = new org.bytedeco.ngraph.TensorVector(output)

    val executable = ngraphBackend.compile(ngraphFunc)

    def t = {
      val before = System.nanoTime
      executable.call(outputVector, inputVector)
      val after = System.nanoTime

      executable.close
//      println("Elapsed per Op: " + "  : " + (after - before))
    }

    t

    ngraphFunc.close
    executable.close

    //convert result to onnx-scala Tensor

    val result = tensorVectorToOutputTensor[T9](outputVector, outputShape)

    inputTensors.foreach { x: (Pointer, org.bytedeco.ngraph.Type) =>
      x._1.close //close pointers
      x._2.close //close shapes
    }

    inputShapes.foreach { x: org.bytedeco.ngraph.Shape =>
      x.close
    }

    ngraphInputs.foreach { x: org.bytedeco.ngraph.Tensor =>
      x.close
    }

    outputType.close
    inputVector.close
    output.close
    outputVector.close
    outputShape.close
    scope.close
    (result)
  }

  def callOp[
      T: ClassTag,
      T1: ClassTag,
      T2: ClassTag,
      T3: ClassTag,
      T4: ClassTag,
      T5: ClassTag,
      T6: ClassTag,
      T7: ClassTag,
      T8: ClassTag,
      T9: ClassTag,
      T10: ClassTag,
      T11: ClassTag,
      T12: ClassTag,
      T13: ClassTag,
      T14: ClassTag,
      T15: ClassTag,
      T16: ClassTag,
      T17: ClassTag
  ](
      name: String,
      opName: String,
      inputs: Tuple9[T, T1, T2, T3, T4, T5, T6, T7, T8],
      //    outName: String,
      attrs: Map[String, Any]
  ): (T9) = {
    //TODO: Separate out
    val onnxBytes = opToONNXBytes(name, opName, inputs, "outName", attrs)
    callByteArrayOp[T, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17](
      onnxBytes,
      inputs
    )
  }

  override def close(): Unit = {
    ngraphBackend.close
    scope.close
    super.close
  }
}