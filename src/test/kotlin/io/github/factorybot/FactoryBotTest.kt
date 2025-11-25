package io.github.factorybot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

data class User(
    var firstName: String = "",
    var lastName: String = "",
    var email: String = "",
    var role: Role = Role.USER,
    var id: Long? = null,
    var initialized: Boolean = false
)

enum class Role {
    USER, ADMIN
}

data class Post(
    var title: String = "",
    var content: String = "",
    var author: User? = null,
    var id: Long? = null
)

class FactoryBotTest {
    
    @BeforeEach
    fun setup() {
        FactoryBot.clear()
    }
    
    @Test
    fun `test basic factory definition and build`() {
        factory<User> {
            firstName { "John" }
            lastName { "Doe" }
            email { "john.doe@example.com" }
        }
        
        val user = build<User>()
        assertEquals("John", user.firstName)
        assertEquals("Doe", user.lastName)
        assertEquals("john.doe@example.com", user.email)
    }
    
    @Test
    fun `test override attributes at build time`() {
        factory<User> {
            firstName { "John" }
            lastName { "Doe" }
        }
        
        val user = build<User> {
            firstName { "Jane" }
        }
        
        assertEquals("Jane", user.firstName)
        assertEquals("Doe", user.lastName)
    }
    
    @Test
    fun `test sequences`() {
        factory<User> {
            firstName { "User" }
            email { sequence { n -> "user$n@example.com" } }
        }
        
        val user1 = build<User>()
        val user2 = build<User>()
        val user3 = build<User>()
        
        assertEquals("user1@example.com", user1.email)
        assertEquals("user2@example.com", user2.email)
        assertEquals("user3@example.com", user3.email)
    }
    
    @Test
    fun `test global sequences`() {
        defineSequence("user_email") { n -> "user$n@test.com" }
        
        factory<User> {
            email { generate("user_email") }
        }
        
        val user1 = build<User>()
        val user2 = build<User>()
        
        assertEquals("user1@test.com", user1.email)
        assertEquals("user2@test.com", user2.email)
    }
    
    @Test
    fun `test traits`() {
        factory<User> {
            firstName { "John" }
            role { Role.USER }
            
            trait("admin") {
                role { Role.ADMIN }
                firstName { "Admin" }
            }
        }
        
        val normalUser = build<User>()
        assertEquals(Role.USER, normalUser.role)
        assertEquals("John", normalUser.firstName)
        
        val adminUser = build<User>("admin")
        assertEquals(Role.ADMIN, adminUser.role)
        assertEquals("Admin", adminUser.firstName)
    }
    
    @Test
    fun `test multiple traits composition`() {
        factory<User> {
            firstName { "John" }
            role { Role.USER }
            
            trait("admin") {
                role { Role.ADMIN }
            }
            
            trait("verified") {
                email { "verified@example.com" }
            }
        }
        
        val user = build<User>("admin", "verified")
        assertEquals(Role.ADMIN, user.role)
        assertEquals("verified@example.com", user.email)
    }
    
    @Test
    fun `test associations`() {
        factory<User> {
            firstName { "Author" }
            lastName { "Name" }
        }
        
        factory<Post> {
            title { "My Post" }
            content { "Content" }
            author { association<User>() }
        }
        
        val post = build<Post>()
        assertNotNull(post.author)
        assertEquals("Author", post.author?.firstName)
        assertEquals("Name", post.author?.lastName)
    }
    
    @Test
    fun `test afterBuild callback`() {
        factory<User> {
            firstName { "John" }
            
            afterBuild { user ->
                user.initialized = true
            }
        }
        
        val user = build<User>()
        assertTrue(user.initialized)
    }
    
    @Test
    fun `test afterCreate callback`() {
        var createdUserId: Long? = null
        
        factory<User> {
            firstName { "John" }
            
            afterCreate { user ->
                user.id = 123L
                createdUserId = user.id
            }
        }
        
        val user = create<User>()
        assertEquals(123L, user.id)
        assertEquals(123L, createdUserId)
    }
    
    @Test
    fun `test build vs create strategies`() {
        var buildCallbackCalled = false
        var createCallbackCalled = false

        factory<User> {
            firstName { "John" }

            afterBuild {
                buildCallbackCalled = true
            }

            afterCreate {
                createCallbackCalled = true
            }
        }

        build<User>()
        assertTrue(buildCallbackCalled)
        assertTrue(!createCallbackCalled)

        buildCallbackCalled = false
        createCallbackCalled = false

        create<User>()
        assertTrue(buildCallbackCalled)
        assertTrue(createCallbackCalled)
    }
    
    @Test
    fun `test buildStubbed strategy`() {
        var stubbedId = 1L
        
        factory<User> {
            firstName { "John" }
        }
        
        val user = buildStubbed<User>()
        assertEquals("John", user.firstName)
    }
    
    @Test
    fun `test factory inheritance`() {
        factory<User> {
            firstName { "Base" }
            lastName { "User" }
            role { Role.USER }
        }
        
        factory<User>("admin_user", parent = "User") {
            role { Role.ADMIN }
        }
        
        val baseUser = build<User>()
        assertEquals("Base", baseUser.firstName)
        assertEquals(Role.USER, baseUser.role)
        
        val adminUser = build<User>("admin_user")
        assertEquals("Base", adminUser.firstName)
        assertEquals(Role.ADMIN, adminUser.role)
    }
    
    @Test
    fun `test attribute lazy evaluation`() {
        var evaluationCount = 0
        
        factory<User> {
            firstName { 
                evaluationCount++
                "John"
            }
        }
        
        assertEquals(0, evaluationCount)
        
        build<User>()
        assertEquals(1, evaluationCount)
        
        build<User>()
        assertEquals(2, evaluationCount)
    }
    
    @Test
    fun `test different instances from factory`() {
        factory<User> {
            firstName { "John" }
        }

        val user1 = build<User>()
        val user2 = build<User>()

        assertTrue(user1 !== user2)
        user1.firstName = "Changed"
        assertEquals("Changed", user1.firstName)
        assertEquals("John", user2.firstName)
    }

    @Test
    fun `build throws when factory not found`() {
        val exception = assertThrows<IllegalArgumentException> {
            FactoryBot.build<User>("NonExistentFactory")
        }
        assertTrue(exception.message?.contains("Factory not found") == true)
    }

    @Test
    fun `build throws when trait not found`() {
        factory<User> {
            firstName { "John" }
        }

        val exception = assertThrows<IllegalArgumentException> {
            build<User>("nonExistentTrait")
        }
        assertTrue(exception.message?.contains("Trait 'nonExistentTrait' not found") == true)
        assertTrue(exception.message?.contains("Available traits:") == true)
    }

    @Test
    fun `build throws when property not found`() {
        factory<User> {
            firstName { "John" }
            attribute("invalidProperty") { "value" }
        }

        val exception = assertThrows<IllegalArgumentException> {
            build<User>()
        }
        assertTrue(exception.message?.contains("Property 'invalidProperty' not found") == true)
        assertTrue(exception.message?.contains("Available properties:") == true)
    }

    @Test
    fun `nextSequence throws when sequence not found`() {
        val exception = assertThrows<IllegalArgumentException> {
            FactoryBot.nextSequence<String>("nonExistentSequence")
        }
        assertTrue(exception.message?.contains("Sequence not found") == true)
    }

    @Test
    fun `generate throws when sequence not found`() {
        factory<User> {
            email { generate("nonExistentSequence") }
        }

        assertThrows<IllegalArgumentException> {
            build<User>()
        }
    }

    @Test
    fun `sequence is thread-safe`() = runBlocking {
        defineSequence("thread_safe_email") { n -> "user$n@test.com" }

        factory<User> {
            email { generate("thread_safe_email") }
        }

        val results = (1..100).map {
            async(Dispatchers.Default) {
                build<User>().email
            }
        }.awaitAll()

        results.forEach { email ->
            assertTrue(email.matches(Regex("user\\d+@test\\.com")))
        }

        assertEquals(100, results.size)
        assertEquals(100, results.toSet().size, "All emails should be unique: ${results.groupingBy { it }.eachCount().filter { it.value > 1 }}")
    }

    @Test
    fun `concurrent builds produce independent instances`() = runBlocking {
        factory<User> {
            firstName { "John" }
            lastName { "Doe" }
        }

        val users = (1..100).map {
            async(Dispatchers.Default) {
                build<User>()
            }
        }.awaitAll()

        assertEquals(100, users.map { System.identityHashCode(it) }.toSet().size)

        users.forEach { user ->
            assertEquals("John", user.firstName)
            assertEquals("Doe", user.lastName)
        }
    }

    @Test
    fun `concurrent factory registration is thread-safe`() = runBlocking {
        (1..50).map { index ->
            async(Dispatchers.Default) {
                factory<User>("User$index") {
                    firstName { "User$index" }
                }
            }
        }.awaitAll()

        (1..50).forEach { index ->
            val user = FactoryBot.build<User>("User$index")
            assertEquals("User$index", user.firstName)
        }
    }

    @Test
    fun `concurrent sequence generation maintains monotonicity`() = runBlocking {
        defineSequence("concurrent_seq") { n -> n }

        val results = (1..1000).map {
            async(Dispatchers.Default) {
                FactoryBot.nextSequence<Long>("concurrent_seq")
            }
        }.awaitAll()

        assertEquals(1000, results.toSet().size)

        val sortedResults = results.sorted()
        assertEquals((1L..1000L).toList(), sortedResults)
    }

    @Test
    fun `trait callbacks are executed in correct order`() {
        val executionOrder = mutableListOf<String>()

        factory<User> {
            firstName { "John" }

            afterBuild { executionOrder.add("factory-build") }
            afterCreate { executionOrder.add("factory-create") }

            trait("admin") {
                role { Role.ADMIN }

                afterBuild { executionOrder.add("trait-build") }
                afterCreate { executionOrder.add("trait-create") }
            }
        }

        executionOrder.clear()
        build<User>("admin")
        assertEquals(listOf("factory-build", "trait-build"), executionOrder)

        executionOrder.clear()
        create<User>("admin")
        assertEquals(listOf("factory-build", "trait-build", "factory-create", "trait-create"), executionOrder)
    }

    @Test
    fun `parent callbacks are inherited and executed first`() {
        val executionOrder = mutableListOf<String>()

        factory<User> {
            firstName { "Base" }
            afterBuild { executionOrder.add("parent") }
        }

        factory<User>("admin_user", parent = "User") {
            role { Role.ADMIN }
            afterBuild { executionOrder.add("child") }
        }

        build<User>("admin_user")
        assertEquals(listOf("parent", "child"), executionOrder)
    }

    @Test
    fun `override has highest priority`() {
        factory<User> {
            firstName { "Factory" }

            trait("admin") {
                firstName { "Trait" }
            }
        }

        val user = FactoryBot.build<User>(traitNames = arrayOf("admin")) {
            firstName { "Override" }
        }

        assertEquals("Override", user.firstName)
    }

    @Test
    fun `trait overrides factory attributes`() {
        factory<User> {
            firstName { "Factory" }
            role { Role.USER }

            trait("admin") {
                firstName { "Admin" }
            }
        }

        val user = build<User>("admin")
        assertEquals("Admin", user.firstName)
    }

    @Test
    fun `sequence identity caching works correctly`() {
        factory<User> {
            email { sequence { n -> "user$n@test.com" } }
            firstName { sequence { n -> "User$n" } }
        }

        val user1 = build<User>()
        val user2 = build<User>()

        assertEquals("user1@test.com", user1.email)
        assertEquals("user2@test.com", user2.email)
        assertEquals("User1", user1.firstName)
        assertEquals("User2", user2.firstName)
    }

    @Test
    fun `association with different strategies`() {
        var userCreated = false

        factory<User> {
            firstName { "Author" }
            afterCreate { userCreated = true }
        }

        factory<Post> {
            title { "Post" }
            author { association<User>() }
        }

        val post = build<Post>()
        assertNotNull(post.author)
        assertTrue(!userCreated)

        userCreated = false
        val createdPost = create<Post>()
        assertNotNull(createdPost.author)
        assertTrue(!userCreated)
    }

    @Test
    fun `multiple traits applied in order`() {
        factory<User> {
            firstName { "Base" }
            lastName { "Base" }

            trait("first") {
                firstName { "First" }
            }

            trait("second") {
                firstName { "Second" }
                lastName { "Second" }
            }
        }

        val user = build<User>("first", "second")
        assertEquals("Second", user.firstName)
        assertEquals("Second", user.lastName)
    }

    @Test
    fun `EvaluationContext stores and retrieves attributes`() {
        val context = EvaluationContext(BuildStrategy.Build, "User")

        context.setAttribute("key1", "value1")
        context.setAttribute("key2", 42)

        assertEquals("value1", context.getAttribute("key1"))
        assertEquals(42, context.getAttribute("key2"))
        assertEquals(null, context.getAttribute("nonExistent"))
    }

    @Test
    fun `clear resets all factories and sequences`() {
        factory<User> {
            firstName { "John" }
        }
        defineSequence("test_seq") { n -> n }

        build<User>()
        FactoryBot.nextSequence<Long>("test_seq")

        FactoryBot.clear()

        assertThrows<IllegalArgumentException> {
            build<User>()
        }

        assertThrows<IllegalArgumentException> {
            FactoryBot.nextSequence<Long>("test_seq")
        }
    }
}
