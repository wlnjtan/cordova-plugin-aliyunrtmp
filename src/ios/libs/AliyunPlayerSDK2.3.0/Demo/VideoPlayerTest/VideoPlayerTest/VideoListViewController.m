//
//  VideoListViewController.m
//  TBMediaPlayerTest
//
//  Created by shiping chen on 15-7-20.
//  Copyright (c) 2015年 shiping chen. All rights reserved.
//

#import "VideoListViewController.h"
#import "AliVcMoiveViewController.h"


@interface VideoListViewController ()
{
    UIAlertView* customAlertView;
}

@end

@implementation VideoListViewController

@synthesize videolists,datasource;

- (id)initWithStyle:(UITableViewStyle)style
{
    self = [super initWithStyle:style];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (NSArray *)getFilenamelistOfType:(NSString *)type fromDirPath:(NSString *)dirPath
{
    NSMutableArray *filenamelist = [NSMutableArray arrayWithCapacity:10];
    NSArray *tmplist = [[NSFileManager defaultManager] contentsOfDirectoryAtPath:dirPath error:nil];
    
    for (NSString *filename in tmplist) {
        NSString *fullpath = [dirPath stringByAppendingPathComponent:filename];
        if ([self isFileExistAtPath:fullpath]) {
            if ([[filename pathExtension] isEqualToString:type]) {
                [filenamelist  addObject:filename];
            }
        }
    }
    
    return filenamelist;
}

- (BOOL)isFileExistAtPath:(NSString*)fileFullPath {
    BOOL isExist = NO;
    isExist = [[NSFileManager defaultManager] fileExistsAtPath:fileFullPath];
    return isExist;
}

- (void)loadLocalVideo
{
    NSArray *pathArray = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *docDir = [pathArray objectAtIndex:0];
    
    NSMutableArray* video_extension = [[NSMutableArray alloc] init];
    [video_extension addObject:@"mp4"];
    [video_extension addObject:@"mkv"];
    [video_extension addObject:@"rmvb"];
    [video_extension addObject:@"rm"];
    [video_extension addObject:@"avs"];
    [video_extension addObject:@"mpg"];
    [video_extension addObject:@"3g2"];
    [video_extension addObject:@"asf"];
    [video_extension addObject:@"mov"];
    [video_extension addObject:@"avi"];
    [video_extension addObject:@"wmv"];
    [video_extension addObject:@"flv"];
    [video_extension addObject:@"m4v"];
    [video_extension addObject:@"swf"];
    [video_extension addObject:@"webm"];
    [video_extension addObject:@"3gp"];
    
    for(NSString* ext in video_extension) {
        
        NSArray *filename = [self getFilenamelistOfType:ext
                                            fromDirPath:docDir];
        
        for (NSString* name in filename) {
            
            NSMutableString* fullname = [NSMutableString stringWithString:docDir];
            [fullname appendString:@"/"];
            [fullname appendString:name];
            
            [videolists setObject:fullname forKey:name];
        }
    }
    
    datasource = [videolists allKeys];
}

- (void)loadRemoteVideo
{
    NSArray *pathArray = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
    NSString *docDir = [pathArray objectAtIndex:0];
    NSString *videolistPath = [docDir stringByAppendingFormat:@"/videolist.txt"];
    FILE *file = fopen([videolistPath UTF8String], "rb");
    if(file == NULL)
        return;
    
    char VideoPath[200] = {0};
    fgets(VideoPath, 200, file);
    
    do{
        VideoPath[strlen(VideoPath)] = '\0';
        NSString *srcFile = [NSString stringWithUTF8String:VideoPath];
        
        NSRange range1 = [srcFile rangeOfString:@"["];
        NSRange range2 = [srcFile rangeOfString:@"]"];
        if(range1.location == NSNotFound || range2.location == NSNotFound)
            continue;
        NSRange rangeName;
        rangeName.location = range1.location+1;
        rangeName.length = range2.location-range1.location-1;
        NSString* filename = [srcFile substringWithRange:rangeName];
        
        NSRange range;
        range = [srcFile rangeOfString:@"http:"];
        if(range.location == NSNotFound){ //m3u8
            range = [srcFile rangeOfString:@"rtmp:"];
            if(range.location == NSNotFound){ //rtmp
                continue;
            }
        }
    
        NSString* m3u8file = [srcFile substringFromIndex:range.location];
        NSRange rangeEnd = [srcFile rangeOfString:@"\n"];
        if(rangeEnd.location != NSNotFound) {
            rangeEnd.location = 0;
            rangeEnd.length = m3u8file.length-1;
            m3u8file = [m3u8file substringWithRange:rangeEnd];
        }
        rangeEnd = [srcFile rangeOfString:@"\r"];
        if(rangeEnd.location != NSNotFound) {
            rangeEnd.location = 0;
            rangeEnd.length = m3u8file.length-1;
            m3u8file = [m3u8file substringWithRange:rangeEnd];
        }
        
        [videolists setObject:m3u8file forKey:filename];
        
    
    }while (fgets(VideoPath, 200, file));
    
    fclose(file);
}


//添加地址到视频播放列表中
//按照如下格式进行添加
-(void) addVideoToList
{
    [videolists setObject:@"http:://yourAddress.m3u8" forKey:@"videoName"];
}

NSString* accessKeyID = @"LTAIJsvkiSzW349g";
NSString* accessKeySecret = @"ASnkQdZr74k6wnnTxUFeP9BjyoiE34";


-(AliVcAccesskey*)getAccessKeyIDSecret
{
    AliVcAccesskey* accessKey = [[AliVcAccesskey alloc] init];
    accessKey.accessKeyId = accessKeyID;
    accessKey.accessKeySecret = accessKeySecret;
    return accessKey;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    customAlertView = nil;
    
    [AliVcMediaPlayer setAccessKeyDelegate:self];

    videolists = [[NSMutableDictionary alloc]init];
    
    [videolists setObject:@"videolist" forKey:@"手动输入地址"];
    
    [self loadRemoteVideo];
    [self loadLocalVideo];
    [self addVideoToList];
    
    datasource = [videolists allKeys];

    // Uncomment the following line to preserve selection between presentations.
    // self.clearsSelectionOnViewWillAppear = NO;
    
    // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
    // self.navigationItem.rightBarButtonItem = self.editButtonItem;
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

#pragma mark - Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView
{
    // Return the number of sections.
    return 1;
}

- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    // Return the number of rows in the section.
    return [datasource count];
}


- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    static NSString *CellIdentifier = @"Cell";
    UITableViewCell *cell = (UITableViewCell *) [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    if (cell == nil) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:CellIdentifier];
    }
    
    //---------- CELL BACKGROUND IMAGE -----------------------------
    UIImageView *imageView = [[UIImageView alloc] initWithFrame:cell.frame];
    UIImage *image = [UIImage imageNamed:@"LightGrey@2x.png"];
    imageView.image = image;
    cell.backgroundView = imageView;
    [[cell textLabel] setBackgroundColor:[UIColor clearColor]];
    [[cell detailTextLabel] setBackgroundColor:[UIColor clearColor]];
    
    cell.textLabel.text = [datasource objectAtIndex:indexPath.row];
    
    //Arrow
    cell.accessoryType = UITableViewCellAccessoryDisclosureIndicator;
    
    return cell;
}

-(void)alertView:(UIAlertView *)alertView clickedButtonAtIndex:(NSInteger)buttonIndex{
    
    if (buttonIndex == alertView.firstOtherButtonIndex) {
        UITextField *urlField = [alertView textFieldAtIndex:0];
        
        TBMoiveViewController* currentView = [[TBMoiveViewController alloc] init];
        NSString* strUrl = urlField.text;
        strUrl = [strUrl stringByTrimmingCharactersInSet:[NSCharacterSet whitespaceCharacterSet]];
        NSURL* url = [NSURL URLWithString:strUrl];
        if(url == nil) {
            UIAlertView *alter = [[UIAlertView alloc] initWithTitle:@"错误" message:@"输入地址无效" delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
            
            [alter show];
            return;
        }
        [currentView SetMoiveSource:url];
        
        [self presentViewController:currentView animated:YES completion:nil ];
    }
}

-(void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath{
    
    NSString* source = [datasource objectAtIndex:indexPath.row];
    
    NSFileManager *fileManager = [NSFileManager defaultManager];
    NSString* vs = [videolists objectForKey:source];
    if ([vs isEqualToString:@"videolist"]) {
        
        if (customAlertView==nil) {
            customAlertView = [[UIAlertView alloc] initWithTitle:@"输入播放地址" message:nil delegate:self cancelButtonTitle:@"取消" otherButtonTitles:@"确定", nil];
        }
        [customAlertView setAlertViewStyle:UIAlertViewStylePlainTextInput];
    
        UITextField *urlField = [customAlertView textFieldAtIndex:0];
        [urlField setSecureTextEntry:NO];
        urlField.placeholder = @"请输入一个URL";
        urlField.text = @"";
        
        [customAlertView show];
        
        return;
    }
    
    vs = [vs stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
    NSURL* url = nil;
    TBMoiveViewController* currentView = [[TBMoiveViewController alloc] init];
    if([fileManager fileExistsAtPath:vs]){
        url = [NSURL fileURLWithPath:vs];
        
        
        
    }
    else {
        url = [NSURL URLWithString:vs];
    }
    
    if (url == nil) {
        UIAlertView *alter = [[UIAlertView alloc] initWithTitle:@"错误" message:@"输入地址无效" delegate:nil cancelButtonTitle:@"OK" otherButtonTitles:nil];
        
        [alter show];
        return;
    }
    
    [currentView SetMoiveSource:url];
    [self presentViewController:currentView animated:YES completion:nil ];
}

/*
 
// Override to support conditional editing of the table view.
- (BOOL)tableView:(UITableView *)tableView canEditRowAtIndexPath:(NSIndexPath *)indexPath
{
    // Return NO if you do not want the specified item to be editable
    return YES;
}

/*
// Override to support editing the table view.
- (void)tableView:(UITableView *)tableView commitEditingStyle:(UITableViewCellEditingStyle)editingStyle forRowAtIndexPath:(NSIndexPath *)indexPath
{
    if (editingStyle == UITableViewCellEditingStyleDelete) {
        // Delete the row from the data source
        [tableView deleteRowsAtIndexPaths:@[indexPath] withRowAnimation:UITableViewRowAnimationFade];
    } else if (editingStyle == UITableViewCellEditingStyleInsert) {
        // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
    }   
}
*/


// Override to support rearranging the table view.
//- (void)tableView:(UITableView *)tableView moveRowAtIndexPath:(NSIndexPath *)fromIndexPath toIndexPath:(NSIndexPath *)toIndexPath
//{
//}


/*
// Override to support conditional rearranging of the table view.
- (BOOL)tableView:(UITableView *)tableView canMoveRowAtIndexPath:(NSIndexPath *)indexPath
{
    // Return NO if you do not want the item to be re-orderable.
    return YES;
}
*/

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
