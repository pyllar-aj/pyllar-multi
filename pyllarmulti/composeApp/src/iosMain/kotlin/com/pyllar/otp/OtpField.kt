package com.pyllar.otp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import platform.UIKit.UITextField
import platform.UIKit.UITextContentTypeOneTimeCode
import platform.UIKit.UIKeyboardTypeNumberPad
import platform.UIKit.UIControlEventEditingChanged
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UITextBorderStyle
import platform.UIKit.UIColor
import platform.darwin.NSObject
import platform.Foundation.NSSelectorFromString
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCAction

@OptIn(BetaInteropApi::class)
private class TextFieldTarget(
    private val onTextChanged: (String) -> Unit
) : NSObject() {
    @ObjCAction
    fun textFieldDidChange(sender: UITextField) {
        onTextChanged(sender.text ?: "")
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun OtpField(
    length: Int,
    modifier: Modifier,
    enabled: Boolean,
    isError: Boolean,
    otpFieldValue: TextFieldValue,
    onOtpFieldValueChange: (TextFieldValue) -> Unit,
    onOtpComplete: () -> Unit,
) {
    val target = remember(onOtpFieldValueChange) {
        TextFieldTarget { text ->
            val cleanText = text.take(length).filter { it.isDigit() }
            onOtpFieldValueChange(TextFieldValue(cleanText, TextRange(cleanText.length)))
            if (cleanText.length == length) {
                onOtpComplete()
            }
        }
    }

    UIKitView(
        factory = {
            val textField = UITextField().apply {
                this.keyboardType = UIKeyboardTypeNumberPad
                this.textContentType = UITextContentTypeOneTimeCode
                this.textAlignment = NSTextAlignmentCenter
                this.borderStyle = UITextBorderStyle.UITextBorderStyleRoundedRect
                this.placeholder = "••••••"
                
                this.addTarget(
                    target = target,
                    action = NSSelectorFromString("textFieldDidChange:"),
                    forControlEvents = UIControlEventEditingChanged
                )
            }
            textField
        },
        modifier = modifier,
        update = { textField ->
            textField.enabled = enabled
            if (textField.text != otpFieldValue.text) {
                textField.text = otpFieldValue.text
            }
            if (isError) {
                textField.layer.borderColor = UIColor.redColor.CGColor
                textField.layer.borderWidth = 1.0
            } else {
                textField.layer.borderColor = null
                textField.layer.borderWidth = 0.0
            }
        }
    )
}
