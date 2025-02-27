export class DomEventTuple {
    constructor(target = window, event, callback) {
        this.target = target;
        this.event = event;
        this.callback = callback;
    }
}

export class WindowEventCancer {
    constructor(domEventTuples) {
        this.cancers = domEventTuples;
    }

    Cancel() {
        for (const one of this.cancers) {
            one.target.removeEventListener(one.event, one.callback)
        }
    }
}