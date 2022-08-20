# Maven-Enforcer-Custom-Plugin
This plugin will raise an exception if there exist more than one versions of specific dependency to avoid diamond dependency conflict.

### Build and Deploy above plugin in artifactory and you below plugin syntax in your pom to access it:

<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>track-commons-version-maven-plugin</artifactId>
  <version>1.0-SNAPSHOT</version>
  <executions>
    <execution>
      <goals>
        <goal>track-commons-version</goal>
      </goals>
    </execution>
  </executions>
  <configuration>
    <ignoreDependency>finicity-direct-integration-common</ignoreDependency>
    <!-- By default the non-unique versions are matched, which means the 
      X.Y-SNAPSHOT instead of the timestamped versions. If you want to use the 
      unique versions of the dependencies, you can set below property to true. -->
    <uniqueVersion>true</uniqueVersion>
  </configuration>
</plugin>
