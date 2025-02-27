class EventBus {
    events = {}

    clear(eventId, cb) {
        this.events[eventId]?.clear();
    }

    add(eventId, cb) {
        (this.events[eventId] ??= {}).add(cb);
    }

    remove(eventId, cb) {
        this.events[eventId]?.delete(cb);
    }

    handle(eventId, cb, addOrRemove) {
        if (addOrRemove === true) {
            return this.add(eventId, cb);
        } else if (addOrRemove === false) {
            return this.remove(eventId, cb);
        }
        return false;
    }

    fire(eventId, ...args) {
        this.events[eventId]?.forEach((cb) => cb(...args));
    }
}

export {
    EventBus
}