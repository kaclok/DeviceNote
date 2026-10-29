import CpNotFound from '@/framework/components/CpNotFound.vue'

export const PREFIX = '/pages/smlj/cbs/index.html'

const _homeRouter = {
    path: '/home', name: 'home', redirect: () => {
        console.warn('redirect ------ current: /home -> to:', "/home/org")
        return '/home/org'
    }, component: () => import('../views/home.vue'),
    children: [
        // meta.perms：路由级权限控制，用户必须拥有其中任一权限码才能访问
        {path: 'org', name: 'home_org', component: () => import('../views/org.vue'), meta: {title: '组织管理', perms: ['cbs:org']}},
        {path: 'contractor', name: 'home_contractor', component: () => import('../views/contractor.vue'), meta: {title: '承包商管理', perms: ['cbs:contractor']}},
        {path: 'project', name: 'home_project', component: () => import('../views/project.vue'), meta: {title: '项目管理', perms: ['cbs:project']}},
        {path: 'promise', name: 'home_promise', component: () => import('../views/promise.vue'), meta: {title: '安全承诺', perms: ['cbs:promise']}},
        {path: 'spotcheck', name: 'home_spotcheck', component: () => import('../views/spotcheck.vue'), meta: {title: '在线抽查', perms: ['cbs:spotcheck']}},
        {path: 'baseinfo', name: 'home_baseinfo', component: () => import('../views/baseinfo.vue'), meta: {title: '基础设置', perms: ['cbs:baseinfo']}},
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
