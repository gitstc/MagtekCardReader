//
//  CDVMagtekPlugin.m
//  MagtekReader
//
//  Created by Apple on 11/29/16.
//  Copyright © 2016 Apple. All rights reserved.
//

#import "MagtekPlugin.h"

#import <MediaPlayer/MediaPlayer.h>

@implementation MagtekPlugin {
    MTSCRA *lib;
}

+ (MagtekPlugin*)sharedInstance {
    static MagtekPlugin* instance = nil;
    if (instance == nil) {
        instance = [[MagtekPlugin alloc] init];
        instance->lib = [MTSCRA new];
        instance->lib.delegate = instance;
        [instance->lib setDeviceType:MAGTEKAUDIOREADER];
    }
    
    return instance;
}

- (void)openReader:(void(^)(int result))callback
{
    self.openCB = callback;
    @try {
        if(!lib.isDeviceOpened )
        {
            [lib openDevice];
        }
    }
    @catch(NSException *ex) {
        if(self.openCB) {
            self.openCB(-1);
            self.openCB = nil;
        }
    }
}

- (void)closeReader:(void(^)(int result))callback
{
    self.closeCB = callback;
    @try {
        if(lib.isDeviceOpened )
        {
            [lib closeDevice];
        }
    }
    @catch(NSException *ex) {
        if(self.closeCB) {
            self.closeCB(-1);
            self.closeCB = nil;
        }
    }
}

- (void)readCard:(void(^)(NSDictionary *data))callback
{
    self.readCB = callback;
    MPMusicPlayerController *musicPlayer = [MPMusicPlayerController applicationMusicPlayer];
    musicPlayer.volume = 1.0f;
}

//DELEGATE FUNCTIONS
- (void)onDeviceConnectionDidChange:(MTSCRADeviceType)deviceType connected:(BOOL)connected instance:(id)instance {
    if(connected) {
        if(self.openCB) {
            self.openCB(1);
            self.openCB = nil;
        }
    }
    else {
        if(self.closeCB) {
            self.closeCB(1);
            self.closeCB = nil;
        }
    }
}

- (void) onDataReceived: (MTCardData*)cardDataObj instance:(id)instance {
    NSLog(@"Got Card Data!");
    
    NSMutableDictionary *data = [NSMutableDictionary new];

    NSLog(@"Track 1 Data: %@", cardDataObj.maskedTrack1);
    NSLog(@"Track 2 Data: %@", cardDataObj.maskedTrack1);
    
    [data setValue:cardDataObj.maskedTrack1 forKey:@"Track1"];
    [data setValue:cardDataObj.maskedTrack2 forKey:@"Track2"];
    [data setValue:cardDataObj.maskedTrack3 forKey:@"Track3"];
    
    if(self.readCB) {
        self.readCB(data);
        self.readCB = nil;
    }
    else {
        NSLog(@"readCB not found!");
    }
}

@end
