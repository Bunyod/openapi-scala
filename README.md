# OpenAPI Scala

This project contains an opinionated library and SBT Plugin for Scala code generation from [OpenAPI 3.0](https://swagger.io/specification/) 
compliant YAML. 

This will generate Components (i.e., schemas or types), and Paths (i.e., route definitions) for REST APIs.

We do not support all OpenAPI 3.0 features. For more details on what is supported, look at [Support and Limitations](#support-and-limitations) 
section.

### OpenAPI Scala

SBT sub-project `openapi-scala` contains the main logic and library for loading, representing, and translating YAML to Scala code.
These contain components that are translated to Scala's `case classes` and Routes that each translate to a Scala `trait` declaring interfaces
for that particular HTTP route.

### sbt-openapi

SBT sub-project `sbt-openapi` contains an SBT plugin that allows the use of `openapi-scala` library to a given YAML file that will be loaded
and used to generate managed Scala sources.

## Quickstart

To use the plugin, you first need to make it available in your SBT.
```
// project/plugins.sbt
addSbtPlugin("com.enfore" % "sbt-openapi" % "0.0.7-RC2")
```

Once the plugin is available in your project you can enable it on a given an SBT sub-project and use the setting `openAPIOutputPackage` to specify
the package name for your components.

```
// build.sbt
lazy val root = (project in ("."))
    .settings(
        openAPIOutputPackage := "com.enfore.model"
    )
    .enablePlugins(OpenapiPlugin)
```


### SBT Settings

Following are the settings available for your SBT project.

**openAPISource**: Source directory for OpenAPI. Defaults to `src/main/openapi` of the project.

**openAPIOutput**: Output directory for the OpenAPI. Defaults to managed sources — `openapi`.

**openAPIOutputPacakge**: Name of the package to be used for OpenAPI components.

## Support And Limitations

This generator is highly opinionated and skips over certain features defined OpenAPI 3.0 specification for the sake of API consistency
and compatibility with modern typed languages.

### Limitations

- We only support a _single file_ for loading the OpenAPI definition at the moment. If there are any references that point to components or contents
in a different file, they need to be resolved before using our library/plugin.
- We do not support any anonymous types for representing HTTP requests and responses.
- We do not support nested type definitions. Therefore, any objects that are used must be defined in the components and must not contain
any other nested objects. Nested arrays, however, are supported.