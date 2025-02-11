/**
 * 自定义悬浮拖拽指令
 * https://developer.aliyun.com/article/1586430
 */
const directive = {
    beforeMount(el: any, binding: any) {
        // 判断是否可拖拽
        if (!binding.value) {
            return
        }

        // 获取相关元素
        const container = el.querySelector('.d-d_container')
        const header = el.querySelector('.d-d_container_header')
        header.style.cssText += ';cursor:move;'

        // 获取元素原有属性
        const sty = (function () {
            if ((document.body as any).currentStyle) {
                return (dom: any, attr: any) => dom.currentStyle[attr] // 兼容IE写法
            }
            return (dom: any, attr: any) => getComputedStyle(dom, null)[attr]
        })()

        /**
         * 鼠标按下事件
         */
        header.onmousedown = (e: any) => {
            const disX = e.clientX - header.offsetLeft
            const disY = e.clientY - header.offsetTop
            const screenWidth = document.body.clientWidth // document.body的可见区域宽度
            const screenHeight = document.documentElement.clientHeight // 可见区域高度(应为body高度，可某些环境下无法获取)

            const containerWidth = container.offsetWidth // 对话框宽度
            const containerHeight = container.offsetHeight // 对话框高度

            const minContainerLeft = container.offsetLeft
            const maxContainerLeft =
                screenWidth - container.offsetLeft - containerWidth

            const minContainerTop = container.offsetTop
            const maxContainerTop =
                screenHeight - container.offsetTop - containerHeight

            // 左偏移距离
            let styL = sty(container, 'left')
            if (styL === 'auto') {
                styL = '0px' // 兼容IE写法
            }

            // 上偏移距离
            let styT = sty(container, 'top')

            // 注意在IE中，第一次获取到的值为组件自带50%，移动之后赋值为px
            if (styL.includes('%')) {
                styL =
                    +document.body.clientWidth * (+styL.replace(/%/g, '') / 100)
                styT =
                    +document.body.clientHeight *
                    (+styT.replace(/%/g, '') / 100)
            } else {
                styL = +styL.replace(/px/g, '')
                styT = +styT.replace(/px/g, '')
            }

            /**
             * 鼠标移动事件
             */
            document.onmousemove = function (e) {
                // 通过事件委托，计算移动的距离
                let left = e.clientX - disX
                let top = e.clientY - disY

                // 边界处理
                if (-left > minContainerLeft) {
                    left = -minContainerLeft
                } else if (left > maxContainerLeft) {
                    left = maxContainerLeft
                }

                if (-top > minContainerTop) {
                    top = -minContainerTop
                } else if (top > maxContainerTop) {
                    top = maxContainerTop
                }

                // 移动当前元素
                container.style.cssText += `;left:${left + styL}px;top:${
                    top + styT
                }px;`
            }

            /**
             * 鼠标松开事件
             */
            document.onmouseup = function (e: any) {
                document.onmousemove = null
                document.onmouseup = null
            }

            return false
        }
    },
}

/**
 * 导出指令集
 */
export default {
    directive,
}

