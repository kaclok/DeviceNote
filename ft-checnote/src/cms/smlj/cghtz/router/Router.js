import CpNotFound from '@/framework/components/CpNotFound.vue'

export const PREFIX = '/pages/smlj/cghtz/index.html'

const _homeRouter = {
    // redirect 直接写在父路由上：无论按 name（router.push({name:'home'})）还是按路径（/home）导航都会触发，
    // 无需在守卫里做容器路由检测；children 中不再需要 path:'' 的 redirect 占位子路由
    path: '/home', name: 'home', redirect: () => {
        console.warn('redirect ------ current: /home -> to:', "/home/ledger")
        return '/home/ledger'
    }, component: () => import('../views/home.vue'),
    children: [
        // meta.perms：路由级权限控制，用户必须拥有其中任一权限码才能访问
        {path: 'ledger', name: 'home_ledger', component: () => import('../views/ledger.vue'), meta: {title: '合同台账'}},
        {path: 'import', name: 'home_import', component: () => import('../views/import.vue'), meta: {title: '批量导入'}},
        {path: 'users', name: 'home_users', component: () => import('../views/users.vue'), meta: {title: '账号与权限', perms: ['perm:assign']}},
    ],
}

const _indexRouter = {
    path: "/", redirect: () => {
        console.warn('redirect ------ current: / -> to:', _homeRouter.path)
        return _homeRouter.path
    },
}

const _loginRouter = {
    path: '/login', name: 'login', component: () => import('../views/login.vue'),
}

const _404Router = {
    path: '/:pathMatch(.*)*', name: 'notFound', component: CpNotFound,
}

// 定义的所有router全部在此注册
export const routers = [
    _indexRouter,
    _loginRouter,
    _homeRouter,
    _404Router,
];
