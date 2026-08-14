import CpNotFound from '@/framework/components/CpNotFound.vue'

export const PREFIX = '/pages/smlj/cghtz/index.html'

const _homeRouter = {
    path: '/home', name: 'home', component: () => import('../views/home.vue'),
    children: [
        {path: '', name: 'home_default', redirect: '/home/ledger',},
        // meta.perms：路由级权限控制，用户必须拥有其中任一权限码才能访问
        {path: 'ledger', name: 'home_ledger', component: () => import('../views/ledger.vue'), meta: {title: '合同台账', perms: ['contract.view']}},
        {path: 'import', name: 'home_import', component: () => import('../views/import.vue'), meta: {title: '批量导入', perms: ['contract.import']}},
        {path: 'users', name: 'home_users', component: () => import('../views/users.vue'), meta: {title: '账号与权限', perms: ['permission.assign']}},
    ],
}

const _indexRouter = {
    path: "/", redirect: _homeRouter.path,
}

const _loginRouter = {
    path: '/login', name: 'login', component: () => import('../views/login.vue'),
}

const _404Router = {
    path: '/:pathMatch(.*)*', name: 'notFound', component: CpNotFound,
}

export const pathToRouter = {
    [_indexRouter.path]: _indexRouter,
    [_loginRouter.path]: _loginRouter,
    [_homeRouter.path]: _homeRouter,
    [_404Router.path]: _404Router,
}

const currentPath = ref(window.location.hash)
window.addEventListener('hashchange', () => {
    currentPath.value = window.location.hash;

    console.log("hashchange currentRoutePath:" + currentPath.value)
})

console.log("currentRoutePath:" + currentPath.value)

// 定义的所有router全部在此注册
export const routers = [
    _indexRouter,
    _loginRouter,
    _homeRouter,
    _404Router,
];
