# freemarker-test-graalvm-natice

Test project for GraalVM support

Requirements : 

- GraalVM 21+

## Quickstart

1. Build the main Apache FreeMarker proejct : 

```shell
./gradlew "-Pfreemarker.signMethod=none" "-Pfreemarker.allowUnsignedReleaseBuild=true" clean build
```

2. Build the test module native image with GraalVM : 

```shell
./gradlew :freemarker-test-graalvm-native:nativeCompile 
```

3. Run the project :

```shell
./freemarker-test-graalvm-native/build/native/nativeCompile/freemarker-test-graalvm-native 
```

Output should be similar to : 

```txt
INFO: name : FreeMarker Native Demo, version : 2.3.35-nightly
Jan 15, 2025 4:28:19 PM freemarker.log._JULLoggerFactory$JULLogger info
INFO: result :
<html>
    <head>
        <title>Hello : FreeMarker GraalVM Native Demo</title>
    </head>
    <body>
        <h1>Hello : FreeMarker GraalVM Native Demo</h1>
        <p>Test template for Apache FreeMarker GraalVM native support (2.3.35-nightly)</p>
    </body>
</html>
```

## CI (GitHub workflow)

GraalVM native test for this module is included in the GitHub  [CI](../.github/workflows/ci.yml) worflow.