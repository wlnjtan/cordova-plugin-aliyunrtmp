//
//  AlivcLiveViewController.h
//  DevAlivcLiveVideo
//
//  Created by lyz on 16/3/21.
//  Copyright © 2016年 Alivc. All rights reserved.
//

#import <UIKit/UIKit.h>

@interface AlivcLiveViewController : UIViewController

@property (weak, nonatomic) IBOutlet UITextView *textView;
@property (nonatomic, assign) BOOL isScreenHorizontal;
- (instancetype)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil url:(NSString *)url;

@end
