package org.drewcarlson.ktor.permissions

import io.ktor.server.routing.*
import kotlin.reflect.KClass

/**
 * Routes defined in [build] will only be invoked when the
 * [io.ktor.server.auth.Principal] contains [permission].
 *
 * If a global permission is defined, [io.ktor.server.auth.Principal]s with
 * that permission will ignore the requirement.
 */
fun <P : Any> Route.withPermission(permission: P, build: Route.() -> Unit) =
    authorizedRoute(all = setOf(permission), build = build)

/**
 * Routes defined in [build] will only be invoked when the
 * [io.ktor.server.auth.Principal] contains all of [permissions].
 *
 * If a global permission is defined, [io.ktor.server.auth.Principal]s with
 * that permission will ignore the requirement.
 */
fun <P : Any> Route.withAllPermissions(vararg permissions: P, build: Route.() -> Unit) =
    authorizedRoute(all = permissions.toSet(), build = build)

/**
 * Routes defined in [build] will only be invoked when the
 * [io.ktor.server.auth.Principal] contains any of [permissions].
 *
 * If a global permission is defined, [io.ktor.server.auth.Principal]s with
 * that permission will ignore the requirement.
 */
fun <P : Any> Route.withAnyPermission(vararg permissions: P, build: Route.() -> Unit) =
    authorizedRoute(any = permissions.toSet(), build = build)

/**
 * Routes defined in [build] will only be invoked when the
 * [io.ktor.server.auth.Principal] does not contain any of [permissions].
 *
 * If a global permission is defined, [io.ktor.server.auth.Principal]s with
 * that permission will ignore the requirement.
 */
fun <P : Any> Route.withoutPermissions(vararg permissions: P, build: Route.() -> Unit) =
    authorizedRoute(none = permissions.toSet(), build = build)

fun <P : Any> Route.withPermission(
    builder: WithPermissionGroup<P>.() -> Unit,
    buildRoute: Route.() -> Unit
): Route {
    val permissions = WithPermissionGroup<P>().apply(builder).permissions.toMap()
    val description = "anyOf (${permissions.mapNotNull { (_, v) -> v.first?.toString() }.joinToString()})"
    return createChild(AuthorizedRouteSelector(description)).also { route ->
        route.install(
            CustomVerifierPermissionsRouteAuthorization(
                @Suppress("UNCHECKED_CAST")
                RoutePermissionSets.CustomVerifier(
                    permissions as MutableMap<KClass<P>, Triple<P?, Verifier<P>, PermissionSelect<P>?>>
                )
            )
        )
        route.buildRoute()
    }
}

private fun <P : Any> Route.authorizedRoute(
    any: Set<P>? = null,
    all: Set<P>? = null,
    none: Set<P>? = null,
    build: Route.() -> Unit
): Route {
    val description = listOfNotNull(
        any?.let { "anyOf (${any.joinToString(" ")})" },
        all?.let { "allOf (${all.joinToString(" ")})" },
        none?.let { "noneOf (${none.joinToString(" ")})" }
    ).joinToString(",")
    return createChild(AuthorizedRouteSelector(description)).also { route ->
        route.install(SimplePermissionsRouteAuthorization) {
            this.any = any
            this.all = all
            this.none = none
        }
        route.build()
    }
}
