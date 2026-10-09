package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE status = :status ORDER BY createdAt DESC")
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    suspend fun getOrderById(id: String): OrderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :status WHERE id = :id")
    suspend fun updateOrderStatus(id: String, status: String)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrder(id: String)
}

@Dao
interface PartnerAccountDao {
    @Query("SELECT * FROM partner_accounts WHERE mobileNumber = :mobile LIMIT 1")
    suspend fun getPartnerByMobile(mobile: String): PartnerAccountEntity?

    @Query("SELECT * FROM partner_accounts WHERE id = :id LIMIT 1")
    suspend fun getPartnerById(id: String): PartnerAccountEntity?

    @Query("SELECT * FROM partner_accounts ORDER BY createdAt DESC")
    fun getAllPartners(): Flow<List<PartnerAccountEntity>>

    @Query("SELECT * FROM partner_accounts WHERE status = 'PENDING_VERIFICATION' ORDER BY createdAt DESC")
    fun getPendingPartners(): Flow<List<PartnerAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPartner(partner: PartnerAccountEntity)

    @Update
    suspend fun updatePartner(partner: PartnerAccountEntity)

    @Query("UPDATE partner_accounts SET status = :status, reviewedBy = :reviewer, approvedAt = :approvedTime WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, reviewer: String, approvedTime: Long)

    @Query("UPDATE partner_accounts SET status = 'REJECTED', rejectionReason = :reason, reviewedBy = :reviewer WHERE id = :id")
    suspend fun rejectPartner(id: String, reason: String, reviewer: String)

    @Query("UPDATE partner_accounts SET status = 'ACTION_REQUIRED', actionRequiredMessage = :msg, reviewedBy = :reviewer WHERE id = :id")
    suspend fun requestAction(id: String, msg: String, reviewer: String)

    @Query("UPDATE partner_accounts SET status = 'SUSPENDED', rejectionReason = :reason, reviewedBy = :reviewer WHERE id = :id")
    suspend fun suspendPartner(id: String, reason: String, reviewer: String)
}

@Dao
interface WorkCategoryDao {
    @Query("SELECT * FROM work_categories ORDER BY baseRate ASC")
    fun getAllCategories(): Flow<List<WorkCategoryEntity>>

    @Query("SELECT * FROM work_categories")
    suspend fun getCategoriesList(): List<WorkCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<WorkCategoryEntity>)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM active_session WHERE id = 1 LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSession(session: SessionEntity)

    @Query("DELETE FROM active_session")
    suspend fun clearSession()
}
