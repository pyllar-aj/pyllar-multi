import UIKit
import SwiftUI
import ComposeApp
import CryptoKit
import GoogleSignIn
import Clarity
import AppsFlyerLib
import Singular

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Initialize bridge for KMP
        SwiftCryptoScope.shared.bridge = SwiftCryptoBridge()
        SwiftGoogleSignInScope.shared.bridge = SwiftGoogleSignInBridge()
        SwiftAnalyticsScope.shared.bridge = SwiftAnalyticsBridge()
        return MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

class SwiftCryptoBridge: IosCryptoBridge {
    func generateEcdhKeyPair() -> EcdhKeyResult {
        let privateKey = P256.KeyAgreement.PrivateKey()
        let publicKeyRaw = privateKey.publicKey.x963Representation
        
        let spkiHeader: [UInt8] = [
            0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x02, 0x01,
            0x06, 0x08, 0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00
        ]
        var publicKeyBytes = spkiHeader
        publicKeyBytes.append(contentsOf: publicKeyRaw)
        
        let publicKeyKotlin = KotlinByteArray(size: Int32(publicKeyBytes.count))
        for i in 0..<publicKeyBytes.count {
            publicKeyKotlin.set(index: Int32(i), value: Int8(bitPattern: publicKeyBytes[i]))
        }
        
        return EcdhKeyResult(publicKey: publicKeyKotlin, privateKeyOpaque: privateKey)
    }
    
    func deriveSharedSecret(privateKeyOpaque: Any, serverPublicKey: KotlinByteArray) -> KotlinByteArray {
        guard let privateKey = privateKeyOpaque as? P256.KeyAgreement.PrivateKey else {
            fatalError("Invalid private key format")
        }
        
        let serverPublicKeyBytes = toSwiftArray(serverPublicKey)
        
        // Strip SPKI header if present (26 bytes for P-256)
        var rawKeyBytes = serverPublicKeyBytes
        if serverPublicKeyBytes.count == 91 && serverPublicKeyBytes[0] == 0x30 {
            rawKeyBytes = Array(serverPublicKeyBytes[26...])
        }
        
        let serverKey = try! P256.KeyAgreement.PublicKey(x963Representation: rawKeyBytes)
        let sharedSecret = try! privateKey.sharedSecretFromKeyAgreement(with: serverKey)
        
        let secretBytes = sharedSecret.withUnsafeBytes { buffer in
            Array(buffer.bindMemory(to: UInt8.self))
        }
        return toKotlinByteArray(secretBytes)
    }
    
    func encryptAesGcm(plaintext: KotlinByteArray, key: KotlinByteArray, iv: KotlinByteArray) -> KotlinByteArray {
        let plainBytes = toSwiftArray(plaintext)
        let keyBytes = toSwiftArray(key)
        let ivBytes = toSwiftArray(iv)
        
        let symmetricKey = SymmetricKey(data: keyBytes)
        let nonce = try! AES.GCM.Nonce(data: ivBytes)
        
        let sealedBox = try! AES.GCM.seal(plainBytes, using: symmetricKey, nonce: nonce)
        let ciphertext = sealedBox.ciphertext
        let tag = sealedBox.tag
        
        var combined = [UInt8]()
        combined.append(contentsOf: ciphertext)
        combined.append(contentsOf: tag)
        
        return toKotlinByteArray(combined)
    }
    
    func decryptAesGcm(ciphertext: KotlinByteArray, key: KotlinByteArray, iv: KotlinByteArray) -> KotlinByteArray {
        let combinedBytes = toSwiftArray(ciphertext)
        let keyBytes = toSwiftArray(key)
        let ivBytes = toSwiftArray(iv)
        
        let symmetricKey = SymmetricKey(data: keyBytes)
        let nonce = try! AES.GCM.Nonce(data: ivBytes)
        
        let tagLength = 16
        let actualCiphertextCount = combinedBytes.count - tagLength
        let actualCiphertextData = Data(combinedBytes[0..<actualCiphertextCount])
        let tagData = Data(combinedBytes[actualCiphertextCount..<combinedBytes.count])
        
        let sealedBox = try! AES.GCM.SealedBox(nonce: nonce, ciphertext: actualCiphertextData, tag: tagData)
        let decrypted = try! AES.GCM.open(sealedBox, using: symmetricKey)
        
        return toKotlinByteArray(Array(decrypted))
    }
    
    func computeHmacSha256(data: KotlinByteArray, key: KotlinByteArray) -> KotlinByteArray {
        let dataBytes = toSwiftArray(data)
        let keyBytes = toSwiftArray(key)
        
        let symmetricKey = SymmetricKey(data: keyBytes)
        let mac = HMAC<SHA256>.authenticationCode(for: dataBytes, using: symmetricKey)
        
        return toKotlinByteArray(Array(mac))
    }
    
    func saveToKeychain(key: String, value: String) -> Bool {
        guard let data = value.data(using: .utf8) else { return false }
        
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlock
        ]
        
        SecItemDelete(query as CFDictionary) // Delete any existing item
        let status = SecItemAdd(query as CFDictionary, nil)
        return status == errSecSuccess
    }
    
    func loadFromKeychain(key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var dataTypeRef: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &dataTypeRef)
        
        if status == errSecSuccess, let data = dataTypeRef as? Data {
            return String(data: data, encoding: .utf8)
        }
        return nil
    }
    
    func deleteFromKeychain(key: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key
        ]
        SecItemDelete(query as CFDictionary)
    }
    
    private func toSwiftArray(_ kotlinByteArray: KotlinByteArray) -> [UInt8] {
        let count = Int(kotlinByteArray.size)
        var array = [UInt8](repeating: 0, count: count)
        for i in 0..<count {
            array[i] = UInt8(bitPattern: kotlinByteArray.get(index: Int32(i)))
        }
        return array
    }
    
    private func toKotlinByteArray(_ swiftArray: [UInt8]) -> KotlinByteArray {
        let kotlinArray = KotlinByteArray(size: Int32(swiftArray.count))
        for i in 0..<swiftArray.count {
            kotlinArray.set(index: Int32(i), value: Int8(bitPattern: swiftArray[i]))
        }
        return kotlinArray
    }
}

private var accessoryViewKey: UInt8 = 0

@objc class KeyboardAccessorySwizzler: NSObject {
    static let shared = KeyboardAccessorySwizzler()
    
    private var hasSwizzled = false
    
    func swizzle() {
        guard !hasSwizzled else { return }
        hasSwizzled = true
        
        let originalSelector = #selector(getter: UIResponder.inputAccessoryView)
        let swizzledSelector = #selector(getter: UIResponder.customInputAccessoryView)
        
        guard let originalMethod = class_getInstanceMethod(UIResponder.self, originalSelector),
              let swizzledMethod = class_getInstanceMethod(UIResponder.self, swizzledSelector) else {
            return
        }
        
        method_exchangeImplementations(originalMethod, swizzledMethod)
    }
}

extension UIResponder {
    @objc var customInputAccessoryView: UIView? {
        // If there's already an original accessory view, respect it
        if let originalAccessory = self.customInputAccessoryView {
            return originalAccessory
        }
        
        guard let view = self as? UIView else {
            return nil
        }
        
        // First, check if there's already an accessory view set via associated object
        if let accessory = objc_getAssociatedObject(view, &accessoryViewKey) as? UIView {
            return accessory
        }
        
        // Check if this view conforms to UITextInput (standard and custom text fields)
        if view.conforms(to: UITextInput.self) {
            if let traits = view as? UITextInputTraits {
                let type = traits.keyboardType
                if type == .numberPad || type == .decimalPad || type == .phonePad {
                    // Create native accessory view toolbar:
                    // Height: 44 pt (standard compact iOS system toolbar height)
                    let toolbar = UIToolbar(frame: CGRect(x: 0, y: 0, width: UIScreen.main.bounds.width, height: 44))
                    
                    // Native styling:
                    // - light gray background (#F2F2F7)
                    toolbar.barTintColor = UIColor(red: 242/255, green: 242/255, blue: 247/255, alpha: 1.0)
                    toolbar.isTranslucent = false
                    
                    // Subtle top border line:
                    let borderLine = UIView(frame: CGRect(x: 0, y: 0, width: toolbar.frame.width, height: 0.5))
                    borderLine.backgroundColor = UIColor.lightGray.withAlphaComponent(0.3)
                    borderLine.autoresizingMask = [.flexibleWidth, .flexibleBottomMargin]
                    toolbar.addSubview(borderLine)
                    
                    // Done button (dark green background circle with thick white checkmark/tick icon)
                    let tickButton = UIButton(type: .custom)
                    tickButton.backgroundColor = UIColor(red: 7/255, green: 91/255, blue: 50/255, alpha: 1.0)
                    tickButton.layer.cornerRadius = 15
                    tickButton.layer.masksToBounds = true
                    tickButton.tintColor = .white

                    let config = UIImage.SymbolConfiguration(pointSize: 12, weight: .heavy)
                    let tickImage = UIImage(systemName: "checkmark", withConfiguration: config)
                    tickButton.setImage(tickImage, for: .normal)

                    tickButton.translatesAutoresizingMaskIntoConstraints = false
                    NSLayoutConstraint.activate([
                        tickButton.widthAnchor.constraint(equalToConstant: 30),
                        tickButton.heightAnchor.constraint(equalToConstant: 30)
                    ])

                    tickButton.addTarget(self, action: #selector(customDismissKeyboard), for: .touchUpInside)

                    let doneButton = UIBarButtonItem(customView: tickButton)
                    
                    let flexibleSpace = UIBarButtonItem(
                        barButtonSystemItem: .flexibleSpace,
                        target: nil,
                        action: nil
                    )
                    
                    toolbar.items = [flexibleSpace, doneButton]
                    
                    objc_setAssociatedObject(view, &accessoryViewKey, toolbar, .OBJC_ASSOCIATION_RETAIN_NONATOMIC)
                    return toolbar
                }
            }
        }
        
        return nil
    }
    
    @objc func customDismissKeyboard() {
        self.resignFirstResponder()
    }
}

class SwiftGoogleSignInBridge: NSObject, IosGoogleSignInBridge {
    // Called on screen load — silently restores a prior session with no UI.
    func tryRestoreEmail(completion: @escaping (String?) -> Void) {
        GIDSignIn.sharedInstance.restorePreviousSignIn { user, _ in
            completion(user?.profile?.email)
        }
    }

    // Called when user explicitly taps the field to pick/change email.
    // Always shows the interactive account picker so any user on the device
    // can select their own account, even if another account was auto-filled.
    func pickEmail(completion: @escaping (String?) -> Void) {
        DispatchQueue.main.async {
            self.showInteractiveSignIn(completion: completion)
        }
    }

    private func showInteractiveSignIn(completion: @escaping (String?) -> Void) {
        guard let rootVC = self.getTopViewController() else {
            completion(nil)
            return
        }
        GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { signInResult, error in
            if let error = error {
                print("Google Sign-In error: \(error.localizedDescription)")
                completion(nil)
                return
            }
            completion(signInResult?.user.profile?.email)
        }
    }

    private func getTopViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
        let windowScene = scenes.first as? UIWindowScene
        var topVC = windowScene?.windows.first { $0.isKeyWindow }?.rootViewController
        while let presentedVC = topVC?.presentedViewController {
            topVC = presentedVC
        }
        return topVC
    }
}

class SwiftAnalyticsBridge: NSObject, IosAnalyticsBridge {
    static var attributionData: [String: String] = [:]

    func logEvent(name: String, params: [String : Any]) {
        AppsFlyerLib.shared().logEvent(name, withValues: params)
        ClaritySDK.sendCustomEvent(value: name)
        Singular.event(name, withArgs: params)
    }

    func logScreenView(screenName: String) {
        AppsFlyerLib.shared().logEvent("screen_view", withValues: ["screen_name": screenName])
        ClaritySDK.setCurrentScreenName(screenName)
        Singular.event("screen_view", withArgs: ["screen_name": screenName])
    }

    func setUserId(userId: String) {
        AppsFlyerLib.shared().customerUserID = userId
        ClaritySDK.setCustomUserId(userId)
        Singular.setCustomUserId(userId)
    }

    func generateReferralLink(referrerId: String, onComplete: @escaping (String?) -> Void) {
        AppsFlyerShareInviteHelper.generateInviteUrl(linkGenerator: { generator in
            generator.addParameterValue("referral", forKey: "deep_link_value")
            generator.addParameterValue(referrerId, forKey: "deep_link_sub1")
            generator.addParameterValue(referrerId, forKey: "af_sub1")
            return generator
        }, completionHandler: { url in
            onComplete(url?.absoluteString)
        })
    }

    func getAttributionData() -> [String : Any] {
        return SwiftAnalyticsBridge.attributionData
    }
}
