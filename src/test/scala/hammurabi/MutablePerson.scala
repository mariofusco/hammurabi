package hammurabi
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
/**
 * @author Mario Fusco
 */

class MutablePerson(n: String) {
  val name = n
  var pos: Int = _
  var color: String = _

  override def toString = name + " is in pos " +
    (if (pos == 0) "unknown" else pos) +
    " with color " +
    (if (color == null) "unknown" else color)

  override def equals(obj: Any) = obj match {
    case p: MutablePerson => p.name == name
    case _ => false
  }

  override def hashCode = name.hashCode
}
