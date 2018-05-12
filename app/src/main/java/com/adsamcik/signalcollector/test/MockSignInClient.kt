package com.adsamcik.signalcollector.test

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import com.adsamcik.signalcollector.signin.ISignInClient
import com.adsamcik.signalcollector.signin.User
import com.adsamcik.signalcollector.utility.Preferences
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.concurrent.TimeUnit

/**
 * Mocking Sign in client
 */
class MockSignInClient : ISignInClient {
    private var u: User? = null
    private var userValueCallback: ((Context, User?) -> Unit)? = null

    override fun signIn(activity: AppCompatActivity, userValueCallback: (Context, User?) -> Unit) {
        signInSilent(activity, userValueCallback)
    }

    override fun signInSilent(context: Context, userValueCallback: (Context, User?) -> Unit) {
        if (u != null) {
            userValueCallback.invoke(context, u)
            return
        }

        val state = (Math.random() * 4).toInt()
        if (state == 2) {
            userValueCallback.invoke(context, null)
            return
        }

        val user = User("MOCKED", "BLEH")
        Preferences.getPref(context).edit().putString(Preferences.PREF_USER_ID, user.id).apply()
        when (state) {
            0 -> user.mockServerData()
            1 -> {
                //server data received later on
                launch {
                    delay(2, TimeUnit.SECONDS)
                    user.mockServerData()
                }
            }
            3 -> {
            }
        }//no server data received

        userValueCallback.invoke(context, user)
        u = user
        this.userValueCallback = userValueCallback
    }

    override fun signOut(context: Context) {
        userValueCallback!!.invoke(context, null)
        u = null
    }

    override fun onSignInResult(activity: AppCompatActivity, resultCode: Int, data: Intent) {
        //do nothing
    }

}
