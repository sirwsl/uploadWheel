function upload() {
    let file = document.getElementById("uploadFile").files[0];//IE10以下不支持
    let fd = new FormData();
    fd.append('file', file);
    document.getElementById("detail1").innerHTML = '文件正在上传... ...';
    let call = function (result) {
        let res = JSON.parse(result);
        if (res.code == 200 && res.data != '') {
            console.log(res);
            document.getElementById("detail1").innerHTML = '文件上传成功!</br>路径为：' + res.data;
        }
    }
    request("POST", "/uploadFile", fd, call);


}

function uploadBigFile() {
    let file = document.getElementById("uploadBigFile").files[0];//IE10以下不支持
    let name = file.name,        //文件名
        size = file.size;        //总大小
    let GUID = guid();
    let shardSize = 1 * 1024 * 1024,    //以1MB为一个分片
        shardCount = Math.ceil(size / shardSize);  //总片数
    let count = 0;
    for (let i = 0; i < shardCount; ++i) {
        //计算每一片的起始与结束位置
        let start = i * shardSize,
            end = Math.min(size, start + shardSize);
        let partFile = file.slice(start, end);
        let call = function (result) {
            let res = JSON.parse(result);
            if (res.code == 200) {
                document.getElementById("detail2").innerHTML = '此次文件共切片' + shardCount + '片</br>切片上传进度：' + (i + 1) + "/" + shardCount;
                count++;
            }

            if (count == shardCount) {
                let call2 = function (result) {
                    let res = JSON.parse(result);
                    if (res.code == 200) {
                        document.getElementById("detail2").innerHTML = "切片上传成功！地址：" + res.data;
                    }
                }
                document.getElementById("detail2").innerHTML = "正在进行文件合并并上传至文件服务";
                mergeFile(GUID, name, "/uploadMerge", "POST", call2);
            }
        }
        partUpload(GUID, partFile, name, shardCount, i, "/uploadSlice", "POST", call);
    }


}


function uploadBigFileBySocket() {
    let file = document.getElementById("uploadBigFile").files[0];//IE10以下不支持
    socketUpload("file",file,"detail2");
}

function uploadVideoBySocket() {
    let file = document.getElementById("uploadVideo").files[0];//IE10以下不支持
    socketUpload("video",file,"detail3");

}

function socketUpload(type,file,dom){
    let name = file.name,        //文件名
        size = file.size;        //总大小

    //socket数据针过大会导致发送断开
    let shardSize = 5 * 1024 * 1024,    //以1MB为一个分片
        shardCount = Math.ceil(size / shardSize);  //总片数

    let GUID = guid();
    let ws = new WebSocket('ws://localhost:8080/file/' + GUID);

    let map = {code:null,type:type,name: null, chunks: null};
    ws.onopen = () => {
        console.log('建立文件上传通道 ...');
        map.chunks = shardCount;
        map.name = name;
        map.code = 0;
        //创建服务器存储目录
        ws.send(JSON.stringify(map));
    }
    ws.onmessage = (evt) => {
        console.log('Received Message: ' + evt.data);
        let parse = JSON.parse(evt.data);
        if (parse.code == 101) {
            console.log('通道已建立 ...');
            for (let i = 0; i < shardCount; ++i) {
                //计算每一片的起始与结束位置
                let start = i * shardSize,
                    end = Math.min(size, start + shardSize);
                let fileBlob = file.slice(start, end);
                ws.send(fileBlob);
            }
        }
        if (parse.code == 200) {
            document.getElementById(dom).innerHTML = "当前上传进度为：" + parse.msg + "%";
        } else if (parse.code == 202){
            document.getElementById(dom).innerHTML = "文件正在解析";
        } else if (parse.code == 201){
            document.getElementById(dom).innerHTML = "解析成功，地址为："+parse.data;
            if (type ==='video'){
                changeVideo(parse.data);
            }
        }else {
            document.getElementById(dom).innerHTML = parse.data;
        }
        return null;
    }
    ws.onclose = function (e) {
        console.log('websocket 断开: ' + e.code + ' ' + e.reason + ' ' + e.wasClean)
        console.log(e)
    }
}


function uploadVideoSlice() {
    let file = document.getElementById("uploadVideo").files[0];//IE10以下不支持
    let name = file.name,        //文件名
        size = file.size;        //总大小
    let GUID = this.guid();
    let shardSize = 5 * 1024 * 1024,    //以1MB为一个分片
        shardCount = Math.ceil(size / shardSize);  //总片数
    let count = 0;
    for (let i = 0; i < shardCount; ++i) {
        //计算每一片的起始与结束位置
        let start = i * shardSize,
            end = Math.min(size, start + shardSize);
        let partFile = file.slice(start, end);
        let call1 = function (result) {
            let res = JSON.parse(result);
            if (res.code == 200) {
                document.getElementById("detail3").innerHTML = '此次文件共切片' + shardCount + '切片上传进度：' + (i + 1) + "/" + shardCount;
                count++;
            }
            if (count == shardCount) {
                document.getElementById("detail3").innerHTML = "正在进行视频合并、视频转码并转存、请耐心等待...如果觉得看不到进度，可以自己用Socket实现";
                let call2 = function (result) {
                    let res = JSON.parse(result);
                    if (res.code == 200) {
                        document.getElementById("detail3").innerHTML = "视频转码成功，且已存储！地址：" + res.data;
                        changeVideo(res.data);
                    }
                }
                mergeFile(GUID, name, "/uploadVideoMerge", "POST", call2);
            }
        }
        partUpload(GUID, partFile, name, shardCount, i, "/uploadSlice", "POST", call1);
    }
}

function uploadVideo() {
    let file = document.getElementById("uploadVideo").files[0];//IE10以下不支持
    let fd = new FormData();
    fd.append('file', file);
    document.getElementById("detail3").innerHTML = '视频上传中...';
    let call = function (result) {
        let res = JSON.parse(result)
        if (res.code == 200 && res.data != '') {
            document.getElementById("detail3").innerHTML = '文件上传成功正在加载,解析后路径为路径为：' + res.data;
            changeVideo(res.data);
        }
    }
    request("POST", "/uploadVideo", fd, call);
}

/************************
 * 普通文件上传 内部方法
 * *********************
 */

async function request(method,path,param,callback) {
    let XHR = null;
    if (window.XMLHttpRequest) {
        XHR = new XMLHttpRequest();
    } else if (window.ActiveXObject) {
        XHR = new ActiveXObject("Microsoft.XMLHTTP");
    } else {
        XHR = null;
    }
    if (XHR) {
        XHR.open(method, path,true);
        XHR.onreadystatechange = function () {
            if (XHR.readyState == 4 && XHR.status == 200) {
                callback(XHR.responseText);
            }
        }
        XHR.send(param);
    }
}


/**
 * 切片上传
 * @returns {*}
 */
async function partUpload(GUID, partFile, name, chunks, chunk, partUrl, partMethod,callback) {
    const form = new FormData();
    form.append("guid", GUID);
    form.append("file", partFile);  //slice方法用于切出文件的一部分
    form.append("fileName", name);
    form.append("chunks", chunks);  //总片数
    form.append("chunk", chunk);    //当前是第几片
    return request(partMethod,partUrl,form,callback);
}

/**
 * 文件合并
 * @returns {*}
 */
function mergeFile(GUID, name, mergeUrl, partMethod,callback) {
    const formMerge = new FormData();
    formMerge.append("guid", GUID);
    formMerge.append("fileName", name);
    return request(partMethod,mergeUrl,formMerge,callback);
}

/**
 * 生成id
 * @returns {string}
 */
 function guid(prefix) {
    let counter = 0;
    let guid = (+new Date()).toString(32),
        i = 0;
    for (; i < 5; i++) {
        guid += Math.floor(Math.random() * 65535).toString(32);
    }
    return (prefix || 'sirwsl_') + guid + (counter++).toString(32);
}


/**
 * m3u8 播放器
 */
let myVideo = videojs('myVideo', {
    bigPlayButton: true,
    textTrackDisplay: false,
    posterImage: false,
    errorDisplay: false,
})
myVideo.play()

let changeVideo = function (vdoSrc) {
    if (/\.m3u8$/.test(vdoSrc)) {
        myVideo.src({
            src: vdoSrc,
            type: 'application/x-mpegURL'
        })
    } else {
        myVideo.src(vdoSrc)
    }
    myVideo.load();
    myVideo.play();
}
