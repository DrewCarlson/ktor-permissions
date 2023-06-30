package org.drewcarlson.ktor.permissions

import io.ktor.server.application.*
import kotlin.reflect.KClass

typealias Verifier <P> = (P) -> Boolean

typealias PermissionSelect <P> = ApplicationCall.(Set<P>) -> Set<P>

class WithPermissionGroup<P : Any> {
    internal val permissions = mutableMapOf<KClass<*>, Triple<P?, Verifier<P>, PermissionSelect<P>?>>()

    inline fun <reified T : P> add(noinline build: DynamicPermissionBuilder<T>.() -> Unit) {
        add(T::class, build)
    }

    @PublishedApi
    internal fun <T : P> add(kclass: KClass<T>, build: DynamicPermissionBuilder<T>.() -> Unit) {
        var stub: T? = null
        var verify: Verifier<T>? = null
        var permissionSelect: PermissionSelect<T>? = null
        build(object : DynamicPermissionBuilder<T> {
            override fun select(selector: ApplicationCall.(Set<T>) -> Set<T>) {
                permissionSelect = selector
            }

            override fun stub(permission: T) {
                stub = permission
            }

            override fun verify(verifier: (T) -> Boolean) {
                verify = verifier
            }
        })

        val verifyFunc = requireNotNull(verify) {
            "A custom verifier must be provided for `withPermission({ verify(...) }) { ... }`"
        }
        @Suppress("UNCHECKED_CAST")
        permissions[kclass] = Triple(stub, verifyFunc, permissionSelect) as Triple<P?, Verifier<P>, PermissionSelect<P>?>
    }
}