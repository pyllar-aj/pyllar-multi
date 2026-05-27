import SwiftUI
import ComposeApp
import UserNotifications
import Clarity
import AppsFlyerLib

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self

        // Request notification permissions
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
            if granted {
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            }
        }

        // Initialize AppsFlyer
        AppsFlyerLib.shared().appsFlyerDevKey = "gog7ERykY2ivzocSRnpKPi" // Add AppsFlyer Dev Key here
        AppsFlyerLib.shared().appleAppID = "6767513475"      // Add iTunes App ID here
        #if DEBUG
        AppsFlyerLib.shared().isDebug = true
        #endif
        AppsFlyerLib.shared().start()

        // Initialize Clarity
        let clarityConfig = ClarityConfig(projectId: "vkt8sc281d")
        ClaritySDK.initialize(config: clarityConfig)
        
        let appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0.0"
        _ = ClaritySDK.setCustomTag(key: "app_version", value: appVersion)

        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let tokenParts = deviceToken.map { data in String(format: "%02.2hhx", data) }
        let token = tokenParts.joined()
        print("Device Token: \(token)")

        // Push the token to the KMP shared manager
        PushTokenManager.shared.setToken(token: token)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("Failed to register for remote notifications: \(error.localizedDescription)")
    }

    // Handle notification when app is in foreground
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .list, .sound])
    }

    // Handle notification tap or action (when app is in background or foreground)
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        print("Notification tapped with userInfo: \(userInfo)")

        let action = userInfo["action"] as? String ?? userInfo["screen"] as? String
        let url = userInfo["url"] as? String
        let route = userInfo["route"] as? String
        
        sendNotificationPayload(action: action, url: url, route: route)
        completionHandler()
    }

    // Handle background / silent remote notifications
    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        print("Received remote notification in background: \(userInfo)")
        
        let action = userInfo["action"] as? String ?? userInfo["screen"] as? String
        let url = userInfo["url"] as? String
        let route = userInfo["route"] as? String
        
        sendNotificationPayload(action: action, url: url, route: route)
        completionHandler(.newData)
    }

    private func sendNotificationPayload(action: String?, url: String?, route: String?) {
        var dict = [String: String]()
        if let action = action { dict["action"] = action }
        if let url = url { dict["url"] = url }
        if let route = route { dict["route"] = route }
        
        if let jsonData = try? JSONSerialization.data(withJSONObject: dict, options: []),
           let jsonString = String(data: jsonData, encoding: .utf8) {
            PushTokenManager.shared.setNotificationPayload(payload: jsonString)
        } else if let action = action {
            PushTokenManager.shared.setNotificationPayload(payload: action)
        }
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