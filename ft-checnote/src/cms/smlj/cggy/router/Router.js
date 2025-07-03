import CpHome from '../views/home.vue'
import CpLogin from '../views/login.vue'
import CpNotFound from '@/framework/components/CpNotFound.vue'

export let PREFIX = '/pages/smlj/cggy'

const _homeRouter = {
    path: PREFIX + '/home',
    name: 'home',
    component: CpHome,
}

const _indexFullRouter = {
    path: PREFIX + "/index.html",
    redirect: _homeRouter.path,
}

const _indexRouter = {
    path: PREFIX + "/",
    redirect: _homeRouter.path,
}

const _loginRouter = {
    path: PREFIX + '/login',
    name: 'login',
    component: CpLogin,
}

const _404Router = {
    path: PREFIX + '/:pathMatch(.*)*',
    name: 'notFound',
    component: CpNotFound,
}

export const pathToRouter = {
    [_indexFullRouter.path]: _indexFullRouter,
    [_indexRouter.path]: _indexRouter,
    [_loginRouter.path]: _loginRouter,
    [_homeRouter.path]: _homeRouter,
    [_404Router.path]: _404Router,
}

/*const currentPath = ref(window.location.hash)
window.addEventListener('hashchange', () => {
    console.error("hashchange")
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

console.error("currentPath:" + currentPath.value)
console.error("CurrentRouter:" + currentRouter.value.name)
console.table(currentView.value)*/

// 定义的所有router全部在此注册
export const routers = [
    _indexFullRouter,
    _indexRouter,
    _loginRouter,
    _homeRouter,
    _404Router,
];
