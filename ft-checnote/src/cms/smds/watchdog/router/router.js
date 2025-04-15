import {createRouter, createWebHashHistory} from 'vue-router'

import MainPage from '../components/routerPage/mainPage.vue'
import SettingPage from '../components/routerPage/settingPage.vue'
import AddItem from '../components/routerPage/addItem.vue'
import DeleteItem from '../components/routerPage/deleteItem.vue'
import ChangeItem from '../components/routerPage/changeItem.vue'
import AllItem from '../components/routerPage/allItem.vue'

const router = createRouter({
    history: createWebHashHistory(),
    routes:[{
        path:"/",
        redirect:'/home'
    }, {
        path:"/home",
        component:MainPage
    }, {
        path:"/settings",
        component:SettingPage,
        children: [
            {
                path:'addItem',
                component:AddItem,
            }, 
            {
                path:'deleteItem',
                component:DeleteItem,
            },
            {
                path:'changeItem',
                component:ChangeItem,
            },
            {
                path:'allItem',
                component:AllItem,
            },
        ]
    }]
})

export default router