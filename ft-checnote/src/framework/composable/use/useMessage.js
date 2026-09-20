import {ElMessage, ElMessageBox, ElNotification} from 'element-plus'
import {useI18n} from './useI18n'

export const useMessage = () => {
    const {t} = useI18n()
    return {
        // 消息提示
        info(content) {
            ElMessage.info(content)
        },
        // 错误消息
        error(content) {
            ElMessage.error(content)
        },
        // 成功消息
        success(content) {
            ElMessage.success(content)
        },
        // 警告消息
        warning(content) {
            ElMessage.warning(content)
        },
        // 弹出提示
        alert(content) {
            ElMessageBox.alert(content, t('common.confirmTitle'))
        },
        // 错误提示
        alertError(content) {
            ElMessageBox.alert(content, t('common.confirmTitle'), {type: 'error'})
        },
        // 成功提示
        alertSuccess(content) {
            ElMessageBox.alert(content, t('common.confirmTitle'), {type: 'success'})
        },
        // 警告提示
        alertWarning(content) {
            ElMessageBox.alert(content, t('common.confirmTitle'), {type: 'warning'})
        },
        // 通知提示
        notify(content) {
            ElNotification.info(content)
        },
        // 错误通知
        notifyError(content) {
            ElNotification.error(content)
        },
        // 成功通知
        notifySuccess(content) {
            ElNotification.success(content)
        },
        // 警告通知
        notifyWarning(content) {
            ElNotification.warning(content)
        },
        // 确认窗体
        confirm(content, tip) {
            return ElMessageBox.confirm(content, tip ? tip : t('common.confirmTitle'), {
                confirmButtonText: t('common.ok'),
                cancelButtonText: t('common.cancel'),
                type: 'warning'
            })
        },
        // 删除窗体
        delConfirm(content, tip) {
            return ElMessageBox.confirm(
                content ? content : t('common.delMessage'),
                tip ? tip : t('common.confirmTitle'),
                {
                    confirmButtonText: t('common.ok'),
                    cancelButtonText: t('common.cancel'),
                    type: 'warning'
                }
            )
        },
        // 导出窗体
        exportConfirm(content, tip) {
            return ElMessageBox.confirm(
                content ? content : t('common.exportMessage'),
                tip ? tip : t('common.confirmTitle'),
                {
                    confirmButtonText: t('common.ok'),
                    cancelButtonText: t('common.cancel'),
                    type: 'warning'
                }
            )
        },
        // 提交内容
        prompt(content, tip) {
            return ElMessageBox.prompt(content, tip, {
                confirmButtonText: t('common.ok'),
                cancelButtonText: t('common.cancel'),
                type: 'warning'
            })
        }
    }
}
