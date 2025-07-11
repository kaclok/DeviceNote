import CpNotFound from '@/framework/components/CpNotFound.vue'

export const PREFIX = '/pages/smlj/cggy/index.html'

const _homeRouter = {
    path: '/home', name: 'home', component: () => import('../views/home.vue'),
    children: [{path: '', name: 'home_default', redirect: '/home/bill',},
        {path: 'jhy', name: 'home_jhy', component: () => import('../views/home_jhy.vue'),},
        {path: 'cgy', name: 'home_cgy', component: () => import('../views/home_cgy.vue'),},
        {
            path: 'bill', name: 'home_bill', component: () => import('../views/home_bill.vue'),
            children: [{path: '', name: 'home_bill_default', redirect: '/home/bill/input'},
                {path: 'input', name: 'home_bill_input', component: () => import('../views/home_bill_input.vue'),},

                // https://router.vuejs.org/zh/guide/essentials/passing-props.html
                // props: true 将 params 自动转为 props
                // (\\d+)限制必须为数字
                // 传递参数：router.push({name: "home_bill_table", params: {goodsId: curLevelId.value, timestamp: now}});
                // {path: 'table/:goodsId(\\d+)/:timestamp(\\d+)', name: 'home_bill_table', component: () => import('../views/home_bill_table.vue'), props: true},]
                {path: 'table', name: 'home_bill_table', component: () => import('../views/home_bill_table.vue'), props: false},]
        },],
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
