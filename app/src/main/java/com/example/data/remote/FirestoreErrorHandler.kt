package com.example.data.remote

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType {
    CREATE, UPDATE, DELETE, GET, LIST, WRITE
}

fun handleFirestoreError(
    error: Throwable,
    operationType: OperationType,
    path: String?
): Exception {
    val currentUser = try {
        Firebase.auth.currentUser
    } catch (_: Exception) {
        null
    }

    val providerDataArray = JSONArray()
    currentUser?.providerData?.forEach { userInfo ->
        providerDataArray.put(
            JSONObject().apply {
                put("providerId", userInfo.providerId)
                put("displayName", userInfo.displayName ?: JSONObject.NULL)
                put("email", userInfo.email ?: JSONObject.NULL)
                put("photoUrl", userInfo.photoUrl?.toString() ?: JSONObject.NULL)
            }
        )
    }

    val authInfo = JSONObject().apply {
        put("userId", currentUser?.uid ?: JSONObject.NULL)
        put("email", currentUser?.email ?: JSONObject.NULL)
        put("emailVerified", currentUser?.isEmailVerified ?: JSONObject.NULL)
        put("isAnonymous", currentUser?.isAnonymous ?: JSONObject.NULL)
        put("tenantId", currentUser?.tenantId ?: JSONObject.NULL)
        put("providerInfo", providerDataArray)
    }

    val errInfo = JSONObject().apply {
        put("error", error.message ?: error.toString())
        put("operationType", operationType.name.lowercase())
        put("path", path ?: JSONObject.NULL)
        put("authInfo", authInfo)
    }

    val jsonString = errInfo.toString()
    Log.e("FirestoreError", jsonString, error)

    return RuntimeException(jsonString, error)
}
