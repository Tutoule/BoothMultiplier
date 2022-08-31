package Multiplier

import chisel3._
import chisel3.util._



class Compressor42Unit(val w: Int) extends Module {
  val io = IO(new Bundle() {
    val p0: UInt = Input(UInt(w.W))
    val p1: UInt = Input(UInt(w.W))
    val p2: UInt = Input(UInt(w.W))
    val p3: UInt = Input(UInt(w.W))
    val cin: UInt = Input(UInt(w.W))
    val s: UInt = Output(UInt(w.W))
    val ca: UInt = Output(UInt(w.W))
    val cout: UInt = Output(UInt(w.W))
  })
  val xor0: UInt = Wire(UInt(w.W))
  val xor1: UInt = Wire(UInt(w.W))
  val xor2: UInt = Wire(UInt(w.W))

  xor0 := io.p0 ^ io.p1
  xor1 := io.p2 ^ io.p3
  xor2 := xor1 ^ xor0

  io.cout := xor0 & io.p2 | ((~xor0).asUInt & io.p0)
  io.s := xor2 ^ io.cin
  io.ca := xor2 & io.cin | ((~xor2).asUInt & io.p3)
}
class Compressor42(val w: Int) extends Module {
  val io = IO(new Bundle() {
    val p0: UInt = Input(UInt(w.W))
    val p1: UInt = Input(UInt(w.W))
    val p2: UInt = Input(UInt(w.W))
    val p3: UInt = Input(UInt(w.W))
    val s: UInt = Input(UInt(w.W))
    val ca: UInt = Input(UInt(w.W))
  })
  val compressor42Unit: Compressor42Unit = Module(new Compressor42Unit(w))
  compressor42Unit.io.p0 := io.p0
  compressor42Unit.io.p1 := io.p1
  compressor42Unit.io.p2 := io.p2
  compressor42Unit.io.p3 := io.p3
  compressor42Unit.io.cin := Cat(compressor42Unit.io.cout(w-2,0), 0.U(1.W))
  io.s := compressor42Unit.io.s
  io.ca := compressor42Unit.io.ca
}

object Compressor42{
  def apply(p: Vec[Value]): CompressorOutput = {
    val offsets: Seq[Int] = for(l <- p) yield l.offset
    val offsetMin: Int = offsets.min
    val width: Seq[Int] = for(w <- p) yield w.value.getWidth
    val length: Seq[Int] = for(l <- offsets.zip(width)) yield l._1 + l._2
    val lengthSorted: Seq[Int] = length.sorted
    val widthMax: Int = if(lengthSorted(3) > lengthSorted(0) && lengthSorted(3) > lengthSorted(1)) {
      lengthSorted(3) - offsetMin
    }else{
      lengthSorted(3) - offsetMin + 1
    }
    // Sort p by length
    val pSorted: Seq[(Value, Int)] = p.zip(length).sortBy(p0 => p0._2)

    val compressor42: Compressor42 = Module(new Compressor42(widthMax))
    compressor42.io.p0 := pSorted(0)._1.value
    compressor42.io.p1 := Cat(pSorted(1)._1.value, Fill(pSorted(1)._1.offset-offsetMin, 0.U(1.W)))
    compressor42.io.p2 := Cat(pSorted(2)._1.value, Fill(pSorted(2)._1.offset-offsetMin, 0.U(1.W)))
    compressor42.io.p3 := Cat(pSorted(3)._1.value, Fill(pSorted(3)._1.offset-offsetMin, 0.U(1.W)))
    val compressorOutput: CompressorOutput = new CompressorOutput(widthMax)
    compressorOutput.s.value := compressor42.io.s
    compressorOutput.ca.value := compressor42.io.ca
    compressorOutput.s.offset = offsetMin
    compressorOutput.ca.offset = offsetMin + 1
    compressorOutput
  }

  def apply(p0: Value, p1: Value, p2: Value, p3: Value): CompressorOutput = {
    val p = VecInit(p0, p1, p2, p3)
    apply(p)
  }
}