package org.drewcarlson.ktor.permissions

import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

private const val PRINCIPAL_PERMISSIONS_MISSING_ALL = "Principal '%s' is missing required permission(s) %s"
private const val PRINCIPAL_PERMISSIONS_MISSING_ANY = "Principal '%s' is missing all possible permission(s) %s"
private const val PRINCIPAL_PERMISSIONS_MATCHED_EXCLUDE = "Principal '%s' has excluded permission(s) %s"
private const val EXTRACT_PERMISSIONS_NOT_DEFINED =
    "Principal permission extractor must be defined, ex: `extract { (it as Session).permissions }`."


class PermissionAuthorizationConfig internal constructor() {
    internal var globalPermission: Any? = null
    internal var extractPermissions: (Any) -> Set<Any> =
        { throw NotImplementedError(EXTRACT_PERMISSIONS_NOT_DEFINED) }

    /**
     * Define the Global permission to ignore route specific
     * permission requirements when attached to the [Principal].
     */
    fun <P : Any> global(permission: P) {
        globalPermission = permission
    }

    /**
     * Define how to extract the user's permission sent from
     * the [Principal] instance.
     *
     * Note: This should be a fast value mapping function,
     * do not read from an expensive data source.
     */
    fun <P : Any> extract(body: (Any) -> Set<P>) {
        extractPermissions = body
    }
}

class PermissionAuthorization internal constructor(
    internal val configuration: PermissionAuthorizationConfig
) {
    companion object Plugin :
        BaseApplicationPlugin<ApplicationCallPipeline, PermissionAuthorizationConfig, PermissionAuthorization> {
        override val key = AttributeKey<PermissionAuthorization>("PermissionAuthorization")
        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: PermissionAuthorizationConfig.() -> Unit
        ): PermissionAuthorization {
            return PermissionAuthorization(PermissionAuthorizationConfig().also(configure))
        }
    }
}

internal sealed class RoutePermissionSets<P : Any> {
    data class SimplePermissions<P : Any>(
        var any: Set<P>? = null,
        var all: Set<P>? = null,
        var none: Set<P>? = null,
    ) : RoutePermissionSets<P>()

    data class CustomVerifier<P : Any>(
        val permissionChecks: MutableMap<KClass<P>, Triple<P?, Verifier<P>, PermissionSelect<P>?>> = mutableMapOf(),
    ) : RoutePermissionSets<P>()
}

internal val SimplePermissionsRouteAuthorization = createRouteScopedPlugin(
    name = "RoutePermissionAuthorization",
    createConfiguration = { RoutePermissionSets.SimplePermissions<Any>() }
) {
    val configuration = application.plugin(PermissionAuthorization).configuration
    on(AuthenticationChecked) { call ->
        val (any, all, none) = pluginConfig
        val principal = call.authentication.principal<Any>() ?: return@on call.respond(Forbidden)
        val activePermissions = configuration.extractPermissions(principal)
        configuration.globalPermission?.let {
            if (activePermissions.contains(it)) {
                return@on
            }
        }
        val denyReasons = mutableListOf<String>()
        all?.let {
            val missing = all - activePermissions
            if (missing.isNotEmpty()) {
                denyReasons += PRINCIPAL_PERMISSIONS_MISSING_ALL.format(principal, missing.joinToString(" and "))
            }
        }
        any?.let {
            if (any.none { it in activePermissions }) {
                denyReasons += PRINCIPAL_PERMISSIONS_MISSING_ANY.format(principal, any.joinToString(" or "))
            }
        }
        none?.let {
            if (none.any { it in activePermissions }) {
                val permissions = none.intersect(activePermissions).joinToString(" and ")
                denyReasons += PRINCIPAL_PERMISSIONS_MATCHED_EXCLUDE.format(principal, permissions)
            }
        }
        if (denyReasons.isNotEmpty()) {
            val message = denyReasons.joinToString(". ")
            call.application.log.warn("Authorization failed for ${call.request.path()}. $message")
            call.respond(Forbidden)
        }
    }
}

internal fun <P : Any> CustomVerifierPermissionsRouteAuthorization(
    config: RoutePermissionSets.CustomVerifier<P>
) = createRouteScopedPlugin(
    name = "RoutePermissionAuthorization",
    createConfiguration = { config }
) {
    val configuration = application.plugin(PermissionAuthorization).configuration
    val permissionChecks = pluginConfig.permissionChecks
    on(AuthenticationChecked) { call ->
        val principal = call.authentication.principal<Any>() ?: return@on call.respond(Forbidden)
        val permissions = configuration.extractPermissions(principal)
        configuration.globalPermission?.let {
            if (permissions.contains(it)) {
                return@on
            }
        }

        val denyReasons = mutableListOf<String>()
        permissionChecks.forEach { (kClass, permissionCheck) ->
            val (stub, verify, select) = permissionCheck
            val result = (select?.invoke(call, permissions.mapNotNull(kClass::safeCast).toSet()) ?: permissions)
                .mapNotNull(kClass::safeCast).toSet()
                .filter { verify(it) }
            if (result.any()) {
                return@on
            } else {
                denyReasons += PRINCIPAL_PERMISSIONS_MISSING_ALL.format(principal, stub ?: "<no stub>")
            }
        }

        if (denyReasons.isNotEmpty()) {
            val message = denyReasons.joinToString(". ")
            call.application.log.warn("Authorization failed for ${call.request.path()}. $message")
            call.respond(Forbidden)
        }
    }
}

