//
//  ViewController.h
//  MagtekReader
//
//  Created by Apple on 11/29/16.
//  Copyright © 2016 Apple. All rights reserved.
//


#import <Cordova/CDVPlugin.h>
#import "MagtekPlugin.h"


@interface CDVMagtekPlugin : CDVPlugin

- (void)openDevice:(CDVInvokedUrlCommand *)command;
- (void)closeDevice:(CDVInvokedUrlCommand *)command;
- (void)readCard:(CDVInvokedUrlCommand *)command;

@end

