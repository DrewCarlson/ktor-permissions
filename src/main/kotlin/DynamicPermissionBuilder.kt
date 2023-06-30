package org.drewcarlson.ktor.permissions

import io.ktor.server.application.*


interface DynamicPermissionBuilder<P : Any> {

    fun stub(permission: P)

    fun select(selector: ApplicationCall.(Set<P>) -> Set<P>)

    fun verify(verifier: (P) -> Boolean)
}
