//
//  ViewController.m
//  MagtekReader
//
//  Created by Apple on 11/29/16.
//  Copyright © 2016 Apple. All rights reserved.
//

#import "CDVMagtekPlugin.h"

@implementation CDVMagtekPlugin

- (void)openDevice:(CDVInvokedUrlCommand *)command {
    NSLog(@"openDevice");
    [self.commandDelegate runInBackground:^{
        
        MagtekPlugin *plugin = [MagtekPlugin sharedInstance];

        [plugin openReader:^(int openResult){
            CDVPluginResult *result = nil;
            
            if(openResult < 0) {
                NSLog(@"Open Error!");
                return;
            }
            
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:openResult];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }];
    }];
}
- (void)closeDevice:(CDVInvokedUrlCommand *)command {
    NSLog(@"closeDevice");
    [self.commandDelegate runInBackground:^{
        
        MagtekPlugin *plugin = [MagtekPlugin sharedInstance];

        [plugin closeReader:^(int closeResult){
            CDVPluginResult *result = nil;
            
            if(closeResult < 0) {
                NSLog(@"close Error!");
                return;
            }
            
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsInt:closeResult];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }];
    }];
}
- (void)readCard:(CDVInvokedUrlCommand *)command {
    NSLog(@"readCard");
    [self.commandDelegate runInBackground:^{
        
        MagtekPlugin *plugin = [MagtekPlugin sharedInstance];

        [plugin readCard:^(NSDictionary *data){
            CDVPluginResult *result = nil;
            
            if(data == nil ) {
                data = [NSDictionary new];
            }
            
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:data];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        }];
    }];
}

@end