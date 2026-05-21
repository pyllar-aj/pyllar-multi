import SwiftUI

@main
struct iOSApp: App {
    init() {
        KeyboardAccessorySwizzler.shared.swizzle()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}