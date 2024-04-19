package projectname

import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axilite.{AxiLite4, AxiLite4Config, AxiLite4SlaveFactory}

case class GlitchCmdStruct(glitchCounterWidth: Int, glitchPulseWidth: Int) extends Bundle {
  val cnt_offset = UInt(glitchCounterWidth bits)
  val cnt_pulse = UInt(glitchPulseWidth bits)
  val force_trigger = Bool()
}

case class AxiLite4GlitchGeneratorAsyncCmdFifo_SDIOSniffer(axiLite4Config: AxiLite4Config, glitchCounterWidth: Int, glitchPulseWidth: Int, glitchClock: ClockDomain = null) extends Component {
  val io = new Bundle {
    val axi4_slave = slave(AxiLite4(axiLite4Config))
    val axi4_sdio_slave = slave(AxiLite4(axiLite4Config))
    val out_glitch = out Bool()

    // from sdio clock domain
    val sdio_sclk = in Bool()
    val sdio_cmd = in Bool()
  }

  val inst_ClockDomain: ClockDomain = clockDomain
  var inst_glitchClock: ClockDomain = glitchClock
  if(glitchClock == null) {
    inst_glitchClock = ClockDomain.external("glitchClock")
  }
  var inst_sdioClock: ClockDomain = ClockDomain(io.sdio_sclk)

  val inst_glitch = AxiLite4GlitchGeneratorAsyncCmdFifo(axiLite4Config, glitchCounterWidth, glitchPulseWidth, inst_glitchClock)
  val inst_trigger = AXI4LiteSDIOSnifferWithTrigger(axiClk = inst_ClockDomain, glitchClk = inst_glitchClock, axiLite4Config = axiLite4Config)

  inst_trigger.io.sdio_sclk := io.sdio_sclk
  inst_trigger.io.sdio_cmd := io.sdio_cmd

  val glitch_area = new ClockingArea(inst_glitchClock) {
    inst_glitch.io.cnt_trigger := inst_trigger.io.trigger_cmd_trigger
    inst_trigger.io.trigger_cmd_clr := inst_glitch.io.out_trigger_clr
    io.out_glitch := inst_glitch.io.out_glitch
  }

  // for simulate: use direct connection instead as there is a bug to simulate pipeline
  io.axi4_slave.writeCmd >/-> inst_glitch.io.axi4_slave.writeCmd
  io.axi4_slave.writeData >/-> inst_glitch.io.axi4_slave.writeData
  io.axi4_slave.writeRsp <-/< inst_glitch.io.axi4_slave.writeRsp
  io.axi4_slave.readCmd >/-> inst_glitch.io.axi4_slave.readCmd
  io.axi4_slave.readRsp <-/< inst_glitch.io.axi4_slave.readRsp

  // for simulate: use direct connection instead as there is a bug to simulate pipeline
  io.axi4_sdio_slave.writeCmd >/-> inst_trigger.io.axi.writeCmd
  io.axi4_sdio_slave.writeData >/-> inst_trigger.io.axi.writeData
  io.axi4_sdio_slave.writeRsp <-/< inst_trigger.io.axi.writeRsp
  io.axi4_sdio_slave.readCmd >/-> inst_trigger.io.axi.readCmd
  io.axi4_sdio_slave.readRsp <-/< inst_trigger.io.axi.readRsp

  //io.axi4_slave <> inst_glitch.io.axi4_slave
  //io.axi4_sdio_slave <> inst_trigger.io.axi
}

case class AxiLite4GlitchGeneratorAsyncCmdFifo_IOPulse(axiLite4Config: AxiLite4Config, glitchCounterWidth: Int, glitchPulseWidth: Int, glitchClock: ClockDomain = null) extends Component {
  val io = new Bundle {
    val axi4_slave = slave(AxiLite4(axiLite4Config))
    val cnt_trigger = in Bool()
    val out_glitch = out Bool()
  }

  var inst_glitchClock: ClockDomain = glitchClock
  if(glitchClock == null) {
    inst_glitchClock = ClockDomain.external("glitchClock")
  }

  val inst_glitch = AxiLite4GlitchGeneratorAsyncCmdFifo(axiLite4Config, glitchCounterWidth, glitchPulseWidth, inst_glitchClock)
  val glitch_area = new ClockingArea(inst_glitchClock) {
    val inst_trigger = IOPulseTrigger()

    inst_trigger.io.in_trigger := io.cnt_trigger
    inst_trigger.io.in_trigger_clr := inst_glitch.io.out_trigger_clr
    inst_glitch.io.cnt_trigger := inst_trigger.io.out_trigger
    io.out_glitch := inst_glitch.io.out_glitch
  }

  /*
  io.axi4_slave.writeCmd >/-> inst_glitch.io.axi4_slave.writeCmd
  io.axi4_slave.writeData >/-> inst_glitch.io.axi4_slave.writeData
  io.axi4_slave.writeRsp <-/< inst_glitch.io.axi4_slave.writeRsp
  io.axi4_slave.readCmd >/-> inst_glitch.io.axi4_slave.readCmd
  io.axi4_slave.readRsp <-/< inst_glitch.io.axi4_slave.readRsp
   */
  io.axi4_slave <> inst_glitch.io.axi4_slave
}

case class AxiLite4GlitchGeneratorAsyncCmdFifo(axiLite4Config: AxiLite4Config, glitchCounterWidth: Int, glitchPulseWidth: Int, glitchClock: ClockDomain = null) extends Component {
  val io = new Bundle {
    val axi4_slave = slave(AxiLite4(axiLite4Config))
    val cnt_trigger = in Bool()
    val out_glitch = out Bool()
    val out_trigger_clr = out Bool()
  }

  var inst_glitchClock: ClockDomain = glitchClock
  if(glitchClock == null) {
    inst_glitchClock = ClockDomain.external("glitchClock")
  }

  val cmdfifo = StreamFifoCC(
    dataType = GlitchCmdStruct(glitchCounterWidth, glitchPulseWidth),
    depth = 32,
    pushClock = clockDomain,
    popClock = inst_glitchClock
  )

  val axiLite4SlaveFactory = AxiLite4SlaveFactory(io.axi4_slave)
  val enable_pulse = Bool()
  enable_pulse := False
  cmdfifo.io.push.valid := False
  axiLite4SlaveFactory.write(enable_pulse, 0x00, 0, "Enable pulse generation")
  axiLite4SlaveFactory.write(cmdfifo.io.push.valid, 0x00, 1, "Fifo push valid")
  axiLite4SlaveFactory.driveAndRead(cmdfifo.io.push.payload.cnt_offset, 0x04, 0, "Glitch counter offset")
  axiLite4SlaveFactory.driveAndRead(cmdfifo.io.push.payload.cnt_pulse, 0x08, 0, "Glitch pulse width")
  axiLite4SlaveFactory.driveAndRead(cmdfifo.io.push.payload.force_trigger, 0x0C, 0, "Force trigger")

  val glitch_area = new ClockingArea(inst_glitchClock) {
    val enable_pulse_cc = BufferCC(enable_pulse)
    val busy = RegInit(False) clearWhen(!cmdfifo.io.pop.valid && cmdfifo.io.pop.ready) setWhen(enable_pulse_cc)
    val inst_glitch = GlitchGenerator(glitchCounterWidth, glitchPulseWidth)
    val force_trigger_reg = RegNextWhen(cmdfifo.io.pop.payload.force_trigger, cmdfifo.io.pop.fire)
    inst_glitch.io.cnt_trigger := io.cnt_trigger | force_trigger_reg
    inst_glitch.io.pulse_len := cmdfifo.io.pop.payload.cnt_pulse
    inst_glitch.io.cnt := cmdfifo.io.pop.payload.cnt_offset
    inst_glitch.io.en := cmdfifo.io.pop.fire
    cmdfifo.io.pop.ready := busy && inst_glitch.io.out_finish
    io.out_glitch := inst_glitch.io.out_glitch
    io.out_trigger_clr := inst_glitch.io.en
  }

  val busy_cc = BufferCC(glitch_area.busy)
  axiLite4SlaveFactory.read(busy_cc, 0x00, 0, "Busy status")
  axiLite4SlaveFactory.read(cmdfifo.io.push.ready, 0x00, 1, "Fifo can accept data (not full)")
}

case class AxiLite4GlitchGeneratorWithReset_IOPulse(axiLite4Config: AxiLite4Config, glitchCounterWidth: Int, glitchPulseWidth: Int) extends Component {
  val io = new Bundle {
    val axi4_slave = slave(AxiLite4(axiLite4Config))
    val cnt_trigger = in Bool()
    val out_glitch = out Bool()
  }

  val inst_trigger = IOPulseTrigger()
  val inst_glitch = AxiLite4GlitchGeneratorWithReset(axiLite4Config, glitchCounterWidth, glitchPulseWidth)

  inst_trigger.io.in_trigger := io.cnt_trigger
  inst_trigger.io.in_trigger_clr := inst_glitch.io.out_trigger_clr
  inst_glitch.io.cnt_trigger := inst_trigger.io.out_trigger
  io.out_glitch := inst_glitch.io.out_glitch

  io.axi4_slave.writeCmd >/-> inst_glitch.io.axi4_slave.writeCmd
  io.axi4_slave.writeData >/-> inst_glitch.io.axi4_slave.writeData
  io.axi4_slave.writeRsp <-/< inst_glitch.io.axi4_slave.writeRsp
  io.axi4_slave.readCmd >/-> inst_glitch.io.axi4_slave.readCmd
  io.axi4_slave.readRsp <-/< inst_glitch.io.axi4_slave.readRsp
}

case class AxiLite4GlitchGeneratorWithReset(axiLite4Config: AxiLite4Config, glitchCounterWidth: Int, glitchPulseWidth: Int) extends Component {
  val io = new Bundle {
    val axi4_slave = slave(AxiLite4(axiLite4Config))
    val cnt_trigger = in Bool()
    val out_glitch = out Bool()
    val out_trigger_clr = out Bool()
  }

  val force_trigger_reg = Reg(Bool()) init False

  val reset_inst = GlitchGenerator(1, glitchPulseWidth)
  reset_inst.io.cnt_trigger := True
  reset_inst.io.cnt := 0

  val inst = GlitchGenerator(glitchCounterWidth, glitchPulseWidth)
  inst.io.cnt_trigger := (io.cnt_trigger & reset_inst.io.out_finish) | force_trigger_reg
  io.out_glitch := inst.io.out_glitch | reset_inst.io.out_glitch

  val axi4_slaveFactory = AxiLite4SlaveFactory(io.axi4_slave)
  io.out_trigger_clr := ~reset_inst.io.out_finish
  inst.io.en := False
  reset_inst.io.en := inst.io.en & ~force_trigger_reg
  // 0x00 -> command register
  //  - write [0] = enable
  //
  //  - read [0] = main glitch finish
  //  - read [1] = reset glitch finish
  //  - read [2] = external trigger signal present
  //  - read [3] = main glitch trigger status
  axi4_slaveFactory.write(inst.io.en, 0x00, 0, "Write 1 to enable the glitch generator")
  axi4_slaveFactory.read(inst.io.out_finish, 0x00, 0, "Main glitch finish flag")
  axi4_slaveFactory.read(reset_inst.io.out_finish, 0x00, 1, "Reset glitch finish flag")
  axi4_slaveFactory.read(io.cnt_trigger, 0x00, 2, "External trigger signal present")
  axi4_slaveFactory.read(inst.io.cnt_trigger, 0x00, 3, "Main glitch trigger status")
  // 0x04 -> main glitch trigger delay counter register
  axi4_slaveFactory.driveAndRead(inst.io.cnt, 0x04, 0, "Main glitch trigger delay counter")
  // 0x08 -> main glitch pulse width register
  axi4_slaveFactory.driveAndRead(inst.io.pulse_len, 0x08, 0, "Main glitch pulse width, CANNOT be 0")
  // 0x0C -> force trigger mode register
  axi4_slaveFactory.driveAndRead(force_trigger_reg, 0x0C, 0, "Force trigger mode flag")
  // 0x10 -> reset glitch pulse width register
  axi4_slaveFactory.driveAndRead(reset_inst.io.pulse_len, 0x10, 0, "Reset glitch pulse width, CANNOT be 0")

  axi4_slaveFactory.printDataModel()
}

case class AxiLite4GlitchGenerator_IOPulse(axiLite4Config: AxiLite4Config, glitchCounterWidth: Int, glitchPulseWidth: Int) extends Component {
  val io = new Bundle {
    val axi4_slave = slave(AxiLite4(axiLite4Config))
    val cnt_trigger = in Bool()
    val out_glitch = out Bool()
  }

  val inst_trigger = IOPulseTrigger()
  val inst_glitch = AxiLite4GlitchGenerator(axiLite4Config, glitchCounterWidth, glitchPulseWidth)

  inst_trigger.io.in_trigger := io.cnt_trigger
  inst_trigger.io.in_trigger_clr := inst_glitch.io.out_trigger_clr
  inst_glitch.io.cnt_trigger := inst_trigger.io.out_trigger
  io.out_glitch := inst_glitch.io.out_glitch

  io.axi4_slave <> inst_glitch.io.axi4_slave
}

case class AxiLite4GlitchGenerator(axiLite4Config: AxiLite4Config, glitchCounterWidth: Int, glitchPulseWidth: Int) extends Component {
  val io = new Bundle {
    val axi4_slave = slave(AxiLite4(axiLite4Config))
    val cnt_trigger = in Bool()
    val out_glitch = out Bool()
    val out_trigger_clr = out Bool()
  }

  val force_trigger_reg = Reg(Bool()) init False

  val inst = GlitchGenerator(glitchCounterWidth, glitchPulseWidth)
  inst.io.cnt_trigger := io.cnt_trigger | force_trigger_reg
  io.out_glitch := inst.io.out_glitch

  val axi4_slaveFactory = AxiLite4SlaveFactory(io.axi4_slave)
  io.out_trigger_clr := False
  inst.io.en := io.out_trigger_clr
  axi4_slaveFactory.write(io.out_trigger_clr, 0x00, 0)
  axi4_slaveFactory.read(inst.io.out_finish, 0x00, 0)

  axi4_slaveFactory.driveAndRead(inst.io.cnt, 0x04, 0)
  axi4_slaveFactory.driveAndRead(inst.io.pulse_len, 0x08, 0)
  axi4_slaveFactory.driveAndRead(force_trigger_reg, 0x0C, 0)
}

case class GlitchGenerator(glitchCounterWidth: Int, glitchPulseWidth: Int) extends Component {
  val io = new Bundle {
    val en = in Bool()
    val cnt = in UInt(glitchCounterWidth bits)
    val cnt_trigger = in Bool()
    val pulse_len = in UInt(glitchPulseWidth bits)
    val out_finish = out Bool()
    val out_glitch = out Bool()
  }

  val reg_cnt = Reg(UInt(glitchCounterWidth bits))
  val reg_pulse_len = Reg(UInt(glitchPulseWidth bits))
  val reg_busy = Reg(Bool()) init False
  val reg_out_finish = Reg(Bool()) init True
  val reg_out_glitch = Reg(Bool()) init False

  io.out_finish := reg_out_finish
  io.out_glitch := reg_out_glitch

  when(reg_busy === False && io.en === True)
  {
    reg_out_finish := False
    reg_out_glitch := False
    reg_cnt := io.cnt
    reg_pulse_len := io.pulse_len
    reg_busy := True
  }

  when(io.cnt_trigger === True && reg_out_glitch === False && reg_busy === True)
  {
    reg_cnt := reg_cnt - 1
  }

  when(io.cnt_trigger === True && reg_cnt === 0 && reg_busy === True)
  {
    reg_out_glitch := True
    reg_pulse_len := reg_pulse_len - 1
  }

  when(reg_out_glitch === True)
  {
    reg_pulse_len := reg_pulse_len - 1
  }

  when(reg_out_glitch === True && reg_pulse_len === 0)
  {
    reg_out_finish := True
    reg_out_glitch := False
    reg_busy := False
  }
}
