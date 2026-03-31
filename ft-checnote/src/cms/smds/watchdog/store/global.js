import {defineStore} from 'pinia';

export const branchInfo = defineStore('global', {
    state: () => ({
        allBranchInfo: [
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
            {
                id: 4,
                name: '白灰分厂',
                flag: false
            },
            {
                id: 5,
                name: '兰炭分厂',
                flag: true
            },
            {
                id: 6,
                name: '热电分厂',
                flag: true
            },
        ],
        socketInfo: [],
        msgByBranchId: new Map(),
    }),
    actions: {
        insertSocketInfo(result) {
            result = JSON.parse(result);
            let data = result.data;
            let indicatorInfo = data.indicatorInfo
            let branchId = indicatorInfo.branchId;
            let branchName = this.allBranchInfo[branchId - 1].name

            let message = indicatorInfo.indicatorName + "  " + indicatorInfo.namespace + "  " + indicatorInfo.tag;
            let mm = {
                timestamp: result.timestamp,
                index: result.timestamp + data.index,
                batchIndex: data.batchIndex,
                dataIndex: data.index,
                branchId: branchId,
                branchName: branchName,
                message: message,
                detail: indicatorInfo,
                visible: true
            };
            this.socketInfo.push(mm)

            if (!this.msgByBranchId.has(branchId)) {
                this.msgByBranchId.set(branchId, [])
            }

            this.msgByBranchId.get(branchId).push(mm)
        },

        removeSocketInfo(mm) {
            let branchId = mm.branchId;

            this.socketInfo = this.socketInfo.filter(item => {
                return item.index !== mm.index
            })

            if (this.msgByBranchId.has(branchId)) {
                let filtered = this.msgByBranchId.get(branchId).filter(item => item.index !== mm.index)
                this.msgByBranchId.set(branchId, filtered)
            }
        },
        removeByBranchId(branchId) {
            this.socketInfo = this.socketInfo.filter(item => {
                return item.branchId !== branchId
            })

            if (this.msgByBranchId.has(branchId)) {
                let filtered = this.msgByBranchId.get(branchId).filter(item => item.branchId !== branchId)
                this.msgByBranchId.set(branchId, filtered)
            }
        },
        removeAll() {
            this.socketInfo = []
            this.msgByBranchId = new Map()
        },

        getBranchInfo_1(branchId) {
            if (this.msgByBranchId.has(branchId)) {
                return this.msgByBranchId.get(branchId);
            }
            return []
        },
        getBranchInfo_2(branchId) {
            return this.socketInfo.filter(item => {
                return item.branchId === branchId
            })
        }
    },
});
