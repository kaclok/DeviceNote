import CpNotFound from '@/framework/components/CpNotFound.vue'

export const PREFIX = '/pages/smlj/plantmap/index.html'

const _homeRouter = {
    path: '/home',
    name: 'home',
    redirect: '/home/map',
    component: () => import('../views/home.vue'),
    children: [
        {path: 'map', name: 'home_map', component: () => import('../views/map.vue'), meta: {title: '厂区寻路'}},
        {path: 'scales', name: 'home_scales', component: () => import('../views/scales.vue'), meta: {title: '磅房与绑定'}},
        {path: 'config', name: 'home_config', component: () => import('../views/config.vue'), meta: {title: '地图配置'}},
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
