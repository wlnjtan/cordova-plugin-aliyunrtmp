//
//  VideoListViewController.h
//  TBMediaPlayerTest
//
//  Created by shiping chen on 15-7-20.
//  Copyright (c) 2015年 shiping chen. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <AliyunPlayerSDK/AliyunPlayerSDK.h>

@interface VideoListViewController : UITableViewController<AliVcAccessKeyProtocol>

@property(nonatomic,retain) NSMutableDictionary *videolists;
@property(nonatomic,retain) NSArray *datasource;

-(NSArray *)getFilenamelistOfType:(NSString *)type fromDirPath:(NSString *)dirPath;
-(BOOL)isFileExistAtPath:(NSString*)fileFullPath;

@end
