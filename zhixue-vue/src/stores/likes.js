// 模块级点赞状态缓存，页面间共享（刷新后重置）
const likes = {}

export function setLike(id, val)   { likes[id] = val }
export function getLike(id)        { return likes[id] }
export function clearLike(id)      { delete likes[id] }
