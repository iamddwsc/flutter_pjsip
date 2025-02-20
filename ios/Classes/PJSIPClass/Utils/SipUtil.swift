//
//  SipUtil.swift
//  Runner
//
//  Created by ddwsc on 20/2/25.
//

import Foundation
import UIKit

@objc(SipUtil)
public class SipUtil: NSObject {

    // Singleton instance
    @objc static let shared = SipUtil()

    private override init() {}

    // Function to generate User Agent for SIP Account
    @objc func userAgentForSIPAccount() -> String {
        let appName = Bundle.main.object(forInfoDictionaryKey: "CFBundleDisplayName") as? String ?? "UnknownApp"
        let phoneDeviceName = UIDevice.current.name
        let systemVersion = UIDevice.current.systemVersion

        var content = "\(appName)_\(phoneDeviceName)_iOS\(systemVersion)"

        // Replace commas and spaces with dots
        content = content.replacingOccurrences(of: ",", with: ".")
        content = content.replacingOccurrences(of: " ", with: ".")
        
        return content
    }

    // Function to terminate all calls
    @objc func terminatesAllCalls() {
//        AppDelegate.shared?.terminatesAllCalls()
    }
}
