## Disco 'AWS SDK' Support

Serving as both an example of how to author a Disco library/plugin/api, and also as a usable
Event producer for requests made using versions 1 and 2 of the AWS SDK for Java, this subproject is laid out as follows:

1. In this folder, the Installables to intercept AWS SDK requests & responses, and issue appropriate Event Bus Events.
1. In the `disco-java-agent-aws-plugin` subfolder, a proper Disco plugin, bundled as a plugin JAR file with Manifest.
1. In the `disco-java-agent-aws-api` subfolder, a collection of Event classes which are implemented & published by
the intstallables in this package.

See the READMEs of the submodules for more information on each.

## Feature Status

This installable will publish `ServiceDownstreamEvent`s for requests and responses from V1 of the AWS SDK. These 
events are populated with the standard Disco metadata of origin, service name, and operation name, in addition to
supplying the Request and Response objects created by the AWS SDK. For V2 of the AWS SDK, the installable publishes
`AwsServiceDownstreamEvent`s. These events are an extension `ServiceDownstreamEvent`s, meaning they provide all the
same metadata in addition to the AWS region, number of retries, and request ID as indicated by the "additional
metadata" column in the table below.

For both versions of the AWS SDK, the published events expose a `replaceHeader(String, String)` method to add
arbitrary headers to requests.

|                                                                                        | Intercept client requests | Replace headers    | Standard metadata  | Additional metadata       |
|----------------------------------------------------------------------------------------|---------------------------|--------------------|--------------------|---------------------------|
| [AWS SDK V1](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/welcome.html) | :heavy_check_mark: *      | :heavy_check_mark: | :heavy_check_mark: | :heavy_multiplication_x:  |
| [AWS SDK V2](https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/welcome.html) | :heavy_check_mark:        | :heavy_check_mark: | :heavy_check_mark: | :heavy_check_mark:        |

\* Interception for S3 clients made with V1 of the AWS SDK is not yet available

## Package description

`AwsSupport` is a Disco Package that can be installed by standalone Agents to gain interception and
event publication for the features described above.
