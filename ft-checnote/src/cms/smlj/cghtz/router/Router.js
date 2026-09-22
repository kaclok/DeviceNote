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
        // 合同编辑：/home/contract/<物理表名>/<unique_id?>。一条合同的字段结构由它所在部门的
        // 合同模板决定，所以"编辑页"是用 tb 参数化出来的 —— 每个模板一套字段，同一个页面渲染。
        // 只给持有写权限的人开这个门（读的人没有理由进来）：meta.perms 满足其一即可。
        {
            path: 'contract/:tb/:uid?',
            name: 'home_contractEdit',
            component: () => import('../views/contractEdit.vue'),
            meta: {title: '合同编辑', perms: ['contract:create', 'contract:update']},
        },
        // 部门合同模板：与账号与权限同一道权限门 —— 都是「配置类」页面，能调账号的人才能调模版绑定
        {path: 'deptTpl', name: 'home_deptTpl', component: () => import('../views/deptTpl.vue'), meta: {title: '部门合同模板', perms: ['perm:assign']}},
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
