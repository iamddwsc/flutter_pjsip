//
//  FlutterAppDelegate+Pjsip.m
//  Runner
//
//  Created by gejianmin on 2019/8/15.
//  Copyright © 2019 The Chromium Authors. All rights reserved.
//

#import "FlutterAppDelegate+Pjsip.h"
#import "PJSIPViewController.h"
#import "PJSIPModel.h"
#include <pjsua-lib/pjsua.h>

/** 信号通道*/
#define flutterMethodChannel  @"flutter_pjsip"
/** pjsip初始化*/
#define method_pjsip_init  @"method_pjsip_init"
/** pjsip登录*/
#define method_pjsip_login  @"method_pjsip_login"
/** pjsip拨打电话*/
#define method_pjsip_call  @"method_pjsip_call"
/** 接收电话*/
#define method_pjsip_receive  @"method_pjsip_receive"
/** 挂断&&拒接*/
#define method_pjsip_refuse  @"method_pjsip_refuse"
/** 免提*/
#define method_pjsip_hands_free  @"method_pjsip_hands_free"
/** 静音*/
#define method_pjsip_mute  @"method_pjsip_mute"
/** pjsip登出*/
#define method_pjsip_logout  @"method_pjsip_logout"
/** pjsip销毁*/
#define method_pjsip_deinit  @"method_pjsip_deinit"
/** 直接拨打SIP URI*/
#define method_pjsip_call_direct_uri  @"method_pjsip_call_direct_uri"
/** 检查PJSIP状态*/
#define method_pjsip_check_state  @"method_pjsip_check_state"

#define method_pjsip_login_with_info  @"method_pjsip_login_with_info"

@implementation FlutterAppDelegate (Pjsip)

- (void)setupPjsip:(UIApplication *)launchOptions rootController:(UIViewController *)rootController{
    
    [self methodChannelFunctionWithRootController:rootController];
    
}
- (void)methodChannelFunctionWithRootController:(UIViewController *)rootController{
    
    FlutterMethodChannel* methodChannel = [FlutterMethodChannel
                                           methodChannelWithName:flutterMethodChannel binaryMessenger:(FlutterViewController *)rootController];
    [PJSipManager manager].methodChannel = methodChannel;
    //设置监听
    [methodChannel setMethodCallHandler:^(FlutterMethodCall* call, FlutterResult result) {
        NSString *method=call.method;
        NSDictionary * dict = (NSDictionary *)call.arguments;
        if ([method isEqualToString:method_pjsip_init]) {/** 初始化*/
            NSLog(@"PJSIP - Initializing PJSIP manager...");
            PJSipManager *manager = [PJSipManager manager];
            if (manager) {
                NSLog(@"PJSIP - PJSIP manager initialized successfully");
                result(@(YES));
            } else {
                NSLog(@"PJSIP - Failed to initialize PJSIP manager");
                result(@(NO));
            }
        }else if ([method isEqualToString:method_pjsip_login]) {/** 登录*/
            NSLog(@"登录名称：%@",[dict objectForKey:@"username"]);
            if ([[PJSipManager manager] registerAccountWithName:[dict objectForKey:@"username"] password:[dict objectForKey:@"password"] IPAddress:[NSString stringWithFormat:@"%@:%@",[dict objectForKey:@"ip"],[dict objectForKey:@"port"]]]) {
                result(@(YES));
            }else{
                result(@(NO));
            }
        }else if ([method isEqualToString:method_pjsip_call]) {/** 拨打电话*/
            NSLog(@"拨打的电话号码：%@",[dict objectForKey:@"username"]);
            [[PJSipManager manager] dailWithPhonenumber:[dict objectForKey:@"username"]];
            result(@(YES));
        }else if ([method isEqualToString:method_pjsip_receive]) {/** 接收电话*/
            [[PJSipManager manager] incommingCallReceive];
            result(@(YES));
        }else if ([method isEqualToString:method_pjsip_hands_free]) {/** 免提*/
            [[PJSipManager manager]setAudioSession];
            result(@(YES));
        }else if ([method isEqualToString:method_pjsip_mute]) {/** 静音*/
            [[PJSipManager manager] muteMicrophone];
            result(@(YES));
        }else if ([method isEqualToString:method_pjsip_refuse]) {/** 挂断&&拒接*/
            [[PJSipManager manager]hangup];
            result(@(YES));
        }else if ([method isEqualToString:method_pjsip_logout]) {/** 登出*/
            if ([[PJSipManager manager]logOut]) {
                result(@(YES));
            }else{
                result(@(NO));
            }
        }else if ([method isEqualToString:method_pjsip_deinit]) {/** 销毁*/
            [PJSipManager attempDealloc];
            result(@(YES));
        }else if ([method isEqualToString:method_pjsip_call_direct_uri]) {/** 直接拨打SIP URI*/
            NSString *sipUri = [dict objectForKey:@"sipUri"];
            if (sipUri && ![sipUri isEqualToString:@""]) {
                NSLog(@"PJSIP - Calling direct SIP URI: %@", sipUri);
                BOOL success = [[PJSipManager manager] callDirectToSipUri:sipUri];
                result(@(success));
            } else {
                NSLog(@"PJSIP - Error: Missing or empty sipUri parameter");
                result(@(NO));
            }
        }else if ([method isEqualToString:method_pjsip_check_state]) {/** 检查PJSIP状态*/
            pjsua_state state = pjsua_get_state();
            unsigned acc_count = pjsua_acc_get_count();
            NSDictionary *stateInfo = @{
                @"pjsua_state": @(state),
                @"is_running": @(state == PJSUA_STATE_RUNNING),
                @"account_count": @(acc_count),
                @"state_name": state == PJSUA_STATE_RUNNING ? @"RUNNING" : @"NOT_RUNNING"
            };
            NSLog(@"PJSIP - State check: %@", stateInfo);
            result(stateInfo);
        }else if ([method isEqualToString:method_pjsip_login_with_info]) {
            NSLog(@"Login with info: %@",[dict objectForKey:@"username"]);
            if ([[PJSipManager manager] registerSIPAccountWithInfo: dict]) {
                result(@(YES));
            }else{
                result(@(NO));
            }
        }
    }];
}


@end
