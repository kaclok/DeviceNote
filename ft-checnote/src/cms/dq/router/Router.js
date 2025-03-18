import {computed, ref} from "vue";

import CpRecord from '@/cms/daily_paper/views/Record.vue'
import CpLogin from '@/cms/daily_paper/views/Login.vue'
import CpNotFound from '@/framework/components/CpNotFound.vue'

const _loginRouter = {
    path: '/login',
    name: 'loginRouter',
    component: CpLogin,
}

const _recordRouter = {
    path: '/record',
    name: 'recordRouter',
    component: CpRecord,
}

const _indexRouter = {
    path: '/',
    redirect: _recordRouter.path,
}

const _404Router = {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: CpNotFound,
}

export const pathToRouter = {
    [_indexRouter.path]: _indexRouter,
    [_loginRouter.path]: _loginRouter,
    [_recordRouter.path]: _recordRouter,
    [_404Router.path]: _404Router,
}

const currentPath = ref(window.location.hash)
window.addEventListener('hashchange', () => {
    console.log("hashchange")
    currentPath.value = window.location.hash;
})

const currentRouter = computed(() => {
    let key = currentPath.value.slice(1) || '/';
    return pathToRouter[key] || _404Router;
})

const currentView = computed(() => {
    if (currentRouter) {
        // computed返回值其实也是一个ref,computed监听的也是一个ref或者reactive
        return currentRouter.value.component;
    }
    return CpNotFound;
})

console.log("CurrentRouter:" + currentRouter.value.name)
console.log("CurrentView:" + currentView.value)

// 定义的所有router全部在此注册
export const routers = [
    _indexRouter,
    _loginRouter,
    _recordRouter,
    _404Router,
];

export {
    currentRouter,
    currentView,
}
