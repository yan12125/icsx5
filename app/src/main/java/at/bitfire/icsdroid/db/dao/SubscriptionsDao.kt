package at.bitfire.icsdroid.db.dao

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.room.*
import at.bitfire.icsdroid.db.entity.Subscription

@Dao
interface SubscriptionsDao {

    @Insert
    fun add(vararg subscriptions: Subscription)

    @Delete
    fun delete(vararg subscriptions: Subscription)

    @Query("SELECT * FROM subscriptions")
    fun getAllLive(): LiveData<List<Subscription>>

    @Query("SELECT * FROM subscriptions")
    fun getAll(): List<Subscription>

    @Query("SELECT * FROM subscriptions WHERE id=:id")
    fun getById(id: Long): Subscription?

    @Query("SELECT errorMessage FROM subscriptions WHERE id=:id")
    fun getErrorMessageLive(id: Long): LiveData<String?>

    @Update
    fun update(vararg subscriptions: Subscription)

    @Query("UPDATE subscriptions SET lastSync=:lastSync WHERE id=:id")
    fun updateStatusNotModified(id: Long, lastSync: Long = System.currentTimeMillis())

    @Query("UPDATE subscriptions SET eTag=:eTag, lastModified=:lastModified, lastSync=:lastSync, errorMessage=null WHERE id=:id")
    fun updateStatusSuccess(id: Long, eTag: String?, lastModified: Long?, lastSync: Long = System.currentTimeMillis())

    @Query("UPDATE subscriptions SET errorMessage=:message WHERE id=:id")
    fun updateStatusError(id: Long, message: String?)

    @Query("UPDATE subscriptions SET url=:url WHERE id=:id")
    fun updateUrl(id: Long, url: Uri)

}