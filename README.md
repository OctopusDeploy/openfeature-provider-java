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

The Octopus OpenFeature provider for Java is currently published to [GitHub Packages](https://github.com/OctopusDeploy/openfeature-provider-java/packages/2528379). It will be published to the Maven Central repository soon.

To add Maven package dependencies from GitHub Packages, you must configure an [authentication token](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages).

Once this is configured, you can add the Octopus OpenFeature provider as a dependency. The Maven example is shown below.


```xml
<dependency>
    <groupId>com.octopus.openfeature</groupId>
    <artifactId>octopus-openfeature-provider</artifactId>
    <version>0.1.0</version> <!-- use current version number -->
</dependency>
```

### Usage 

```java
import dev.openfeature.sdk.*;
import com.octopus.openfeature.provider.*;

public class Main {
    
    public static void main(String[] args) {
        var openFeature = OpenFeatureAPI.getInstance();
        openFeature.setProvider(new OctopusProvider(new OctopusConfiguration("Your Octopus client identifier")));
        var openFeatureClient = openFeature.getClient(); 
        
        var darkModeIsEnabled = openFeatureClient.getBooleanValue("dark-mode", false);
    }
}
```

For information on using the OpenFeature client please refer to the [OpenFeature Documentation](https://docs.openfeature.dev/docs/reference/concepts/evaluation-api/).