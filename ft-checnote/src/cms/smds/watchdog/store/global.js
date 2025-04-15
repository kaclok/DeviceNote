import {defineStore} from 'pinia';

export const branchInfo = defineStore('global', {
    state: () => ({
        allBranchInfo1: [
            {
                id: 1,
                name: '电石一分厂',
                flag: true
            },
            {
                id: 2,
                name: '电石二分厂',
                flag: false
            },
            {
                id: 3,
                name: '电石三分厂',
                flag: true
            },
        ],
        allBranchInfo2: [
            {
                id: 4,
                name: '兰炭分厂',
                flag: true
            },
            {
                id: 5,
                name: '热电分厂',
                flag: true
            }, {
                id: 6,
                name: '白灰分厂',
                flag: false
            }],
        currentBranchId: 1,
        socketInfo: []
    }),
    actions: {
        setCurrentBranchId(currentBranchId) {
            this.currentBranchId = currentBranchId;
        },
        branchInfoWithId(branchId) {

        },
        insertSocketInfo(info) {
            this.socketInfo.push({message: info, visible: true})
        }
    },
});