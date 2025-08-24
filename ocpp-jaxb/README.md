# ocpp-jaxb
Java data model mappings for the following [OCPP](https://www.openchargealliance.org/protocols/) versions:
* OCPP 1.2 (targets JSON and SOAP)
* OCPP 1.5 (targets JSON and SOAP)
* OCPP 1.6 (targets JSON and SOAP)
* OCPP 2.0.1 (targets JSON, requires [Jackson](https://github.com/FasterXML/jackson))

The Java data model covers all requests and responses between the central system and charge points.

The classes can be found within the following packages:
* OCPP 1.2: `ocpp.cp._2010._08` and `ocpp.cs._2010._08`
* OCPP 1.5: `ocpp.cp._2012._06` and `ocpp.cs._2012._06`
* OCPP 1.6: `ocpp.cp._2015._10` and `ocpp.cs._2015._10`
* OCPP 2.0.1: `ocpp._2020._03`

# How to use it?

This package is not pushed to Maven central... yet. But still you can use it via [jitpack.io](https://jitpack.io/docs/).

Add the jitpack repository to your maven repositories:

 ```
<repositories>
    ...
    <repository>
        <id>JitPack</id>
        <url>https://jitpack.io</url>
    </repository>
    ...
</repositories>
 ```

Then, add the following to your pom.xml (use any version from [github tags page](https://github.com/RWTH-i5-IDSG/ocpp-jaxb/tags)):

 ```
<dependency>
    <groupId>com.github.steve-community</groupId>
    <artifactId>ocpp-jaxb</artifactId>
    <version>${ocpp-jaxb.version}</version>
</dependency>
 ```
