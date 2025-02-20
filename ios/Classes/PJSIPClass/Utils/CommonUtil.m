//
//  CommonUtil.m
//  flutter_pjsip
//
//  Created by ddwsc on 20/2/25.
//

#import "CommonUtil.h"

@implementation CommonUtil

+ (BOOL)isNullOrEmpty:(NSString*)string{
    if (![string isKindOfClass:[NSString class]]) {
        return true;
    }
    return string == nil || string==(id)[NSNull null] || [string isEqualToString: @""];
}

+ (long)getIdFromObjectInfo:(NSDictionary *)info withKey:(NSString *)key {
    long result = 0;
    if ([info isKindOfClass:[NSDictionary class]] && [info.allKeys containsObject: key]) {
        id object = [info objectForKey:key];
        if ([object isKindOfClass:[NSString class]]) {
            result = (long)[(NSString *)object longLongValue];
            
        }else if ([object isKindOfClass:[NSNumber class]]) {
            result = [(NSNumber *)object longValue];
        }
    }
    return result;
}

@end
