import SwiftUI
import AppsFlyerLib
import Clarity
import Singular
import FirebaseCore
import FirebaseCrashlytics
import FirebaseMessaging
import UserNotifications
import ComposeApp

class AppDelegate: NSObject, UIApplicationDelegate, AppsFlyerLibDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        // Configure Firebase (only if not using placeholder values to avoid crashes)
        if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
           let dict = NSDictionary(contentsOfFile: path),
           let googleAppID = dict["GOOGLE_APP_ID"] as? String,
           !googleAppID.contains("placeholder") {
            FirebaseApp.configure()

            // Set delegates
            UNUserNotificationCenter.current().delegate = self
            Messaging.messaging().delegate = self

            // Request push authorization and register
            let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
            UNUserNotificationCenter.current().requestAuthorization(options: authOptions) { _, _ in }

            application.registerForRemoteNotifications()
        } else {
            print("⚠️ Firebase App ID is a placeholder or GoogleService-Info.plist is missing. Skipping Firebase configuration.")
        }

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

        // Configure Singular
        if let singularConfig = SingularConfig(apiKey: "pyllar_f3135c51", andSecret: "f0d918a1e372e68b8c4a46b14bbe82c8") {
            singularConfig.launchOptions = launchOptions
            singularConfig.singularLinksHandler = { params in
                guard let params = params else { return }
                var dict: [String: String] = [:]
                if let deeplink = params.getDeepLink() {
                    dict["deeplink"] = deeplink
                }
                if let passthrough = params.getPassthrough() {
                    dict["passthrough"] = passthrough
                }
                dict["is_deferred"] = params.isDeferred() ? "true" : "false"
                if let urlParams = params.getUrlParameters() as? [String: String] {
                    dict.merge(urlParams) { _, new in new }
                }
                print("[Singular] Link params received: \(dict)")
                SwiftAnalyticsBridge.singularAttributionData = dict
            }
            Singular.start(singularConfig)
        }

        return true
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Start / Resume tracking on app launch or foregrounding
        AppsFlyerLib.shared().start()
    }

    // Warm-start case: app already installed/running, user taps a pyllar:// Singular Link.
    // Cold start is already covered by singularConfig.launchOptions above; this SDK does not
    // swizzle application(_:open:options:) itself, so it must be forwarded explicitly.
    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        Singular.startSession("pyllar_f3135c51", withKey: "f0d918a1e372e68b8c4a46b14bbe82c8", andLaunch: url)
        return true
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

    // MARK: - Push Notifications & Firebase Messaging Delegate

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        print("Firebase registration token: \(String(describing: fcmToken))")
        if let token = fcmToken {
            // Pass the token to the KMP shared PushTokenManager
            PushTokenManager.shared.setToken(token: token)
        }
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Handle foreground notification presentation
        completionHandler([.banner, .list, .sound])
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