import {computed, ref} from "vue";

import CpMain from '@/cms/daily_paper/App.vue'
import CpNotFound from '@/framework/components/CpNotFound.vue'

export const _mainRouter = {
    path: '/',
    name: 'mainRouter',
    component: CpMain,
}

export const _404Router = {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: CpNotFound,
}

export const pathToRouter = {
    [_mainRouter.path]: _mainRouter,
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
    _mainRouter,
    _404Router,
];

export {
    currentRouter,
    currentView,
}
