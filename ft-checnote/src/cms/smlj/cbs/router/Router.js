import CpNotFound from '@/framework/components/CpNotFound.vue'

export const PREFIX = '/pages/smlj/cbs/index.html'

const _homeRouter = {
    // /home = 总览（模块宫格，对齐目标站 dashboard），子路由 = 各功能页
    path: '/home', name: 'home', component: () => import('../views/home.vue'),
    children: [
        // meta.perms：路由级权限控制，用户必须拥有其中任一权限码才能访问
        {path: 'org/:tab', name: 'home_org', component: () => import('../views/org.vue'), props: true, meta: {title: '基础设置', perms: ['cbs:org']}},
        {path: 'org', redirect: '/home/org/dept'},
        {path: 'contractor/:tab', name: 'home_contractor', component: () => import('../views/contractor.vue'), props: true, meta: {title: '承包商', perms: ['cbs:contractor']}},
        {path: 'contractor', redirect: '/home/contractor/unit'},
        {path: 'project/:tab', name: 'home_project', component: () => import('../views/project.vue'), props: true, meta: {title: '项目管理', perms: ['cbs:project']}},
        {path: 'project', redirect: '/home/project/proj'},
        {path: 'promise', name: 'home_promise', component: () => import('../views/promise.vue'), meta: {title: '安全承诺', perms: ['cbs:promise']}},
        {path: 'spotcheck', name: 'home_spotcheck', component: () => import('../views/spotcheck.vue'), meta: {title: '在线抽查', perms: ['cbs:spotcheck']}},
        {path: 'baseinfo/:tab', name: 'home_baseinfo', component: () => import('../views/baseinfo.vue'), props: true, meta: {title: '基础设置', perms: ['cbs:baseinfo']}},
        {path: 'baseinfo', redirect: '/home/baseinfo/ent'},
        // 承包商 / 基础设置 / 承包商类型|承包商处罚类型|项目类型 —— 一页只放一个类型
        {path: 'dict/:type', name: 'home_dict', component: () => import('../views/dict.vue'), props: true, meta: {title: '承包商基础设置', perms: ['cbs:baseinfo']}},
        {path: 'dict', redirect: '/home/dict/contractor_type'},
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
