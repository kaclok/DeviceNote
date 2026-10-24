/**
 * 浏览器语音朗读：司机页用手机自带 TTS 播报物资介绍，无需上传音频。
 * 注意：必须在用户点击等手势内调用，否则部分手机浏览器会拦截。
 */

export function speechSupported() {
    return typeof window !== 'undefined' && 'speechSynthesis' in window
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
    window.speechSynthesis.cancel()
    const utterance = new SpeechSynthesisUtterance(text.trim())
    utterance.lang = 'zh-CN'
    utterance.rate = 0.95
    utterance.pitch = 1
    const voice = pickChineseVoice()
    if (voice) utterance.voice = voice
    const finish = () => onEnd?.()
    utterance.onend = finish
    utterance.onerror = finish
    window.speechSynthesis.speak(utterance)
    return true
}

export function stopSpeak() {
    if (!speechSupported()) return
    window.speechSynthesis.cancel()
}
