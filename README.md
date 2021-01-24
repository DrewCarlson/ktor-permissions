Ktor Permissions
===

![Bintray](https://img.shields.io/bintray/v/drewcarlson/Ktor/Ktor-Features?color=blue)
![](https://img.shields.io/maven-metadata/v?label=artifactory&logoColor=lightgrey&metadataUrl=https%3A%2F%2Foss.jfrog.org%2Fartifactory%2Foss-snapshot-local%2Fdrewcarlson%2Fktor%2Fktor-permissions%2Fmaven-metadata.xml&color=lightgrey)
![](https://github.com/DrewCarlson/ktor-permissions/workflows/Build/badge.svg)

Simple route permissions for Ktor using the Authentication feature.


### Usage

First define your permissions, this could be anything: strings, enum, sealed class.

```kotlin
enum class Permission {
    GLOBAL, VIEW_DATA, EDIT_DATA,
}
```

Next configure the [authentication](https://ktor.io/docs/authentication.html), [sessions](https://ktor.io/docs/sessions.html), and `PermissionAuthorization` features.

```kotlin
data class UserSession(
    val userId: String,
    val permissions: Set<Permission>
) : Principal

fun Application.module() {
    install(PermissionAuthorization) {
        // Given the Principal, extract the user's permissions
        extract { (it as UserSession).permissions }

        // When the Principal contains the 'global' permission,
        // all route specific permission checks are ignored
        global(Permission.Global)
    }
}
```

The last remaining bit is to specify permission requirements for your routes using
`withPermission`, `withAllPermissions`, `withAnyPermissions`, `withoutPermissions`.

```kotlin
fun Application.module() {
    routing {
        authenticate {
            withPermission(Permission.VIEW_DATA) {
                get("/data") {
                    // ...
                }
            }
            withPermission(Permission.EDIT_DATA) {
                post("/data") {
                    // ...
                }
            }
        }
    }
}
```


### Download

Artifacts are available on [Bintray](https://bintray.com/drewcarlson/Ktor).

```groovy
repositories {
    jcenter()
    // Or snapshots
    maven { setUrl("http://oss.jfrog.org/artifactory/oss-snapshot-local") }
}

dependencies {
    implementation("drewcarlson.ktor:ktor-permissions:$KTOR_PERMISSIONS_VERSION")
}
```
