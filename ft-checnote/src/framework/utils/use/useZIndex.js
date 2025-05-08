function useZIndex(base = __Z_BASE__) {
    const currentZIndex = ref(base)

    const next = () => {
        currentZIndex.value += __Z_STEP__
        return currentZIndex.value
    }

    return {currentZIndex, next}
}

export {
    useZIndex,
}