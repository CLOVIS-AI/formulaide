package formulaide.server

import at.favre.lib.crypto.bcrypt.BCrypt
import formulaide.api.types.Email
import formulaide.api.types.Ref
import formulaide.api.users.NewUser
import formulaide.api.users.PasswordLogin
import formulaide.api.users.User
import formulaide.db.document.createService
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthTest {

	@Test
	fun testHashingManual() {
		val password = "1234"
		val bcryptHashString = BCrypt.withDefaults()
			.hashToString(12, password.toCharArray())

		val result = BCrypt.verifyer().verify(password.toCharArray(), bcryptHashString)
		assertTrue(result.verified)
	}

	@Test
	fun testHashing() {
		val message = "This is a super secret message"
		println(message)

		val hashed1 = Auth.hash(message)
		val hashed2 = Auth.hash(message)
		println("Hashes:\n - $hashed1\n - $hashed2")

		assertTrue(Auth.checkHash(message, hashed1))
		assertTrue(Auth.checkHash(message, hashed2))
	}

	@Test
	fun testAuth() = runBlocking {
		val db = testDatabase()
		val auth = Auth(db)
		val service = db.createService("Service des tests")

		val email = "new${Random.nextInt()}@ville-arcachon.fr"
		val password = "this is my super-safe password"

		// Creating the account

		val apiUser = User(
			Email(email), "Auth Test User", setOf(Ref(service.id.toString())), false
		)
		val user = NewUser(password, apiUser)
		val (token1, dbUser1) = auth.newAccount(user)

		// Logging in

		val (token2, _, dbUser2) = auth.login(PasswordLogin(password, email))

		// Checking token validity

		assertEquals(
			dbUser1,
			dbUser2,
			"I should retrieve the same user as the one that was created"
		)
		assertNotNull(auth.checkToken(token1))
		assertNotNull(auth.checkToken(token2))
		Unit
	}

}
