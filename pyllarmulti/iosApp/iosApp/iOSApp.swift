import SwiftUI
import AppsFlyerLib
import Clarity

class AppDelegate: NSObject, UIApplicationDelegate, AppsFlyerLibDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        // Configure AppsFlyer
        AppsFlyerLib.shared().appsFlyerDevKey = "gog7ERykY2ivzocSRnpKPi"
        AppsFlyerLib.shared().appleAppID = "6767513475"
        AppsFlyerLib.shared().appInviteOneLinkID = "JV5P"
        AppsFlyerLib.shared().delegate = self

        #if DEBUG
        AppsFlyerLib.shared().isDebug = true
        #endif

        // Configure Microsoft Clarity
        let clarityConfig = ClarityConfig(projectId: "vkt8sc281d")
        #if DEBUG
        clarityConfig.logLevel = .verbose
        #endif
        ClaritySDK.initialize(config: clarityConfig)

        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Start / Resume tracking on app launch or foregrounding
        AppsFlyerLib.shared().start()
    }

    // MARK: - AppsFlyerLibDelegate

    func onConversionDataSuccess(_ conversionInfo: [AnyHashable : Any]) {
        print("[AppsFlyer] Conversion Data Success: \(conversionInfo)")
        // Map and cache attribution parameters in the analytics bridge
        var dict: [String: String] = [:]
        for (key, value) in conversionInfo {
            if let strKey = key as? String, let strVal = value as? String {
                dict[strKey] = strVal
            }
        }
        SwiftAnalyticsBridge.attributionData = dict
    }

    func onConversionDataFail(_ error: Error) {
        print("[AppsFlyer] Conversion Data Error: \(error.localizedDescription)")
    }

    func onAppOpenAttribution(_ attributionData: [AnyHashable : Any]) {
        print("[AppsFlyer] App Open Attribution: \(attributionData)")
    }

    func onAppOpenAttributionFailure(_ error: Error) {
        print("[AppsFlyer] App Open Attribution Error: \(error.localizedDescription)")
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        KeyboardAccessorySwizzler.shared.swizzle()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}