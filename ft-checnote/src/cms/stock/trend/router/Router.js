import CpNotFound from '@/framework/components/CpNotFound.vue'

// 入口文件全路径：hash 前是入口 html，hash 后才是业务路由
export const PREFIX = '/pages/stock/trend/index.html'

const _homeRouter = {
    path: '/home', name: 'home',
    redirect: () => '/home/overview',
    component: () => import('../views/home.vue'),
    children: [
        {path: 'overview', name: 'home_overview', component: () => import('../views/overview.vue'), meta: {title: '大盘总览', icon: 'DataLine'}},
        {path: 'stock', name: 'home_stock', component: () => import('../views/stock.vue'), meta: {title: '个股行情', icon: 'Search'}},
        {path: 'limitUp', name: 'home_limitUp', component: () => import('../views/limitUp.vue'), meta: {title: '涨停榜', icon: 'Top'}},
        {path: 'sectors', name: 'home_sectors', component: () => import('../views/sectors.vue'), meta: {title: '板块分析', icon: 'Grid'}},
        {path: 'reason', name: 'home_reason', component: () => import('../views/reason.vue'), meta: {title: '涨停归因', icon: 'MagicStick'}},
        {path: 'review', name: 'home_review', component: () => import('../views/review.vue'), meta: {title: '盘后复盘', icon: 'Histogram'}},
    ],
}

const _indexRouter = {
    path: '/', redirect: () => _homeRouter.path,
}

const _404Router = {
    path: '/:pathMatch(.*)*', name: 'notFound', component: CpNotFound,
}

// 定义的所有 router 全部在此注册
export const routers = [
    _indexRouter,
    _homeRouter,
    _404Router,
]
