import CpNotFound from '@/framework/components/CpNotFound.vue'

export const PREFIX = '/pages/smlj/plantmap/index.html'

const _homeRouter = {
    path: '/home',
    name: 'home',
    redirect: '/home/map',
    component: () => import('../views/home.vue'),
    children: [
        {path: 'map', name: 'home_map', component: () => import('../views/map.vue'), meta: {title: '厂区寻路'}},
        {path: 'gb', name: 'home_gb', component: () => import('../views/gbquery.vue'), meta: {title: '地磅查询'}},
        {path: 'setting', name: 'home_setting', component: () => import('../views/setting.vue'), meta: {title: '配置'}},
    ],
}

const _indexRouter = {
    path: '/',
    redirect: '/home',
}

const _404Router = {
    path: '/:pathMatch(.*)*',
    name: 'notFound',
    component: CpNotFound,
}

export const routers = [
    _indexRouter,
    _homeRouter,
    _404Router,
]
