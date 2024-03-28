# cloud-function-webhook

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./mvnw package
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/cloud-function-webhook-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/maven-tooling.

## Related Guides

- Google Cloud Firestore ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-google-cloud-services/main/firestore.html)): Use Google Cloud Firestore NOSQL database service
- Google Cloud Secret Manager ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-google-cloud-services/main/secretmanager.html)): Use Google Cloud Secret Manager service
- Google Cloud Functions ([guide](https://quarkus.io/guides/gcp-functions)): Write Google Cloud functions
- Google Cloud Logging ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-google-cloud-services/main/logging.html)): Use Google Cloud operations logging

## Provided Code

### Google Cloud Functions Integration examples

Examples of Google Cloud HTTP, Background and Cloud Event functions for Quarkus.

[Related guide section...](https://quarkus.io/guides/gcp-functions)

Three examples have been generated under `src/main/java/org/acme/googlecloudfunctions`, you must remove them before deploying to 
Google Cloud Functions or setup multi-functions support, see https://quarkus.io/guides/gcp-functions#choose-your-function.

> :warning: **INCOMPATIBLE WITH DEV MODE**: Google Cloud Functions is not compatible with dev mode yet!
