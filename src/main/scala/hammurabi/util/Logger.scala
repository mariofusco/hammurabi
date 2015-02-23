package hammurabi.util
/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
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
