package com.example.inventoryapp

import com.example.inventoryapp.data.local.dao.SupplierIssueDao
import com.example.inventoryapp.data.local.entity.ConnectionStatus
import com.example.inventoryapp.data.local.entity.IssueSeverity
import com.example.inventoryapp.data.local.entity.PartnerConnectionEntity
import com.example.inventoryapp.data.local.entity.PartnerConnectionWithUser
import com.example.inventoryapp.data.local.entity.PurchaseOrderEntity
import com.example.inventoryapp.data.local.entity.PurchaseOrderStatus
import com.example.inventoryapp.data.local.entity.SupplierIssueEntity
import com.example.inventoryapp.data.local.entity.SupplierIssueStatus
import com.example.inventoryapp.data.local.entity.UserEntity
import com.example.inventoryapp.data.local.entity.UserRole
import com.example.inventoryapp.data.repository.SupplierIssueRepository
import com.example.inventoryapp.ui.supplier.buildMiddlemanDirectory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSupplierIssueDao : SupplierIssueDao {
    private val store = linkedMapOf<String, SupplierIssueEntity>()
    private val flow = MutableStateFlow<List<SupplierIssueEntity>>(emptyList())

    private fun refresh() {
        flow.value = store.values.sortedByDescending { it.createdAt }
    }

    override fun observeAllIssues(): Flow<List<SupplierIssueEntity>> = flow

    override fun observeIssuesForSupplier(supplierId: String): Flow<List<SupplierIssueEntity>> =
        MutableStateFlow(store.values.filter { it.supplierId == supplierId })

    override fun observeOpenIssuesCount(): Flow<Int> =
        MutableStateFlow(
            store.values.count {
                it.status == SupplierIssueStatus.OPEN ||
                    it.status == SupplierIssueStatus.ACKNOWLEDGED ||
                    it.status == SupplierIssueStatus.REPLACEMENT_SENT
            }
        )

    override suspend fun getIssueById(id: String): SupplierIssueEntity? = store[id]

    override suspend fun insertIssue(issue: SupplierIssueEntity) {
        store[issue.id] = issue
        refresh()
    }

    override suspend fun updateIssue(issue: SupplierIssueEntity) {
        store[issue.id] = issue
        refresh()
    }

    override suspend fun deleteIssue(id: String) {
        store.remove(id)
        refresh()
    }
}

class SupplierIssueAndMiddlemanUnitTest {
    private lateinit var fakeDao: FakeSupplierIssueDao
    private lateinit var repository: SupplierIssueRepository

    @Before
    fun setup() {
        fakeDao = FakeSupplierIssueDao()
        repository = SupplierIssueRepository(fakeDao)
    }

    @Test
    fun testReportAndResolveIssueLifecycle() = runBlocking {
        // Blank description should fail validation
        val invalid = repository.reportIssue(
            supplierId = "sup_pacific_seafood",
            supplierName = "Pacific Seafood Supply",
            middlemanId = "user_middleman",
            middlemanName = "Anthony",
            middlemanCode = "MID-1001",
            issueType = "Damaged Goods",
            severity = IssueSeverity.HIGH,
            affectedItemName = "King Crab Cluster",
            affectedQuantity = 2,
            description = "   "
        )
        assertTrue(invalid.isFailure)

        // Valid issue report
        val createdRes = repository.reportIssue(
            supplierId = "sup_pacific_seafood",
            supplierName = "Pacific Seafood Supply",
            middlemanId = "user_middleman",
            middlemanName = "Anthony",
            middlemanCode = "MID-1001",
            orderId = "po_real_001",
            orderNumber = "PO-2026-0842",
            issueType = "Missing / Short Quantity",
            severity = IssueSeverity.HIGH,
            affectedItemName = "Coca-Cola Original (330ml Can)",
            affectedQuantity = 5,
            description = "5 cans missing from sealed case #2"
        )
        assertTrue(createdRes.isSuccess)
        val created = createdRes.getOrThrow()
        assertTrue(created.issueCode.startsWith("ISS-"))
        assertEquals(SupplierIssueStatus.OPEN, created.status)
        assertEquals(1, repository.allIssues.first().size)

        // Supplier acknowledges and resolves
        val ackRes = repository.updateIssueStatus(
            created.id,
            SupplierIssueStatus.ACKNOWLEDGED,
            "Checking dispatch manifest"
        )
        assertTrue(ackRes.isSuccess)
        assertEquals(SupplierIssueStatus.ACKNOWLEDGED, ackRes.getOrThrow().status)

        val resolvedRes = repository.updateIssueStatus(
            created.id,
            SupplierIssueStatus.RESOLVED,
            "Replacement 5 cans delivered"
        )
        assertTrue(resolvedRes.isSuccess)
        val resolved = resolvedRes.getOrThrow()
        assertEquals(SupplierIssueStatus.RESOLVED, resolved.status)
        assertNotNull(resolved.resolvedAt)
        assertEquals("Replacement 5 cans delivered", resolved.supplierResponse)
    }

    @Test
    fun testBuildMiddlemanDirectoryAggregatesOrdersIssuesAndConnections() {
        val middleman1 = UserEntity(
            id = "user_middleman",
            fullName = "Anthony (Middle Man)",
            email = "middleman@nawano.com",
            passwordHash = "hash",
            role = UserRole.OWNER,
            uniqueCode = "MID-1001",
            phoneNumber = "+1 555 019 2831"
        )
        val middleman2 = UserEntity(
            id = "user_middleman_david",
            fullName = "David Kim (Middle Man)",
            email = "david@nawano.com",
            passwordHash = "hash",
            role = UserRole.OWNER,
            uniqueCode = "MID-1002",
            phoneNumber = "+1 555 012 4480"
        )
        val supplierUser = UserEntity(
            id = "user_supplier_pacific",
            fullName = "Pacific Seafood Supply (Supplier)",
            email = "supplier@nawano.com",
            passwordHash = "hash",
            role = UserRole.SUPPLIER,
            uniqueCode = "SUP-2001"
        )

        val connections = listOf(
            PartnerConnectionWithUser(
                connection = PartnerConnectionEntity(
                    requesterId = middleman1.id,
                    receiverId = supplierUser.id,
                    requesterRole = UserRole.OWNER,
                    receiverRole = UserRole.SUPPLIER,
                    status = ConnectionStatus.ACCEPTED
                ),
                partnerUser = middleman1
            )
        )

        val orders = listOf(
            PurchaseOrderEntity(
                id = "po1",
                orderNumber = "PO-2026-0843",
                supplierId = "sup_foodie",
                supplierName = "Foodie Supplier Co.",
                status = PurchaseOrderStatus.DELIVERED,
                totalAmount = 243.0,
                totalQuantity = 54,
                picSender = "David Kim"
            ),
            PurchaseOrderEntity(
                id = "po2",
                orderNumber = "PO-2026-0844",
                supplierId = "sup_pacific_seafood",
                supplierName = "Pacific Seafood Supply",
                status = PurchaseOrderStatus.PENDING,
                totalAmount = 100.0,
                totalQuantity = 20,
                picSender = "Anthony"
            )
        )

        val issues = listOf(
            SupplierIssueEntity(
                id = "iss1",
                issueCode = "ISS-001",
                supplierId = "sup_foodie",
                supplierName = "Foodie Supplier Co.",
                middlemanId = middleman2.id,
                middlemanName = "David Kim",
                middlemanCode = "MID-1002",
                issueType = "Damaged Goods",
                description = "Cracked jars",
                status = SupplierIssueStatus.OPEN
            )
        )

        val directory = buildMiddlemanDirectory(
            middlemanUsers = listOf(middleman1, middleman2),
            activeConnections = connections,
            orders = orders,
            issues = issues
        )

        assertEquals(2, directory.size)
        // Sorted by openIssuesCount descending, so David Kim comes first
        val davidEntry = directory.first()
        assertEquals("David Kim", davidEntry.displayName)
        assertEquals(1, davidEntry.openIssuesCount)
        assertEquals(1, davidEntry.ordersCount)
        assertEquals(243.0, davidEntry.totalVolumeUsd, 0.01)

        val anthonyEntry = directory[1]
        assertEquals("Anthony", anthonyEntry.displayName)
        assertTrue(anthonyEntry.isConnectedPartner)
        assertEquals(1, anthonyEntry.ordersCount)
    }
}
