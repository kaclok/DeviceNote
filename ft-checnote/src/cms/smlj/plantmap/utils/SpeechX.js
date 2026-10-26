/**
 * 浏览器语音朗读：司机页用手机自带 TTS 播报物资介绍，无需上传音频。
 * 注意：必须在用户点击等手势内调用，否则部分手机浏览器会拦截。
 */

// 当前朗读的 utterance 必须持有引用：否则可能被 GC 回收，导致安卓 Chrome 播到一半静音甚至完全没声音
let currentUtterance = null

export function speechSupported() {
    return typeof window !== 'undefined' && 'speechSynthesis' in window
}

/** 提前加载语音列表：部分安卓机首次 getVoices() 为空，直接朗读会没有声音 */
export function primeSpeech() {
    if (!speechSupported()) return
    const synth = window.speechSynthesis
    synth.getVoices()
    synth.addEventListener?.('voiceschanged', () => synth.getVoices())
}

function pickChineseVoice() {
    const voices = window.speechSynthesis.getVoices() || []
    return voices.find((voice) => /^zh[-_]CN/i.test(voice.lang))
        || voices.find((voice) => /^zh/i.test(voice.lang))
        || null
}

/** 朗读文本；返回是否成功发起 */
export function speak(text, {onEnd} = {}) {
    if (!speechSupported() || !text || !text.trim()) return false
    const synth = window.speechSynthesis
    // 只有在确实播报时才 cancel：空闲状态下先 cancel 再 speak，部分安卓机会把这一条丢掉
    if (synth.speaking || synth.pending || synth.paused) synth.cancel()
    // 引擎处于暂停态时 speak 不生效，先恢复
    if (synth.paused) synth.resume()

    const utterance = new SpeechSynthesisUtterance(text.trim())
    utterance.lang = 'zh-CN'
    utterance.rate = 0.95
    utterance.pitch = 1
    const voice = pickChineseVoice()
    if (voice) utterance.voice = voice
    currentUtterance = utterance

    let finished = false
    const finish = () => {
        if (finished) return
        finished = true
        if (currentUtterance === utterance) currentUtterance = null
        onEnd?.()
    }
    utterance.onend = finish
    utterance.onerror = finish
    synth.speak(utterance)
    return true
}

/** 是否真的在出声：用于提示手机静音、媒体音量为 0 等无声情况 */
export function isSpeaking() {
    if (!speechSupported()) return false
    return window.speechSynthesis.speaking || window.speechSynthesis.pending
}

export function stopSpeak() {
    if (!speechSupported()) return
    window.speechSynthesis.cancel()
    currentUtterance = null
}
