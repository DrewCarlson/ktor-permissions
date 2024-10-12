package org.drewcarlson.ktor.permissions

import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Base64
import kotlin.random.Random

private const val TOKEN = "TOKEN"

suspend fun ClientProvider.tokenWith(vararg permissions: Permission): String {
    return try {
        client.request {
            method = HttpMethod.Post
            url.takeFrom("/token")
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(permissions))
        }
    } catch (e: ClientRequestException) {
        e.response
    }.headers[TOKEN]!!
}

suspend fun ClientProvider.statusFor(
    uri: String,
    token: String,
): HttpStatusCode {
    return try {
        client.request {
            method = HttpMethod.Get
            url.takeFrom(uri)
            header(TOKEN, token)
        }
    } catch (e: ClientRequestException) {
        e.response
    }.status
}

fun runPermissionTest(
    setGlobal: Boolean,
    test: suspend ApplicationTestBuilder.() -> Unit,
) {
    testApplication {
        install(Authentication) {
            session<UserSession> {
                challenge { call.respond(HttpStatusCode.Unauthorized) }
                validate { it }
            }
        }

        install(Sessions) {
            header<UserSession>(TOKEN, SessionStorageMemory()) {
                identity { Base64.getEncoder().encodeToString(Random.nextBytes(12)) }
            }
        }

        install(PermissionAuthorization) {
            if (setGlobal) {
                global(Permission.Z)
            }
            extract { (it as UserSession).permissions }
        }

        install(ContentNegotiation) {
            json()
        }

        routing {
            post("/token") {
                val permissions = call.receiveNullable<List<Permission>>()?.toSet()
                call.sessions.getOrSet {
                    UserSession("test", permissions ?: emptySet())
                }
                call.respond(HttpStatusCode.OK)
            }
            authenticate {
                val perms = Permission.entries
                perms.forEach { permission ->
                    withPermission(permission) {
                        get("/${permission.name}") {
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }
                perms.fold(emptyList<Permission>()) { acc, permission ->
                    val set = (acc + permission).toSet()
                    withAllPermissions(*set.toTypedArray()) {
                        get("/all/${set.joinToString("") { it.name }}") {
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                    withoutPermissions(*set.toTypedArray()) {
                        get("/without/${set.joinToString("") { it.name }}") {
                            call.respond(HttpStatusCode.OK)
                        }
                    }
                    withAnyPermission(*set.toTypedArray()) {
                        get("/any/${set.joinToString("") { it.name }}") {
                            call.respond(HttpStatusCode.OK)
                        }
                    }

                    if (set.size > 1) {
                        withAllPermissions(permission) {
                            get("/all/${permission.name}") {
                                call.respond(HttpStatusCode.OK)
                            }
                        }
                        withoutPermissions(permission) {
                            get("/without/${permission.name}") {
                                call.respond(HttpStatusCode.OK)
                            }
                        }
                        withAnyPermission(permission) {
                            get("/any/${permission.name}") {
                                call.respond(HttpStatusCode.OK)
                            }
                        }
                    }
                    acc + permission
                }
            }
        }

        test()
    }
}
