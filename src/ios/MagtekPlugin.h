//
//  CDVMagtekPlugin.h
//  MagtekReader
//
//  Created by Apple on 11/29/16.
//  Copyright © 2016 Apple. All rights reserved.
//

#import <Foundation/Foundation.h>

#import "MTSCRA.h"

@interface MagtekPlugin : NSObject <MTSCRAEventDelegate>

+ (MagtekPlugin*)sharedInstance;

@property (strong, nonatomic) void (^openCB)(int result);
@property (strong, nonatomic) void (^readCB)(NSDictionary *data);
@property (strong, nonatomic) void (^closeCB)(int result);

- (void)openReader:(void(^)(int result))callback;
- (void)closeReader:(void(^)(int result))callback;
- (void)readCard:(void(^)(NSDictionary *data))callback;

@end
