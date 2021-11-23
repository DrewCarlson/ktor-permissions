package org.drewcarlson.ktor.permissions

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.session
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveOrNull
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.serialization.DefaultJson
import io.ktor.serialization.json
import io.ktor.server.testing.*
import io.ktor.sessions.SessionStorageMemory
import io.ktor.sessions.Sessions
import io.ktor.sessions.getOrSet
import io.ktor.sessions.header
import io.ktor.sessions.sessions
import kotlinx.serialization.encodeToString
import java.util.Base64
import kotlin.random.Random

private const val TOKEN = "TOKEN"

fun TestApplicationEngine.tokenWith(vararg permissions: Permission): String {
    return handleRequest(HttpMethod.Post, "/token") {
        addHeader("Content-Type", ContentType.Application.Json.toString())
        setBody(DefaultJson.encodeToString(permissions))
    }.response.headers[TOKEN]!!
}

fun TestApplicationEngine.statusFor(
    uri: String,
    token: String,
): HttpStatusCode {
    return handleRequest(HttpMethod.Get, uri) {
        addHeader(TOKEN, token)
    }.response.status()!!
}

fun runPermissionTest(
    setGlobal: Boolean,
    test: TestApplicationEngine.() -> Unit,
) {
    withTestApplication({
        install(Authentication) {
            session<UserSession> {
                challenge { context.respond(HttpStatusCode.Unauthorized) }
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
                val permissions = call.receiveOrNull<List<Permission>>()?.toSet()
                call.sessions.getOrSet {
                    UserSession("test", permissions ?: emptySet())
                }
                call.respond(HttpStatusCode.OK)
            }
            authenticate {
                val perms = Permission.values().toList()
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
    }, test = test)
}
