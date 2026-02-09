import {axiosInst as axiosR} from './AxiosInst.js'
import {config} from './Config.js'

const {default_headers} = config

function _request(option) {
    const {url, method, params, data, headersType, responseType, ...config} = option
    return axiosR({
        url: url,
        method,
        params,
        data,
        ...config,
        responseType: responseType,
        headers: {
            'Content-Type': headersType || default_headers,
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
