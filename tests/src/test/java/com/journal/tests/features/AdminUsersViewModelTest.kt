package com.journal.tests.features

import com.journal.core.data.repository.AdminRepository
import com.journal.core.data.repository.AdminUsersData
import com.journal.core.data.util.Resource
import com.journal.core.model.teacher.AdminUser
import com.journal.features.admin.users.AdminUsersViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminUsersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AdminRepository

    private val user = AdminUser(id = "user-1", fullName = "Ada Lovelace")
    private val data = AdminUsersData(users = listOf(user), total = 1, page = 1, pageSize = 20)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `success updates users and total`() = runTest {
        every {
            repository.getUsers(any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Success(data))

        val vm = AdminUsersViewModel(repository)
        vm.loadUsers(page = 1, pageSize = 20, query = "", userType = "", status = "")
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(user), users)
            assertEquals(1, total)
            assertNull(error)
            assertFalse(isOffline)
        }
    }

    @Test
    fun `error with cached data marks state offline`() = runTest {
        every {
            repository.getUsers(any(), any(), any(), any(), any(), any())
        } returns flowOf(Resource.Error(RuntimeException("offline"), data))

        val vm = AdminUsersViewModel(repository)
        vm.loadUsers(page = 1, pageSize = 20, query = "", userType = "", status = "")
        testDispatcher.scheduler.advanceUntilIdle()

        with(vm.uiState.value) {
            assertFalse(isLoading)
            assertEquals(listOf(user), users)
            assertNull(error)
            assertTrue(isOffline)
        }
    }
}
