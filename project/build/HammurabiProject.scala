/**
 * @author Mario Fusco
 */

import sbt._

class HammurabiProject(info: ProjectInfo) extends DefaultProject(info) {

  val logbackCore = "ch.qos.logback" % "logback-core" % "0.9.28"
  val logbackClassic = "ch.qos.logback" % "logback-classic" % "0.9.28"

  val junit = "junit" % "junit" % "4.7" % "test"
  val scalatest = "org.scalatest" %% "scalatest" % "1.4.1" % "test"

  override def managedStyle = ManagedStyle.Maven
  val publishTo = "Hammurabi repository" at "dav:https://hammurabi.googlecode.com/svn/repo/releases"
  Credentials(Path.userHome / ".ivy2" / "credentials.txt", log)

  override def pomExtra =
    <description>The Scala rule engine</description>
    <url>http://code.google.com/p/hammurabi</url>
    <licenses>
      <license>
        <name>The Apache Software License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <connection>scm:svn:http://hammurabi.googlecode.com/svn/trunk</connection>
      <developerConnection>scm:svn:https://hammurabi.googlecode.com/svn/trunk</developerConnection>
    </scm>
    <distributionManagement>
      <repository>
        <id>googlecode.hammurabi</id>
        <name>Internal Release Repository</name>
        <url>dav:https://hammurabi.googlecode.com/svn/repo/releases</url>
      </repository>
      <snapshotRepository>
        <id>googlecode.hammurabi</id>
        <name>Internal Snapshots Repository</name>
        <url>dav:https://hammurabi.googlecode.com/svn/repo/snapshots</url>
      </snapshotRepository>
    </distributionManagement>
    <build>
      <sourceDirectory>src/main/scala</sourceDirectory>
      <testSourceDirectory>src/test/scala</testSourceDirectory>
      <plugins>
        <plugin>
          <groupId>org.scala-tools</groupId>
          <artifactId>maven-scala-plugin</artifactId>
          <executions>
            <execution>
              <goals>
                <goal>compile</goal>
                <goal>testCompile</goal>
              </goals>
            </execution>
          </executions>
          <configuration>
            <scalaVersion>2.8.1</scalaVersion>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-eclipse-plugin</artifactId>
          <configuration>
            <downloadSources>true</downloadSources>
            <buildcommands>
              <buildcommand>ch.epfl.lamp.sdt.core.scalabuilder</buildcommand>
            </buildcommands>
            <additionalProjectnatures>
              <projectnature>ch.epfl.lamp.sdt.core.scalanature</projectnature>
            </additionalProjectnatures>
            <classpathContainers>
              <classpathContainer>org.eclipse.jdt.launching.JRE_CONTAINER</classpathContainer>
              <classpathContainer>ch.epfl.lamp.sdt.launching.SCALA_CONTAINER</classpathContainer>
            </classpathContainers>
          </configuration>
        </plugin>
      </plugins>
    </build>
    <profiles>
      <profile>
        <id>deploy</id>
        <build>
          <plugins>
            <plugin>
              <groupId>org.apache.maven.plugins</groupId>
              <artifactId>maven-gpg-plugin</artifactId>
              <executions>
                <execution>
                  <id>sign-artifacts</id>
                  <phase>verify</phase>
                  <goals>
                    <goal>sign</goal>
                  </goals>
                </execution>
              </executions>
            </plugin>
          </plugins>
        </build>
      </profile>
    </profiles>
    <reporting>
      <plugins>
        <plugin>
          <groupId>org.scala-tools</groupId>
          <artifactId>maven-scala-plugin</artifactId>
          <configuration>
            <scalaVersion>2.8.1</scalaVersion>
          </configuration>
        </plugin>
      </plugins>
    </reporting>
}