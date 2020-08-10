## Disco 'AWS SDK' Support

Serving as both an example of how to author a Disco library/plugin/api, and also as a usable
Event producer for requests made using versions 1 and 2 of the AWS SDK for Java, this subproject is laid out as follows:

1. In this folder, the Installables to intercept AWS SDK requests & responses, and issue appropriate Event Bus Events.
1. In the disco-java-agent-aws-plugin subfolder, a proper Disco plugin, bundled as a plugin JAR file with Manifest.
1. In the disco-java-agent-aws-api subfolder, a collection of Event classes which are implemented & published by
the intstallables in this package.

See the READMEs of the submodules for more information on each.

## Feature Status

TODO

## Package description

`AwsSupport` is a Disco Package that can be installed by standalone Agents to gain interception and
event publication for the features described above.
