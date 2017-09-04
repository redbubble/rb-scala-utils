# Redbubble Scala Utilities

Miscellaneous utilities (common code) for building Scala-based services, using Finch. This code was extracted out of the services template used in production at Redbubble for building fast, stateless services in Scala, including the API that powers our [iOS app](https://itunes.apple.com/au/app/redbubble/id1145737091?mt=8).

If you like this, you might like other open source code from Redbubble (which make use of this project):

* [finch-template](https://github.com/redbubble/finch-template) - A template project for Finch-based services.
* [rb-graphql-template](https://github.com/redbubble/rb-graphql-template) - A template for Scala HTTP GraphQL services.
* [finch-sangria](https://github.com/redbubble/finch-sangria) - A simple wrapper for using Sangria from within Finch;
* [finagle-hawk](https://github.com/redbubble/finagle-hawk) - HTTP Holder-Of-Key Authentication Scheme for Finagle.

# Setup

You will need to add something like the following to your `build.sbt`:

```scala
resolvers += Resolver.jcenterRepo

libraryDependencies += "com.redbubble" %% "rb-scala-utils" % "0.1.0"
```

# Release

For contributors, a cheat sheet to making a new release:

```shell
$ git commit -m "New things" && git push
$ git tag -a v0.0.3 -m "v0.0.3"
$ git push --tags
$ ./sbt publish
```

# Contributing

Issues and pull requests are welcome. Code contributions should be aligned with the above scope to be included, and include unit tests.
