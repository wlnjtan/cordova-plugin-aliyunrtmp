var argscheck = require('cordova/argscheck'),
exec = require('cordova/exec');

module.exports={
  
start:function(streamUrl, videoResolution, filter, outputStreamType, onSuccess,onError){
    
    exec(onSuccess,onError,"AliyunRTMP","start",[streamUrl, videoResolution, filter, outputStreamType]);
    
},
    
stop:function(){
    
    exec(null,null,"AliyunRTMP","stop");
    
}, 
pause:function(){
    
    exec(null,null,"AliyunRTMP","pause");
    
},
resume:function(){
    
    exec(null,null,"AliyunRTMP","resume");
    
},
  
play:function(streamUrl, playMediaType, playStartPosition, onSuccess,onError){
    
    exec(onSuccess,onError,"AliyunRTMP","play",[streamUrl, playMediaType, playStartPosition]);
    
},
playstop:function(onSuccess,onError){
    
    exec(onSuccess,onError,"AliyunRTMP","playstop");
    
},
playpause:function(onSuccess,onError){
    
    exec(onSuccess,onError,"AliyunRTMP","playpause");
    
},
playresume:function(onSuccess,onError){
    
    exec(onSuccess,onError,"AliyunRTMP","playresume");
    
},
getplayduration:function(onSuccess,onError){
    
    exec(onSuccess,onError,"AliyunRTMP","getplayduration");
    
},
playseek:function(seekPos, onSuccess,onError){
    
    exec(onSuccess,onError,"AliyunRTMP","playseek",[seekPos]);
    
},
getcurrentplayposition:function(onSuccess,onError){
    
    exec(onSuccess,onError,"AliyunRTMP","getcurrentplayposition");
    
},
checkplaystatus:function(onSuccess,onError){
    
    exec(onSuccess,onError,"AliyunRTMP","checkplaystatus");
},
checkpushstatus:function(onSuccess,onError){
    
    exec(onSuccess,onError,"AliyunRTMP","checkpushstatus");
}

};