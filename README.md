Ktor Permissions
===

![Maven Central](https://img.shields.io/maven-central/v/org.drewcarlson/ktor-permissions?label=maven&color=blue)
![](https://github.com/DrewCarlson/ktor-permissions/workflows/Build/badge.svg)

Simple route permissions for Ktor.


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

![Maven Central](https://img.shields.io/maven-central/v/org.drewcarlson/ktor-permissions?label=maven&color=blue)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/org.drewcarlson/ktor-permissions?server=https%3A%2F%2Fs01.oss.sonatype.org)

```kotlin
repositories {
    mavenCentral()
    // Or snapshots
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("org.drewcarlson:ktor-permissions:$KTOR_PERMISSIONS_VERSION")
}
```
