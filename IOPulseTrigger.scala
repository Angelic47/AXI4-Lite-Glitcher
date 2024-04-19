package projectname

import spinal.core._

case class IOPulseTrigger() extends Component {
  val io = new Bundle {
    val in_trigger = in Bool()
    val in_trigger_clr = in Bool()
    val out_trigger = out Bool()
  }

  val reg_out_trigger = Reg(Bool()) init False
  when(io.in_trigger_clr === True) {
    reg_out_trigger := False
  }.elsewhen(io.in_trigger === True) {
    reg_out_trigger := True
  }

  io.out_trigger := reg_out_trigger
}
