// https://www.bilibili.com/video/BV1DKDMYBETU?spm_id_from=333.788.videopod.sections&vd_source=5c9f5bd891aee351c325bcf632b5550f
import {HttpStatusCode} from "axios";

const httpCodeMap = {
    [HttpStatusCode.BadRequest]: (response) => {

    },
    [HttpStatusCode.Forbidden]: (response) => {

    },
    [HttpStatusCode.NotFound]: (response) => {

    },
}

export {
    httpCodeMap,
}