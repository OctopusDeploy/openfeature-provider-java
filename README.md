# Octopus Deploy OpenFeature provider for Java  

The OctopusDeploy [OpenFeature provider
](https://openfeature.dev/docs/reference/concepts/provider/) for Java.

This provider works with the OpenFeature Server-Side SDK. It is not intended for use in clients such as browser or desktop applications. 

## About Octopus Deploy 

[Octopus Deploy](https://octopus.com) is a sophisticated, best-of-breed continuous delivery (CD) platform for modern software teams. Octopus offers powerful release orchestration, deployment automation, and runbook automation, while handling the scale, complexity and governance expectations of even the largest organizations with the most complex deployment challenges.

## Supported Java Versions 
This provider will work with Java 11 and above.

## Getting Started

### Installation

The Octopus OpenFeature provider for Java is available as a [Maven package](https://central.sonatype.com/artifact/com.octopus.openfeature/octopus-openfeature-provider). You can add it to your project using your dependency manager (e.g. Maven or Gradle).

```xml
<dependency>
    <groupId>com.octopus.openfeature</groupId>
    <artifactId>octopus-openfeature-provider</artifactId>
    <version>0.2.0</version> <!-- use current version number -->
</dependency>
```

```groovy
implementation group: 'com.octopus.openfeature', name: 'octopus-openfeature-provider', version: '0.2.0'
// Use current version number
```

### Usage 

```java
import dev.openfeature.sdk.*;
import com.octopus.openfeature.provider.*;

public class Main {
    
    public static void main(String[] args) {
        var openFeature = OpenFeatureAPI.getInstance();
        openFeature.setProviderAndWait(new OctopusProvider(new OctopusConfiguration("Your Octopus client identifier")));
        var openFeatureClient = openFeature.getClient(); 
        
        var darkModeIsEnabled = openFeatureClient.getBooleanValue("dark-mode", false);
    }
}
```

For information on using the OpenFeature client please refer to the [OpenFeature Documentation](https://docs.openfeature.dev/docs/reference/concepts/evaluation-api/).