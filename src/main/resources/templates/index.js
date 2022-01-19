/************************
 * 普通文件上传
 * *********************
 */

function request(method,path,param,callback) {
    let XHR = null;
    if (window.XMLHttpRequest) {
        XHR = new XMLHttpRequest();
    } else if (window.ActiveXObject) {
        XHR = new ActiveXObject("Microsoft.XMLHTTP");
    } else {
        XHR = null;
    }
    if (XHR) {
        XHR.open(method, path);
        XHR.onreadystatechange = function () {
            if (XHR.readyState == 4 && XHR.status == 200) {
                callback(XHR.responseText);
            }
        }
    }
    XHR.send(param);
}


/**
 * 切片上传
 * @returns {*}
 */
function partUpload(GUID, partFile, name, chunks, chunk, partUrl, partMethod,callback) {
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

