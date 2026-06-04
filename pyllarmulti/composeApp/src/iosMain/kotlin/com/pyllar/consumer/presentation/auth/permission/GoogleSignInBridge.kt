package com.pyllar.consumer.presentation.auth.permission

interface IosGoogleSignInBridge {
    fun tryRestoreEmail(completion: (String?) -> Unit)
    fun pickEmail(completion: (String?) -> Unit)
}

object SwiftGoogleSignInScope {
    var bridge: IosGoogleSignInBridge? = null
}
