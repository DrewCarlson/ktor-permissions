package drewcarlson.ktor.permissions

import drewcarlson.ktor.permissions.Permission.A
import drewcarlson.ktor.permissions.Permission.B
import drewcarlson.ktor.permissions.Permission.C
import drewcarlson.ktor.permissions.Permission.Z
import io.ktor.auth.Principal
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.OK
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

enum class Permission {
    A, B, C, Z
}

@Serializable
data class UserSession(
    val userId: String,
    val permissions: Set<Permission>,
) : Principal

class PermissionTests {

    @Test
    fun testUndefinedGlobalHasNoRestrictedAccess() {
        runPermissionTest(setGlobal = false) {
            val token = tokenWith(Z)

            Permission.values()
                .filter { it != Z }
                .fold(listOf<Permission>()) { prev, perm ->
                    val set = (prev + perm).toSet().joinToString("") { it.name }
                    assertEquals(Forbidden, statusFor("/all/$set", token))
                    assertEquals(Forbidden, statusFor("/any/$set", token))
                    assertEquals(Forbidden, statusFor("/$perm", token))
                    prev + perm
                }
        }
    }

    @Test
    fun testGlobalAllowsBypassesAllRules() {
        runPermissionTest(setGlobal = true) {
            val token = tokenWith(Z)

            Permission.values()
                .fold(listOf<Permission>()) { prev, perm ->
                    val set = (prev + perm).toSet().joinToString("") { it.name }
                    assertEquals(OK, statusFor("/all/$set", token))
                    assertEquals(OK, statusFor("/any/$set", token))
                    assertEquals(OK, statusFor("/$perm", token))
                    assertEquals(OK, statusFor("/without/$perm", token))
                    prev + perm
                }
        }
    }

    @Test
    fun testWithPermission() {
        runPermissionTest(setGlobal = true) {
            val tokenA = tokenWith(A)

            assertEquals(OK, statusFor("/A", tokenA))
            assertEquals(Forbidden, statusFor("/B", tokenA))
            assertEquals(Forbidden, statusFor("/C", tokenA))
            assertEquals(Forbidden, statusFor("/Z", tokenA))

            val tokenB = tokenWith(B)

            assertEquals(OK, statusFor("/B", tokenB))
            assertEquals(Forbidden, statusFor("/A", tokenB))
            assertEquals(Forbidden, statusFor("/C", tokenB))
            assertEquals(Forbidden, statusFor("/Z", tokenB))

            val tokenC = tokenWith(C)

            assertEquals(OK, statusFor("/C", tokenC))
            assertEquals(Forbidden, statusFor("/A", tokenC))
            assertEquals(Forbidden, statusFor("/B", tokenC))
            assertEquals(Forbidden, statusFor("/Z", tokenC))
        }
    }

    @Test
    fun testWithAllPermission() {
        runPermissionTest(setGlobal = true) {
            val tokenA = tokenWith(A)

            assertEquals(OK, statusFor("/all/A", tokenA))
            assertEquals(Forbidden, statusFor("/all/AB", tokenA))
            assertEquals(Forbidden, statusFor("/all/ABC", tokenA))
            assertEquals(Forbidden, statusFor("/all/ABCZ", tokenA))

            val tokenB = tokenWith(B)

            assertEquals(OK, statusFor("/all/B", tokenB))
            assertEquals(Forbidden, statusFor("/all/A", tokenB))
            assertEquals(Forbidden, statusFor("/all/AB", tokenB))
            assertEquals(Forbidden, statusFor("/all/ABC", tokenB))
            assertEquals(Forbidden, statusFor("/all/ABCZ", tokenB))

            val tokenC = tokenWith(C)

            assertEquals(OK, statusFor("/all/C", tokenC))
            assertEquals(Forbidden, statusFor("/all/A", tokenC))
            assertEquals(Forbidden, statusFor("/all/AB", tokenC))
            assertEquals(Forbidden, statusFor("/all/ABC", tokenC))
            assertEquals(Forbidden, statusFor("/all/ABCZ", tokenC))
        }
    }

    @Test
    fun testWithAnyPermission() {
        runPermissionTest(setGlobal = true) {
            val tokenA = tokenWith(A)

            assertEquals(OK, statusFor("/any/A", tokenA))
            assertEquals(OK, statusFor("/any/AB", tokenA))
            assertEquals(OK, statusFor("/any/ABC", tokenA))
            assertEquals(Forbidden, statusFor("/any/B", tokenA))
            assertEquals(Forbidden, statusFor("/any/C", tokenA))
            assertEquals(Forbidden, statusFor("/any/Z", tokenA))

            val tokenB = tokenWith(B)

            assertEquals(OK, statusFor("/any/ABC", tokenB))
            assertEquals(OK, statusFor("/any/AB", tokenB))
            assertEquals(OK, statusFor("/any/B", tokenB))
            assertEquals(Forbidden, statusFor("/any/C", tokenB))
            assertEquals(Forbidden, statusFor("/any/A", tokenB))
            assertEquals(Forbidden, statusFor("/any/Z", tokenB))

            val tokenC = tokenWith(C)

            assertEquals(OK, statusFor("/any/C", tokenC))
            assertEquals(OK, statusFor("/any/ABC", tokenC))
            assertEquals(Forbidden, statusFor("/any/A", tokenC))
            assertEquals(Forbidden, statusFor("/any/AB", tokenC))
            assertEquals(Forbidden, statusFor("/any/B", tokenC))
            assertEquals(Forbidden, statusFor("/any/Z", tokenC))
        }
    }

    @Test
    fun testWithoutPermission() {
        runPermissionTest(setGlobal = true) {
            val tokenA = tokenWith(A)

            assertEquals(Forbidden, statusFor("/without/A", tokenA))
            assertEquals(Forbidden, statusFor("/without/AB", tokenA))
            assertEquals(Forbidden, statusFor("/without/ABC", tokenA))
            assertEquals(OK, statusFor("/without/B", tokenA))
            assertEquals(OK, statusFor("/without/C", tokenA))
            assertEquals(OK, statusFor("/without/Z", tokenA))

            val tokenB = tokenWith(B)

            assertEquals(Forbidden, statusFor("/without/ABC", tokenB))
            assertEquals(Forbidden, statusFor("/without/AB", tokenB))
            assertEquals(Forbidden, statusFor("/without/B", tokenB))
            assertEquals(OK, statusFor("/without/C", tokenB))
            assertEquals(OK, statusFor("/without/A", tokenB))

            val tokenC = tokenWith(C)

            assertEquals(Forbidden, statusFor("/without/C", tokenC))
            assertEquals(Forbidden, statusFor("/without/ABC", tokenC))
            assertEquals(OK, statusFor("/without/A", tokenC))
            assertEquals(OK, statusFor("/without/AB", tokenC))
            assertEquals(OK, statusFor("/without/B", tokenC))
        }
    }
}
