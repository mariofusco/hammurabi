package hammurabi.util

import org.slf4j.LoggerFactory

/**
 * @author Mario Fusco
 */
trait Logger {

  private lazy val logger = {
    val name  = getClass.getName
    val end   = name.indexOf('$')
    val name2 = name.substring(0, if (end == -1) name.size else end)
    LoggerFactory.getLogger(name2)
  }

  def trace(msg: => String): Unit = if (logger.isTraceEnabled) logger.trace(msg)
  def trace(msg: => String, err: Throwable): Unit = if (logger.isTraceEnabled) logger.trace(msg, err)

  def debug(msg: => String): Unit = if (logger.isDebugEnabled) logger.debug(msg)
  def debug(msg: => String, err: Throwable): Unit = if (logger.isDebugEnabled) logger.debug(msg, err)

  def info(msg: => String): Unit = if (logger.isInfoEnabled) logger.info(msg)
  def info(msg: => String, err: Throwable): Unit = if (logger.isInfoEnabled) logger.info(msg, err)

  def warn(msg: => String): Unit = if (logger.isWarnEnabled) logger.warn(msg)
  def warn(msg: => String, err: Throwable): Unit = if (logger.isWarnEnabled) logger.warn(msg, err)

  def error(msg: String): Unit = logger.error(msg)
  def error(msg: String, err: Throwable): Unit = logger.error(msg, err)

  def logExceptions(action: => Unit): Unit =
    try {
      action
    } catch {
      case e: Exception => error(e.getMessage, e)
    }
}
