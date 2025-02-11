export class DomEventTuple {
    constructor(event, callback) {
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
            window.removeEventListener(one.event, one.callback)
        }
    }
}