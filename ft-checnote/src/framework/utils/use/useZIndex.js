function useZIndex(base = __Z_BASE__) {
    const currentZIndex = ref(base)

    const next = () => {
        currentZIndex.value += 1
        return currentZIndex.value
    }

    return {currentZIndex, next}
}

export {
    useZIndex,
}