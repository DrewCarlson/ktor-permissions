package org.drewcarlson.ktor.permissions

import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*

private const val PRINCIPAL_OBJECT_MISSING = "Principal missing, is the route wrapped in `authenticate {  }`?"
private const val PRINCIPAL_PERMISSIONS_MISSING_ALL = "Principal '%s' is missing required permission(s) %s"
private const val PRINCIPAL_PERMISSIONS_MISSING_ANY = "Principal '%s' is missing all possible permission(s) %s"
private const val PRINCIPAL_PERMISSIONS_MATCHED_EXCLUDE = "Principal '%s' has excluded permission(s) %s"
private const val EXTRACT_PERMISSIONS_NOT_DEFINED =
    "Principal permission extractor must be defined, ex: `extract { (it as Session).permissions }`."

class PermissionAuthorization internal constructor(
    private val configuration: Configuration
) {

    class Configuration internal constructor() {
        internal var globalPermission: Any? = null
        internal var extractPermissions: (Principal) -> Set<Any> =
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
        fun <P : Any> extract(body: (Principal) -> Set<P>) {
            extractPermissions = body
        }
    }

    fun <P : Any> interceptPipeline(
        pipeline: ApplicationCallPipeline,
        any: Set<P>? = null,
        all: Set<P>? = null,
        none: Set<P>? = null
    ) {
        AuthenticationChecked.install(pipeline) { call ->
            val principal = checkNotNull(call.authentication.principal()) { PRINCIPAL_OBJECT_MISSING }
            val activePermissions = configuration.extractPermissions(principal)
            configuration.globalPermission?.let {
                if (activePermissions.contains(it)) {
                    return@install
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


    companion object Plugin :
        BaseApplicationPlugin<ApplicationCallPipeline, Configuration, PermissionAuthorization> {
        override val key = AttributeKey<PermissionAuthorization>("PermissionAuthorization")
        override fun install(
            pipeline: ApplicationCallPipeline,
            configure: Configuration.() -> Unit
        ): PermissionAuthorization {
            return PermissionAuthorization(Configuration().also(configure))
        }
    }
}
