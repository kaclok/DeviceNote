export class EventEmitterTuple {
    constructor(emitter, eventId, callback) {
        this.emitter = emitter;
        this.eventId = eventId;
        this.callback = callback;
    }
}

export class EventEmitterCancer {
    constructor(emitterTuples) {
        this.cancers = emitterTuples;
    }

    Cancel() {
        for (const one of this.cancers) {
            one.emitter.remove(one.eventId, one.callback);
        }
    }
}