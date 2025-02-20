//
//  SipUtil.h
//  AppID
//
//  Created by Mac on 15/10/2023.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface SipUtil : NSObject

+ (SipUtil *)shared;

- (NSString *)userAgentForSIPAccount;

- (void)terminatesAllCalls;

@end

NS_ASSUME_NONNULL_END
