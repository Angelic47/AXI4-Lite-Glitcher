package projectname

import spinal.core._
import spinal.lib.bus.amba4.axilite.{AxiLite4, AxiLite4Config, AxiLite4SlaveFactory}
import spinal.lib.slave

case class AxiLite4GlitchTimeoutResetGenerator(axiLite4Config: AxiLite4Config, timeoutBits: Int, glitchClock: ClockDomain) extends Component {
  val io = new Bundle {
    val axi4_slave = slave(AxiLite4(axiLite4Config))
    val startTrigger = in Bool()
    val endTrigger = in Bool()
    val out_reset = out Bool()
  }

  val axiLite4SlaveFactory = AxiLite4SlaveFactory(io.axi4_slave)
  val axi_resetGen = Bool()
  val axi_cntReset = Bool()
  val axi_resetBypass = Bool()
  val axi_resetStartTriggerEnable = Bool()
  val axi_resetEndTriggerEnable = Bool()
  val axi_timerSettingVal = UInt(timeoutBits bits)
  val axi_timerRealVal = UInt(timeoutBits bits)
  val axi_flagIsStart = Bool()
  val axi_flagIsFirst = Bool()
  val axi_flagIsResetGen = Bool()

  val glitchClockArea = new ClockingArea(glitchClock) {
    val timeoutResetGenerator = TimeoutResetGenerator(timeoutBits)

    // As for the timing is not critical, we can use the crossClockDomain directly
    timeoutResetGenerator.io.inputReset := axi_resetGen
    axi_resetGen.addTag(crossClockDomain)
    timeoutResetGenerator.io.timerValSetting := axi_timerSettingVal
    axi_timerSettingVal.addTag(crossClockDomain)

    axi_timerRealVal := timeoutResetGenerator.io.timerValReal
    axi_timerRealVal.addTag(crossClockDomain)

    timeoutResetGenerator.io.startTrigger := io.startTrigger & axi_resetStartTriggerEnable
    timeoutResetGenerator.io.endTrigger := io.endTrigger & axi_resetEndTriggerEnable
    axi_resetStartTriggerEnable.addTag(crossClockDomain)
    axi_resetEndTriggerEnable.addTag(crossClockDomain)

    timeoutResetGenerator.io.cntReset := axi_cntReset
    axi_cntReset.addTag(crossClockDomain)

    axi_flagIsFirst := timeoutResetGenerator.io.flagIsFirst
    axi_flagIsFirst.addTag(crossClockDomain)

    axi_flagIsStart := timeoutResetGenerator.io.flagIsStart
    axi_flagIsStart.addTag(crossClockDomain)

    axi_flagIsResetGen := timeoutResetGenerator.io.flagIsResetGen
    axi_flagIsResetGen.addTag(crossClockDomain)

    when(axi_resetBypass) {
      io.out_reset := axi_resetGen
    }.otherwise {
      io.out_reset := timeoutResetGenerator.io.outputReset
    }
  }

  axiLite4SlaveFactory.driveAndRead(axi_resetGen, 0x00, 0, "Reset Signal Main Control")
  axiLite4SlaveFactory.driveAndRead(axi_resetBypass, 0x00, 1, "Reset Signal Bypass")
  axiLite4SlaveFactory.driveAndRead(axi_cntReset, 0x00, 2, "Timer Internal Logic Reset")
  axiLite4SlaveFactory.driveAndRead(axi_timerSettingVal, 0x04, 0, "Timer Limit Setting Value")
  axiLite4SlaveFactory.read(axi_timerRealVal, 0x08, 0, "Timer Real Value")
  axiLite4SlaveFactory.driveAndRead(axi_resetStartTriggerEnable, 0x0C, 0, "Start Trigger Enable")
  axiLite4SlaveFactory.driveAndRead(axi_resetEndTriggerEnable, 0x0C, 1, "End Trigger Enable")
  axiLite4SlaveFactory.read(axi_flagIsStart, 0x0C, 2, "Status Flag IS_START: Is timer counting now")
  axiLite4SlaveFactory.read(axi_flagIsFirst, 0x0C, 3, "Status Flag IS_FIRST: Is timer ready to trigger")
  axiLite4SlaveFactory.read(axi_flagIsResetGen, 0x0C, 4, "Status Flag IS_RESET_GEN: Is reset signal by timer generated")
}

case class TimeoutResetGenerator(timeoutBits: Int) extends Component {
  val io = new Bundle {
    val inputReset = in Bool()
    val cntReset = in Bool()
    val startTrigger = in Bool()
    val endTrigger = in Bool()
    val timerValSetting = in UInt (timeoutBits bits)
    val timerValReal = out UInt (timeoutBits bits)
    val outputReset = out Bool()
    val flagIsStart = out Bool()
    val flagIsFirst = out Bool()
    val flagIsResetGen = out Bool()
  }

  val timerReg = Reg(UInt(timeoutBits bits))
  val outputResetReg = Reg(Bool()) init False
  val isStart = Reg(Bool()) init False
  val isFirst = Reg(Bool()) init True

  when(io.cntReset) {
    timerReg := 0
    isStart := False
    isFirst := True
    outputResetReg := False
  }.otherwise{
    when(io.timerValSetting === io.timerValReal) {
      outputResetReg := True
      isStart := False
    }
    when(io.startTrigger === True && isFirst === True) {
      isStart := True
      isFirst := False
    }

    when(io.endTrigger === True) {
      isStart := False
    }

    when(isStart) {
      timerReg := timerReg + 1
    }
  }

  io.timerValReal := timerReg
  io.outputReset := outputResetReg | io.inputReset
  io.flagIsResetGen := outputResetReg
  io.flagIsStart := isStart
  io.flagIsFirst := isFirst
}
