//
//  CommonUtil.h
//  flutter_pjsip
//
//  Created by ddwsc on 20/2/25.
//

#define SFM(format, ...) [NSString stringWithFormat:(format), ##__VA_ARGS__]
#define transport_udp       @"udp"
#define transport_tcp       @"tcp"
#define transport_tls       @"tls"

#import <Foundation/Foundation.h>

#ifndef CommonUtil_h
#define CommonUtil_h

@interface CommonUtil : NSObject

+ (BOOL)isNullOrEmpty:(NSString*)string;

+ (long)getIdFromObjectInfo:(NSDictionary *)info withKey:(NSString *)key;

#endif /* CommonUtil_h */

@end
