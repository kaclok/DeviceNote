import {changeHttpCodeMap, changeNwCodeMap} from "@/framework/services/net/AxiosInst.js";
import {NwCodeMap} from "@/framework/services/net/NwCodeMap.js";
import {httpCodeMap} from "@/framework/services/net/HttpCodeMap.js";

changeNwCodeMap(NwCodeMap);
changeHttpCodeMap(httpCodeMap);
