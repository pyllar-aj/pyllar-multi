import UIKit
import SwiftUI
import ComposeApp
import CryptoKit

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Initialize bridge for KMP
        SwiftCryptoScope.shared.bridge = SwiftCryptoBridge()
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
