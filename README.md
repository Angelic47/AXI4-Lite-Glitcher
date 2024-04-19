# ⚡ AXI4 Lite Glitcher
<img src="images/banner-gpt.webp" width="600px" alt="logo" align="center"/>

[![License](https://img.shields.io/badge/License-LGPLv3.0-blue.svg)](https://opensource.org/licenses/LGPL-3.0)
[![Language](https://img.shields.io/badge/Language-SpinalHDL-green.svg)](https://spinalhdl.github.io/SpinalDoc-RTD/SpinalHDL/)
[![Platform](https://img.shields.io/badge/Platform-FPGA-red.svg)](https://www.xilinx.com/products/silicon-devices/soc/zynq-7000.html)
[![Performance](https://img.shields.io/badge/Performance-250MHz-yellow.svg)](https://en.wikipedia.org/wiki/Clock_rate)

The AXI4 Lite Glitcher is a fault injection generator with programmable parameters that utilizes the AXI4 Lite bus for communication.  
It is designed to be highly optimized for FPGAs, developed using SpinalHDL, and features significant scalability.  
Tested on the Xilinx-Zynq-7000 series FPGA, the AXI4 Lite Glitcher can precisely generate a fault injection pulse at a working frequency of 250MHz (4ns).

> The AXI4 Lite Glitcher mocks your security boot implementation but never mocks **you**.

## 🔥 Why Choose AXI4 Lite Glitcher
- 🩸 Bleeding-edge technology
- 🚀 High-intensity performance optimization
- ⚙️ Minimal hardware resource usage
- 🔗 Designed expandability
- 🔧 Dynamically configurable parameters
- 🌍 Open source and independently controllable
- 🖥️ Platform-independent

## 💥 Background Story
Fault injection is a common semi-invasive attack technique,  
which operates on the premise: if software-level attacks are impervious, then we might try temporarily disrupting hardware integrity to induce software errors.  
Thus, fault injection came into being.

Typically, this technique requires precise timing to generate a brief fault injection pulse at the optimal moment to disrupt normal hardware operation.  
This moment is usually during critical hardware operations such as password verification, encryption/decryption, integrity checks, state transitions, etc.

Such methods can cause the following faults in target devices:
1. Arithmetic operation disturbances, causing the target device's ALU to produce incorrect results;
2. Destruction of instruction parameters, causing data to be loaded into the wrong registers, such as erroneously into the PC;
3. Interference with instruction decoding, causing the meaning of executed instructions to be incorrect;
4. Destruction of boundary values, inducing errors similar to buffer overflows;
5. Control of jump logic, causing the target to execute incorrect branches.

Although fault injection can lead to unpredictable outcomes, and more often causes device crashes, repeated testing and fuzzy search of parameters can quickly identify an optimal fault injection timing for highly repeatable anticipated attack outcomes.

As this attack method is executed on the hardware, it is often impervious to software-level security mechanisms.
Moreover, this attack cannot be prevented simply by modifying the PCB design.

Now, the AXI4 Lite Glitcher offers you a highly controllable, programmable, high-performance fault injection generator to test and assess the security of your devices.
> You have the right to know if your devices are truly secure.

## 🗂️ File Structure Overview
- `GlitchGenerator.scala`: The main logic of the fault injection generator and includes various AXI implementations.
- `IOPulseTrigger.scala`: Example of a trigger module, IO pulse trigger.
- `SDIOSnifferModule.scala`: Example of a trigger module, SDIO protocol monitor trigger.
- `TimeoutResetGenerator.scala`: An interceptor implementation that automatically resets the chip if it fails to respond within a set timeout to prevent mishaps.

## 📄 License
This project is licensed under LGPLv3.0. For details, please refer to the [LICENSE](LICENSE) file.

----

# ⚡ AXI4 Lite Glitcher
AXI4 Lite Glitcher是一个使用AXI4 Lite总线通讯的、可编程参数的故障注入发生器。  
它被设计为高度针对FPGA优化，使用SpinalHDL进行开发，并高度拥有可拓展性。  
经过测试，在Xilinx-Zynq-7000系列FPGA上，AXI4 Lite Glitcher可以在250MHz (4ns)的工作频率，精准产生一个故障注入脉冲。  

> AXI4 Lite Glitcher嘲笑你的安全启动实现，但从来不会嘲笑**你**”。  

## 🔥 为什么选择AXI4 Lite Glitcher
- 🩸 尖端技术
- 🚀 高强度性能优化
- ⚙️ 少量硬件资源使用
- 🔗 预留可拓展性
- 🔧 参数可动态配置编程
- 🌍 开源自主可控
- 🖥️ 平台无关

## 💥 背景故事
错误注入是一种常见的半侵入式攻击技术，  
它的核心思路是：假如软件层面上攻击是无懈可击的，那么我们可以尝试短暂的破坏硬件完整性，让软件的执行出错。  
因此，错误注入横空出世。  

一般情况下，这项技术要求使用精确的计时，在最佳时机产生一个短暂的故障注入脉冲，破坏硬件的正常运行。  
这个时机通常是在硬件执行关键操作的时候，比如密码验证、加密解密、完整性校验、状态转移等等。  

这样的方法往往会给目标设备造成如下的故障:  
1. 算数运算干扰，让目标设备的ALU产生错误的计算结果;
2. 破坏指令参数，让目标数据装入错误的寄存器，如误装入PC;
3. 干扰指令解码，让目标执行的指令意义出错;
4. 破坏边界数值，让目标产生类似缓冲区溢出的错误;
5. 控制跳转逻辑，让目标执行错误的分支.

虽然故障注入可能导致目标设备产生不可预期的结果，更多的情况是让目标设备崩溃，  
但经过反复测试和参数的模糊搜寻，其实很快就能找到一个最佳的故障注入时机，让目标设备高度可重复的产生预期的攻击结果。  

而这种攻击方式，由于是在硬件上进行攻击，其往往是无法被软件层面的安全机制所防范的。  
不仅如此，该攻击方式也无法通过简单的修改PCB设计等方式来防范。  

现在，AXI4 Lite Glitcher为你提供了一个高度可控的、可编程参数的、高性能的故障注入发生器，供你测试和评估自己的设备的安全性。  
> 你有权利知道自己的设备到底是不是真正安全的。

## 🗂️ 文件结构介绍
- `GlitchGenerator.scala`: 故障注入发生器的主要逻辑, 以及包含多种AXI实现方式.
- `IOPulseTrigger.scala`: 触发器模块例子，IO脉冲触发器.
- `SDIOSnifferModule.scala`: 触发器模块例子，SDIO协议监控触发器.
- `TimeoutResetGenerator.scala`: 一个拦截器实现，用于在芯片超时未回复时，自动重置芯片，以免发生意外.

## 📄 License
该项目使用了LGPLv3.0协议，详情请参考[LICENSE](LICENSE)文件。  