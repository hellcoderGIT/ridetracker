package com.adsamcik.signalcollector.signin

import android.annotation.SuppressLint
import androidx.annotation.RestrictTo
import com.adsamcik.signalcollector.utility.Constants.DAY_IN_MILLISECONDS
import com.squareup.moshi.FromJson
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import java.util.*

/**
 * Class that contains all the data associated with the user.
 * It has two states, the first is signed-in with basic info available.
 * The second is when all data from server are available.
 */
class User(@Transient val id: String, @Transient val token: String) {
    /**
     * Number of Wireless Points the user owns.
     * It might not reflect the actual amount, because server does not instantly update this amount on the clientAuth.
     */
    var wirelessPoints: Long = 0
        private set

    /**
     * Server information about availability of services etc.
     */
    var networkInfo: NetworkInfo? = null
        private set

    /**
     * Server preferences.
     */
    var networkPreferences: NetworkPreferences? = null
        private set

    private var callbackList: MutableList<(User) -> Unit>? = null

    /**
     * Returns true if server data are available
     */
    val isServerDataAvailable: Boolean
        get() = networkInfo != null && networkPreferences != null

    /**
     * Add wireless points to the user.
     * This method helps with offsetting some synchronisation issues.
     */
    fun addWirelessPoints(value: Long) {
        wirelessPoints += value
    }

    //No need to use from json value because it is already updated
    @SuppressLint("CheckResult")
            /**
             * This method should be called when server data are available.
             * It automatically fills in the data from the server to this instance.
             *
             * @param json Serialized JSON with server data
             */
    fun deserializeServerData(json: String) {
        val moshi = Moshi.Builder().add(ServerUserDeserializer(this)).build()
        val adapter = moshi.adapter(User::class.java)
        adapter.fromJson(json)
    }

    /**
     * Sets server data.
     * This is internal function, do not use.
     * It is internal, because it needs to be exposed to inner class.
     */
    @RestrictTo(RestrictTo.Scope.SUBCLASSES)
    internal fun setServerData(wirelessPoints: Long, networkInfo: NetworkInfo, networkPreferences: NetworkPreferences) {
        this.wirelessPoints = wirelessPoints
        this.networkInfo = networkInfo
        this.networkPreferences = networkPreferences

        if (callbackList != null) {
            for (cb in callbackList!!)
                cb.invoke(this)
            callbackList = null
        }
    }

    /**
     * Mocks server data
     */
    internal fun mockServerData() {
        //Needs to be handled better due to pre-launch testing and firebase Test Lab
        /*if (useMock && !BuildConfig.DEBUG)
            throw RuntimeException("Cannot mock server data on production version")*/
        val networkPreferences = NetworkPreferences()
        networkPreferences.renewMap = true
        networkPreferences.renewPersonalMap = false

        val networkInfo = NetworkInfo()

        networkInfo.feedbackAccess = false
        networkInfo.mapAccessUntil = System.currentTimeMillis() + DAY_IN_MILLISECONDS
        networkInfo.personalMapAccessUntil = 0

        setServerData((Math.random() * 64546).toLong(), networkInfo, networkPreferences)
    }

    /**
     * Add callback to when server data are available.
     */
    fun addServerDataCallback(callback: (User) -> Unit) {
        if (isServerDataAvailable)
            callback.invoke(this)
        else {
            if (callbackList == null)
                callbackList = ArrayList()
            callbackList!!.add(callback)
        }
    }



    private inner class ServerUserDeserializer constructor(private val user: User) {
        @FromJson
        fun fromJson(userJson: UserJson): User? {
            return if (!userJson.isValid())
                null
            else {
                user.setServerData(userJson.wirelessPoints!!, userJson.networkInfo!!, userJson.networkPreferences!!)
                user
            }
        }

        @ToJson
        fun toJson(user: User?): String {
            throw NotImplementedError()
        }
    }
}

/**
 * Class that holds information about user's basic information.
 */
class NetworkInfo {
    /**
     * When does user's map access expire.
     */
    var mapAccessUntil: Long = 0

    /**
     * When does user's personal map access expire.
     */
    var personalMapAccessUntil: Long = 0

    /**
     * Can the user upload feedback.
     */
    var feedbackAccess: Boolean = false

    /**
     * Upload access is currently unused on the mobile device, because synchronization needs to be tested first.
     * todo add this to the uploader so restriction on upload is applied sooner.
     */
    var uploadAccess: Boolean = false


    /**
     * Returns true if user has access to the map.
     */
    fun hasMapAccess(): Boolean = System.currentTimeMillis() < mapAccessUntil

    /**
     * Returns true if user has access to the personal map.
     */
    fun hasPersonalMapAccess(): Boolean = System.currentTimeMillis() < personalMapAccessUntil
}

/**
 * Class that holds information about network preferences.
 */
class NetworkPreferences {
    var renewMap: Boolean = false
    var renewPersonalMap: Boolean = false
}

internal class UserJson {
    var wirelessPoints: Long? = null
    var networkInfo: NetworkInfo? = null
    var networkPreferences: NetworkPreferences? = null

    fun isValid() = wirelessPoints != null && networkInfo != null && networkPreferences != null
}
