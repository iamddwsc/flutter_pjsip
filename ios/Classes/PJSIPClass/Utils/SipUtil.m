//
//  SipUtil.m
//  AppID
//
//  Created by Mac on 15/10/2023.
//

#import "SipUtil.h"
#import <sys/utsname.h>
#import <UIKit/UIKit.h>

#define SFM(format, ...) [NSString stringWithFormat:(format), ##__VA_ARGS__]

@implementation SipUtil

+(SipUtil *)shared {
    static SipUtil *sipUtil = nil;
    if(sipUtil == nil){
        sipUtil = [[SipUtil alloc] init];
    }
    return sipUtil;
}

- (NSString *)userAgentForSIPAccount {
    NSString *bundleDisplayName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleDisplayName"];
    NSString *deviceName = [self phoneDeviceName];  // Get the device name
    NSString *systemVersion = [UIDevice currentDevice].systemVersion;  // Get the system version
    NSString *content = SFM(@"%@_%@_iOS%@", bundleDisplayName, deviceName, systemVersion);
    
    content = [content stringByReplacingOccurrencesOfString:@"," withString:@"."];
    content = [content stringByReplacingOccurrencesOfString:@" " withString:@"."];
    return content;
}

- (void)terminatesAllCalls {
    //    [AppDelegate.shared terminatesAllCalls];
}

- (NSString *)phoneDeviceModel {
    struct utsname systemInfo;
    uname(&systemInfo);
    NSString *modelType =  [NSString stringWithCString:systemInfo.machine encoding:NSUTF8StringEncoding];
    return modelType;
}

- (NSString *)phoneDeviceName {
    NSString *model = [self phoneDeviceModel];
    if ([model isEqualToString:@"iPhone5,1"] || [model isEqualToString:@"iPhone5,2"]) {
        return @"iPhone 5";
        
    }else if ([model isEqualToString:@"iPhone5,3"] || [model isEqualToString:@"iPhone5,4"]) {
        return @"iPhone 5c";
        
    }else if ([model isEqualToString:@"iPhone6,1"] || [model isEqualToString:@"iPhone6,2"]) {
        return @"iPhone 5s";
        
    }else if ([model isEqualToString:@"iPhone7,2"]) {
        return @"iPhone 6";
        
    }else if ([model isEqualToString:@"iPhone7,1"]) {
        return @"iPhone 6 Plus";
        
    }else if ([model isEqualToString:@"iPhone8,1"]) {
        return @"iPhone 6s";
        
    }else if ([model isEqualToString:@"iPhone8,2"]) {
        return @"iPhone 6s Plus";
        
    }else if ([model isEqualToString:@"iPhone8,4"]) {
        return @"iPhone SE (1st generation)";
        
    }else if ([model isEqualToString:@"iPhone9,1"] || [model isEqualToString:@"iPhone9,3"]) {
        return @"iPhone 7";
        
    }else if ([model isEqualToString:@"iPhone9,2"] || [model isEqualToString:@"iPhone9,4"]) {
        return @"iPhone 7 Plus";
        
    }else if ([model isEqualToString:@"iPhone10,1"] || [model isEqualToString:@"iPhone10,4"]) {
        return @"iPhone 8";
        
    }else if ([model isEqualToString:@"iPhone10,2"] || [model isEqualToString:@"iPhone10,5"]) {
        return @"iPhone 8 Plus";
        
    }else if ([model isEqualToString:@"iPhone10,3"] || [model isEqualToString:@"iPhone10,6"]) {
        return @"iPhone X";
        
    }else if ([model isEqualToString:@"iPhone11,8"]) {
        return @"iPhone XR";
        
    }else if ([model isEqualToString:@"iPhone11,2"]) {
        return @"iPhone XS";
        
    }else if ([model isEqualToString:@"iPhone11,6"] || [model isEqualToString:@"iPhone11,4"]) {
        return @"iPhone XS Max";
        
    }else if ([model isEqualToString:@"iPhone12,1"]) {
        return @"iPhone 11";
        
    }else if ([model isEqualToString:@"iPhone12,3"]) {
        return @"iPhone 11 Pro";
        
    }else if ([model isEqualToString:@"iPhone12,5"]) {
        return @"iPhone 11 Pro Max";
        
    }else if ([model isEqualToString:@"iPhone12,8"]) {
        return @"iPhone SE (2nd generation)";
        
    }else if ([model isEqualToString:@"iPhone13,1"]) {
        return @"iPhone 12 mini";
        
    }else if ([model isEqualToString:@"iPhone13,2"]) {
        return @"iPhone 12";
        
    }else if ([model isEqualToString:@"iPhone13,3"]) {
        return @"iPhone 12 Pro";
        
    }else if ([model isEqualToString:@"iPhone13,4"]) {
        return @"iPhone 12 Pro Max";
        
    }else if ([model isEqualToString:@"iPhone14,4"]) {
        return @"iPhone 13 mini";
        
    }else if ([model isEqualToString:@"iPhone14,5"]) {
        return @"iPhone 13";
        
    }else if ([model isEqualToString:@"iPhone14,2"]) {
        return @"iPhone 13 Pro";
        
    }else if ([model isEqualToString:@"iPhone14,3"]) {
        return @"iPhone 13 Pro Max";
        
    }else if ([model isEqualToString:@"iPhone14,6"]) {
        return @"iPhone SE (3rd generation)";
        
    }else if ([model isEqualToString:@"iPhone14,7"]) {
        return @"iPhone 14";
        
    }else if ([model isEqualToString:@"iPhone14,8"]) {
        return @"iPhone 14 Plus";
        
    }else if ([model isEqualToString:@"iPhone15,2"]) {
        return @"iPhone 14 Pro";
        
    }else if ([model isEqualToString:@"iPhone15,3"]) {
        return @"iPhone 14 Pro Max";
        
    }else if ([model isEqualToString:@"iPod1,1"]) {
        return @"iPod touch";
        
    }else if ([model isEqualToString:@"iPod2,1"]) {
        return @"iPod touch (2nd generation)";
        
    }else if ([model isEqualToString:@"iPod3,1"]) {
        return @"iPod touch (3rd generation)";
        
    }else if ([model isEqualToString:@"iPod4,1"]) {
        return @"iPod touch (4th generation)";
        
    }else if ([model isEqualToString:@"iPod5,1"]) {
        return @"iPod touch (5th generation)";
        
    }else if ([model isEqualToString:@"iPod7,1"]) {
        return @"iPod touch (6th generation)";
        
    }else if ([model isEqualToString:@"iPod9,1"]) {
        return @"iPod touch (7th generation)";
        
    }else if ([model isEqualToString:@"iPad1,1"]) {
        return @"iPad";
        
    }else if ([model isEqualToString:@"iPad2,1"] || [model isEqualToString:@"iPad2,2"] || [model isEqualToString:@"iPad2,3"] || [model isEqualToString:@"iPad2,4"]) {
        return @"iPad 2";
        
    }else if ([model isEqualToString:@"iPad3,1"] || [model isEqualToString:@"iPad3,2"] || [model isEqualToString:@"iPad3,3"]) {
        return @"iPad (3rd generation)";
        
    }else if ([model isEqualToString:@"iPad3,4"] || [model isEqualToString:@"iPad3,5"] || [model isEqualToString:@"iPad3,6"]) {
        return @"iPad (4th generation)";
        
    }else if ([model isEqualToString:@"iPad6,11"] || [model isEqualToString:@"iPad6,12"]) {
        return @"iPad (5th generation)";
        
    }else if ([model isEqualToString:@"iPad7,5"] || [model isEqualToString:@"iPad7,6"]) {
        return @"iPad (6th generation)";
        
    }else if ([model isEqualToString:@"iPad7,11"] || [model isEqualToString:@"iPad7,12"]) {
        return @"iPad (7th generation)";
        
    }else if ([model isEqualToString:@"iPad11,6"] || [model isEqualToString:@"iPad11,7"]) {
        return @"iPad (8th generation)";
        
    }else if ([model isEqualToString:@"iPad12,1"] || [model isEqualToString:@"iPad12,2"]) {
        return @"iPad (9th generation)";
        
    }else if ([model isEqualToString:@"iPad4,1"] || [model isEqualToString:@"iPad4,2"] || [model isEqualToString:@"iPad4,3"]) {
        return @"iPad Air";
        
    }else if ([model isEqualToString:@"iPad5,3"] || [model isEqualToString:@"iPad5,4"]) {
        return @"iPad Air 2";
        
    }else if ([model isEqualToString:@"iPad11,3"] || [model isEqualToString:@"iPad11,4"]) {
        return @"iPad Air (3rd generation)";
        
    }else if ([model isEqualToString:@"iPad13,1"] || [model isEqualToString:@"iPad13,2"]) {
        return @"iPad Air (4th generation)";
        
    }else if ([model isEqualToString:@"iPad6,7"] || [model isEqualToString:@"iPad6,8"]) {
        return @"iPad Pro (12.9-inch)";
        
    }else if ([model isEqualToString:@"iPad6,3"] || [model isEqualToString:@"iPad6,4"]) {
        return @"iPad Pro (9.7-inch)";
        
    }else if ([model isEqualToString:@"iPad7,1"] || [model isEqualToString:@"iPad7,2"]) {
        return @"iPad Pro (12.9-inch) (2nd generation)";
        
    }else if ([model isEqualToString:@"iPad7,3"] || [model isEqualToString:@"iPad7,4"]) {
        return @"iPad Pro (10.5-inch)";
        
    }else if ([model isEqualToString:@"iPad8,1"] || [model isEqualToString:@"iPad8,2"] || [model isEqualToString:@"iPad8,3"] || [model isEqualToString:@"iPad8,4"]) {
        return @"iPad Pro (11-inch)";
        
    }else if ([model isEqualToString:@"iPad8,5"] || [model isEqualToString:@"iPad8,6"] || [model isEqualToString:@"iPad8,7"] || [model isEqualToString:@"iPad8,8"]) {
        return @"iPad Pro (12.9-inch) (3rd generation)";
        
    }else if ([model isEqualToString:@"iPad8,9"] || [model isEqualToString:@"iPad8,10"]) {
        return @"iPad Pro (11-inch) (2nd generation)";
        
    }else if ([model isEqualToString:@"iPad8,11"] || [model isEqualToString:@"iPad8,12"]) {
        return @"iPad Pro (12.9-inch) (4th generation)";
        
    }else if ([model isEqualToString:@"iPad13,4"] || [model isEqualToString:@"iPad13,5"] || [model isEqualToString:@"iPad13,6"] || [model isEqualToString:@"iPad13,7"]) {
        return @"iPad Pro (11-inch) (3rd generation)";
        
    }else if ([model isEqualToString:@"iPad13,8"] || [model isEqualToString:@"iPad13,9"] || [model isEqualToString:@"iPad13,10"] || [model isEqualToString:@"iPad13,11"]) {
        return @"iPad Pro (12.9-inch) (5th generation)";
        
    }else if ([model isEqualToString:@"iPad2,5"] || [model isEqualToString:@"iPad2,6"] || [model isEqualToString:@"iPad2,7"]) {
        return @"iPad mini";
        
    }else if ([model isEqualToString:@"iPad4,4"] || [model isEqualToString:@"iPad4,5"] || [model isEqualToString:@"iPad4,6"]) {
        return @"iPad mini 2";
        
    }else if ([model isEqualToString:@"iPad4,7"] || [model isEqualToString:@"iPad4,8"] || [model isEqualToString:@"iPad4,9"]) {
        return @"iPad mini 3";
        
    }else if ([model isEqualToString:@"iPad5,1"] || [model isEqualToString:@"iPad5,2"]) {
        return @"iPad mini 4";
        
    }else if ([model isEqualToString:@"iPad11,1"] || [model isEqualToString:@"iPad11,2"]) {
        return @"iPad mini (5th generation)";
        
    }else if ([model isEqualToString:@"iPad14,1"] || [model isEqualToString:@"iPad14,2"]) {
        return @"iPad mini (6th generation)";
        
    }
    return model;
}

@end
