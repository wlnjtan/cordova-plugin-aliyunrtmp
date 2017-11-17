

#import <Cordova/CDV.h>

@interface CDVAliyunRTMP:CDVPlugin 


- (void)start:(CDVInvokedUrlCommand *)command;

- (void)stop:(CDVInvokedUrlCommand *)command;

- (void)play:(CDVInvokedUrlCommand *)command;

- (void)playstop:(CDVInvokedUrlCommand *)command;

- (void)checkpushstatus:(CDVInvokedUrlCommand *)command;

- (void)checkplaystatus:(CDVInvokedUrlCommand *)command;

@end
