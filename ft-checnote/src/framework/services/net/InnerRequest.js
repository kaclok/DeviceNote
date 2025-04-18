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

async function get(option) {
    const res = await _request({method: 'GET', ...option})
    return res.data
}

async function post(option) {
    const res = await _request({method: 'POST', ...option})
    return res.data
}

async function reqOriginal(method, option) {
    return _request({method, ...option});
}

async function _delete(option) {
    const res = await _request({method: 'DELETE', ...option})
    return res.data
}

async function put(option) {
    const res = await _request({method: 'PUT', ...option})
    return res.data
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

export {get, post, reqOriginal, _delete, put, download, upload}
