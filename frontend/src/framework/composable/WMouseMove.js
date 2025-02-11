// mouse.js
import {ref} from 'vue'
import {useDomListener} from '@/framework/composable/Dom.js'

export function useMouseMove() {
    const x = ref(0)
    const y = ref(0)

    useDomListener(window, 'mousemove', (event) => {
        x.value = event.pageX
        y.value = event.pageY
    })

    return {x, y}
}