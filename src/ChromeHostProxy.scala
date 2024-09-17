//> using scala 3.3.3

import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import scala.sys.process.*
import java.io.{OutputStream, InputStream, PrintWriter}
import scala.io.Source

object ChromeHostProxy {

  val cmd = Array("TODO")
  val log = new PrintWriter(new File("TODO/proxy.log"))

  def debug(message: String) = {
    log.append(message)
    log.flush()
  }

  def readFrame(direction: String, log: PrintWriter, in: InputStream): Array[Byte] = {
    val order  = ByteOrder.nativeOrder()
    val buff   = new BufferedInputStream(in)
    val header = buff.readNBytes(4)
    val len    = ByteBuffer.wrap(header).order(order).getInt()
    debug(s"[$direction] LEN\n${len}\n")

    val body    = buff.readNBytes(len)
    val message = new String(body)
    debug(s"[$direction] MSG\n${message}\n")

    header ++ body
  }

  def send(message: Array[Byte])(channel: OutputStream) = {
    channel.write(message)
    channel.flush()
    channel.close() // Important to close the stream so the process knows you're done
  }

  def receive(channel: InputStream) = {
    val bytes = readFrame("RCV", log, channel)
    System.out.write(bytes)
  }

  def err(channel: InputStream) = {
    debug(s"ERR = ${Source.fromInputStream(channel).mkString}\n")
  }
  
  def main(args: Array[String]) = {
    debug(s"\nSTART (${args.mkString(", ")})\n")
    while(true) {
      val received = readFrame("SND", log, System.in)
      val io = new ProcessIO(send(received), receive, err, false)
      val proc = Process(cmd ++ args).run(io)
      val result = proc.exitValue()
      debug(s"RESULT = $result\n")
    }
    debug("DONE\n")
    ()
  }
}