import {axiosInst as axiosR} from './AxiosInst.js'
import {config} from './Config.js'

const {default_headers} = config

function _request(option) {
    // ...configs其实是将option中其他所有属性归并到configs中
    const {url, method, params, data, headersType, responseType, headers, ...configs} = option
    return axiosR({
        url,
        method, //（key 和 value 同名时），否则一般是url: url
        params,
        data,
        ...configs, // 如果configs中有headers,会被下面的headers覆盖
        responseType: responseType,
        headers: {
            'Content-Type': headersType || default_headers,
            ...headers,
        },
    })
}

function _requestAsync(method, option, callback) {
    _request({method: method, ...option})
        .then(res => {
            callback(true, res.data)
        })
        .catch(err => {
            callback(false, err)
        })
}

async function get(option) {
    const res = await _request({method: 'GET', ...option})
    return res.data
}

async function post(option) {
    const res = await _request({method: 'POST', ...option})
    return res.data
}

async function put(option) {
    const res = await _request({method: 'PUT', ...option})
    return res.data
}

async function getAsync(option, callback) {
    return _requestAsync('GET', option, callback)
}

async function postAsync(option, callback) {
    return _requestAsync('POST', option, callback)
}

async function putAsync(option, callback) {
    return _requestAsync('PUT', option, callback)
}

async function download(option) {
    return _request({
        method: 'POST',
        responseType: 'blob',
        ...option,
    });
}

async function upload(option) {
    return _request({method: 'POST', ...option});
}

export {
    get, post, put,
    getAsync, postAsync, putAsync,
    download, upload
}
