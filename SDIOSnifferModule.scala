package projectname

import spinal.core._
import spinal.lib.bus.amba4.axilite._
import spinal.lib._

case class AXI4LiteSDIOSnifferWithTrigger(axiLite4Config: AxiLite4Config, axiClk: ClockDomain, glitchClk: ClockDomain) extends Component {
  val io = new Bundle {
    val axi = slave(AxiLite4(axiLite4Config))

    // from glitch clock domain
    val trigger_cmd_clr = in Bool()
    val trigger_cmd_trigger = out Bool()

    // from sdio clock domain
    val sdio_sclk = in Bool()
    val sdio_cmd = in Bool()
  }

  val area_glitch = new ClockingArea(glitchClk) {
    val trigger_inst = SDIOTrigger()
    trigger_inst.io.trigger_cmd_clr := io.trigger_cmd_clr
    io.trigger_cmd_trigger := trigger_inst.io.trigger_cmd_trigger
  }

  val resultFifo = StreamFifoCC(
    dataType = Bits(48 bits),
    depth = 8,
    pushClock = area_glitch.trigger_inst.sdio_clk,
    popClock = axiClk
  )

  val area_sdio = new ClockingArea(area_glitch.trigger_inst.sdio_clk) {
    area_glitch.trigger_inst.io.sdio_sclk := io.sdio_sclk
    area_glitch.trigger_inst.io.sdio_cmd := io.sdio_cmd
    resultFifo.io.push.valid := area_glitch.trigger_inst.io.finish_pulse && area_glitch.trigger_inst.io.cmd_triggered
    resultFifo.io.push.payload := area_glitch.trigger_inst.io.cmd_bits
  }

  val area_axi = new ClockingArea(axiClk) {
    val axiLite4SlaveFactory = AxiLite4SlaveFactory(io.axi)
    resultFifo.io.pop.ready := False
    axiLite4SlaveFactory.read(resultFifo.io.pop.valid, 0x0, 0, "Result FIFO has data")
    axiLite4SlaveFactory.write(resultFifo.io.pop.ready, 0x0, 0, "Result FIFO next element")
    axiLite4SlaveFactory.read(resultFifo.io.pop.payload(31 downto 0), 0x4, 0, "Result FIFO data bits 31:0")
    axiLite4SlaveFactory.read(resultFifo.io.pop.payload(47 downto 32), 0x8, 0, "Result FIFO data bits 47:32")
  }
}

case class SDIOTrigger() extends Component {
  val io = new Bundle {
    val trigger_cmd_clr = in Bool()
    val trigger_cmd_trigger = out Bool()

    // from sdio clock domain
    val sdio_sclk = in Bool()
    val sdio_cmd = in Bool()
    val finish_pulse = out Bool()
    val cmd_bits = out Bits(48 bits)
    val cmd_triggered = out Bool()
  }

  val sdio_clk = ClockDomain(clock = io.sdio_sclk, reset = io.trigger_cmd_clr, config = ClockDomainConfig(
    clockEdge = RISING,
    resetKind = ASYNC,
    resetActiveLevel = HIGH
  ))

  val sdio_sniffer = new ClockingArea(sdio_clk) {
    val sniffer = SDIOSnifferModule()

    val trig_flag_cmd18_reg = Reg(Bool()) init False
    val trig_flag_stop_transmission_reg = Reg(Bool()) init False

    sniffer.io.sdio_cmd := io.sdio_cmd
    io.finish_pulse := sniffer.io.finish_pulse
    io.cmd_bits := sniffer.io.cmd_bits

    when(sniffer.io.finish_pulse && sniffer.io.cmd_bits(46 downto 40) === 0x52) { // CMD18 Host to card READ_MULTIPLE_BLOCK
      trig_flag_cmd18_reg := True
    }
    when(trig_flag_cmd18_reg && sniffer.io.finish_pulse && sniffer.io.cmd_bits(46 downto 40) === 0x4C) { // CMD12 Host to card STOP_TRANSMISSION
      trig_flag_stop_transmission_reg := True
    }
    io.cmd_triggered := trig_flag_cmd18_reg && trig_flag_stop_transmission_reg
  }
  io.trigger_cmd_trigger := RegNext(io.cmd_triggered, False) init False addTag(crossClockDomain)
}

case class SDIOSnifferModule() extends Component {

  val io = new Bundle {
    val sdio_cmd = in Bool()
    val finish_pulse = out Bool()
    val cmd_bits = out Bits(48 bits)
  }

  val cmd_bits_reg = Reg(Bits(48 bits))
  val start_flag_reg = Reg(Bool()) init False
  val cmd_bits_cnt_reg = Reg(UInt(log2Up(47) bits)) init 0
  val finish_pulse_reg = Reg(Bool()) init False

  io.finish_pulse := finish_pulse_reg
  io.cmd_bits := cmd_bits_reg

  cmd_bits_reg := cmd_bits_reg(46 downto 0) ## io.sdio_cmd
  when(io.sdio_cmd === False) { // Start bit always 0
    start_flag_reg := True
  }
  when(start_flag_reg === True) {
    cmd_bits_cnt_reg := cmd_bits_cnt_reg + 1
  } otherwise {
    cmd_bits_cnt_reg := 0
    finish_pulse_reg := False
  }
  when(cmd_bits_cnt_reg === 46) {
    start_flag_reg := False
    finish_pulse_reg := True
  }

}
